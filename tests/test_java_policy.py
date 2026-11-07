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
    MainHookSystemUISimulator, StatusIconGroundTruthOracle as Oracle,
)

SIM_MODES = ["both", "separate"]

# Expected IconPolicy.RADIO_TYPE_* for a mobile type label.
RADIO_TEXTS = {
    **{t: 5 for t in CA_TEXTS},
    "5G": 4,
    "4G+": 3, "LTE+": 3, "4G LTE+": 3, "4.5G+": 3, "LTE-A": 3,
    "4G": 2, "LTE": 2, "4G LTE": 2, "4.5G": 2,
    "3G": 1, "H": 1, "E": 1,
    "": 0,
}


def expected_names(ca, basic, rat):
    eff_ca = Oracle.get_expected_effective_5g_ca_text(ca)
    names = [f"x{i}" for i in range(14)]
    names[12] = eff_ca
    names[13] = eff_ca if ca != "default" else "5GA"
    names[10] = names[11] = eff_ca if basic == "follow_ca" else "5G"
    names[6] = Oracle.get_expected_4g_text(RADIO_4G, rat)
    names[7] = Oracle.get_expected_4g_text(RADIO_4G_CA, rat)
    # Stock layout keeps the LTE-branded pair distinct only when no style is forced.
    names[8] = "LTE+" if rat == "default" else names[7]
    names[9] = "LTE" if rat == "default" else names[6]
    return "|".join(names)


def flag(v):
    return v == "1"


def run_probe():
    javac, java = shutil.which("javac"), shutil.which("java")
    if not javac or not java:
        print("[FAIL] javac/java not found; the Java policy test needs a JDK")
        sys.exit(1)
    out_dir = tempfile.mkdtemp(prefix="hypericons-probe-")
    try:
        subprocess.run([javac, "-d", out_dir,
                        os.path.join(ROOT, "src/com/midori/hypericons/IconPolicy.java"),
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
        ]
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
    if kind == "VOLTE":
        volte, wifi_pref, wifi_on, active, policy, show_when_wifi, drip_end, act = cols[1:]
        exp = Oracle.get_expected_volte_wifi_visibility(
            wifi_pref, volte, flag(wifi_on), flag(active), flag(policy), flag(show_when_wifi), flag(drip_end))
        return exp == flag(act), exp
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
        + len(VOLTE_OPTIONS) * len(WIFI_VOLTE_OPTIONS) * 32
        + len(SIM_MODES) * len(WIFI_VOLTE_OPTIONS) ** 3
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
