/*
 * Gamepad aim assist shared by the engines (header only, C and C++).
 *
 * The launcher sets the level in the environment variable Q3E_AIM_ASSIST (0 off, 1 light, 2 medium,
 * 3 strong), from the controller settings. The right stick reaches the engines as mouse motion, so the
 * assist works on the look delta of a frame:
 *   - friction: the look slows down while the crosshair is on or near an enemy
 *   - magnet: while the player is aiming, the view is pulled a little toward that enemy
 * A fast turn, or a move away from the target, disengages it, so it never feels glued.
 *
 * Each engine gives its enemies (aim point, radius) and a line of sight test:
 *
 *   float aim[2] = { pitch, yaw };     // view angles before this frame's look input (degrees)
 *   float delta[2] = { dpitch, dyaw }; // this frame's look input (degrees)
 *   Q3E_AimAssist_Apply(eye, aim, delta, dt, targets, numTargets, visible, user);
 *   // then apply delta instead of the original input
 *
 * Angles follow the Quake convention: yaw counterclockwise from +X, pitch positive looking down.
 *
 * Every 10 seconds of play with the assist on, a line goes to stdout (stdout.txt in the game folder):
 * the frames with look input and enemies given, those with one in the assist zone, with one visible
 * there, and the look speed, so that a game where it does nothing can be told why.
 */
#ifndef _Q3E_AIMASSIST_H
#define _Q3E_AIMASSIST_H

#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#define Q3E_AIMASSIST_MAX_TARGETS 64

typedef struct
{
    float origin[3]; // point to aim at (chest)
    float radius; // body radius around it, in world units
} q3e_aimTarget_t;

// line of sight from eye to point
typedef int (*q3e_aimVisible_f)(const float eye[3], const float point[3], void *user);

// idTech 4: Q3E_AimAssistTargets, optional in the game library: the local player's eye, view delta
// angles (pitch, yaw) and visible enemies (chest point x, y, z and body radius), returns their number
typedef int (*q3eAimAssistTargets_t)(float eye[3], float viewDelta[2], float (*targets)[4], int maxTargets);

typedef struct
{
    float slowdown; // look slowdown on target, 0..1
    float magnet; // share of the angle to the target pulled per frame, at most turnRate
    float turnRate; // max pull, degrees per second
} q3e_aimAssistParms_t;

static const q3e_aimAssistParms_t q3e_aimAssistLevels[4] = {
    { 0.0f, 0.0f, 0.0f },
    { 0.35f, 0.30f, 120.0f }, // light
    { 0.55f, 0.50f, 200.0f }, // medium
    { 0.70f, 0.70f, 300.0f }, // strong
};

#define Q3E_AIMASSIST_BREAKOUT_SPEED 540.0f // degrees per second of look input: above it the player turns, no assist
#define Q3E_AIMASSIST_MAX_DISTANCE 4096.0f
#define Q3E_AIMASSIST_CONE_SCALE 6.0f // the assist zone is this many body radii around the aim point
#define Q3E_AIMASSIST_MIN_CONE 8.0f // degrees: far enemies are a few degrees wide, the zone must not be (Jedi Outcast: 2.5 did nothing)
#define Q3E_AIMASSIST_MAX_CONE 15.0f // degrees
#define Q3E_AIMASSIST_FRICTION_ZONE 0.4f // the slowdown is in this inner share of the zone, the pull in all of it
#define Q3E_AIMASSIST_MIN_STRENGTH 0.25f // at the edge of the zone
#define Q3E_AIMASSIST_MIN_SCALE 0.4f // slowest look on target, share of the input
#define Q3E_AIMASSIST_REPORT_SECONDS 10

static int Q3E_AimAssist_Level(void)
{
    static int level = -1;
    if(level < 0)
    {
        const char *env = getenv("Q3E_AIM_ASSIST");
        level = env ? atoi(env) : 0;
        if(level < 0 || level > 3)
            level = 0;
        printf("Q3E aim assist: level %d\n", level);
    }
    return level;
}

static float Q3E_AimAssist_Normalize180(float angle)
{
    angle = fmodf(angle, 360.0f);
    if(angle > 180.0f)
        angle -= 360.0f;
    else if(angle < -180.0f)
        angle += 360.0f;
    return angle;
}

// what the assist did lately (one per engine library), see the top of this file
static struct
{
    time_t since;
    int frames; // with look input and enemies given
    int enemies; // given in those frames
    int inZone; // frames with an enemy in the assist zone
    int visible; // ... and visible
    float speed; // sum of the look speeds
} q3e_aimAssistStats;

static void Q3E_AimAssist_Report(void)
{
    time_t now = time(NULL);

    if(!q3e_aimAssistStats.since)
        q3e_aimAssistStats.since = now;
    if(now - q3e_aimAssistStats.since < Q3E_AIMASSIST_REPORT_SECONDS)
        return;
    if(q3e_aimAssistStats.frames)
    {
        printf("Q3E aim assist: %d frames aiming, %.1f enemies given on average, %d with one in the zone, %d with it visible (assisted), look speed %.0f deg/s on average\n",
               q3e_aimAssistStats.frames, (float)q3e_aimAssistStats.enemies / q3e_aimAssistStats.frames,
               q3e_aimAssistStats.inZone, q3e_aimAssistStats.visible, q3e_aimAssistStats.speed / q3e_aimAssistStats.frames);
    }
    memset(&q3e_aimAssistStats, 0, sizeof(q3e_aimAssistStats));
    q3e_aimAssistStats.since = now;
}

