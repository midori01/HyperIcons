#!/data/data/com.termux/files/usr/bin/env python3
"""
Runs the real Java IconPolicy (src/com/midori/hypericons/IconPolicy.java) on a
plain JVM through tests/java/PolicyProbe.java and checks every case against the
independent oracle in test_matrix.py. Unlike the simulator in test_matrix.py,
this exercises the exact code the SystemUI hook executes.
"""

import os
import shutil
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)

from test_matrix import (  # noqa: E402
    BASIC_OPTIONS, CA_OPTIONS, CA_TEXTS, RADIO_4G, RADIO_4G_CA, RAT_4G_OPTIONS,
    RAT_WIFI_OPTIONS, VOLTE_OPTIONS, WIFI_VOLTE_OPTIONS,
    MainHookSystemUISimulator, StatusIconGroundTruthOracle as Oracle, split_pref,
)

SIM_MODES = ["both", "separate"]

# ConfigKeys.resolve with a global CA label "G" and a SIM 2 override "S2": SIM 1
# falls back to the global label, and the never-set base label stays "default".
# Every key must also be writable through the provider (allStringKeys).
EXPECTED_CONFIG = {
    "pref_5g_ca_text": "G", "pref_sim1_5g_ca_text": "G", "pref_sim2_5g_ca_text": "S2",
    "pref_5g_basic_text": "default", "pref_sim1_5g_basic_text": "default", "pref_sim2_5g_basic_text": "default",
}

# Raw custom labels and the IconPolicy.checkCustomText code each must get
# (0 ok, 1 empty, 2 too long, 3 bad character, 4 reserved). The shown label is
# taken from the oracle, so both the code and the label are checked.
CHECK_CASES = [
    ("5G UC", 0), ("  NR  ", 0), ("", 1), ("   ", 1), (" ", 1), ("　NR　", 0),
    ("1234567890", 0), ("12345678901", 2), ("\U0001F600" * 5, 0), ("\U0001F600" * 6, 2),
    ("A\nB", 3), ("A\tB", 3), ("A​B", 3), ("A B", 3),
    ("default", 4), ("5G", 4), ("LTE", 4), ("4G+", 4), ("H+", 4), ("4.5G+", 4), ("LTE-A", 4),
    ("lte", 0), ("5G+", 0), ("5Ｇ", 0), ("日本語", 0), ("a,b", 0),
    (" " * 62 + "NR", 0), (" " * 63 + "NR", 2), ("NR" * 40000, 2),
]

# Expected IconPolicy.RADIO_TYPE_* for a mobile type label.
RADIO_TEXTS = {
    **{t: 5 for t in CA_TEXTS},
    "5G": 4,
    "4G+": 3, "LTE+": 3, "4G LTE+": 3, "4.5G+": 3, "LTE-A": 3,
    "4G": 2, "LTE": 2, "4G LTE": 2, "4.5G": 2,
    "3G": 1, "H": 1, "E": 1,
    "": 0,
    # Labels the custom options use, as stock SystemUI would read them.
    "5G UC": 1, "LTE X": 2, "NR": 1,
}


def expected_names(ca, basic, rat):
    eff_ca = Oracle.get_expected_effective_5g_ca_text(ca)
    names = [f"x{i}" for i in range(14)]
    names[12] = eff_ca
    names[13] = eff_ca if Oracle.get_expected_5g_ca_target(ca) else "5GA"
    if basic == "follow_ca":
        names[10] = names[11] = eff_ca
    else:
        names[10] = names[11] = Oracle.get_expected_5g_basic_custom(basic, ca) or "5G"
    names[6] = Oracle.get_expected_4g_text(RADIO_4G, rat)
    names[7] = Oracle.get_expected_4g_text(RADIO_4G_CA, rat)
    # Stock layout keeps the LTE-branded pair distinct only when no style is forced.
    names[8] = "LTE+" if rat == "default" else names[7]
    names[9] = "LTE" if rat == "default" else names[6]
    return "|".join(names)


def flag(v):
    return v == "1"


def bit(v):
    return "1" if v else "0"


def code_points(s):
    return " ".join(f"U+{ord(ch):04X}" for ch in s)


def expected_custom_radio(ca, basic, text):
    """radioType, is5gCa, isCapsuleTarget and usesNativeBase5gPill for a label under custom options."""
    ca_custom = Oracle.get_expected_5g_ca_target(ca) if split_pref(ca)[0] == "custom" else ""
    basic_custom = Oracle.get_expected_5g_basic_custom(basic, ca)
    if text and text == ca_custom:
        radio, is_ca, capsule = 5, True, True
    elif text and text == basic_custom:
        radio, is_ca, capsule = 4, False, True
    else:
        radio = RADIO_TEXTS[text]
        is_ca = text in CA_TEXTS
        capsule = text.startswith("5G") or "4G" in text or "LTE" in text or "4.5G" in text
    pill = split_pref(basic)[0] != "follow_ca" and not basic_custom
    return "\t".join([str(radio), bit(is_ca), bit(capsule), bit(pill)])


def expected_nsa_icon_action(nr_config, bearer, data, since):
    """0 keeps the 5G icon, 1 keeps it during the hold window, 2 clears it."""
    if nr_config != 0 or bearer > 0 or data not in (13, 19):
        return 0
    return 1 if since < 11000 else 2


