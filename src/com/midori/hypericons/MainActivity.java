package com.midori.hypericons;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity {
    public static final String VERSION_NAME = "6.0.0";
    public static final int VERSION_CODE = 32;
    public static final String PREF_NAME = "hyper_icons_config";
    public static final String ACTION_RELOAD = "com.midori.hypericons.ACTION_RELOAD";
    public static final String PROJECT_URL = "https://github.com/midori01/HyperIcons";

    // Mode & Tabs
    public static final String KEY_SIM_MODE = ConfigKeys.KEY_SIM_MODE;           // "both" | "separate"
    public static final String KEY_TARGET_TAB = ConfigKeys.KEY_TARGET_TAB;       // "sim1" | "sim2"

    public static final String VAL_SIM_MODE_BOTH = "both";
    public static final String VAL_SIM_MODE_SEPARATE = "separate";
    public static final String TAB_BOTH = "both";
    public static final String TAB_SIM1 = "sim1";
    public static final String TAB_SIM2 = "sim2";

    // Per-SIM option keys (see ConfigKeys for the slot overrides)
    public static final String KEY_5G_CA = ConfigKeys.KEY_5G_CA;
    public static final String KEY_5G_BASIC = ConfigKeys.KEY_5G_BASIC;
    public static final String KEY_CAPSULE_STYLE = ConfigKeys.KEY_CAPSULE_STYLE;
    public static final String KEY_4G_RAT = ConfigKeys.KEY_4G_RAT;
    public static final String KEY_ALWAYS_SHOW_RAT = ConfigKeys.KEY_ALWAYS_SHOW_RAT;
    public static final String KEY_VOLTE_STYLE = ConfigKeys.KEY_VOLTE_STYLE;
    public static final String KEY_VOWIFI_STYLE = ConfigKeys.KEY_VOWIFI_STYLE;
    public static final String KEY_ROAMING_STYLE = ConfigKeys.KEY_ROAMING_STYLE;
    public static final String KEY_VONR_VISIBILITY = ConfigKeys.KEY_VONR_VISIBILITY;
    public static final String KEY_WIFI_VOLTE_VISIBILITY = ConfigKeys.KEY_WIFI_VOLTE_VISIBILITY;

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
    public static final String VAL_ALWAYS_SHOW_RAT_DATA_SIM = "data_sim";

    public static final String VAL_VOLTE_INTL_SOLID = "intl_solid";
    public static final String VAL_VOLTE_INTL_4G = "intl_4g";
    public static final String VAL_VOLTE_INTL_HOLLOW = "intl_hollow";
    public static final String VAL_VOLTE_INTL_HD_VOICE = "intl_hd_voice";
    public static final String VAL_VOLTE_CHINA_HD = "china_hd";
    public static final String VAL_VOLTE_CHINA_HD_PLUS = "china_hd_plus";
    public static final String VAL_VOLTE_INTL_VO4G = "intl_vo4g";
    public static final String VAL_VOLTE_HIDE = "hide";

    public static final String VAL_WIFI_VOLTE_DEFAULT = "default";
    public static final String VAL_WIFI_VOLTE_SHOW = "show";
    public static final String VAL_WIFI_VOLTE_HIDE = "hide";

    public static final String VAL_VONR_VISIBILITY_DEFAULT = "default";
    public static final String VAL_VONR_VISIBILITY_SHOW = "show";
    public static final String VAL_VONR_VISIBILITY_HIDE = "hide";

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
    private RadioGroup mRgWifiVolte;
    private RadioGroup mRgVonrVisibility;
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
    private TextView mTvStatusDetail;

    private final SharedPreferences.OnSharedPreferenceChangeListener mReportListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    if (key != null && key.startsWith(IconConfigProvider.KEY_HOOK_REPORT_PREFIX)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateStatusBanner();
                            }
                        });
                    }
                }
            };
    private TextView mBadgeCapsuleStyle;
    private TextView mBadge5gCa;
    private TextView mBadge5gBasic;
    private TextView mBadge4gRat;
    private TextView mBadgeAlwaysShowRat;
    private TextView mBadgeVolteStyle;
    private TextView mBadgeWifiVolte;
    private TextView mBadgeVonrVisibility;
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
        mTvStatusDetail = findViewById(R.id.tv_status_detail);
        View statusCard = findViewById(R.id.card_status_banner);
        if (statusCard != null) {
            statusCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showHookStatusDialog();
                }
            });
        }
        mBadgeCapsuleStyle = findViewById(R.id.badge_capsule_style);
        mBadge5gCa = findViewById(R.id.badge_5g_ca);
        mBadge5gBasic = findViewById(R.id.badge_5g_basic);
        mBadge4gRat = findViewById(R.id.badge_4g_rat);
        mBadgeAlwaysShowRat = findViewById(R.id.badge_always_show_rat);
        mBadgeVolteStyle = findViewById(R.id.badge_volte_style);
        mBadgeWifiVolte = findViewById(R.id.badge_wifi_volte);
        mBadgeVonrVisibility = findViewById(R.id.badge_vonr_visibility);
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
        mRgWifiVolte = findViewById(R.id.rg_wifi_volte);
        mRgVonrVisibility = findViewById(R.id.rg_vonr_visibility);
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
        setupCollapsibleCards();

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
        mPrefs.registerOnSharedPreferenceChangeListener(mReportListener);
        requestHookReports();
    }

    /** Reports older than this request are treated as stale (e.g. the module was disabled since). */
    private long mReportRequestedAt = 0;
    private boolean mReportWaitOver = false;
    private static final long REPORT_TIMEOUT_MS = 4000;
    private static final long CLOCK_SLACK_MS = 1000;

    private void requestHookReports() {
        mReportRequestedAt = System.currentTimeMillis();
        mReportWaitOver = false;
        Intent sysui = new Intent(HookEnv.ACTION_REQUEST_REPORT);
        sysui.setPackage("com.android.systemui");
        sendBroadcast(sysui);
        Intent phone = new Intent(HookEnv.ACTION_REQUEST_REPORT);
        phone.setPackage("com.android.phone");
        sendBroadcast(phone);
        updateStatusBanner();
        final long requestedAt = mReportRequestedAt;
        getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (requestedAt != mReportRequestedAt) return;
                mReportWaitOver = true;
                updateStatusBanner();
            }
        }, REPORT_TIMEOUT_MS);
    }

    private HookReport freshReport(String process) {
        HookReport r = HookReport.parse(mPrefs.getString(IconConfigProvider.KEY_HOOK_REPORT_PREFIX + process, null));
        return r != null && r.time >= mReportRequestedAt - CLOCK_SLACK_MS ? r : null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener(mReportListener);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /**
     * Option cards start collapsed to their title and current value; tapping the
     * title (or a collapsed card) expands the options.
     */
    private void setupCollapsibleCards() {
        View content = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        if (!(content instanceof ScrollView)) return;
        View column = ((ScrollView) content).getChildAt(0);
        if (!(column instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) column;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof LinearLayout && hasRadioGroup((LinearLayout) child)) {
                makeCollapsible((LinearLayout) child);
            }
        }
    }

    private static boolean hasRadioGroup(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            if (group.getChildAt(i) instanceof RadioGroup) return true;
        }
        return false;
    }

    private void makeCollapsible(final LinearLayout card) {
        View first = card.getChildAt(0);
        if (!(first instanceof LinearLayout)) return;
        final LinearLayout header = (LinearLayout) first;
        header.setGravity(Gravity.CENTER_VERTICAL);
        final ImageView chevron = new ImageView(this);
        chevron.setImageResource(R.drawable.ic_chevron);
        chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(22), dp(22));
        lp.setMarginStart(dp(6));
        header.addView(chevron, lp);

        LayoutTransition transition = new LayoutTransition();
        transition.enableTransitionType(LayoutTransition.CHANGING);
        card.setLayoutTransition(transition);
        setCardExpanded(card, chevron, false);

        header.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCardExpanded(card, chevron, !isCardExpanded(card));
            }
        });
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isCardExpanded(card)) setCardExpanded(card, chevron, true);
            }
        });
    }

    private static boolean isCardExpanded(LinearLayout card) {
        return card.getChildCount() > 1 && card.getChildAt(1).getVisibility() == View.VISIBLE;
    }

    private static void setCardExpanded(LinearLayout card, ImageView chevron, boolean expanded) {
        for (int i = 1; i < card.getChildCount(); i++) {
            card.getChildAt(i).setVisibility(expanded ? View.VISIBLE : View.GONE);
        }
        chevron.animate().rotation(expanded ? 180f : 0f).setDuration(200).start();
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            card.getChildAt(0).setStateDescription(card.getContext().getString(
                    expanded ? R.string.card_state_expanded : R.string.card_state_collapsed));
        }
    }

    private void showHookStatusDialog() {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        HookReport sys = HookReport.parse(mPrefs.getString(IconConfigProvider.KEY_HOOK_REPORT_PREFIX + "systemui", null));
        HookReport phone = HookReport.parse(mPrefs.getString(IconConfigProvider.KEY_HOOK_REPORT_PREFIX + "phone", null));
        HookReport any = sys != null ? sys : phone;
        if (any != null) {
            sb.append(getString(R.string.dialog_hooks_framework, any.framework, any.frameworkVersion, any.apiVersion));
            sb.append("\n");
        }
        appendReport(sb, getString(R.string.dialog_hooks_systemui), sys);
        appendReport(sb, getString(R.string.dialog_hooks_phone), phone);
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_hooks_title)
                .setMessage(sb)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void appendReport(SpannableStringBuilder sb, String process, HookReport report) {
        sb.append("\n");
        int start = sb.length();
        sb.append(process);
        sb.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append("\n");
        if (report == null) {
            sb.append(getString(R.string.dialog_hooks_not_loaded)).append("\n");
            return;
        }
        int okColor = getColor(R.color.status_ok);
        int failColor = getColor(R.color.status_error);
        for (HookReport.Entry e : report.groups) {
            int markStart = sb.length();
            sb.append(e.failure == null ? "✓ " : "✗ ");
            sb.setSpan(new ForegroundColorSpan(e.failure == null ? okColor : failColor),
                    markStart, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append(e.name);
            if (e.failure != null) sb.append(" — ").append(e.failure);
            sb.append("\n");
        }
        sb.append(getString(R.string.dialog_hooks_points,
                HookReport.countOk(report.points), report.points.size())).append("\n");
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
        return ConfigKeys.slotKey(baseKey, 0);
    }

    private String getSlot2Key(String baseKey) {
        return ConfigKeys.slotKey(baseKey, 1);
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
        } else if (VAL_ALWAYS_SHOW_RAT_DATA_SIM.equals(alwaysRatVal)) {
            mRgAlwaysShowRat.check(R.id.rb_always_show_rat_data_sim);
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

        String wifiVolteVal = getEffectiveConfigValue(KEY_WIFI_VOLTE_VISIBILITY);
        if (VAL_WIFI_VOLTE_SHOW.equals(wifiVolteVal)) {
            mRgWifiVolte.check(R.id.rb_wifi_volte_show);
        } else if (VAL_WIFI_VOLTE_HIDE.equals(wifiVolteVal)) {
            mRgWifiVolte.check(R.id.rb_wifi_volte_hide);
        } else {
            mRgWifiVolte.check(R.id.rb_wifi_volte_default);
        }

        String vonrVisVal = getEffectiveConfigValue(KEY_VONR_VISIBILITY);
        if (VAL_VONR_VISIBILITY_SHOW.equals(vonrVisVal)) {
            mRgVonrVisibility.check(R.id.rb_vonr_visibility_show);
        } else if (VAL_VONR_VISIBILITY_HIDE.equals(vonrVisVal)) {
            mRgVonrVisibility.check(R.id.rb_vonr_visibility_hide);
        } else {
            mRgVonrVisibility.check(R.id.rb_vonr_visibility_default);
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
                else if (checkedId == R.id.rb_always_show_rat_data_sim) val = VAL_ALWAYS_SHOW_RAT_DATA_SIM;
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

        mRgWifiVolte.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (mIsInitializing) return;
                String val = VAL_WIFI_VOLTE_DEFAULT;
                if (checkedId == R.id.rb_wifi_volte_show) val = VAL_WIFI_VOLTE_SHOW;
                else if (checkedId == R.id.rb_wifi_volte_hide) val = VAL_WIFI_VOLTE_HIDE;

                saveConfigValue(KEY_WIFI_VOLTE_VISIBILITY, val);
            }
        });

        mRgVonrVisibility.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (mIsInitializing) return;
                String val = VAL_VONR_VISIBILITY_DEFAULT;
                if (checkedId == R.id.rb_vonr_visibility_show) val = VAL_VONR_VISIBILITY_SHOW;
                else if (checkedId == R.id.rb_vonr_visibility_hide) val = VAL_VONR_VISIBILITY_HIDE;

                saveConfigValue(KEY_VONR_VISIBILITY, val);
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

        View projectLink = findViewById(R.id.tv_project_link);
        if (projectLink != null) {
            projectLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openProjectPage();
                }
            });
        }
    }

    private void openProjectPage() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_URL)));
        } catch (Throwable t) {
            Toast.makeText(this, PROJECT_URL, Toast.LENGTH_LONG).show();
        }
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
        try {
            getContentResolver().notifyChange(IconConfigProvider.CONTENT_URI, null);
        } catch (Throwable ignored) {}

        Intent intent = new Intent(ACTION_RELOAD);
        intent.setPackage("com.android.systemui");

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
        HookReport sys = freshReport("systemui");
        HookReport phone = freshReport("phone");

        int color;
        String title;
        String detail;
        if (sys == null && !mReportWaitOver) {
            color = R.color.status_off;
            title = getString(R.string.status_title_checking);
            detail = "";
        } else if (sys == null) {
            // No answer to this request: the module is disabled, or SystemUI runs a build without reports.
            color = R.color.status_off;
            title = getString(R.string.status_title_inactive);
            detail = getString(R.string.status_detail_inactive);
        } else if (!VERSION_NAME.equals(sys.moduleVersion)) {
            color = R.color.status_warn;
            title = getString(R.string.status_title_restart);
            detail = getString(R.string.status_detail_restart, sys.moduleVersion, VERSION_NAME);
        } else if (phone != null && !VERSION_NAME.equals(phone.moduleVersion)) {
            color = R.color.status_warn;
            title = getString(R.string.status_title_restart_phone);
            detail = getString(R.string.status_detail_restart, phone.moduleVersion, VERSION_NAME);
        } else {
            int sysOk = HookReport.countOk(sys.groups);
            int sysAll = sys.groups.size();
            boolean healthy = sysAll > 0 && sysOk == sysAll;
            if (phone != null) {
                int phoneOk = HookReport.countOk(phone.groups);
                int phoneAll = phone.groups.size();
                healthy = healthy && phoneAll > 0 && phoneOk == phoneAll;
                detail = getString(R.string.status_detail_processes, sysOk, sysAll, phoneOk, phoneAll);
            } else {
                detail = getString(R.string.status_detail_phone_missing, sysOk, sysAll);
            }
            color = healthy ? R.color.status_ok : R.color.status_warn;
            title = getString(healthy ? R.string.status_title_ok : R.string.status_title_partial);
        }

        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(getColor(color));
        mDotStatus.setBackground(dot);
        mTvStatusText.setText(title);
        if (mTvStatusDetail != null) mTvStatusDetail.setText(detail);
    }

    private void updateBadges() {
        String capsuleVal = getEffectiveConfigValue(KEY_CAPSULE_STYLE);
        if (mBadgeCapsuleStyle != null) {
            String text = VAL_CAPSULE_KOREAN.equals(capsuleVal) ? getString(R.string.opt_capsule_korean) : getString(R.string.opt_capsule_default);
            mBadgeCapsuleStyle.setText(text);
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
            mBadge5gCa.setText(text);
        }

        String basicVal = getEffectiveConfigValue(KEY_5G_BASIC);
        if (mBadge5gBasic != null) {
            String text = VAL_5G_FOLLOW_CA.equals(basicVal) ? getString(R.string.opt_5g_follow_ca) : getString(R.string.opt_5g_default);
            mBadge5gBasic.setText(text);
        }

        String ratVal = getEffectiveConfigValue(KEY_4G_RAT);
        if (mBadge4gRat != null) {
            String text = getString(R.string.opt_4g_default);
            if (VAL_4G_FORCE_4G.equals(ratVal)) text = "4G / 4G+";
            else if (VAL_4G_FORCE_LTE.equals(ratVal)) text = "LTE / LTE+";
            else if (VAL_4G_FORCE_4G_LTE.equals(ratVal)) text = "4G LTE";
            else if (VAL_4G_FORCE_45G.equals(ratVal)) text = "4.5G";
            else if (VAL_4G_FORCE_LTEA.equals(ratVal)) text = "LTE-A";
            mBadge4gRat.setText(text);
        }

        String alwaysRatVal = getEffectiveConfigValue(KEY_ALWAYS_SHOW_RAT);
        if (mBadgeAlwaysShowRat != null) {
            String text = getString(R.string.opt_always_show_rat_default);
            if (VAL_ALWAYS_SHOW_RAT_ALWAYS.equals(alwaysRatVal)) text = getString(R.string.opt_always_show_rat_always);
            else if (VAL_ALWAYS_SHOW_RAT_DATA_SIM.equals(alwaysRatVal)) text = getString(R.string.opt_always_show_rat_data_sim);
            else if (VAL_ALWAYS_SHOW_RAT_WIFI_HIDE.equals(alwaysRatVal)) text = getString(R.string.opt_always_show_rat_wifi_hide);
            mBadgeAlwaysShowRat.setText(text);
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
            mBadgeVolteStyle.setText(text);
        }

        String wifiVolteVal = getEffectiveConfigValue(KEY_WIFI_VOLTE_VISIBILITY);
        if (mBadgeWifiVolte != null) {
            String text = getString(R.string.opt_wifi_volte_default);
            if (VAL_WIFI_VOLTE_SHOW.equals(wifiVolteVal)) text = getString(R.string.opt_wifi_volte_show);
            else if (VAL_WIFI_VOLTE_HIDE.equals(wifiVolteVal)) text = getString(R.string.opt_wifi_volte_hide);
            mBadgeWifiVolte.setText(text);
        }

        String vonrVisVal = getEffectiveConfigValue(KEY_VONR_VISIBILITY);
        if (mBadgeVonrVisibility != null) {
            String text = getString(R.string.opt_vonr_visibility_default);
            if (VAL_VONR_VISIBILITY_SHOW.equals(vonrVisVal)) text = getString(R.string.opt_vonr_visibility_show);
            else if (VAL_VONR_VISIBILITY_HIDE.equals(vonrVisVal)) text = getString(R.string.opt_vonr_visibility_hide);
            mBadgeVonrVisibility.setText(text);
        }

        String vowifiVal = getEffectiveConfigValue(KEY_VOWIFI_STYLE);
        if (mBadgeVowifiStyle != null) {
            String text = getString(R.string.opt_vowifi_default);
            if (VAL_VOWIFI_STANDARD.equals(vowifiVal)) text = getString(R.string.opt_vowifi_standard);
            else if (VAL_VOWIFI_WIFI.equals(vowifiVal)) text = getString(R.string.opt_vowifi_wifi);
            else if (VAL_VOWIFI_CALL.equals(vowifiVal)) text = getString(R.string.opt_vowifi_call);
            else if (VAL_VOWIFI_HIDE.equals(vowifiVal)) text = getString(R.string.opt_vowifi_hide);
            mBadgeVowifiStyle.setText(text);
        }

        String roamingVal = getEffectiveConfigValue(KEY_ROAMING_STYLE);
        if (mBadgeRoamingStyle != null) {
            String text = getString(R.string.opt_roaming_default);
            if (VAL_ROAMING_SMALL.equals(roamingVal)) text = getString(R.string.opt_roaming_small);
            else if (VAL_ROAMING_HIDE.equals(roamingVal)) text = getString(R.string.opt_roaming_hide);
            mBadgeRoamingStyle.setText(text);
        }
    }
}