/*
 * aim: pitch, yaw before this frame's input; delta: this frame's input (pitch, yaw), changed in place.
 * Returns 1 when a target was found.
 */
static int Q3E_AimAssist_Apply(const float eye[3], const float aim[2], float delta[2], float dt,
                               const q3e_aimTarget_t *targets, int numTargets, q3e_aimVisible_f visible, void *user)
{
    const q3e_aimAssistParms_t *parms;
    float inLen, speed, relief, escape;
    float bestErr[2] = { 0.0f, 0.0f };
    float bestStrength = 0.0f;
    float bestAngle = 1e9f;
    float bestCone = 1.0f;
    int inZone = 0;
    int i;

    int level = Q3E_AimAssist_Level();
    if(level <= 0 || numTargets <= 0 || dt <= 0.0f)
        return 0;
    parms = &q3e_aimAssistLevels[level];

    inLen = sqrtf(delta[0] * delta[0] + delta[1] * delta[1]);
    if(inLen <= 0.0f)
        return 0; // only while the player aims
    speed = inLen / dt;

    Q3E_AimAssist_Report();
    q3e_aimAssistStats.frames++;
    q3e_aimAssistStats.enemies += numTargets;
    q3e_aimAssistStats.speed += speed;

    if(speed >= Q3E_AIMASSIST_BREAKOUT_SPEED)
        return 0;

    for(i = 0; i < numTargets; i++)
    {
        const q3e_aimTarget_t *t = &targets[i];
        float dir[3], flat, dist, errYaw, errPitch, angle, cone;

        dir[0] = t->origin[0] - eye[0];
        dir[1] = t->origin[1] - eye[1];
        dir[2] = t->origin[2] - eye[2];
        flat = sqrtf(dir[0] * dir[0] + dir[1] * dir[1]);
        dist = sqrtf(flat * flat + dir[2] * dir[2]);
        if(dist < 1.0f || dist > Q3E_AIMASSIST_MAX_DISTANCE)
            continue;

        errYaw = Q3E_AimAssist_Normalize180(atan2f(dir[1], dir[0]) * (180.0f / (float)M_PI) - aim[1]);
        errPitch = Q3E_AimAssist_Normalize180(-atan2f(dir[2], flat) * (180.0f / (float)M_PI) - aim[0]);
        angle = sqrtf(errYaw * errYaw + errPitch * errPitch);

        cone = atan2f(t->radius * Q3E_AIMASSIST_CONE_SCALE, dist) * (180.0f / (float)M_PI);
        if(cone < Q3E_AIMASSIST_MIN_CONE)
            cone = Q3E_AIMASSIST_MIN_CONE;
        else if(cone > Q3E_AIMASSIST_MAX_CONE)
            cone = Q3E_AIMASSIST_MAX_CONE;

        if(angle >= cone || angle >= bestAngle)
            continue;
        inZone = 1;
        if(visible && !visible(eye, t->origin, user))
            continue;

        bestAngle = angle;
        bestCone = cone;
        bestStrength = Q3E_AIMASSIST_MIN_STRENGTH + (1.0f - Q3E_AIMASSIST_MIN_STRENGTH) * (1.0f - angle / cone);
        bestErr[0] = errPitch;
        bestErr[1] = errYaw;
    }

    q3e_aimAssistStats.inZone += inZone;
    if(bestStrength <= 0.0f)
        return 0;
    q3e_aimAssistStats.visible++;

    // weaker only as the input nears a fast turn
    relief = speed / Q3E_AIMASSIST_BREAKOUT_SPEED;
    relief = 1.0f - relief * relief;

    // moving away from the target fades the assist out
    escape = 1.0f;
    if(bestAngle > 0.001f)
    {
        float align = (delta[0] * bestErr[0] + delta[1] * bestErr[1]) / (inLen * bestAngle); // -1 away .. +1 toward
        if(align < 0.0f)
        {
            escape = 1.0f + align / 0.35f;
            if(escape < 0.0f)
                escape = 0.0f;
        }
    }
    if(escape <= 0.0f)
        return 1;

    // friction, near the target only: slowing down the approach from the edge of the zone would feel like drag
    {
        float closeness = 1.0f - bestAngle / (bestCone * Q3E_AIMASSIST_FRICTION_ZONE);
        float scale;
        if(closeness < 0.0f)
            closeness = 0.0f;
        scale = 1.0f - parms->slowdown * closeness * escape * relief;
        if(scale < Q3E_AIMASSIST_MIN_SCALE)
            scale = Q3E_AIMASSIST_MIN_SCALE;
        delta[0] *= scale;
        delta[1] *= scale;
    }

    // magnet
    if(bestAngle > 0.001f)
    {
        float step = bestAngle * parms->magnet;
        float maxStep = parms->turnRate * dt;
        if(step > maxStep)
            step = maxStep;
        step *= bestStrength * escape * relief;
        delta[0] += bestErr[0] / bestAngle * step;
        delta[1] += bestErr[1] / bestAngle * step;
    }

    return 1;
}

#endif // _Q3E_AIMASSIST_H
