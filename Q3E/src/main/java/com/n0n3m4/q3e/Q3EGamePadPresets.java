package com.n0n3m4.q3e;

import com.n0n3m4.q3e.keycode.KeyCodesGeneric;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Recommended gamepad layout of each game: the pad's buttons on the keys the game binds by default
 * (its own default config), console style. A game uses it until its buttons are set up in
 * Configure controller, and gets it back with Reset there.
 */
public final class Q3EGamePadPresets
{
    // game -> { button, KeyCodesGeneric field name, ... }
    private static final Map<String, String[]> PRESETS = new HashMap<>();

    private static void Add(String game, String... buttonKeys)
    {
        PRESETS.put(game, buttonKeys);
    }

    // from each game's default.cfg: R2 fire, L2 alt fire / zoom, A jump, B crouch, Start menu, Select scores / objectives
    static
    {
        // Jedi Outcast (assets0.pk3 default.cfg)
        Add(Q3EGameConstants.GAME_JO,
                "button_r2", "K_MOUSE1",      // +attack
                "button_l2", "K_MOUSE2",      // +altattack
                "button_a", "K_SPACE",        // +moveup
                "button_b", "K_C",            // +movedown
                "button_x", "K_E",            // +use
                "button_y", "K_F",            // +useforce
                "button_l1", "K_Z",           // forceprev
                "button_r1", "K_X",           // forcenext
                "button_l3", "K_L",           // saberAttackCycle
                "button_r3", "K_R",           // weapnext
                "button_start", "K_ESCAPE",
                "button_select", "K_TAB"      // datapad
        );
        // Jedi Academy (assets2.pk3 default.cfg)
        Add(Q3EGameConstants.GAME_JA,
                "button_r2", "K_MOUSE1",      // +attack
                "button_l2", "K_MOUSE2",      // +altattack
                "button_a", "K_SPACE",        // +moveup
                "button_b", "K_C",            // +movedown
                "button_x", "K_R",            // +use
                "button_y", "K_F",            // +useforce
                "button_l1", "K_Q",           // forceprev
                "button_r1", "K_E",           // forcenext
                "button_l3", "K_L",           // saberAttackCycle
                "button_r3", "K_RBRACKET",    // weapnext
                "button_start", "K_ESCAPE",
                "button_select", "K_TAB"      // datapad
        );
        // Return to Castle Wolfenstein (pak0.pk3 default.cfg)
        Add(Q3EGameConstants.GAME_RTCW,
                "button_r2", "K_MOUSE1",      // +attack
                "button_l2", "K_Z",           // weapalt (scope, silencer...)
                "button_a", "K_SPACE",        // +moveup
                "button_b", "K_C",            // +movedown
                "button_x", "K_R",            // +reload
                "button_y", "K_F",            // +activate
                "button_l1", "K_RBRACKET",    // weapprev
                "button_r1", "K_LBRACKET",    // weapnext
                "button_l3", "K_SHIFT",       // +sprint
                "button_r3", "K_G",           // +kick
                "button_start", "K_ESCAPE",
                "button_select", "K_N"        // notebook
        );
        // Quake 4 (pak021.pk4 default.cfg; crouch is x in the French, Italian and Spanish ones)
        String q4Lang = Locale.getDefault().getLanguage();
        boolean q4Localized = "fr".equals(q4Lang) || "it".equals(q4Lang) || "es".equals(q4Lang);
        Add(Q3EGameConstants.GAME_QUAKE4,
                "button_r2", "K_MOUSE1",      // _attack
                "button_l2", "K_MOUSE2",      // _zoom
                "button_a", "K_SPACE",        // _moveup
                "button_b", q4Localized ? "K_X" : "K_C", // _movedown
                "button_x", "K_R",            // reload
                "button_y", "K_F",            // flashlight
                "button_l1", "K_MWHEELDOWN",  // previous weapon
                "button_r1", "K_MWHEELUP",    // next weapon
                "button_l3", "K_SHIFT",       // _speed
                "button_r3", "K_END",         // center view
                "button_start", "K_ESCAPE",
                "button_select", "K_TAB"      // objectives
        );
        // DOOM 3 (pak000.pk4 default.cfg)
        Add(Q3EGameConstants.GAME_DOOM3,
                "button_r2", "K_MOUSE1",      // _attack
                "button_l2", "K_F",           // flashlight
                "button_a", "K_SPACE",        // _moveup
                "button_b", "K_C",            // _movedown
                "button_x", "K_R",            // reload
                "button_y", "K_Z",            // _zoom
                "button_l1", "K_MWHEELUP",    // previous weapon
                "button_r1", "K_MWHEELDOWN",  // next weapon
                "button_l3", "K_SHIFT",       // _speed
                "button_r3", "K_END",         // center view
                "button_start", "K_ESCAPE",
                "button_select", "K_TAB"      // PDA
        );
        // Prey (pak000.pk4 default.cfg)
        Add(Q3EGameConstants.GAME_PREY,
                "button_r2", "K_MOUSE1",      // _attack
                "button_l2", "K_MOUSE2",      // _attackalt
                "button_a", "K_SPACE",        // _moveup
                "button_b", "K_C",            // _movedown
                "button_x", "K_G",            // crawler grenade
                "button_y", "K_E",            // spirit walk
                "button_l1", "K_MWHEELUP",    // previous weapon
                "button_r1", "K_MWHEELDOWN",  // next weapon
                "button_l3", "K_F",           // lighter
                "button_r3", "K_END",         // center view
                "button_start", "K_ESCAPE",
                "button_select", "K_TAB"      // scores
        );
        // Half-Life (valve config: Half-Life's own keys)
        Add(Q3EGameConstants.GAME_XASH3D,
                "button_r2", "K_MOUSE1",      // +attack
                "button_l2", "K_MOUSE2",      // +attack2
                "button_a", "K_SPACE",        // +jump
                "button_b", "K_CTRL",         // +duck
                "button_x", "K_E",            // +use
                "button_y", "K_R",            // +reload
                "button_l1", "K_MWHEELUP",    // invprev
                "button_r1", "K_MWHEELDOWN",  // invnext
                "button_l3", "K_F",           // flashlight
                "button_r3", "K_Q",           // lastinv
                "button_start", "K_ESCAPE",
                "button_select", "K_TAB"      // +showscores
        );
    }

    private Q3EGamePadPresets() {}

    public static boolean Has(String game)
    {
        return null != game && PRESETS.containsKey(game);
    }

    // the game's layout as a button map ("button:generic code"), or null when it has none
    public static Set<String> Get(String game)
    {
        if(!Has(game))
            return null;
        String[] buttonKeys = PRESETS.get(game);
        Set<String> codeSet = new HashSet<>();
        for(int i = 0; i + 1 < buttonKeys.length; i += 2)
        {
            Integer code = GenericCode(buttonKeys[i + 1]);
            if(null != code)
                codeSet.add(buttonKeys[i] + ":" + code);
        }
        return codeSet;
    }

    // the layout's generic key code of a button, or null
    public static Integer Get(String game, String button)
    {
        if(!Has(game))
            return null;
        String[] buttonKeys = PRESETS.get(game);
        for(int i = 0; i + 1 < buttonKeys.length; i += 2)
        {
            if(buttonKeys[i].equalsIgnoreCase(button))
                return GenericCode(buttonKeys[i + 1]);
        }
        return null;
    }

    private static Integer GenericCode(String name)
    {
        try
        {
            Field field = KeyCodesGeneric.class.getDeclaredField(name);
            return (Integer) field.get(null);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
