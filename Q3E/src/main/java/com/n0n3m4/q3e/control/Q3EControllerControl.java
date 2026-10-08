/*
 Copyright (C) 2012 n0n3m4

 This file contains some code from kwaak3:
 Copyright (C) 2010 Roderick Colenbrander

 This file is part of Q3E.

 Q3E is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 2 of the License, or
 (at your option) any later version.

 Q3E is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Q3E.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.n0n3m4.q3e.control;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.n0n3m4.q3e.Q3E;
import com.n0n3m4.q3e.Q3EControlView;
import com.n0n3m4.q3e.Q3EGamePadPresets;
import com.n0n3m4.q3e.Q3EKeyCodes;
import com.n0n3m4.q3e.Q3EPreference;
import com.n0n3m4.q3e.Q3EUtils;
import com.n0n3m4.q3e.karin.KLog;

public final class Q3EControllerControl
{
    private final Q3EControlView controlView;

    // other controls function
    private float last_joystick_x = 0;
    private float last_joystick_y = 0;
    //MOUSE
    private long oldtime = 0;

    // controller
    private final boolean[] directionPressed = { false, false, false, false }; // up down left right
    private final int[] directionKeys = { 0, 0, 0, 0 }; // the key each pressed direction went down as
    private boolean dpadAsArrowKey = false;
    private float leftJoystickDeadRange = 0.01f;
    private float rightJoystickDeadRange = 0.0f;
    private float rightJoystickSensitivity = 1.0f;
//    private float m_lastLeftTriggerAxis = 0.0f;
//    private float m_lastRightTriggerAxis = 0.0f;
//    private float triggerSensitivity = 1.0f;

    public Q3EControllerControl(Q3EControlView controlView)
    {
        this.controlView = controlView;
    }

    private Context getContext()
    {
        return controlView.getContext();
    }

    public void Init()
    {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(controlView.getContext());

        dpadAsArrowKey = mPrefs.getBoolean(Q3EPreference.pref_harm_dpad_as_arrow_key, false);
        leftJoystickDeadRange = Q3EPreference.GetFloatFromString(mPrefs, Q3EPreference.pref_harm_left_joystick_deadzone, 0.01f);
        rightJoystickDeadRange = Q3EPreference.GetFloatFromString(mPrefs, Q3EPreference.pref_harm_right_joystick_deadzone, 0.0f);
        rightJoystickSensitivity = Q3EPreference.GetFloatFromString(mPrefs, Q3EPreference.pref_harm_right_joystick_sensitivity, 1.0f);

        KLog.I("Controller DPad as arrow keys: " + dpadAsArrowKey);
        KLog.I("Controller left joystick dead zone: " + leftJoystickDeadRange);
        KLog.I("Controller right joystick dead zone: " + rightJoystickDeadRange);
        KLog.I("Controller right joystick sensitivity: " + rightJoystickSensitivity);
    }

    public void Update()
    {
        long t = System.currentTimeMillis();
        float delta = t - oldtime;
        oldtime = t;
        if (delta > 1000)
            delta = 1000;

        delta *= rightJoystickSensitivity;
        if ((last_joystick_x != 0) || (last_joystick_y != 0))
            Q3E.sendMotionEvent(delta * last_joystick_x, delta * last_joystick_y);
    }

    public boolean OnGenericMotionEvent(MotionEvent event)
    {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_MOVE)
        {
            int source = event.getSource();
            InputDevice inputDevice = event.getDevice();
            if ((source & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)
            {
                // left as movement event
                // Process all historical movement samples in the batch
//                final int historySize = event.getHistorySize();

                // Process the movements starting from the
                // earliest historical position in the batch
//                for (int i = 0; i < historySize; i++) {
//                    // Process the event at historical position i
//                    HandleJoyStickMotionEvent(event, inputDevice, i);
//                }

                // Process the current movement sample in the batch (position -1)
                HandleJoyStickMotionEvent(event, inputDevice/*, -1*/);

                // right as view event
                float x = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_Z);
                float y = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_RZ);

                if((Math.abs(x) < rightJoystickDeadRange))
                    x = 0.0f;
                if((Math.abs(y) < rightJoystickDeadRange))
                    y = 0.0f;

                last_joystick_x = x;
                last_joystick_y = y;

