package com.midori.hypericons;

import java.util.List;

/**
 * Pure presentation rules shared by the SystemUI hooks. Nothing here touches
 * Android or Xposed, so tests/java/PolicyProbe.java runs this exact code on a
 * plain JVM and tests/test_matrix.py checks it against the oracle.
 */
public final class IconPolicy {
    private IconPolicy() {}

    public static final int RADIO_TYPE_UNKNOWN = 0;
    public static final int RADIO_TYPE_2G_3G = 1;
    public static final int RADIO_TYPE_4G = 2;
    public static final int RADIO_TYPE_4G_CA = 3;
    public static final int RADIO_TYPE_5G = 4;
    public static final int RADIO_TYPE_5G_CA = 5;

    /** RAT icon always shown. */
    public static final int RAT_SHOW = 0;
    /** RAT icon only on the default data SIM. */
    public static final int RAT_DATA_SIM_ONLY = 1;
    /** RAT icon hidden on Wi-Fi, otherwise only on the default data SIM. */
    public static final int RAT_HIDE_ON_WIFI = 2;

    private static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    // ---- RAT visibility ----

    /** Only "default" depends on the ROM region; callers can skip the lookup otherwise. */
    public static boolean ratRuleUsesRomRegion(String mode) {
        return !"always".equals(mode) && !"data_sim".equals(mode) && !"wifi_hide".equals(mode);
    }

    public static int ratRule(String mode, boolean isInternationalRom) {
        if ("always".equals(mode)) return RAT_SHOW;
        if ("data_sim".equals(mode)) return RAT_DATA_SIM_ONLY;
        if ("wifi_hide".equals(mode)) return RAT_HIDE_ON_WIFI;
        // "default": Global ROMs hide on Wi-Fi and show only the data SIM; China ROMs show both.
        return isInternationalRom ? RAT_HIDE_ON_WIFI : RAT_SHOW;
    }

    public static boolean shouldShowRat(int rule, boolean isWifi, boolean isDataSim) {
        switch (rule) {
            case RAT_SHOW:
                return true;
            case RAT_DATA_SIM_ONLY:
                return isDataSim;
            default:
                return !isWifi && isDataSim;
        }
    }

    // ---- Mobile type text ----

    public static String target5gCaText(String ca) {
        if ("5ga".equals(ca)) return "5GA";
        if ("5g_plus".equals(ca)) return "5G+";
        if ("5g_plus_plus".equals(ca)) return "5G++";
        if ("5guwb".equals(ca)) return "5GUWB";
        if ("5ge".equals(ca)) return "5Ge";
        if ("5g_6rx".equals(ca)) return "5G 6Rx";
        if ("5g_plus_plus_6rx".equals(ca)) return "5G++ 6Rx";
        if ("5guwb_6rx".equals(ca)) return "5GUWB 6Rx";
        return "";
    }

    public static String effective5gCaText(String ca) {
        String text = target5gCaText(ca);
        return isEmpty(text) ? "5G+" : text;
    }

    public static String target5gBasicText(String basic, String ca) {
        return "follow_ca".equals(basic) ? effective5gCaText(ca) : "5G";
    }

    public static boolean is5gCaText(String text) {
        if (isEmpty(text)) return false;
        return "5GA".equals(text) || "5G+".equals(text) || "5G++".equals(text) || "5GUWB".equals(text) || "5Ge".equals(text)
                || "5G 6Rx".equals(text) || "5G++ 6Rx".equals(text) || "5GUWB 6Rx".equals(text);
    }

    public static boolean is4gCaText(String text) {
        if (isEmpty(text)) return false;
        return text.contains("+") || text.contains("-A") || "4.5G+".equals(text);
    }

    public static boolean is5gText(String text) {
        if (isEmpty(text)) return false;
        return text.startsWith("5G");
    }

    public static boolean is4gText(String text) {
        if (isEmpty(text)) return false;
        return text.contains("4G") || text.contains("LTE") || text.contains("4.5G");
    }

    public static boolean isCapsuleTargetText(String text) {
        if (isEmpty(text)) return false;
        return is5gText(text) || is4gText(text);
    }

    public static boolean isTargetBreathText(String text) {
        if (isEmpty(text)) return false;
        return is5gCaText(text) || is4gCaText(text);
    }

