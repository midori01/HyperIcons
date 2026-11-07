package com.midori.hypericons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for the icon config keys shared by the app, the
 * provider and the SystemUI hook. Each per-SIM option has a global key
 * (pref_xxx) plus slot overrides (pref_sim1_xxx, pref_sim2_xxx) that fall
 * back to the global value when unset. Adding an option only needs a new
 * entry in {@link #SIM_OPTION_KEYS}.
 */
public final class ConfigKeys {
    private ConfigKeys() {}

    public static final String KEY_SIM_MODE = "pref_sim_mode";           // "both" | "separate"
    public static final String KEY_TARGET_TAB = "pref_target_tab";       // "sim1" | "sim2"

    public static final String KEY_5G_CA = "pref_5g_ca";
    public static final String KEY_5G_BASIC = "pref_5g_basic";
    public static final String KEY_CAPSULE_STYLE = "pref_capsule_style";
    public static final String KEY_4G_RAT = "pref_4g_rat";
    public static final String KEY_ALWAYS_SHOW_RAT = "pref_always_show_rat";
    public static final String KEY_VOLTE_STYLE = "pref_volte_style";
    public static final String KEY_VOWIFI_STYLE = "pref_vowifi_style";
    public static final String KEY_ROAMING_STYLE = "pref_roaming_style";
    public static final String KEY_VONR_VISIBILITY = "pref_vonr_visibility";
    public static final String KEY_WIFI_VOLTE_VISIBILITY = "pref_wifi_volte_visibility";

    public static final String DEFAULT_VALUE = "default";
    public static final String DEFAULT_SIM_MODE = "both";
    public static final String DEFAULT_TARGET_TAB = "both";

    private static final String[] SIM_OPTION_KEYS = {
        KEY_5G_CA,
        KEY_5G_BASIC,
        KEY_CAPSULE_STYLE,
        KEY_4G_RAT,
        KEY_ALWAYS_SHOW_RAT,
        KEY_VOLTE_STYLE,
        KEY_VOWIFI_STYLE,
        KEY_ROAMING_STYLE,
        KEY_VONR_VISIBILITY,
        KEY_WIFI_VOLTE_VISIBILITY,
    };

    /** Reads one stored value, returning {@code def} when it is missing. */
    public interface Source {
        String get(String key, String def);
    }

    public static boolean isSimOption(String key) {
        for (String k : SIM_OPTION_KEYS) {
            if (k.equals(key)) return true;
        }
        return false;
    }

    /** pref_xxx -> pref_sim1_xxx (slot 0) or pref_sim2_xxx (slot 1); other keys are returned unchanged. */
    public static String slotKey(String baseKey, int slot) {
        if ((slot != 0 && slot != 1) || !isSimOption(baseKey)) return baseKey;
        return "pref_sim" + (slot + 1) + "_" + baseKey.substring("pref_".length());
    }

    /** Every string key the config stores: mode, tab, and each option for global, SIM 1 and SIM 2. */
    public static List<String> allStringKeys() {
        List<String> keys = new ArrayList<String>();
        keys.add(KEY_SIM_MODE);
        keys.add(KEY_TARGET_TAB);
        for (String key : SIM_OPTION_KEYS) {
            keys.add(key);
            keys.add(slotKey(key, 0));
            keys.add(slotKey(key, 1));
        }
        return keys;
    }

    /** Resolves the full config, filling unset slot values from the global value. */
    public static Map<String, String> resolve(Source src) {
        Map<String, String> config = new HashMap<String, String>();
        config.put(KEY_SIM_MODE, src.get(KEY_SIM_MODE, DEFAULT_SIM_MODE));
        config.put(KEY_TARGET_TAB, src.get(KEY_TARGET_TAB, DEFAULT_TARGET_TAB));
        for (String key : SIM_OPTION_KEYS) {
            String global = src.get(key, DEFAULT_VALUE);
            config.put(key, global);
            config.put(slotKey(key, 0), src.get(slotKey(key, 0), global));
            config.put(slotKey(key, 1), src.get(slotKey(key, 1), global));
        }
        return Collections.unmodifiableMap(config);
    }

    /** All options at their defaults. */
    public static Map<String, String> defaults() {
        return resolve(new Source() {
            @Override
            public String get(String key, String def) {
                return def;
            }
        });
    }
}
