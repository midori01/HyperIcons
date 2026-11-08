# HyperIcons

[![Version](https://img.shields.io/github/v/release/midori01/HyperIcons?label=version)](https://github.com/midori01/HyperIcons/releases)
[![Target OS](https://img.shields.io/badge/HyperOS-3%20%7C%204-orange.svg)](#)
[![Android](https://img.shields.io/badge/Android-17%20(API%2037)-green.svg)](#)
[![Framework](https://img.shields.io/badge/Framework-LSPosed-purple.svg)](#)
[![Oracle Verification](https://img.shields.io/badge/Oracle%20Tests-1%2C897%2C519%20Passed%20(100%25)-brightgreen.svg)](tests/)
[![License](https://img.shields.io/badge/License-Apache--2.0-yellow.svg)](LICENSE)

**HyperIcons** is an advanced LSPosed module for **Xiaomi HyperOS 3 and HyperOS 4**. It intercepts the SystemUI cellular icon dispatch pipeline and modem service frameworks to bypass carrier restrictions, delivering granular status bar customization for 5G Carrier Aggregation, legacy RAT indicators, HD voice, Wi-Fi calling, two-tier dual-SIM configurations, and hardware-level cellular feature persistence. Since 6.0.0 it is built on the modern **libxposed API 102** module interface.

---

## 1. Feature Matrix

### 1) 5G Carrier Aggregation (CA / 5G-Advanced) Indicator Customization
When the device is connected to a 5G network with active Carrier Aggregation (CA / 5G-A):
- **Default**: System default behavior (typically 5GA for Chinese carriers, 5G+ for global carriers).
- **5GA**: Force the Chinese 5G-Advanced badge.
- **5G+**: Force the international Standalone (SA) Carrier Aggregation badge.
- **5G++**: Render the stacked dual-plus badge (invokes the native `MobileTypeDrawable` dual-plus rasterizer used by Reliance Jio).
- **5GUWB**: Force the Verizon Ultra Wideband indicator.
- **5Ge**: Force the AT&T marketing indicator.
- **5G 6Rx**: Force the Qualcomm Snapdragon 8 Elite flagship 6-receive antenna indicator (`ic_5g_6rx_mobiledata`).
- **5G++ 6Rx**: Force the composite stacked dual-plus and 6Rx antenna badge (`ic_5g_plus_plus_6rx_mobiledata`).
- **5GUWB 6Rx**: Force the composite Ultra Wideband and 6Rx antenna badge (`ic_5g_uwb_6rx_mobiledata`).
- **Custom**: Show your own label of up to 10 characters (for example `5G UC`). Labels that SystemUI uses for other network types (such as `LTE` or `4G+`) are rejected.

### 2) Basic 5G Display Mode (Single-Carrier NSA / SA)
Configures status bar presentation when connected to standard single-carrier 5G without Carrier Aggregation:
- **Default**: System standard 5G indicator.
- **Korean Dynamic Pulsation**: Activates the Korean-variant dynamic `$special5G` badge, illuminating during active upstream/downstream traffic (`signal_5g_on`) and dimming when idle (`signal_5g_off`).
- **Follow 5G CA**: Mirrors the visual styling configured for 5G Carrier Aggregation regardless of active carrier aggregation state.
- **Custom**: Show your own label of up to 10 characters (for example `NR`). It must differ from what 5G CA shows. With the capsule style on, the label is drawn in the module's capsule instead of the native Korean pill, and like the pill it turns solid while data is active and hollow when idle.

### 3) 4G / LTE RAT Styles
Forces specific cellular network radio access technology badges and carrier aggregation indicators:
- **Default**: Carrier and regional default policy.
- **Force 4G / 4G+**: Displays `4G` on single carrier, `4G+` under LTE CA.
- **Force LTE / LTE+**: Displays `LTE` on single carrier, `LTE+` under LTE CA.
- **Force 4G LTE / 4G LTE+**: Displays `4G LTE` on single carrier, `4G LTE+` under LTE CA.
- **Force 4.5G / 4.5G+**: Displays `4.5G` on single carrier, `4.5G+` under LTE CA (Turkish / LATAM style).
- **Force LTE / LTE-A**: Displays `LTE` on single carrier, `LTE-A` under LTE CA (European / Korean style).

**Data Icon When Connected to Wi-Fi** (global or per SIM):
- **System Default**: Stock behavior (Global ROMs hide the data icon on Wi-Fi; China ROMs keep it).
- **Always Show Data Icon**: Shows the data icon on both SIMs, on Wi-Fi and cellular.
- **Show Data Icon on Data SIM Only**: Shows the data icon only on the active data SIM, on Wi-Fi and cellular; the other SIM's icon stays hidden.
- **Hide Data Icon on Wi-Fi**: Hides the data icon on both SIMs while Wi-Fi is connected.

### 4) VoLTE / VoNR & HD Voice Indicators
- **Default**: System default configuration.
- **International Solid Volte**: Solid filled international pill badge.
- **International 4G Volte**: International Volte badge with 4G branding.
- **International Hollow Volte**: Minimalist outlined Volte badge.
- **International HD Voice**: Standard Western HD Voice glyph.
- **China Telecom/Mobile HD**: Classic boxed HD glyph.
- **China Super Wideband HD+**: Super wideband EVS HD+ indicator (`stat_sys_carrier_signal_hd_plus`).
- **International Vo4G**: Compact Vo4G glyph.
- **Hide Completely**: Completely suppresses VoLTE / VoNR indicators from the status bar.
- **Visibility on Wi-Fi**: System Default / Keep Visible / Hide for the VoLTE / VoNR icon while Wi-Fi is the active connection, applied globally or per SIM. System Default keeps the stock carrier behavior.
- **VoNR Icon Visibility**: Separate System Default / Always Show / Hide override for the VoNR icon, applied globally or per SIM. It overrides `config_show_vonr_icon_in_statusbar` inside SystemUI only; carrier configuration files stay untouched.

### 5) VoWiFi (Wi-Fi Calling) Indicators
- **Default**: System default style.
- **Standard Text**: Classic `VoWiFi` boxed text badge (`stat_sys_vowifi`).
- **Wi-Fi Wave & Handset**: Curved Wi-Fi signal arcs integrated with telephone handset (`stat_sys_vowifi_wifi`).
- **SIM Slot Numbered Handset**: Displays physical SIM slot index (`VoWiFi 1` on Slot 0 / `VoWiFi 2` on Slot 1).
- **Hide Completely**: Completely suppresses the VoWiFi indicator from the status bar.

### 6) Data Roaming Indicator
- **Default**: Standard large roaming indicator (`mobile_roaming`).
- **Compact Small R**: Corner-embedded small R badge (`stat_sys_data_connected_roam_small`).
- **Hide Completely**: Suppresses the data roaming badge.

### 7) Two-Tier Dual-SIM Architecture
- **Tier 1: Global vs Independent Mode**:
  - **Global Unified Mode**: Applying any preference synchronizes settings across both SIM 1 and SIM 2 / eSIM simultaneously. The secondary slot selector collapses cleanly.
  - **Independent Dual-SIM Mode**: Unfolds the secondary slot picker to configure each subscription independently.
- **Tier 2: Hardware Slot Alignment**:
  - **SIM 1 (Slot 0)**: Controls physical SIM slot 0.
  - **SIM 2 / eSIM (Slot 1)**: Controls physical SIM slot 1 (shared mutually by physical SIM 2 and internal eSIM).

### 8) Zero-Reboot Live Reload & Direct View Punch
- Preference modifications are persisted immediately to `SharedPreferences` and synchronized across processes via `IconConfigProvider`.
- Fires `com.midori.hypericons.ACTION_RELOAD` to SystemUI, triggering `updateMiuiOperatorConfig` across active `StateFlow` streams alongside direct view hierarchy traversal for millisecond-level live updates without restarting SystemUI.

### 9) Module Health Check
- SystemUI and the phone process report which features were hooked on the running ROM, along with the Xposed framework name, version and API level.
- The status card in the app shows whether the module is running, whether SystemUI still needs a restart after an update, and which features are unavailable. Tap it for the per-feature details.

### 10) Cellular & 5G+ Keepalive Engine (Experimental)
- **Scope**: Hooks into Android Telephony Service (`com.android.phone`), `FiveGManagerBase`, and `MiuiDsdaManager`.
- **Modem Reset Guard**: On dual-SIM + eSIM devices, toggling between physical SIM 2 and eSIM (Slot 1) causes Qualcomm baseband power-cycling, resetting volatile registers back to 3GPP R16 defaults and clearing user preferences (5G-A, VoNR, DSDA, APN editing).
- **Self-Healing Loop**: Tracks user-configured 3GPP release versions and developer options. During carrier config reload and slot re-enumeration, the engine reapplies the user's exact configuration back to the modem, preserving 5G-A and custom telephony parameters without dialer codes or reboots.

---

## 2. Automated Verification

```bash
bash run_tests.sh
```

- **Dual-Model Oracle** (`tests/test_matrix.py`): 6,256,479 Cartesian combinations across single-SIM, dual-SIM, cross-slot independent controls, custom 5G / 5G CA labels, VoWiFi, Roaming, and VoLTE / VoNR on Wi-Fi states, checked against an independent Ground Truth Oracle (0 mismatches).
- **Real Java rules** (`tests/test_java_policy.py`): the presentation rules in `IconPolicy.java` are the exact code SystemUI runs. `tests/java/PolicyProbe.java` executes them on a plain JVM for every input combination (2,870 cases, including custom label validation and the per-SIM fallback of the label keys), and each result is compared with the same oracle. Requires a JDK.
- **On-device smoke test** (`tests/device_smoke.sh`): over adb, checks the installed version, the hook status reported by SystemUI and the phone process, and the data icon on the live status bar for every Wi-Fi mode, then restores your setting.
  ```bash
  bash tests/device_smoke.sh            # add --rom china on China ROMs
  ```

---

## 3. Installation & Usage

1. Download the latest `HyperIcons.apk` from [GitHub Releases](https://github.com/midori01/HyperIcons/releases).
2. Install the APK on a rooted device running **HyperOS 3 or HyperOS 4** (6.0.0 was verified on HyperOS 4.0 Global, Android 17; the ROM region is detected automatically) with an Xposed framework that implements **libxposed API 101 or newer** (API 102 recommended), such as a current LSPosed / Vector build. Frameworks that only provide the legacy XposedBridge API cannot load 6.x.
3. Open the LSPosed manager and enable the module. The scope is fixed by the module:
   - `System UI (com.android.systemui)`
   - `Phone Services (com.android.phone)`
4. Reboot the device (or restart SystemUI and the phone process).
5. Open **HyperIcons** from the launcher or from the module's settings in the LSPosed manager. The status card at the top confirms that the module is running.

---

## 4. Building from Source

### Standalone On-Device Build (Termux / Linux)
```bash
chmod +x build.sh
bash build.sh
```
Compiles against `libs/api-102.jar` (the `classes.jar` of `io.github.libxposed:api:102.0.0`).

### Standard Gradle Build (Desktop / Android Studio / CI)
```bash
git clone https://github.com/midori01/HyperIcons.git
cd HyperIcons
./gradlew assembleRelease
```

---

## 5. License

This project is licensed under the [Apache License 2.0](LICENSE).