    /** Radio type implied by a mobile type label, used when no per-subscription state is known. */
    public static int radioTypeForText(String text) {
        if (is5gCaText(text)) return RADIO_TYPE_5G_CA;
        if ("5G".equals(text)) return RADIO_TYPE_5G;
        if (text != null && (text.contains("+") || text.contains("-A"))) return RADIO_TYPE_4G_CA;
        if (text != null && (text.contains("4G") || text.contains("LTE") || text.contains("4.5G"))) return RADIO_TYPE_4G;
        if (!isEmpty(text)) return RADIO_TYPE_2G_3G;
        return RADIO_TYPE_UNKNOWN;
    }

    /**
     * Rewrites the operator mobileTypeName list (at least 14 entries):
     * 6/9 single-carrier LTE, 7/8 LTE CA, 10/11 basic 5G, 12/13 5G CA.
     */
    public static void applyMobileTypeNames(List<String> list, String ca, String basic, String rat) {
        String caText = target5gCaText(ca);
        if (!isEmpty(caText)) {
            list.set(12, caText);
            list.set(13, caText);
        } else {
            list.set(12, "5G+");
            list.set(13, "5GA");
        }

        if ("follow_ca".equals(basic)) {
            String basicText = target5gBasicText(basic, ca);
            list.set(10, basicText);
            list.set(11, basicText);
        } else {
            list.set(10, "5G");
            list.set(11, "5G");
        }

        if ("force_4g".equals(rat)) {
            list.set(6, "4G");
            list.set(9, "4G");
            list.set(7, "4G+");
            list.set(8, "4G+");
        } else if ("force_lte".equals(rat)) {
            list.set(6, "LTE");
            list.set(9, "LTE");
            list.set(7, "LTE+");
            list.set(8, "LTE+");
        } else if ("force_4g_lte".equals(rat)) {
            list.set(6, "4G LTE");
            list.set(9, "4G LTE");
            list.set(7, "4G LTE+");
            list.set(8, "4G LTE+");
        } else if ("force_45g".equals(rat)) {
            list.set(6, "4.5G");
            list.set(9, "4.5G");
            list.set(7, "4.5G+");
            list.set(8, "4.5G+");
        } else if ("force_ltea".equals(rat)) {
            list.set(6, "LTE");
            list.set(9, "LTE");
            list.set(7, "LTE-A");
            list.set(8, "LTE-A");
        } else {
            list.set(6, "4G");
            list.set(9, "LTE");
            list.set(7, "4G+");
            list.set(8, "LTE+");
        }
    }

    // ---- VoLTE / VoNR on Wi-Fi ----

    /**
     * Wi-Fi preference for the volteVisibleGlobal lambda when it carries no
     * subscription: the global value, or the shared per-SIM value when both
     * SIMs agree, else "default".
     */
    public static String lambdaWifiVoltePref(String simMode, String global, String sim1, String sim2) {
        if (!"separate".equals(simMode)) return global;
        return sim1 != null && sim1.equals(sim2) ? sim1 : "default";
    }

    /**
     * Rewrites MiuiCellularIconVM.volteVisibleGlobal, whose stock value is
     * Pair(wifiAvailable && !showVolteWhenWifiOnAndDripEnd, volte && !hideVolte).
     * Returns {first, second}.
     */
    public static boolean[] volteVisiblePair(boolean first, boolean second, String volteStyle,
                                             String wifiPref, boolean wifiOn) {
        if ("hide".equals(volteStyle)) {
            second = false;
        }
        if ("hide".equals(wifiPref) && wifiOn) {
            second = false;
        } else if ("show".equals(wifiPref) && !"hide".equals(volteStyle)) {
            // Drop the stock Wi-Fi hide term; VoLTE/VoNR stays visible whenever active.
            first = false;
        }
        return new boolean[] { first, second };
    }

    /** Whether the Wi-Fi preference can override the mobile_volte child visibility at all. */
    public static boolean overridesVolteOnWifi(String volteStyle, String wifiPref) {
        return !"hide".equals(volteStyle) && ("show".equals(wifiPref) || "hide".equals(wifiPref));
    }

    /**
     * Forced visibility for mobile_volte in MobileSignalAnimatorContainer.setChildVisible,
     * or null to keep the value SystemUI asked for.
     */
    public static Boolean volteChildVisibleOverride(String volteStyle, String wifiPref, boolean wifiOn,
                                                    boolean volteActive, boolean policyHidden) {
        if (!overridesVolteOnWifi(volteStyle, wifiPref) || !wifiOn) return null;
        if ("hide".equals(wifiPref)) return Boolean.FALSE;
        if (volteActive && !policyHidden) return Boolean.TRUE;
        return null;
    }
}
