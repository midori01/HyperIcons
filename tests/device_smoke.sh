#!/usr/bin/env bash
# On-device smoke test over adb. Checks the installed build, the hook status
# reported by SystemUI and the phone process, and the data icon (RAT) on the
# live status bar for every "Data Icon When Connected to Wi-Fi" mode.
# The user's own setting is restored afterwards.
#
# Usage: tests/device_smoke.sh [-s SERIAL] [--rom intl|china]
set -u

ADB=(adb)
ROM=intl
while [ $# -gt 0 ]; do
    case "$1" in
        -s) ADB=(adb -s "$2"); shift 2 ;;
        --rom) ROM="$2"; shift 2 ;;
        *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
done

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
URI=content://com.midori.hypericons.provider/config
FAILS=0

pass() { echo "[PASS] $*"; }
fail() { echo "[FAIL] $*"; FAILS=$((FAILS + 1)); }
sh_() { "${ADB[@]}" shell "$@" | tr -d '\r'; }

# Value of KEY in the Bundle printed by `content call`.
bundle_value() {
    sed -n "s/.*[{ ]$1=\([^,}]*\).*/\1/p" | head -n1
}

expected_version=$(bash "$DIR/version.sh" | cut -d' ' -f1)
installed_version=$(sh_ dumpsys package com.midori.hypericons | sed -n 's/.*versionName=//p' | head -n1)
if [ "$installed_version" = "$expected_version" ]; then
    pass "installed version $installed_version"
else
    fail "installed version '$installed_version', expected '$expected_version'"
fi

# --- Hook status: "framework|version|api|module|time;G|name|reason;P|point|reason;..." ---
# Ask both processes for a fresh report so a stale one (e.g. from before the module was
# disabled) cannot pass.
requested=""
for attempt in 1 2 3; do
    requested=$(sh_ content call --uri "$URI" --method requestHookReports 2>/dev/null | bundle_value time)
    [ -n "$requested" ] && break
    sleep 2
done
if [ -z "$requested" ]; then
    fail "requestHookReports failed; hook status cannot be verified"
    PROCS=""
else
    PROCS="systemui phone"
    sleep 3
fi
for proc in $PROCS; do
    report=$(sh_ content call --uri "$URI" --method getHookReports --arg "$proc" | sed -n 's/.*{report=\(.*\)}\]$/\1/p')
    if [ -z "$report" ]; then
        fail "$proc: no hook report (module not loaded, or the process was not restarted)"
        continue
    fi
    lines=$(printf '%s' "$report" | tr ';' '\n')
    head_line=$(printf '%s\n' "$lines" | head -n1)
    framework="$(printf '%s' "$head_line" | cut -d'|' -f1) $(printf '%s' "$head_line" | cut -d'|' -f2)"
    api=$(printf '%s' "$head_line" | cut -d'|' -f3)
    version=$(printf '%s' "$head_line" | cut -d'|' -f4)
    reported=$(printf '%s' "$head_line" | cut -d'|' -f5)
    groups=$(printf '%s\n' "$lines" | grep -c '^G|' || true)
    failed=$(printf '%s\n' "$lines" | awk -F'|' '$1 == "G" && $3 != "" { print $2 ": " $3 }')
    if [ "${reported:-0}" -lt "$((requested - 1000))" ]; then
        fail "$proc: did not answer the report request (module disabled or process not reloaded)"
    elif [ "$groups" -eq 0 ]; then
        fail "$proc: report lists no features"
    elif [ "$version" != "$expected_version" ]; then
        fail "$proc: running v$version, expected v$expected_version (restart it)"
    elif [ -n "$failed" ]; then
        fail "$proc: failing features:"
        printf '%s\n' "$failed" | sed 's/^/    /'
    else
        pass "$proc: $groups features hooked on $framework (API $api)"
    fi
done

# --- Data icon on the live status bar ---
dump_count() {
    sh_ "uiautomator dump --windows /sdcard/hi_smoke.xml >/dev/null 2>&1; grep -o -E '$1' /sdcard/hi_smoke.xml | wc -l"
}

# The data icon is mobile_type, or mobile_special_5G (the Korean 5G pill) in capsule mode.
RAT_VIEWS='systemui:id/mobile_(type|special_5G)"'

wifi_on=0
sh_ cmd wifi status 2>/dev/null | grep -qi "connected to" && wifi_on=1
sims=$(dump_count 'systemui:id/mobile_combo"')

expected_rat() {
    case "$1" in
        always) echo "$sims" ;;
        data_sim) echo 1 ;;
        wifi_hide) [ "$wifi_on" = 1 ] && echo 0 || echo 1 ;;
        default)
            if [ "$ROM" = china ]; then echo "$sims"
            elif [ "$wifi_on" = 1 ]; then echo 0
            else echo 1
            fi ;;
    esac
}

config=$(sh_ content call --uri "$URI" --method getConfig)
# In unified mode SystemUI only reads the global key, so leave the slot keys untouched and the
# restore is exact. In independent mode the slot keys are what applies.
if [ "$(printf '%s' "$config" | bundle_value pref_sim_mode)" = separate ]; then
    KEYS="pref_sim1_always_show_rat pref_sim2_always_show_rat"
else
    KEYS="pref_always_show_rat"
fi
original=""
for key in $KEYS; do
    value=$(printf '%s' "$config" | bundle_value "$key")
    original="$original $key:s:${value:-default}"
done
echo "[i] SIM groups on status bar: $sims, Wi-Fi connected: $wifi_on, ROM: $ROM"

set_all() {
    local extras=()
    for key in $KEYS; do extras+=(--extra "$key:s:$1"); done
    sh_ content call --uri "$URI" --method setConfig "${extras[@]}" | grep -q 'Bundle\[{}\]'
}

distinct=$(for mode in default always data_sim wifi_hide; do expected_rat "$mode"; done | sort -u | wc -l)
if [ "$distinct" -lt 2 ]; then
    echo "[SKIP] data icon modes: every mode expects $(expected_rat default) icon(s) here; use dual SIM on Wi-Fi to tell them apart"
    MODES=""
else
    MODES="default always data_sim wifi_hide"
fi

for mode in $MODES; do
    if ! set_all "$mode"; then
        fail "setConfig rejected for mode $mode"
        continue
    fi
    sleep 3
    got=$(dump_count "$RAT_VIEWS")
    want=$(expected_rat "$mode")
    if [ "$got" = "$want" ]; then
        pass "data icon mode $mode: $got visible"
    else
        fail "data icon mode $mode: $got visible, expected $want"
    fi
done

restore=()
for binding in $original; do restore+=(--extra "$binding"); done
sh_ content call --uri "$URI" --method setConfig "${restore[@]}" | grep -q 'Bundle\[{}\]' || fail "restoring the original setting failed"
sh_ rm -f /sdcard/hi_smoke.xml
echo "[i] restored:$original"

if [ "$FAILS" -gt 0 ]; then
    echo "[FAIL] $FAILS check(s) failed"
    exit 1
fi
echo "[VERIFIED] device smoke test passed"
