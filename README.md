# HyperIcons

[![Version](https://img.shields.io/badge/version-v5.3.2%20(Build%2029)-blue.svg)](https://github.com/midori01/HyperIcons/releases)
[![Target OS](https://img.shields.io/badge/HyperOS-3.0-orange.svg)](#)
[![Android](https://img.shields.io/badge/Android-17%20(API%2037)-green.svg)](#)
[![Framework](https://img.shields.io/badge/Framework-LSPosed-purple.svg)](#)
[![Oracle Verification](https://img.shields.io/badge/Oracle%20Tests-1%2C409%2C935%20Passed%20(100%25)-brightgreen.svg)](tests/)
[![License](https://img.shields.io/badge/License-Apache--2.0-yellow.svg)](LICENSE)

**HyperIcons** is an advanced LSPosed module crafted specifically for **Xiaomi HyperOS 3.0 (Android 17, API 37)**. It intercepts the SystemUI cellular icon dispatch pipeline and modem service frameworks to bypass carrier restrictions, delivering granular status bar customization for 5G Carrier Aggregation, legacy RAT indicators, HD voice, Wi-Fi calling, two-tier dual-SIM configurations, and hardware-level cellular feature persistence.

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

### 2) Basic 5G Display Mode (Single-Carrier NSA / SA)
Configures status bar presentation when connected to standard single-carrier 5G without Carrier Aggregation:
- **Default**: System standard 5G indicator.
- **Korean Dynamic Pulsation**: Activates the Korean-variant dynamic `$special5G` badge, illuminating during active upstream/downstream traffic (`signal_5g_on`) and dimming when idle (`signal_5g_off`).
- **Follow 5G CA**: Mirrors the visual styling configured for 5G Carrier Aggregation regardless of active carrier aggregation state.

### 3) 4G / LTE RAT Styles
Forces specific cellular network radio access technology badges and carrier aggregation indicators:
- **Default**: Carrier and regional default policy.
- **Force 4G / 4G+**: Displays `4G` on single carrier, `4G+` under LTE CA.
- **Force LTE / LTE+**: Displays `LTE` on single carrier, `LTE+` under LTE CA.
- **Force 4G LTE / 4G LTE+**: Displays `4G LTE` on single carrier, `4G LTE+` under LTE CA.
- **Force 4.5G / 4.5G+**: Displays `4.5G` on single carrier, `4.5G+` under LTE CA (Turkish / LATAM style).
- **Force LTE / LTE-A**: Displays `LTE` on single carrier, `LTE-A` under LTE CA (European / Korean style).

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

### 9) Cellular & 5G+ Keepalive Engine (Experimental)
- **Scope**: Hooks into Android Telephony Service (`com.android.phone`), `FiveGManagerBase`, and `MiuiDsdaManager`.
- **Modem Reset Guard**: On dual-SIM + eSIM devices, toggling between physical SIM 2 and eSIM (Slot 1) causes Qualcomm baseband power-cycling, resetting volatile registers back to 3GPP R16 defaults and clearing user preferences (5G-A, VoNR, DSDA, APN editing).
- **Self-Healing Loop**: Tracks user-configured 3GPP release versions and developer options. During carrier config reload and slot re-enumeration, the engine reapplies the user's exact configuration back to the modem, preserving 5G-A and custom telephony parameters without dialer codes or reboots.

---

## 2. Automated Verification (Dual-Model Oracle)

HyperIcons includes an exhaustive mathematical verification engine (`tests/test_matrix.py`) executing a Dual-Model Oracle against simulated SystemUI runtime states:
```bash
bash run_tests.sh
```

- **Permutations Tested**: 1,409,935 total Cartesian combinations across single-SIM, dual-SIM, cross-slot independent controls, VoWiFi, and Roaming states.
- **Accuracy**: **100.00% match (0 mismatches)** against the independent Ground Truth Oracle.

---

## 3. Installation & Usage

1. Download the latest `HyperIcons.apk` from [GitHub Releases](https://github.com/midori01/HyperIcons/releases).
2. Install the APK on a rooted device running **HyperOS 3.0 (Android 17)**.
3. Open **LSPosed Manager** and enable the module. Recommended scopes are automatically declared:
   - `System UI (com.android.systemui)`
   - `Phone Services (com.android.phone)`
4. Reboot the device or restart SystemUI.
5. Launch the **HyperIcons** app from your launcher to customize your status bar.

---

## 4. Building from Source

### Standalone On-Device Build (Termux / Linux)
```bash
chmod +x build.sh
bash build.sh
```

### Standard Gradle Build (Desktop / Android Studio / CI)
```bash
git clone https://github.com/midori01/HyperIcons.git
cd HyperIcons
./gradlew assembleRelease
```

---

## 5. License

This project is licensed under the [Apache License 2.0](LICENSE).
