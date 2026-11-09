import com.midori.hypericons.ConfigKeys;
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
        List<String> checks = new ArrayList<String>();
        for (String arg : args) {
            int eq = arg.indexOf('=');
            String name = arg.substring(0, eq);
            if ("check".equals(name)) {
                // Raw custom labels may contain commas or control characters: one per argument.
                checks.add(arg.substring(eq + 1));
            } else {
                opts.put(name, Arrays.asList(arg.substring(eq + 1).split(",", -1)));
            }
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

        for (int nrConfig : new int[]{ 0, 1 }) {
            for (int bearer : new int[]{ 0, 1, 2 }) {
                for (int data : new int[]{ -1, 0, 13, 19, 20 }) {
                    for (long since : new long[]{ 0L, 10999L, 11000L, Long.MAX_VALUE }) {
                        line(out, "NSAICON", String.valueOf(nrConfig), String.valueOf(bearer),
                                String.valueOf(data), String.valueOf(since),
                                String.valueOf(IconPolicy.nsaIconAction(nrConfig, bearer, data, since, 11000L)));
                    }
                }
            }
        }

        for (int carriers = 0; carriers <= 3; carriers++) {
            line(out, "NRCA", String.valueOf(carriers), b(IconPolicy.isNsaNrCa(carriers)));
        }
        for (int index : new int[]{ 9, 10, 11, 12, 13 }) {
            for (boolean active : BOOLS) {
                line(out, "NSAIDX", String.valueOf(index), b(active),
                        String.valueOf(IconPolicy.mobileTypeIndexForNsa(index, active)));
            }
        }

        for (String ca : opts.get("ca")) {
            for (String basic : opts.get("basic")) {
                for (String rat : opts.get("rat")) {
                    List<String> names = new ArrayList<String>();
                    for (int i = 0; i < 14; i++) names.add("x" + i);
                    IconPolicy.applyMobileTypeNames(names, mode(ca), label(ca), mode(basic), label(basic), rat);
                    line(out, "NAMES", ca, basic, rat, join(names));
                }
            }
        }

        for (String text : opts.get("text")) {
            line(out, "RADIO", text, String.valueOf(IconPolicy.radioTypeForText(text)));
        }

        for (String ca : opts.get("ca")) {
            for (String basic : opts.get("basic")) {
                String caCustom = IconPolicy.custom5gCaText(mode(ca), label(ca));
                String basicCustom = IconPolicy.custom5gBasicText(mode(basic), label(basic), mode(ca), label(ca));
                for (String text : opts.get("text")) {
                    line(out, "CRADIO", ca, basic, text,
                            String.valueOf(IconPolicy.radioTypeForText(text, caCustom, basicCustom)),
                            b(IconPolicy.is5gCaText(text, caCustom, basicCustom)),
                            b(IconPolicy.isCapsuleTargetText(text, caCustom, basicCustom)),
                            b(IconPolicy.usesNativeBase5gPill(mode(basic), basicCustom)));
                }
            }
        }

        // Stored: a global CA label and a SIM 2 override; the base label is never set.
        final Map<String, String> stored = new HashMap<String, String>();
        stored.put(ConfigKeys.KEY_5G_CA_TEXT, "G");
        stored.put(ConfigKeys.slotKey(ConfigKeys.KEY_5G_CA_TEXT, 1), "S2");
        Map<String, String> resolved = ConfigKeys.resolve(new ConfigKeys.Source() {
            @Override
            public String get(String key, String def) {
                return stored.containsKey(key) ? stored.get(key) : def;
            }
        });
        List<String> writable = ConfigKeys.allStringKeys();
        for (String base : new String[] { ConfigKeys.KEY_5G_CA_TEXT, ConfigKeys.KEY_5G_BASIC_TEXT }) {
            for (int slot = -1; slot <= 1; slot++) {
                String key = ConfigKeys.slotKey(base, slot);
                line(out, "CONFIG", key, resolved.get(key), b(writable.contains(key)));
            }
        }

        for (int i = 0; i < checks.size(); i++) {
            String raw = checks.get(i);
            line(out, "CHECK", String.valueOf(i), String.valueOf(IconPolicy.checkCustomText(raw)),
                    escape(IconPolicy.customText(raw)));
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

    /** "custom:<label>" options select the custom mode; others are plain option values. */
    private static String mode(String option) {
        return option.startsWith("custom:") ? IconPolicy.CUSTOM : option;
    }

    private static String label(String option) {
        return option.startsWith("custom:") ? option.substring("custom:".length()) : null;
    }

    /** Code points as U+XXXX so labels with tabs or newlines stay on one probe line. */
    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            if (sb.length() > 0) sb.append(' ');
            sb.append(String.format("U+%04X", cp));
            i += Character.charCount(cp);
        }
        return sb.toString();
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
