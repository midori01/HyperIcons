package com.midori.hypericons;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity {
    public static final String VERSION_NAME = "5.3.2";
    public static final int VERSION_CODE = 29;
    public static final String PREF_NAME = "hyper_icons_config";
    public static final String ACTION_RELOAD = "com.midori.hypericons.ACTION_RELOAD";

    // Mode & Tabs
    public static final String KEY_SIM_MODE = "pref_sim_mode";           // "both" | "separate"
    public static final String KEY_TARGET_TAB = "pref_target_tab";       // "sim1" | "sim2"

    public static final String VAL_SIM_MODE_BOTH = "both";
    public static final String VAL_SIM_MODE_SEPARATE = "separate";
    public static final String TAB_BOTH = "both";
    public static final String TAB_SIM1 = "sim1";
    public static final String TAB_SIM2 = "sim2";

    // Global / Both SIMs keys
    public static final String KEY_5G_CA = "pref_5g_ca";
    public static final String KEY_5G_BASIC = "pref_5g_basic";
    public static final String KEY_CAPSULE_STYLE = "pref_capsule_style";
    public static final String KEY_4G_RAT = "pref_4g_rat";
    public static final String KEY_ALWAYS_SHOW_RAT = "pref_always_show_rat";
    public static final String KEY_VOLTE_STYLE = "pref_volte_style";
    public static final String KEY_VOWIFI_STYLE = "pref_vowifi_style";
    public static final String KEY_ROAMING_STYLE = "pref_roaming_style";

    // SIM 1 (Slot 0) keys
    public static final String KEY_SIM1_5G_CA = "pref_sim1_5g_ca";
    public static final String KEY_SIM1_5G_BASIC = "pref_sim1_5g_basic";
    public static final String KEY_SIM1_CAPSULE_STYLE = "pref_sim1_capsule_style";
    public static final String KEY_SIM1_4G_RAT = "pref_sim1_4g_rat";
    public static final String KEY_SIM1_ALWAYS_SHOW_RAT = "pref_sim1_always_show_rat";
    public static final String KEY_SIM1_VOLTE_STYLE = "pref_sim1_volte_style";
    public static final String KEY_SIM1_VOWIFI_STYLE = "pref_sim1_vowifi_style";
    public static final String KEY_SIM1_ROAMING_STYLE = "pref_sim1_roaming_style";

    // SIM 2 / eSIM (Slot 1) keys
    public static final String KEY_SIM2_5G_CA = "pref_sim2_5g_ca";
    public static final String KEY_SIM2_5G_BASIC = "pref_sim2_5g_basic";
    public static final String KEY_SIM2_CAPSULE_STYLE = "pref_sim2_capsule_style";
    public static final String KEY_SIM2_4G_RAT = "pref_sim2_4g_rat";
    public static final String KEY_SIM2_ALWAYS_SHOW_RAT = "pref_sim2_always_show_rat";
    public static final String KEY_SIM2_VOLTE_STYLE = "pref_sim2_volte_style";
    public static final String KEY_SIM2_VOWIFI_STYLE = "pref_sim2_vowifi_style";
    public static final String KEY_SIM2_ROAMING_STYLE = "pref_sim2_roaming_style";

    // Experimental: 5G+ Feature Center keep-alive
    public static final String KEY_ENABLE_5GA_KEEPALIVE = "pref_enable_5ga_keepalive";

    public static final String VAL_DEFAULT = "default";
    public static final String VAL_5GA = "5ga";
    public static final String VAL_5G_PLUS = "5g_plus";
    public static final String VAL_5G_PLUS_PLUS = "5g_plus_plus";
    public static final String VAL_5GUWB = "5guwb";
    public static final String VAL_5GE = "5ge";
    public static final String VAL_5G_6RX = "5g_6rx";
    public static final String VAL_5G_PLUS_PLUS_6RX = "5g_plus_plus_6rx";
    public static final String VAL_5GUWB_6RX = "5guwb_6rx";

    public static final String VAL_5G_FOLLOW_CA = "follow_ca";

    public static final String VAL_CAPSULE_DEFAULT = "default";
    public static final String VAL_CAPSULE_KOREAN = "korean";

    public static final String VAL_4G_FORCE_4G = "force_4g";
    public static final String VAL_4G_FORCE_LTE = "force_lte";
    public static final String VAL_4G_FORCE_4G_LTE = "force_4g_lte";
    public static final String VAL_4G_FORCE_45G = "force_45g";
    public static final String VAL_4G_FORCE_LTEA = "force_ltea";

    public static final String VAL_ALWAYS_SHOW_RAT_DEFAULT = "default";
    public static final String VAL_ALWAYS_SHOW_RAT_ALWAYS = "always";
    public static final String VAL_ALWAYS_SHOW_RAT_WIFI_HIDE = "wifi_hide";

    public static final String VAL_VOLTE_INTL_SOLID = "intl_solid";
    public static final String VAL_VOLTE_INTL_4G = "intl_4g";
    public static final String VAL_VOLTE_INTL_HOLLOW = "intl_hollow";
    public static final String VAL_VOLTE_INTL_HD_VOICE = "intl_hd_voice";
    public static final String VAL_VOLTE_CHINA_HD = "china_hd";
    public static final String VAL_VOLTE_CHINA_HD_PLUS = "china_hd_plus";
    public static final String VAL_VOLTE_INTL_VO4G = "intl_vo4g";
    public static final String VAL_VOLTE_HIDE = "hide";

    public static final String VAL_VOWIFI_STANDARD = "vowifi_standard";
    public static final String VAL_VOWIFI_WIFI = "vowifi_wifi";
    public static final String VAL_VOWIFI_CALL = "vowifi_call";
    public static final String VAL_VOWIFI_HIDE = "hide";

    public static final String VAL_ROAMING_SMALL = "small";
    public static final String VAL_ROAMING_HIDE = "hide";

    private SharedPreferences mPrefs;
    private RadioGroup mRgCapsuleStyle;
    private RadioGroup mRg5gCa;
    private RadioGroup mRg5gBasic;
    private RadioGroup mRg4gRat;
    private RadioGroup mRgAlwaysShowRat;
    private RadioGroup mRgVolteStyle;
    private RadioGroup mRgVowifiStyle;
    private RadioGroup mRgRoamingStyle;
    private Button mBtnRefresh;
    private Button mBtnRestartSysui;

    // Two-Tier Mode and Slot Selector Views
    private TextView mTabModeUnified;
    private TextView mTabModeSeparate;
    private View mContainerSubTabs;
    private TextView mTabSubSim1;
    private TextView mTabSubSim2;
    private TextView mTvModeHint;

    private View mDotStatus;
    private TextView mTvStatusText;
    private TextView mBadgeCapsuleStyle;
    private TextView mBadge5gCa;
    private TextView mBadge5gBasic;
    private TextView mBadge4gRat;
    private TextView mBadgeAlwaysShowRat;
    private TextView mBadgeVolteStyle;
    private TextView mBadgeVowifiStyle;
    private TextView mBadgeRoamingStyle;

    private String mCurrentSimMode = VAL_SIM_MODE_BOTH;
    private String mCurrentSubSlot = TAB_SIM1;
    private boolean mIsInitializing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefs = getSafePrefs(this);
        fixFilePermissions();

        mDotStatus = findViewById(R.id.dot_status);
        mTvStatusText = findViewById(R.id.tv_status_text);
        mBadgeCapsuleStyle = findViewById(R.id.badge_capsule_style);
        mBadge5gCa = findViewById(R.id.badge_5g_ca);
        mBadge5gBasic = findViewById(R.id.badge_5g_basic);
        mBadge4gRat = findViewById(R.id.badge_4g_rat);
        mBadgeAlwaysShowRat = findViewById(R.id.badge_always_show_rat);
        mBadgeVolteStyle = findViewById(R.id.badge_volte_style);
        mBadgeVowifiStyle = findViewById(R.id.badge_vowifi_style);
        mBadgeRoamingStyle = findViewById(R.id.badge_roaming_style);

        mTabModeUnified = findViewById(R.id.tab_mode_unified);
        mTabModeSeparate = findViewById(R.id.tab_mode_separate);
        mContainerSubTabs = findViewById(R.id.container_sub_tabs);
        mTabSubSim1 = findViewById(R.id.tab_sub_sim1);
        mTabSubSim2 = findViewById(R.id.tab_sub_sim2);
        mTvModeHint = findViewById(R.id.tv_mode_hint);

        mRgCapsuleStyle = findViewById(R.id.rg_capsule_style);
        mRg5gCa = findViewById(R.id.rg_5g_ca);
        mRg5gBasic = findViewById(R.id.rg_5g_basic);
        mRg4gRat = findViewById(R.id.rg_4g_rat);
        mRgAlwaysShowRat = findViewById(R.id.rg_always_show_rat);
        mRgVolteStyle = findViewById(R.id.rg_volte_style);
        mRgVowifiStyle = findViewById(R.id.rg_vowifi_style);
        mRgRoamingStyle = findViewById(R.id.rg_roaming_style);
        mBtnRefresh = findViewById(R.id.btn_refresh);
        mBtnRestartSysui = findViewById(R.id.btn_restart_sysui);

        TextView tvAppVersion = findViewById(R.id.tv_app_version);
        if (tvAppVersion != null) {
            try {
                String verName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                tvAppVersion.setText("v" + verName);
            } catch (Throwable ignored) {
                tvAppVersion.setText("v" + VERSION_NAME);
            }
        }

        mCurrentSimMode = mPrefs.getString(KEY_SIM_MODE, VAL_SIM_MODE_BOTH);
        String targetTab = mPrefs.getString(KEY_TARGET_TAB, TAB_SIM1);
        if (TAB_SIM2.equals(targetTab)) {
            mCurrentSubSlot = TAB_SIM2;
        } else {
            mCurrentSubSlot = TAB_SIM1;
        }

        updateTabVisuals();
        loadUiFromPrefs();
        updateStatusBanner();
        updateBadges();
        setupListeners();

        View btnExp = findViewById(R.id.btn_experimental);
        if (btnExp != null) {
            btnExp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, ExperimentalActivity.class);
                    startActivity(intent);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTabVisuals();
        updateStatusBanner();
    }

    private void updateTabVisuals() {
        if (mTabModeUnified == null || mTabModeSeparate == null || mContainerSubTabs == null ||
            mTabSubSim1 == null || mTabSubSim2 == null || mTvModeHint == null) return;

        int colorSelected = getColor(R.color.btn_text);
        int colorUnselected = getColor(R.color.text_body);

        if (VAL_SIM_MODE_BOTH.equals(mCurrentSimMode)) {
            // Unified Mode
            mTabModeUnified.setBackgroundResource(R.drawable.bg_tab_selected);
            mTabModeUnified.setTextColor(colorSelected);
            mTabModeUnified.setTypeface(null, Typeface.BOLD);

            mTabModeSeparate.setBackgroundResource(R.drawable.bg_tab_unselected);
            mTabModeSeparate.setTextColor(colorUnselected);
            mTabModeSeparate.setTypeface(null, Typeface.NORMAL);

            mContainerSubTabs.setVisibility(View.GONE);
            mTvModeHint.setText(R.string.hint_mode_unified);
        } else {
            // Independent Mode
            mTabModeUnified.setBackgroundResource(R.drawable.bg_tab_unselected);
            mTabModeUnified.setTextColor(colorUnselected);
            mTabModeUnified.setTypeface(null, Typeface.NORMAL);

            mTabModeSeparate.setBackgroundResource(R.drawable.bg_tab_selected);
            mTabModeSeparate.setTextColor(colorSelected);
            mTabModeSeparate.setTypeface(null, Typeface.BOLD);

            mContainerSubTabs.setVisibility(View.VISIBLE);

            if (TAB_SIM2.equals(mCurrentSubSlot)) {
                mTabSubSim1.setBackgroundResource(R.drawable.bg_tab_unselected);
                mTabSubSim1.setTextColor(colorUnselected);
                mTabSubSim1.setTypeface(null, Typeface.NORMAL);

                mTabSubSim2.setBackgroundResource(R.drawable.bg_tab_selected);
                mTabSubSim2.setTextColor(colorSelected);
                mTabSubSim2.setTypeface(null, Typeface.BOLD);

                mTvModeHint.setText(R.string.hint_separate_sim2);
            } else {
                mTabSubSim1.setBackgroundResource(R.drawable.bg_tab_selected);
                mTabSubSim1.setTextColor(colorSelected);
                mTabSubSim1.setTypeface(null, Typeface.BOLD);

                mTabSubSim2.setBackgroundResource(R.drawable.bg_tab_unselected);
                mTabSubSim2.setTextColor(colorUnselected);
                mTabSubSim2.setTypeface(null, Typeface.NORMAL);

                mTvModeHint.setText(R.string.hint_separate_sim1);
            }
        }
    }

    private String getSlot1Key(String baseKey) {
        if (KEY_CAPSULE_STYLE.equals(baseKey)) return KEY_SIM1_CAPSULE_STYLE;
        if (KEY_5G_CA.equals(baseKey)) return KEY_SIM1_5G_CA;
        if (KEY_5G_BASIC.equals(baseKey)) return KEY_SIM1_5G_BASIC;
        if (KEY_4G_RAT.equals(baseKey)) return KEY_SIM1_4G_RAT;
        if (KEY_ALWAYS_SHOW_RAT.equals(baseKey)) return KEY_SIM1_ALWAYS_SHOW_RAT;
        if (KEY_VOLTE_STYLE.equals(baseKey)) return KEY_SIM1_VOLTE_STYLE;
        if (KEY_VOWIFI_STYLE.equals(baseKey)) return KEY_SIM1_VOWIFI_STYLE;
        if (KEY_ROAMING_STYLE.equals(baseKey)) return KEY_SIM1_ROAMING_STYLE;
        return baseKey;
    }

    private String getSlot2Key(String baseKey) {
        if (KEY_CAPSULE_STYLE.equals(baseKey)) return KEY_SIM2_CAPSULE_STYLE;
        if (KEY_5G_CA.equals(baseKey)) return KEY_SIM2_5G_CA;
        if (KEY_5G_BASIC.equals(baseKey)) return KEY_SIM2_5G_BASIC;
        if (KEY_4G_RAT.equals(baseKey)) return KEY_SIM2_4G_RAT;
        if (KEY_ALWAYS_SHOW_RAT.equals(baseKey)) return KEY_SIM2_ALWAYS_SHOW_RAT;
        if (KEY_VOLTE_STYLE.equals(baseKey)) return KEY_SIM2_VOLTE_STYLE;
        if (KEY_VOWIFI_STYLE.equals(baseKey)) return KEY_SIM2_VOWIFI_STYLE;
        if (KEY_ROAMING_STYLE.equals(baseKey)) return KEY_SIM2_ROAMING_STYLE;
        return baseKey;
    }

    private String getActivePrefKey(String baseKey) {
        if (VAL_SIM_MODE_SEPARATE.equals(mCurrentSimMode)) {
            if (TAB_SIM2.equals(mCurrentSubSlot)) {
                return getSlot2Key(baseKey);
            } else {
                return getSlot1Key(baseKey);
            }
        }
        return baseKey;
    }

    private String getEffectiveConfigValue(String baseKey) {
        String activeKey = getActivePrefKey(baseKey);
        if (mPrefs.contains(activeKey)) {
            return mPrefs.getString(activeKey, VAL_DEFAULT);
        }
        // Fallback to global setting if slot setting is not yet explicitly saved
        return mPrefs.getString(baseKey, VAL_DEFAULT);
    }

    private void saveConfigValue(String baseKey, String val) {
        SharedPreferences.Editor editor = mPrefs.edit();
        if (VAL_SIM_MODE_BOTH.equals(mCurrentSimMode)) {
            editor.putString(KEY_SIM_MODE, VAL_SIM_MODE_BOTH);
            editor.putString(baseKey, val);
            // Synchronize to slot keys so switching modes remains consistent
            editor.putString(getSlot1Key(baseKey), val);
            editor.putString(getSlot2Key(baseKey), val);
        } else {
            editor.putString(KEY_SIM_MODE, VAL_SIM_MODE_SEPARATE);
            if (TAB_SIM2.equals(mCurrentSubSlot)) {
                editor.putString(getSlot2Key(baseKey), val);
            } else {
                editor.putString(getSlot1Key(baseKey), val);
            }
        }
        editor.commit();
        fixFilePermissions();
        updateBadges();
        sendReloadBroadcast(false);
    }

    private void switchMode(String mode) {
        if (mode.equals(mCurrentSimMode)) return;
        mCurrentSimMode = mode;
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(KEY_SIM_MODE, mode);
        editor.commit();
        fixFilePermissions();

        updateTabVisuals();
        loadUiFromPrefs();
        updateBadges();
        sendReloadBroadcast(false);
    }

    private void switchSlot(String slot) {
        if (slot.equals(mCurrentSubSlot)) return;
        mCurrentSubSlot = slot;
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(KEY_TARGET_TAB, slot);
        editor.commit();
        fixFilePermissions();

        updateTabVisuals();
        loadUiFromPrefs();
        updateBadges();
        sendReloadBroadcast(false);
    }

    private void loadUiFromPrefs() {
        mIsInitializing = true;

        String capsuleVal = getEffectiveConfigValue(KEY_CAPSULE_STYLE);
        if (VAL_CAPSULE_KOREAN.equals(capsuleVal)) {
            mRgCapsuleStyle.check(R.id.rb_capsule_korean);
        } else {
            mRgCapsuleStyle.check(R.id.rb_capsule_default);
        }

        String caVal = getEffectiveConfigValue(KEY_5G_CA);
        if (VAL_5GA.equals(caVal)) {
            mRg5gCa.check(R.id.rb_ca_5ga);
        } else if (VAL_5G_PLUS.equals(caVal)) {
            mRg5gCa.check(R.id.rb_ca_5g_plus);
        } else if (VAL_5G_PLUS_PLUS.equals(caVal)) {
            mRg5gCa.check(R.id.rb_ca_5g_plus_plus);
        } else if (VAL_5GUWB.equals(caVal)) {
            mRg5gCa.check(R.id.rb_ca_5guwb);
        } else if (VAL_5GE.equals(caVal)) {
            mRg5gCa.check(R.id.rb_ca_5ge);
        } else if (VAL_5G_6RX.equals(caVal)) {
            mRg5gCa.check(R.id.rb_ca_5g_6rx);
        } else if (VAL_5G_PLUS_PLUS_6RX.equals(caVal)) {
            mRg5gCa.check(R.id.rb_ca_5g_plus_plus_6rx);
        } else if (VAL_5GUWB_6RX.equals(caVal)) {
            mRg5gCa.check(R.id.rb_ca_5guwb_6rx);
        } else {
            mRg5gCa.check(R.id.rb_ca_default);
        }

        String basicVal = getEffectiveConfigValue(KEY_5G_BASIC);
        if (VAL_5G_FOLLOW_CA.equals(basicVal)) {
            mRg5gBasic.check(R.id.rb_basic_follow_ca);
        } else {
            mRg5gBasic.check(R.id.rb_basic_default);
        }

        String ratVal = getEffectiveConfigValue(KEY_4G_RAT);
        if (VAL_4G_FORCE_4G.equals(ratVal)) {
            mRg4gRat.check(R.id.rb_4g_force_4g);
        } else if (VAL_4G_FORCE_LTE.equals(ratVal)) {
            mRg4gRat.check(R.id.rb_4g_force_lte);
        } else if (VAL_4G_FORCE_4G_LTE.equals(ratVal)) {
            mRg4gRat.check(R.id.rb_4g_force_4g_lte);
        } else if (VAL_4G_FORCE_45G.equals(ratVal)) {
            mRg4gRat.check(R.id.rb_4g_force_45g);
        } else if (VAL_4G_FORCE_LTEA.equals(ratVal)) {
            mRg4gRat.check(R.id.rb_4g_force_ltea);
        } else {
            mRg4gRat.check(R.id.rb_4g_default);
        }

        String alwaysRatVal = getEffectiveConfigValue(KEY_ALWAYS_SHOW_RAT);
        if (VAL_ALWAYS_SHOW_RAT_ALWAYS.equals(alwaysRatVal)) {
            mRgAlwaysShowRat.check(R.id.rb_always_show_rat_always);
        } else if (VAL_ALWAYS_SHOW_RAT_WIFI_HIDE.equals(alwaysRatVal)) {
            mRgAlwaysShowRat.check(R.id.rb_always_show_rat_wifi_hide);
        } else {
            mRgAlwaysShowRat.check(R.id.rb_always_show_rat_default);
        }

        String volteVal = getEffectiveConfigValue(KEY_VOLTE_STYLE);
        if (VAL_VOLTE_INTL_SOLID.equals(volteVal)) {
            mRgVolteStyle.check(R.id.rb_volte_intl_solid);
        } else if (VAL_VOLTE_INTL_4G.equals(volteVal)) {
            mRgVolteStyle.check(R.id.rb_volte_intl_4g);
        } else if (VAL_VOLTE_INTL_HOLLOW.equals(volteVal)) {
            mRgVolteStyle.check(R.id.rb_volte_intl_hollow);
        } else if (VAL_VOLTE_INTL_HD_VOICE.equals(volteVal)) {
            mRgVolteStyle.check(R.id.rb_volte_intl_hd_voice);
        } else if (VAL_VOLTE_CHINA_HD.equals(volteVal)) {
            mRgVolteStyle.check(R.id.rb_volte_china_hd);
        } else if (VAL_VOLTE_CHINA_HD_PLUS.equals(volteVal)) {
            mRgVolteStyle.check(R.id.rb_volte_china_hd_plus);
        } else if (VAL_VOLTE_INTL_VO4G.equals(volteVal)) {
            mRgVolteStyle.check(R.id.rb_volte_intl_vo4g);
        } else if (VAL_VOLTE_HIDE.equals(volteVal)) {
            mRgVolteStyle.check(R.id.rb_volte_hide);
        } else {
            mRgVolteStyle.check(R.id.rb_volte_default);
        }

        String vowifiVal = getEffectiveConfigValue(KEY_VOWIFI_STYLE);
        if (VAL_VOWIFI_STANDARD.equals(vowifiVal)) {
            mRgVowifiStyle.check(R.id.rb_vowifi_standard);
        } else if (VAL_VOWIFI_WIFI.equals(vowifiVal)) {
            mRgVowifiStyle.check(R.id.rb_vowifi_wifi);
        } else if (VAL_VOWIFI_CALL.equals(vowifiVal)) {
            mRgVowifiStyle.check(R.id.rb_vowifi_call);
        } else if (VAL_VOWIFI_HIDE.equals(vowifiVal)) {
            mRgVowifiStyle.check(R.id.rb_vowifi_hide);
        } else {
            mRgVowifiStyle.check(R.id.rb_vowifi_default);
        }

        String roamingVal = getEffectiveConfigValue(KEY_ROAMING_STYLE);
        if (VAL_ROAMING_SMALL.equals(roamingVal)) {
            mRgRoamingStyle.check(R.id.rb_roaming_small);
        } else if (VAL_ROAMING_HIDE.equals(roamingVal)) {
            mRgRoamingStyle.check(R.id.rb_roaming_hide);
        } else {
            mRgRoamingStyle.check(R.id.rb_roaming_default);
        }

        mIsInitializing = false;
    }

    private void setupListeners() {
        mTabModeUnified.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchMode(VAL_SIM_MODE_BOTH);
            }
        });

        mTabModeSeparate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchMode(VAL_SIM_MODE_SEPARATE);
            }
        });

        mTabSubSim1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchSlot(TAB_SIM1);
            }
        });

        mTabSubSim2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchSlot(TAB_SIM2);
            }
        });

        mRgCapsuleStyle.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (mIsInitializing) return;
                String val = (checkedId == R.id.rb_capsule_korean) ? VAL_CAPSULE_KOREAN : VAL_CAPSULE_DEFAULT;
                saveConfigValue(KEY_CAPSULE_STYLE, val);
            }
        });

        mRg5gCa.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (mIsInitializing) return;
                String val = VAL_DEFAULT;
                if (checkedId == R.id.rb_ca_5ga) val = VAL_5GA;
                else if (checkedId == R.id.rb_ca_5g_plus) val = VAL_5G_PLUS;
                else if (checkedId == R.id.rb_ca_5g_plus_plus) val = VAL_5G_PLUS_PLUS;
                else if (checkedId == R.id.rb_ca_5guwb) val = VAL_5GUWB;
                else if (checkedId == R.id.rb_ca_5ge) val = VAL_5GE;
                else if (checkedId == R.id.rb_ca_5g_6rx) val = VAL_5G_6RX;
                else if (checkedId == R.id.rb_ca_5g_plus_plus_6rx) val = VAL_5G_PLUS_PLUS_6RX;
                else if (checkedId == R.id.rb_ca_5guwb_6rx) val = VAL_5GUWB_6RX;

                saveConfigValue(KEY_5G_CA, val);
            }
        });

        mRg5gBasic.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (mIsInitializing) return;
                String val = (checkedId == R.id.rb_basic_follow_ca) ? VAL_5G_FOLLOW_CA : VAL_DEFAULT;
                saveConfigValue(KEY_5G_BASIC, val);
            }
        });

        mRg4gRat.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (mIsInitializing) return;
                String val = VAL_DEFAULT;
                if (checkedId == R.id.rb_4g_force_4g) val = VAL_4G_FORCE_4G;
                else if (checkedId == R.id.rb_4g_force_lte) val = VAL_4G_FORCE_LTE;
                else if (checkedId == R.id.rb_4g_force_4g_lte) val = VAL_4G_FORCE_4G_LTE;
                else if (checkedId == R.id.rb_4g_force_45g) val = VAL_4G_FORCE_45G;
                else if (checkedId == R.id.rb_4g_force_ltea) val = VAL_4G_FORCE_LTEA;

                saveConfigValue(KEY_4G_RAT, val);
            }
        });

        mRgAlwaysShowRat.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (mIsInitializing) return;
                String val = VAL_DEFAULT;
                if (checkedId == R.id.rb_always_show_rat_always) val = VAL_ALWAYS_SHOW_RAT_ALWAYS;
                else if (checkedId == R.id.rb_always_show_rat_wifi_hide) val = VAL_ALWAYS_SHOW_RAT_WIFI_HIDE;

                saveConfigValue(KEY_ALWAYS_SHOW_RAT, val);
            }
        });

        mRgVolteStyle.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (mIsInitializing) return;
                String val = VAL_DEFAULT;
                if (checkedId == R.id.rb_volte_intl_solid) val = VAL_VOLTE_INTL_SOLID;
                else if (checkedId == R.id.rb_volte_intl_4g) val = VAL_VOLTE_INTL_4G;
                else if (checkedId == R.id.rb_volte_intl_hollow) val = VAL_VOLTE_INTL_HOLLOW;
                else if (checkedId == R.id.rb_volte_intl_hd_voice) val = VAL_VOLTE_INTL_HD_VOICE;
                else if (checkedId == R.id.rb_volte_china_hd) val = VAL_VOLTE_CHINA_HD;
                else if (checkedId == R.id.rb_volte_china_hd_plus) val = VAL_VOLTE_CHINA_HD_PLUS;
                else if (checkedId == R.id.rb_volte_intl_vo4g) val = VAL_VOLTE_INTL_VO4G;
                else if (checkedId == R.id.rb_volte_hide) val = VAL_VOLTE_HIDE;

                saveConfigValue(KEY_VOLTE_STYLE, val);
            }
        });

        mRgVowifiStyle.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (mIsInitializing) return;
                String val = VAL_DEFAULT;
                if (checkedId == R.id.rb_vowifi_standard) val = VAL_VOWIFI_STANDARD;
                else if (checkedId == R.id.rb_vowifi_wifi) val = VAL_VOWIFI_WIFI;
                else if (checkedId == R.id.rb_vowifi_call) val = VAL_VOWIFI_CALL;
                else if (checkedId == R.id.rb_vowifi_hide) val = VAL_VOWIFI_HIDE;

                saveConfigValue(KEY_VOWIFI_STYLE, val);
            }
        });

        mRgRoamingStyle.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (mIsInitializing) return;
                String val = VAL_DEFAULT;
                if (checkedId == R.id.rb_roaming_small) val = VAL_ROAMING_SMALL;
                else if (checkedId == R.id.rb_roaming_hide) val = VAL_ROAMING_HIDE;

                saveConfigValue(KEY_ROAMING_STYLE, val);
            }
        });

        mBtnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendReloadBroadcast(true);
            }
        });

        mBtnRestartSysui.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartSystemUi();
            }
        });
    }

    private void restartSystemUi() {
        Toast.makeText(this, R.string.toast_restart_sysui_requesting, Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                try {
                    Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "pkill -f com.android.systemui"});
                    int exitCode = process.waitFor();
                    success = (exitCode == 0);
                } catch (Throwable t) {
                    success = false;
                }
                final boolean finalSuccess = success;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (finalSuccess) {
                            Toast.makeText(MainActivity.this, R.string.toast_restart_sysui_success, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, R.string.toast_restart_sysui_failed, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void sendReloadBroadcast(boolean showToast) {
        String simMode = mPrefs.getString(KEY_SIM_MODE, VAL_SIM_MODE_BOTH);

        String capsuleVal = mPrefs.getString(KEY_CAPSULE_STYLE, VAL_DEFAULT);
        String caVal = mPrefs.getString(KEY_5G_CA, VAL_DEFAULT);
        String basicVal = mPrefs.getString(KEY_5G_BASIC, VAL_DEFAULT);
        String ratVal = mPrefs.getString(KEY_4G_RAT, VAL_DEFAULT);
        String alwaysRatVal = mPrefs.getString(KEY_ALWAYS_SHOW_RAT, VAL_DEFAULT);
        String volteVal = mPrefs.getString(KEY_VOLTE_STYLE, VAL_DEFAULT);
        String vowifiVal = mPrefs.getString(KEY_VOWIFI_STYLE, VAL_DEFAULT);
        String roamingVal = mPrefs.getString(KEY_ROAMING_STYLE, VAL_DEFAULT);

        try {
            getContentResolver().notifyChange(IconConfigProvider.CONTENT_URI, null);
        } catch (Throwable ignored) {}

        Intent intent = new Intent(ACTION_RELOAD);
        intent.setPackage("com.android.systemui");
        intent.putExtra(KEY_SIM_MODE, simMode);
        intent.putExtra(KEY_TARGET_TAB, mCurrentSubSlot);

        intent.putExtra(KEY_CAPSULE_STYLE, capsuleVal);
        intent.putExtra(KEY_5G_CA, caVal);
        intent.putExtra(KEY_5G_BASIC, basicVal);
        intent.putExtra(KEY_4G_RAT, ratVal);
        intent.putExtra(KEY_ALWAYS_SHOW_RAT, alwaysRatVal);
        intent.putExtra(KEY_VOLTE_STYLE, volteVal);
        intent.putExtra(KEY_VOWIFI_STYLE, vowifiVal);
        intent.putExtra(KEY_ROAMING_STYLE, roamingVal);

        // Put SIM 1 keys
        intent.putExtra(KEY_SIM1_CAPSULE_STYLE, mPrefs.getString(KEY_SIM1_CAPSULE_STYLE, capsuleVal));
        intent.putExtra(KEY_SIM1_5G_CA, mPrefs.getString(KEY_SIM1_5G_CA, caVal));
        intent.putExtra(KEY_SIM1_5G_BASIC, mPrefs.getString(KEY_SIM1_5G_BASIC, basicVal));
        intent.putExtra(KEY_SIM1_4G_RAT, mPrefs.getString(KEY_SIM1_4G_RAT, ratVal));
        intent.putExtra(KEY_SIM1_ALWAYS_SHOW_RAT, mPrefs.getString(KEY_SIM1_ALWAYS_SHOW_RAT, alwaysRatVal));
        intent.putExtra(KEY_SIM1_VOLTE_STYLE, mPrefs.getString(KEY_SIM1_VOLTE_STYLE, volteVal));
        intent.putExtra(KEY_SIM1_VOWIFI_STYLE, mPrefs.getString(KEY_SIM1_VOWIFI_STYLE, vowifiVal));
        intent.putExtra(KEY_SIM1_ROAMING_STYLE, mPrefs.getString(KEY_SIM1_ROAMING_STYLE, roamingVal));

        // Put SIM 2 keys
        intent.putExtra(KEY_SIM2_CAPSULE_STYLE, mPrefs.getString(KEY_SIM2_CAPSULE_STYLE, capsuleVal));
        intent.putExtra(KEY_SIM2_5G_CA, mPrefs.getString(KEY_SIM2_5G_CA, caVal));
        intent.putExtra(KEY_SIM2_5G_BASIC, mPrefs.getString(KEY_SIM2_5G_BASIC, basicVal));
        intent.putExtra(KEY_SIM2_4G_RAT, mPrefs.getString(KEY_SIM2_4G_RAT, ratVal));
        intent.putExtra(KEY_SIM2_ALWAYS_SHOW_RAT, mPrefs.getString(KEY_SIM2_ALWAYS_SHOW_RAT, alwaysRatVal));
        intent.putExtra(KEY_SIM2_VOLTE_STYLE, mPrefs.getString(KEY_SIM2_VOLTE_STYLE, volteVal));
        intent.putExtra(KEY_SIM2_VOWIFI_STYLE, mPrefs.getString(KEY_SIM2_VOWIFI_STYLE, vowifiVal));
        intent.putExtra(KEY_SIM2_ROAMING_STYLE, mPrefs.getString(KEY_SIM2_ROAMING_STYLE, roamingVal));

        sendBroadcast(intent);

        // Also broadcast implicitly
        Intent implicitIntent = new Intent(intent);
        implicitIntent.setPackage(null);
        sendBroadcast(implicitIntent);

        if (showToast) {
            Toast.makeText(this, R.string.toast_refresh_success, Toast.LENGTH_SHORT).show();
        }
    }

    public static SharedPreferences getSafePrefs(Context context) {
        if (context == null) return null;
        Context deContext = context.isDeviceProtectedStorage() ? context : context.createDeviceProtectedStorageContext();
        try {
            deContext.moveSharedPreferencesFrom(context, PREF_NAME);
        } catch (Throwable ignored) {}
        return deContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void fixPermissions(Context context) {
        if (context == null) return;
        try {
            Context deContext = context.isDeviceProtectedStorage() ? context : context.createDeviceProtectedStorageContext();
            File deDir = deContext.getDataDir();
            if (deDir != null) {
                deDir.setReadable(true, false);
                deDir.setExecutable(true, false);
                File prefsDir = new File(deDir, "shared_prefs");
                if (prefsDir.exists()) {
                    prefsDir.setReadable(true, false);
                    prefsDir.setExecutable(true, false);
                }
                File prefsFile = new File(prefsDir, PREF_NAME + ".xml");
                if (prefsFile.exists()) {
                    prefsFile.setReadable(true, false);
                }
            }
        } catch (Throwable ignored) {}

        try {
            File ceDir = context.getDataDir();
            if (ceDir != null) {
                ceDir.setReadable(true, false);
                ceDir.setExecutable(true, false);
                File prefsDir = new File(ceDir, "shared_prefs");
                if (prefsDir.exists()) {
                    prefsDir.setReadable(true, false);
                    prefsDir.setExecutable(true, false);
                }
                File prefsFile = new File(prefsDir, PREF_NAME + ".xml");
                if (prefsFile.exists()) {
                    prefsFile.setReadable(true, false);
                }
            }
        } catch (Throwable ignored) {}
    }

    private void fixFilePermissions() {
        fixPermissions(this);
    }

    private void updateStatusBanner() {
        if (mTvStatusText == null || mDotStatus == null) return;
        long lastPing = mPrefs.getLong("last_systemui_ping", 0);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        int dotPx = (int) (10 * getResources().getDisplayMetrics().density);
        dot.setSize(dotPx, dotPx);

        if (lastPing > 0) {
            dot.setColor(0xFF34C759); // Emerald active green
            mDotStatus.setBackground(dot);
            mTvStatusText.setText(R.string.status_active_desc);
            mTvStatusText.setTextColor(getColor(R.color.text_body));
        } else {
            dot.setColor(0xFF8E8E93); // System standby gray
            mDotStatus.setBackground(dot);
            mTvStatusText.setText(R.string.status_inactive_desc);
            mTvStatusText.setTextColor(getColor(R.color.text_caption));
        }
    }

    private void updateBadges() {
        String capsuleVal = getEffectiveConfigValue(KEY_CAPSULE_STYLE);
        if (mBadgeCapsuleStyle != null) {
            String text = VAL_CAPSULE_KOREAN.equals(capsuleVal) ? getString(R.string.opt_capsule_korean) : getString(R.string.opt_capsule_default);
            mBadgeCapsuleStyle.setText("[" + text + "]");
        }

        String caVal = getEffectiveConfigValue(KEY_5G_CA);
        if (mBadge5gCa != null) {
            String text = getString(R.string.opt_default);
            if (VAL_5GA.equals(caVal)) text = "5GA";
            else if (VAL_5G_PLUS.equals(caVal)) text = "5G+";
            else if (VAL_5G_PLUS_PLUS.equals(caVal)) text = "5G++";
            else if (VAL_5GUWB.equals(caVal)) text = "5GUWB";
            else if (VAL_5GE.equals(caVal)) text = "5Ge";
            else if (VAL_5G_6RX.equals(caVal)) text = "5G 6Rx";
            else if (VAL_5G_PLUS_PLUS_6RX.equals(caVal)) text = "5G++ 6Rx";
            else if (VAL_5GUWB_6RX.equals(caVal)) text = "5GUWB 6Rx";
            mBadge5gCa.setText("[" + text + "]");
        }

        String basicVal = getEffectiveConfigValue(KEY_5G_BASIC);
        if (mBadge5gBasic != null) {
            String text = VAL_5G_FOLLOW_CA.equals(basicVal) ? getString(R.string.opt_5g_follow_ca) : getString(R.string.opt_5g_default);
            mBadge5gBasic.setText("[" + text + "]");
        }

        String ratVal = getEffectiveConfigValue(KEY_4G_RAT);
        if (mBadge4gRat != null) {
            String text = getString(R.string.opt_4g_default);
            if (VAL_4G_FORCE_4G.equals(ratVal)) text = "4G / 4G+";
            else if (VAL_4G_FORCE_LTE.equals(ratVal)) text = "LTE / LTE+";
            else if (VAL_4G_FORCE_4G_LTE.equals(ratVal)) text = "4G LTE";
            else if (VAL_4G_FORCE_45G.equals(ratVal)) text = "4.5G";
            else if (VAL_4G_FORCE_LTEA.equals(ratVal)) text = "LTE-A";
            mBadge4gRat.setText("[" + text + "]");
        }

        String alwaysRatVal = getEffectiveConfigValue(KEY_ALWAYS_SHOW_RAT);
        if (mBadgeAlwaysShowRat != null) {
            String text = getString(R.string.opt_always_show_rat_default);
            if (VAL_ALWAYS_SHOW_RAT_ALWAYS.equals(alwaysRatVal)) text = getString(R.string.opt_always_show_rat_always);
            else if (VAL_ALWAYS_SHOW_RAT_WIFI_HIDE.equals(alwaysRatVal)) text = getString(R.string.opt_always_show_rat_wifi_hide);
            mBadgeAlwaysShowRat.setText("[" + text + "]");
        }

        String volteVal = getEffectiveConfigValue(KEY_VOLTE_STYLE);
        if (mBadgeVolteStyle != null) {
            String text = getString(R.string.opt_volte_default);
            if (VAL_VOLTE_INTL_SOLID.equals(volteVal)) text = getString(R.string.opt_volte_intl_solid);
            else if (VAL_VOLTE_INTL_4G.equals(volteVal)) text = "VoLTE 4G";
            else if (VAL_VOLTE_INTL_HOLLOW.equals(volteVal)) text = getString(R.string.opt_volte_intl_hollow);
            else if (VAL_VOLTE_INTL_HD_VOICE.equals(volteVal)) text = "HD Voice";
            else if (VAL_VOLTE_CHINA_HD.equals(volteVal)) text = getString(R.string.opt_volte_china_hd);
            else if (VAL_VOLTE_CHINA_HD_PLUS.equals(volteVal)) text = getString(R.string.opt_volte_china_hd_plus);
            else if (VAL_VOLTE_INTL_VO4G.equals(volteVal)) text = "Vo4G";
            else if (VAL_VOLTE_HIDE.equals(volteVal)) text = getString(R.string.opt_volte_hide);
            mBadgeVolteStyle.setText("[" + text + "]");
        }

        String vowifiVal = getEffectiveConfigValue(KEY_VOWIFI_STYLE);
        if (mBadgeVowifiStyle != null) {
            String text = getString(R.string.opt_vowifi_default);
            if (VAL_VOWIFI_STANDARD.equals(vowifiVal)) text = getString(R.string.opt_vowifi_standard);
            else if (VAL_VOWIFI_WIFI.equals(vowifiVal)) text = getString(R.string.opt_vowifi_wifi);
            else if (VAL_VOWIFI_CALL.equals(vowifiVal)) text = getString(R.string.opt_vowifi_call);
            else if (VAL_VOWIFI_HIDE.equals(vowifiVal)) text = getString(R.string.opt_vowifi_hide);
            mBadgeVowifiStyle.setText("[" + text + "]");
        }

        String roamingVal = getEffectiveConfigValue(KEY_ROAMING_STYLE);
        if (mBadgeRoamingStyle != null) {
            String text = getString(R.string.opt_roaming_default);
            if (VAL_ROAMING_SMALL.equals(roamingVal)) text = getString(R.string.opt_roaming_small);
            else if (VAL_ROAMING_HIDE.equals(roamingVal)) text = getString(R.string.opt_roaming_hide);
            mBadgeRoamingStyle.setText("[" + text + "]");
        }
    }
}