/*                x = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_LTRIGGER);
                if(x == 0.0f)
                    x = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_BRAKE);
                y = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_RTRIGGER);
                if(y == 0.0f)
                    y = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_GAS);

                if(x > m_lastLeftTriggerAxis)
                {
                    Q3EUtils.q3ei.callbackObj.sendWheelEvent(0.0f, -(x - m_lastLeftTriggerAxis) * triggerSensitivity);
                }
                if(y > m_lastRightTriggerAxis)
                {
                    Q3EUtils.q3ei.callbackObj.sendWheelEvent(0.0f, (m_lastRightTriggerAxis - y) * triggerSensitivity);
                }

                m_lastLeftTriggerAxis = x;
                m_lastRightTriggerAxis = y;*/

                return true;
            }
            else if ((source & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
            {
                if(DPadAsKeys())
                {
                    HandleDPadMotionEvent(event);
                    return true;
                }
            }
        }

        return false;
    }

    private void HandleDPadMotionEvent(MotionEvent event)
    {
        float xaxis = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        float yaxis = event.getAxisValue(MotionEvent.AXIS_HAT_Y);

        // Check if the AXIS_HAT_X value is -1 or 1, and set the D-pad
        // LEFT and RIGHT direction accordingly.
        boolean leftPressed = Float.compare(xaxis, -1.0f) == 0;
        boolean rightPressed = Float.compare(xaxis, 1.0f) == 0;
        // Check if the AXIS_HAT_Y value is -1 or 1, and set the D-pad
        // UP and DOWN direction accordingly.
        boolean upPressed = Float.compare(yaxis, -1.0f) == 0;
        boolean downPressed = Float.compare(yaxis, 1.0f) == 0;

        SendDPadKey(2, leftPressed, Q3EKeyCodes.KeyCodes.K_LEFTARROW);
        SendDPadKey(3, rightPressed, Q3EKeyCodes.KeyCodes.K_RIGHTARROW);
        SendDPadKey(0, upPressed, Q3EKeyCodes.KeyCodes.K_UPARROW);
        SendDPadKey(1, downPressed, Q3EKeyCodes.KeyCodes.K_DOWNARROW);
    }

    // the d-pad as keys: the arrows in menus (or with DPad as arrow keys), in game the keys of the game's
    // gamepad layout (Q3EGamePadPresets: weapons...) when it has some, else it moves with the left stick
    private boolean DPadAsKeys()
    {
        return dpadAsArrowKey || !Q3E.callbackObj.notinmenu || Q3EGamePadPresets.HasDPad(Q3E.q3ei.game);
    }

    private void SendDPadKey(int direction, boolean pressed, int arrowKey)
    {
        if(pressed == directionPressed[direction])
            return;
        directionPressed[direction] = pressed;
        if(pressed)
        {
            int key = arrowKey;
            if(!dpadAsArrowKey && Q3E.callbackObj.notinmenu && Q3EGamePadPresets.HasDPad(Q3E.q3ei.game))
                key = Q3EGamePadPresets.DPadKey(Q3E.q3ei.game, direction);
            directionKeys[direction] = key;
            if(key != 0)
                Q3E.sendKeyEvent(true, key, 0);
        }
        else
        {
            if(directionKeys[direction] != 0) // released as the key it went down as
                Q3E.sendKeyEvent(false, directionKeys[direction], 0);
            directionKeys[direction] = 0;
        }
    }

    private static float getCenteredAxis(MotionEvent event, InputDevice device, int axis)
    {
        final InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());

        // A joystick at rest does not always report an absolute position of
        // (0,0). Use the getFlat() method to determine the range of values
        // bounding the joystick axis center.
        if (range != null) {
            final float flat = range.getFlat();
            final float value = event.getAxisValue(axis);

            // Ignore axis values that are within the 'flat' region of the
            // joystick axis center.
            if (Math.abs(value) > flat) {
                return value;
            }
        }
        return 0;
    }

    private static float getCenteredAxis(MotionEvent event, InputDevice device, int axis, int historyPos) {
        final InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());

        // A joystick at rest does not always report an absolute position of
        // (0,0). Use the getFlat() method to determine the range of values
        // bounding the joystick axis center.
        if (range != null) {
            final float flat = range.getFlat();
            final float value = historyPos < 0 ? event.getAxisValue(axis): event.getHistoricalAxisValue(axis, historyPos);

            // Ignore axis values that are within the 'flat' region of the
            // joystick axis center.
            if (Math.abs(value) > flat) {
                return value;
            }
        }
        return 0;
    }

    private void HandleJoyStickMotionEvent(MotionEvent event, InputDevice inputDevice/*, int historyPos*/) {
        float x = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_X/*, historyPos*/);
        float y = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_Y/*, historyPos*/);

        if(DPadAsKeys())
        {
            HandleDPadMotionEvent(event);
            if(Q3E.callbackObj.notinmenu && !dpadAsArrowKey) // the d-pad sends the game's keys, the stick still moves
                Q3E.sendAnalog((Math.abs(x) > leftJoystickDeadRange) || (Math.abs(y) > leftJoystickDeadRange), x, -y);
        }
        else
        {
            if(x == 0.0f)
                x = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_HAT_X/*, historyPos*/);
            if(y == 0.0f)
                y = getCenteredAxis(event, inputDevice, MotionEvent.AXIS_HAT_Y/*, historyPos*/);

            Q3E.sendAnalog((Math.abs(x) > leftJoystickDeadRange) || (Math.abs(y) > leftJoystickDeadRange), x, -y);
        }
    }

    public static boolean IsGamePadDevice(int deviceId) {
        InputDevice device = InputDevice.getDevice(deviceId);
        if ((device == null) || (deviceId < 0)) {
            return false;
        }
        int sources = device.getSources();

        return ((sources & InputDevice.SOURCE_CLASS_JOYSTICK) != 0 ||
                ((sources & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD) ||
                ((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
        );
    }

    public static boolean IsGamePadKeyEvent(KeyEvent event) {
        return (event.getSource() & (InputDevice.SOURCE_JOYSTICK | InputDevice.SOURCE_GAMEPAD)) != 0;
    }
}
