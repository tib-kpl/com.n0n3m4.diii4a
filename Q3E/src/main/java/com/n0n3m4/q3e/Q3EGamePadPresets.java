package com.n0n3m4.q3e;

import com.n0n3m4.q3e.keycode.KeyCodesGeneric;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
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

    static
    {
        // filled per game from its default bindings
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