def run_probe():
    javac, java = shutil.which("javac"), shutil.which("java")
    if not javac or not java:
        print("[FAIL] javac/java not found; the Java policy test needs a JDK")
        sys.exit(1)
    out_dir = tempfile.mkdtemp(prefix="hypericons-probe-")
    try:
        subprocess.run([javac, "-d", out_dir,
                        os.path.join(ROOT, "src/com/midori/hypericons/IconPolicy.java"),
                        os.path.join(ROOT, "src/com/midori/hypericons/ConfigKeys.java"),
                        os.path.join(HERE, "java/PolicyProbe.java")], check=True)
        args = [
            "ratmode=" + ",".join(RAT_WIFI_OPTIONS),
            "ca=" + ",".join(CA_OPTIONS),
            "basic=" + ",".join(BASIC_OPTIONS),
            "rat=" + ",".join(RAT_4G_OPTIONS),
            "text=" + ",".join(RADIO_TEXTS),
            "volte=" + ",".join(VOLTE_OPTIONS),
            "wifivolte=" + ",".join(WIFI_VOLTE_OPTIONS),
            "simmode=" + ",".join(SIM_MODES),
        ] + ["check=" + raw for raw, _ in CHECK_CASES]
        res = subprocess.run([java, "-cp", out_dir, "PolicyProbe"] + args,
                             check=True, capture_output=True, text=True)
        return res.stdout.splitlines()
    finally:
        shutil.rmtree(out_dir, ignore_errors=True)


def check(line):
    cols = line.split("\t")
    kind = cols[0]
    if kind == "RAT":
        mode, intl, wifi, data_sim, act = cols[1:]
        exp = Oracle.get_expected_rat_allowed(mode, "intl" if flag(intl) else "china", flag(wifi), flag(data_sim))
        return exp == flag(act), exp
    if kind == "NAMES":
        ca, basic, rat, act = cols[1:]
        exp = expected_names(ca, basic, rat)
        return exp == act, exp
    if kind == "RADIO":
        text, act = cols[1:]
        exp = RADIO_TEXTS[text]
        return exp == int(act), exp
    if kind == "CRADIO":
        ca, basic, text = cols[1:4]
        act = "\t".join(cols[4:])
        exp = expected_custom_radio(ca, basic, text)
        return exp == act, exp
    if kind == "CONFIG":
        key, value, writable = cols[1:]
        exp = f"{EXPECTED_CONFIG[key]}\t1"
        return exp == f"{value}\t{writable}", exp
    if kind == "CHECK":
        idx, code, label = cols[1:]
        raw, exp_code = CHECK_CASES[int(idx)]
        exp = f"{exp_code}\t{code_points(Oracle.get_expected_custom_label(raw))}"
        return exp == f"{code}\t{label}", exp
    if kind == "VOLTE":
        volte, wifi_pref, wifi_on, active, policy, show_when_wifi, drip_end, act = cols[1:]
        exp = Oracle.get_expected_volte_wifi_visibility(
            wifi_pref, volte, flag(wifi_on), flag(active), flag(policy), flag(show_when_wifi), flag(drip_end))
        return exp == flag(act), exp
    if kind == "NSAICON":
        nr_config, bearer, data, since, act = map(int, cols[1:])
        exp = expected_nsa_icon_action(nr_config, bearer, data, since)
        return exp == act, exp
    if kind == "LAMBDA":
        sim_mode, g, s1, s2, act = cols[1:]
        exp = MainHookSystemUISimulator.lambda_wifi_pref(sim_mode, g, s1, s2)
        return exp == act, exp
    raise ValueError(f"unknown probe line: {line!r}")


def main():
    print("\n" + "=" * 75)
    print("Java IconPolicy vs Ground Truth Oracle (real code on a plain JVM)...")
    print("=" * 75)
    lines = run_probe()
    expected_total = (
        len(RAT_WIFI_OPTIONS) * 8
        + len(CA_OPTIONS) * len(BASIC_OPTIONS) * len(RAT_4G_OPTIONS)
        + len(RADIO_TEXTS)
        + len(CA_OPTIONS) * len(BASIC_OPTIONS) * len(RADIO_TEXTS)
        + len(CHECK_CASES)
        + len(EXPECTED_CONFIG)
        + len(VOLTE_OPTIONS) * len(WIFI_VOLTE_OPTIONS) * 32
        + len(SIM_MODES) * len(WIFI_VOLTE_OPTIONS) ** 3
        + 2 * 3 * 5 * 4
    )
    if len(lines) != expected_total:
        print(f"[FAIL] probe printed {len(lines)} cases, expected {expected_total}")
        sys.exit(1)

    mismatches = []
    for line in lines:
        ok, exp = check(line)
        if not ok:
            mismatches.append(f"{line!r} expected {exp!r}")

    print(f"- Java Policy Cases:          {len(lines):,}")
    print(f"- Mismatches vs Ground Truth: {len(mismatches)}")
    if mismatches:
        print("\n[FAIL] Java IconPolicy disagrees with the oracle (first 5 shown):")
        for m in mismatches[:5]:
            print(f"  * {m}")
        sys.exit(1)
    print(f"[VERIFIED] All {len(lines):,} Java IconPolicy cases match the Ground Truth Oracle.")


if __name__ == "__main__":
    main()
