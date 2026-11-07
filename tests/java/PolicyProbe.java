import com.midori.hypericons.IconPolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the real IconPolicy over every input combination and prints one
 * tab-separated line per case for tests/test_java_policy.py to compare
 * against the oracle. Option lists come from the command line as
 * name=v1,v2,... so the Python side stays the single source of truth.
 */
public class PolicyProbe {
    private static final boolean[] BOOLS = { false, true };

    public static void main(String[] args) {
        Map<String, List<String>> opts = new HashMap<String, List<String>>();
        for (String arg : args) {
            int eq = arg.indexOf('=');
            opts.put(arg.substring(0, eq), Arrays.asList(arg.substring(eq + 1).split(",", -1)));
        }
        StringBuilder out = new StringBuilder();

        for (String mode : opts.get("ratmode")) {
            for (boolean intl : BOOLS) {
                for (boolean wifi : BOOLS) {
                    for (boolean dataSim : BOOLS) {
                        int rule = IconPolicy.ratRule(mode, intl);
                        line(out, "RAT", mode, b(intl), b(wifi), b(dataSim),
                                b(IconPolicy.shouldShowRat(rule, wifi, dataSim)));
                    }
                }
            }
        }

        for (String ca : opts.get("ca")) {
            for (String basic : opts.get("basic")) {
                for (String rat : opts.get("rat")) {
                    List<String> names = new ArrayList<String>();
                    for (int i = 0; i < 14; i++) names.add("x" + i);
                    IconPolicy.applyMobileTypeNames(names, ca, basic, rat);
                    line(out, "NAMES", ca, basic, rat, join(names));
                }
            }
        }

        for (String text : opts.get("text")) {
            line(out, "RADIO", text, String.valueOf(IconPolicy.radioTypeForText(text)));
        }

        for (String volte : opts.get("volte")) {
            for (String wifiPref : opts.get("wifivolte")) {
                for (boolean wifiOn : BOOLS) {
                    for (boolean active : BOOLS) {
                        for (boolean policyHidden : BOOLS) {
                            for (boolean showWhenWifi : BOOLS) {
                                for (boolean dripEnd : BOOLS) {
                                    line(out, "VOLTE", volte, wifiPref, b(wifiOn), b(active), b(policyHidden),
                                            b(showWhenWifi), b(dripEnd),
                                            b(volteVisible(volte, wifiPref, wifiOn, active, policyHidden,
                                                    showWhenWifi, dripEnd)));
                                }
                            }
                        }
                    }
                }
            }
        }

        for (String simMode : opts.get("simmode")) {
            for (String g : opts.get("wifivolte")) {
                for (String s1 : opts.get("wifivolte")) {
                    for (String s2 : opts.get("wifivolte")) {
                        line(out, "LAMBDA", simMode, g, s1, s2, IconPolicy.lambdaWifiVoltePref(simMode, g, s1, s2));
                    }
                }
            }
        }

        System.out.print(out);
    }

    /**
     * Stock SystemUI path around the two IconPolicy hooks: the volteVisibleGlobal
     * pair feeds the native visibility, then setChildVisible may force it.
     */
    private static boolean volteVisible(String volte, String wifiPref, boolean wifiOn, boolean active,
                                        boolean policyHidden, boolean showWhenWifi, boolean dripEnd) {
        boolean[] pair = IconPolicy.volteVisiblePair(wifiOn && !showWhenWifi, active && !policyHidden,
                volte, wifiPref, wifiOn);
        boolean nativeVisible = !(pair[0] && dripEnd) && pair[1];
        if ("hide".equals(volte)) nativeVisible = false;
        Boolean forced = IconPolicy.volteChildVisibleOverride(volte, wifiPref, wifiOn, active, policyHidden);
        return forced != null ? forced.booleanValue() : nativeVisible;
    }

    private static String b(boolean v) {
        return v ? "1" : "0";
    }

    private static String join(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append('|');
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    private static void line(StringBuilder out, String... cols) {
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) out.append('\t');
            out.append(cols[i]);
        }
        out.append('\n');
    }
}
