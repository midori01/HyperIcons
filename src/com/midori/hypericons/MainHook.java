package com.midori.hypericons;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class MainHook {
    private static final String TAG = "HyperIconsHook";
    public static final String VERSION = BuildConfig.VERSION_NAME;
    public static final String ACTION_RELOAD = "com.midori.hypericons.ACTION_RELOAD";
    public static final String PREF_PACKAGE = "com.midori.hypericons";
    public static final String PREF_FILE = "hyper_icons_config";
    public static final String PROVIDER_AUTHORITY = "com.midori.hypericons.provider";
    public static final Uri PROVIDER_URI = Uri.parse("content://" + PROVIDER_AUTHORITY);
    public static final Uri CONFIG_URI = Uri.parse("content://" + PROVIDER_AUTHORITY + "/config");

    // Mode and Tab Keys
    public static final String KEY_SIM_MODE = ConfigKeys.KEY_SIM_MODE;           // "both" | "separate"
    public static final String KEY_TARGET_TAB = ConfigKeys.KEY_TARGET_TAB;       // "sim1" | "sim2"

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

    // Immutable snapshot of the resolved config; replaced as a whole on reload.
    private static volatile Map<String, String> sConfig = ConfigKeys.defaults();

    public static class IconConfig {
        public String ca = "default";
        public String basic = "default";
        public String caText = "default";
        public String basicText = "default";
        public String capsule = "default";
        public String rat = "default";
        public String alwaysRat = "default";
        public String volte = "default";
        public String vowifi = "default";
        public String roaming = "default";
        public String vonrVisibility = "default";
        public String wifiVolte = "default";
    }

    public static IconConfig getConfigForSlot(int slotId) {
        Map<String, String> c = sConfig;
        int slot = "separate".equals(c.get(KEY_SIM_MODE)) && (slotId == 0 || slotId == 1) ? slotId : -1;
        IconConfig cfg = new IconConfig();
        cfg.ca = configValue(c, KEY_5G_CA, slot);
        cfg.basic = configValue(c, KEY_5G_BASIC, slot);
        cfg.caText = configValue(c, ConfigKeys.KEY_5G_CA_TEXT, slot);
        cfg.basicText = configValue(c, ConfigKeys.KEY_5G_BASIC_TEXT, slot);
        cfg.capsule = configValue(c, KEY_CAPSULE_STYLE, slot);
        cfg.rat = configValue(c, KEY_4G_RAT, slot);
        cfg.alwaysRat = configValue(c, KEY_ALWAYS_SHOW_RAT, slot);
        cfg.volte = configValue(c, KEY_VOLTE_STYLE, slot);
        cfg.vowifi = configValue(c, KEY_VOWIFI_STYLE, slot);
        cfg.roaming = configValue(c, KEY_ROAMING_STYLE, slot);
        cfg.vonrVisibility = configValue(c, KEY_VONR_VISIBILITY, slot);
        cfg.wifiVolte = configValue(c, KEY_WIFI_VOLTE_VISIBILITY, slot);
        return cfg;
    }

    private static String configValue(Map<String, String> c, String baseKey, int slot) {
        String v = c.get(slot >= 0 ? ConfigKeys.slotKey(baseKey, slot) : baseKey);
        return v != null ? v : ConfigKeys.DEFAULT_VALUE;
    }

    static String pref(String key) {
        String v = sConfig.get(key);
        return v != null ? v : ConfigKeys.DEFAULT_VALUE;
    }

    public static int getSlotIdForSubId(int subId) {
        if (subId < 0) return -1;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                int slot = SubscriptionManager.getSlotIndex(subId);
                if (slot >= 0) return slot;
            }
        } catch (Throwable ignored) {}
        try {
            Class<?> smClz = Reflect.findClassIfExists("android.telephony.SubscriptionManager", null);
            if (smClz != null) {
                Object res = Reflect.callStaticMethod(smClz, "getSlotIndex", subId);
                if (res instanceof Integer && ((Integer) res).intValue() >= 0) {
                    return (Integer) res;
                }
            }
        } catch (Throwable ignored) {}
        if (sContext != null) {
            try {
                SubscriptionManager sm = (SubscriptionManager) sContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                if (sm != null) {
                    SubscriptionInfo si = sm.getActiveSubscriptionInfo(subId);
                    if (si != null) {
                        int slot = si.getSimSlotIndex();
                        if (slot >= 0) return slot;
                    }
                }
            } catch (Throwable ignored) {}
        }
        return -1;
    }

    public static int getSubIdForSlotId(int slotId) {
        if (slotId < 0) return -1;
        if (sContext != null) {
            try {
                SubscriptionManager sm = (SubscriptionManager) sContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                if (sm != null) {
                    SubscriptionInfo si = sm.getActiveSubscriptionInfoForSimSlotIndex(slotId);
                    if (si != null) {
                        int subId = si.getSubscriptionId();
                        if (subId >= 0 && subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) return subId;
                    }
                    if (Build.VERSION.SDK_INT >= 29) {
                        int[] subIds = sm.getSubscriptionIds(slotId);
                        if (subIds != null && subIds.length > 0 && subIds[0] >= 0 && subIds[0] != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                            return subIds[0];
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        try {
            Class<?> smClz = Reflect.findClassIfExists("android.telephony.SubscriptionManager", null);
            if (smClz != null) {
                Object res = Reflect.callStaticMethod(smClz, "getSubscriptionId", slotId);
                if (res instanceof Integer && ((Integer) res).intValue() >= 0 && ((Integer) res).intValue() != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    return (Integer) res;
                }
                Object ids = Reflect.callStaticMethod(smClz, "getSubId", slotId);
                if (ids instanceof int[]) {
                    int[] arr = (int[]) ids;
                    if (arr.length > 0 && arr[0] >= 0 && arr[0] != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                        return arr[0];
                    }
                }
            }
        } catch (Throwable ignored) {}
        return -1;
    }

    public static int getSlotIdForView(View view) {
        if (view == null) return -1;
        try {
            Object direct = Reflect.getAdditionalInstanceField(view, "slotId");
            if (direct instanceof Integer && ((Integer) direct).intValue() >= 0) {
                return ((Integer) direct).intValue();
            }
        } catch (Throwable ignored) {}

        int subId = getSubIdForView(view);
        if (subId >= 0) {
            int slot = getSlotIdForSubId(subId);
            if (slot >= 0) return slot;
        }

        View curr = view;
        while (curr != null) {
            try {
                int slotId = Reflect.getIntField(curr, "slotId");
                if (slotId >= 0) return slotId;
            } catch (Throwable ignored) {}
            try {
                int slotId = Reflect.getIntField(curr, "mSlotId");
                if (slotId >= 0) return slotId;
            } catch (Throwable ignored) {}
            if (curr.getParent() instanceof View) {
                curr = (View) curr.getParent();
            } else {
                break;
            }
        }
        return -1;
    }

    public static final int RADIO_TYPE_UNKNOWN = IconPolicy.RADIO_TYPE_UNKNOWN;
    public static final int RADIO_TYPE_2G_3G = IconPolicy.RADIO_TYPE_2G_3G;
    public static final int RADIO_TYPE_4G = IconPolicy.RADIO_TYPE_4G;
    public static final int RADIO_TYPE_4G_CA = IconPolicy.RADIO_TYPE_4G_CA;
    public static final int RADIO_TYPE_5G = IconPolicy.RADIO_TYPE_5G;
    public static final int RADIO_TYPE_5G_CA = IconPolicy.RADIO_TYPE_5G_CA;

    private static volatile boolean sIs5GConnected = false;
    private static volatile boolean sIs5GCaConnected = false;
    private static final java.util.concurrent.ConcurrentHashMap<Integer, Boolean> sSubIdIs5G = new java.util.concurrent.ConcurrentHashMap<Integer, Boolean>();
    private static final java.util.concurrent.ConcurrentHashMap<Integer, Boolean> sSubIdIs5GCa = new java.util.concurrent.ConcurrentHashMap<Integer, Boolean>();
    private static final java.util.concurrent.ConcurrentHashMap<Integer, Integer> sSubIdRadioType = new java.util.concurrent.ConcurrentHashMap<Integer, Integer>();
    private static final java.util.concurrent.ConcurrentHashMap<Integer, Boolean> sSubIdIsRoaming = new java.util.concurrent.ConcurrentHashMap<Integer, Boolean>();

    private static boolean isSubIdRoaming(int subId, Context ctx) {
        if (subId < 0) return false;
        Boolean cached = sSubIdIsRoaming.get(subId);
        if (cached != null) return cached.booleanValue();
        if (ctx != null) {
            try {
                TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
                if (tm != null) {
                    TelephonyManager subTm = tm.createForSubscriptionId(subId);
                    boolean roaming = subTm.isNetworkRoaming();
                    sSubIdIsRoaming.put(subId, Boolean.valueOf(roaming));
                    return roaming;
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }
    private static volatile boolean sNetworkCallbackRegistered = false;
    private static volatile Handler sMainHandler = null;
    private static final Runnable sReloadRunnable = new Runnable() {
        @Override
        public void run() {
            performStatusReload();
        }
    };

    static Handler getMainHandler() {
        if (sMainHandler == null) {
            synchronized (MainHook.class) {
                if (sMainHandler == null) {
                    Looper looper = Looper.getMainLooper();
                    if (looper != null) {
                        sMainHandler = new Handler(looper);
                    }
                }
            }
        }
        return sMainHandler;
    }

    private static volatile Context sContext = null;
    private static volatile Object sPolicyInstance = null;
    private static final java.util.List<WeakReference<Object>> sMobileIconInteractors =
            java.util.Collections.synchronizedList(new java.util.ArrayList<WeakReference<Object>>());
    private static final java.util.Map<Object, Integer> sCellularIconVmSubIds =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<Object, Integer>());
    private static final java.util.Set<Object> sWifiVolteOverriddenVms = java.util.Collections.newSetFromMap(
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<Object, Boolean>()));
    /** Live mobile_volte ImageViews, tagged with their subId on bind. */
    private static final java.util.List<WeakReference<ImageView>> sTrackedVolteViews =
            new java.util.ArrayList<WeakReference<ImageView>>();
    private static final ThreadLocal<Boolean> sVonrStockRead = new ThreadLocal<Boolean>();
    private static volatile boolean sReceiverRegistered = false;
    private static volatile ContentObserver sContentObserver = null;
    private static volatile boolean sDataSwitchListenerRegistered = false;
    private static volatile ContentObserver sDataSwitchObserver = null;
    private static volatile BroadcastReceiver sDataSwitchReceiver = null;
    private static volatile SubscriptionManager.OnSubscriptionsChangedListener sSubChangedListener = null;
    private static final int[] DATA_SWITCH_DELAYS = new int[]{ 150, 350, 750, 1500, 2500 };
    private static final List<Runnable> sDataSwitchRunnables = new ArrayList<Runnable>();

    private static synchronized void scheduleDataSwitchReload() {
        Handler handler = getMainHandler();
        if (handler == null) {
            performStatusReload();
            return;
        }
        for (Runnable r : sDataSwitchRunnables) {
            handler.removeCallbacks(r);
        }
        sDataSwitchRunnables.clear();

        for (final int delay : DATA_SWITCH_DELAYS) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    HookEnv.log(TAG + ": [v" + VERSION + "] Multi-phase data switch reload pass (" + delay + "ms)");
                    performStatusReload();
                }
            };
            sDataSwitchRunnables.add(r);
            handler.postDelayed(r, delay);
        }
    }

    private static PrefsFile sPrefs = null;
    private static volatile boolean sConfigLoaded = false;
    private static volatile boolean sRetryScheduled = false;

    private static int sResVolteSolid = 0;
    private static int sResVolte4g = 0;
    private static int sResVolteNoFrame = 0;
    private static int sResVolteHdVoice = 0;
    private static int sResHdBig = 0;
    private static int sResHdPlus = 0;
    private static int sResVolteVo4g = 0;
    private static int sResVonrSolid = 0;
    private static int sResVonrNoFrame = 0;

    private static int sResVowifi = 0;
    private static int sResVowifiWifi = 0;
    private static int sResVowifiCall = 0;
    private static int sResVowifiCall1 = 0;
    private static int sResVowifiCall2 = 0;

    private static int sResRoam = 0;
    private static int sResRoamSmall = 0;

    private static void initVolteResourceIds(Context ctx) {
        if (ctx == null) return;
        try {
            android.content.res.Resources res = ctx.getResources();
            if (sResVolteSolid == 0) sResVolteSolid = res.getIdentifier("stat_sys_signal_volte", "drawable", "com.android.systemui");
            if (sResVolte4g == 0) sResVolte4g = res.getIdentifier("stat_sys_signal_volte_4g", "drawable", "com.android.systemui");
            if (sResVolteNoFrame == 0) sResVolteNoFrame = res.getIdentifier("stat_sys_signal_volte_no_frame", "drawable", "com.android.systemui");
            if (sResVolteHdVoice == 0) sResVolteHdVoice = res.getIdentifier("stat_sys_signal_volte_hd_voice", "drawable", "com.android.systemui");
            if (sResHdBig == 0) sResHdBig = res.getIdentifier("stat_sys_signal_hd_big", "drawable", "com.android.systemui");
            if (sResHdPlus == 0) sResHdPlus = res.getIdentifier("stat_sys_carrier_signal_hd_plus", "drawable", "com.android.systemui");
            if (sResVolteVo4g == 0) sResVolteVo4g = res.getIdentifier("stat_sys_signal_volte_vo4g", "drawable", "com.android.systemui");
            if (sResVonrSolid == 0) sResVonrSolid = res.getIdentifier("stat_sys_signal_vonr", "drawable", "com.android.systemui");
            if (sResVonrNoFrame == 0) sResVonrNoFrame = res.getIdentifier("stat_sys_signal_vonr_no_frame", "drawable", "com.android.systemui");
        } catch (Throwable t) {
            HookEnv.log(TAG + ": initVolteResourceIds error: " + t);
        }
    }

    private static int getVolteResForStyle(String style, boolean isVonr) {
        if ("intl_solid".equals(style)) {
            return isVonr && sResVonrSolid != 0 ? sResVonrSolid : sResVolteSolid;
        } else if ("intl_4g".equals(style)) {
            return isVonr && sResVonrNoFrame != 0 ? sResVonrNoFrame : sResVolte4g;
        } else if ("intl_hollow".equals(style)) {
            return isVonr && sResVonrNoFrame != 0 ? sResVonrNoFrame : sResVolteNoFrame;
        } else if ("intl_hd_voice".equals(style)) {
            return isVonr && sResVonrSolid != 0 ? sResVonrSolid : sResVolteHdVoice;
        } else if ("china_hd".equals(style)) {
            return sResHdBig;
        } else if ("china_hd_plus".equals(style)) {
            return sResHdPlus != 0 ? sResHdPlus : sResHdBig;
        } else if ("intl_vo4g".equals(style)) {
            return isVonr && sResVonrSolid != 0 ? sResVonrSolid : sResVolteVo4g;
        }
        return 0;
    }

    private static void initVowifiResourceIds(Context ctx) {
        if (ctx == null) return;
        try {
            android.content.res.Resources res = ctx.getResources();
            if (sResVowifi == 0) sResVowifi = res.getIdentifier("stat_sys_vowifi", "drawable", "com.android.systemui");
            if (sResVowifiWifi == 0) sResVowifiWifi = res.getIdentifier("stat_sys_vowifi_wifi", "drawable", "com.android.systemui");
            if (sResVowifiCall == 0) sResVowifiCall = res.getIdentifier("stat_sys_vowifi_call", "drawable", "com.android.systemui");
            if (sResVowifiCall1 == 0) sResVowifiCall1 = res.getIdentifier("stat_sys_vowifi_call_1", "drawable", "com.android.systemui");
            if (sResVowifiCall2 == 0) sResVowifiCall2 = res.getIdentifier("stat_sys_vowifi_call_2", "drawable", "com.android.systemui");
        } catch (Throwable t) {
            HookEnv.log(TAG + ": initVowifiResourceIds error: " + t);
        }
    }

    private static int getVowifiResForStyle(String style, int slotId) {
        if ("vowifi_standard".equals(style)) {
            return sResVowifi != 0 ? sResVowifi : sResVowifiWifi;
        } else if ("vowifi_wifi".equals(style)) {
            return sResVowifiWifi != 0 ? sResVowifiWifi : sResVowifi;
        } else if ("vowifi_call".equals(style)) {
            if (slotId == 1 && sResVowifiCall2 != 0) return sResVowifiCall2;
            if (slotId == 0 && sResVowifiCall1 != 0) return sResVowifiCall1;
            return sResVowifiCall != 0 ? sResVowifiCall : sResVowifiWifi;
        }
        return 0;
    }

    private static void initRoamingResourceIds(Context ctx) {
        if (ctx == null) return;
        try {
            android.content.res.Resources res = ctx.getResources();
            if (sResRoam == 0) sResRoam = res.getIdentifier("stat_sys_data_connected_roam", "drawable", "com.android.systemui");
            if (sResRoamSmall == 0) sResRoamSmall = res.getIdentifier("stat_sys_data_connected_roam_small", "drawable", "com.android.systemui");
        } catch (Throwable t) {
            HookEnv.log(TAG + ": initRoamingResourceIds error: " + t);
        }
    }

    // HyperOS only shows the small roaming R natively while the data type is hidden, so the
    // R glyph shares the top-left corner of the signal bars with the data type label and
    // the 5G pill. While both are shown, the label and the R are placed as one label by
    // SystemUI's own rule for mobile_type, so a longer pair reaches further into the bars
    // like any long label. The R sits on the label's baseline, flat or capsule, and is
    // centred on the 5G pill.
    // Geometry comes from the stat_sys_data_connected_roam_small and signal_5g_off vectors.
    private static final float SMALL_ROAM_WIDTH_DP = 24f;
    private static final float SMALL_ROAM_HEIGHT_DP = 20f;
    private static final float SMALL_ROAM_GLYPH_LEFT_DP = 4.618f;
    private static final float SMALL_ROAM_GLYPH_RIGHT_DP = 8.78f;
    private static final float SMALL_ROAM_GLYPH_TOP_DP = 2.432f;
    private static final float SMALL_ROAM_GLYPH_BOTTOM_DP = 7.695f;
    private static final float SPECIAL_5G_WIDTH_DP = 25f;
    private static final float SPECIAL_5G_HEIGHT_DP = 20f;
    private static final float SPECIAL_5G_PILL_TOP_DP = 1.6f;
    private static final float SPECIAL_5G_PILL_BOTTOM_DP = 7.6f;
    private static final float SPECIAL_5G_PILL_LEFT_DP = 2.1f;
    private static final float SPECIAL_5G_PILL_RIGHT_DP = 12.9f;
    private static final float SIGNAL_WIDTH_DP = 20f;
    private static final float SMALL_ROAM_GAP_DP = 0.8f;
    // The capsule label has inner padding before its border; the native pill's border is its edge.
    private static final float SMALL_ROAM_PILL_GAP_DP = 1.2f;
    private static final float TYPE_HALF_MARGIN_END_OFFSET_DP = -1.8f;
    private static final float TYPE_AVAILABLE_HALF_OFFSET_DP = -10.7f;

    private static int sIdSmallRoam = 0;
    private static int sIdSignal = 0;
    private static int sIdMobileType = 0;
    private static int sIdSpecial5g = 0;
    private static int sIdTypeHalfOffset = 0;
    private static int sIdTypeAvailableOffset = 0;

    private static void installSmallRoamClearance(final ViewGroup container) {
        if (container == null) return;
        if (Reflect.getAdditionalInstanceField(container, "hiSmallRoamClearance") != null) return;
        final android.view.ViewTreeObserver.OnPreDrawListener listener = new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                try {
                    return !applySmallRoamClearance(container);
                } catch (Throwable t) {
                    return true;
                }
            }
        };
        Reflect.setAdditionalInstanceField(container, "hiSmallRoamClearance", listener);
        container.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                v.getViewTreeObserver().addOnPreDrawListener(listener);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                v.getViewTreeObserver().removeOnPreDrawListener(listener);
            }
        });
        if (container.isAttachedToWindow()) {
            container.getViewTreeObserver().addOnPreDrawListener(listener);
        }
    }

    private static boolean applySmallRoamClearance(ViewGroup container) {
        android.content.res.Resources res = container.getResources();
        if (sIdSmallRoam == 0) {
            sIdSmallRoam = res.getIdentifier("mobile_small_roam", "id", "com.android.systemui");
            sIdSignal = res.getIdentifier("mobile_signal", "id", "com.android.systemui");
            sIdMobileType = res.getIdentifier("mobile_type", "id", "com.android.systemui");
            sIdSpecial5g = res.getIdentifier("mobile_special_5G", "id", "com.android.systemui");
            sIdTypeHalfOffset = res.getIdentifier(
                    "status_bar_mobile_type_half_margin_end_offset", "dimen", "com.android.systemui");
            sIdTypeAvailableOffset = res.getIdentifier(
                    "status_bar_mobile_type_available_half_offset", "dimen", "com.android.systemui");
        }
        if (sIdSmallRoam == 0) return false;
        float density = res.getDisplayMetrics().density;
        View smallRoam = container.findViewById(sIdSmallRoam);
        View type = sIdMobileType != 0 ? container.findViewById(sIdMobileType) : null;
        View pill = sIdSpecial5g != 0 ? container.findViewById(sIdSpecial5g) : null;
        View signal = sIdSignal != 0 ? container.findViewById(sIdSignal) : null;
        boolean roamShown = smallRoam != null && smallRoam.getVisibility() == View.VISIBLE;
        boolean typeShown = type != null && type.getVisibility() == View.VISIBLE;
        boolean pillShown = !typeShown && pill != null && pill.getVisibility() == View.VISIBLE;

        // All positions below are measured from the start of mobile_signal. mobile_type ends
        // at -marginEnd; mobile_small_roam and mobile_special_5G end where mobile_signal ends.
        float signalWidth = signal != null && signal.getWidth() > 0
                ? signal.getWidth() : SIGNAL_WIDTH_DP * density;
        float roamScale = (smallRoam != null && smallRoam.getWidth() > 0
                ? smallRoam.getWidth() : SMALL_ROAM_WIDTH_DP * density) / SMALL_ROAM_WIDTH_DP;
        float pillScale = (pill != null && pill.getWidth() > 0
                ? pill.getWidth() : SPECIAL_5G_WIDTH_DP * density) / SPECIAL_5G_WIDTH_DP;
        float glyphWidth = (SMALL_ROAM_GLYPH_RIGHT_DP - SMALL_ROAM_GLYPH_LEFT_DP) * roamScale;
        float glyphStart = signalWidth - (SMALL_ROAM_WIDTH_DP - SMALL_ROAM_GLYPH_LEFT_DP) * roamScale;
        float pillEnd = signalWidth - (SPECIAL_5G_WIDTH_DP - SPECIAL_5G_PILL_RIGHT_DP) * pillScale;
        float gap = (pillShown ? SMALL_ROAM_PILL_GAP_DP : SMALL_ROAM_GAP_DP) * density;

        int typeMarginEnd = 0;
        int pillMarginEnd = 0;
        float shiftX = 0f;
        if (roamShown && (typeShown || pillShown)) {
            float labelWidth;
            if (typeShown) {
                ViewGroup.LayoutParams lp = type.getLayoutParams();
                labelWidth = lp != null && lp.width > 0 ? lp.width : type.getWidth();
            } else {
                labelWidth = (SPECIAL_5G_PILL_RIGHT_DP - SPECIAL_5G_PILL_LEFT_DP) * pillScale;
            }
            float halfOffset = TYPE_HALF_MARGIN_END_OFFSET_DP * density;
            float availableOffset = TYPE_AVAILABLE_HALF_OFFSET_DP * density;
            try {
                if (sIdTypeHalfOffset != 0) halfOffset = res.getDimensionPixelSize(sIdTypeHalfOffset);
                if (sIdTypeAvailableOffset != 0) availableOffset = res.getDimensionPixelSize(sIdTypeAvailableOffset);
            } catch (Throwable ignored) {}
            // SystemUI's mobile_type end margin, applied to the label and the R together.
            float comboWidth = labelWidth + gap + glyphWidth;
            float comboEnd = -Math.max(-comboWidth / 2f + halfOffset, availableOffset);
            int labelEnd = Math.round(comboEnd - glyphWidth - gap);
            if (typeShown) {
                typeMarginEnd = -labelEnd;
            } else {
                pillMarginEnd = Math.round(pillEnd - labelEnd);
            }
            shiftX = Math.round(labelEnd + gap - glyphStart);
        }

        boolean changed = false;
        if (type != null) {
            changed |= applyClearanceMargin(type, roamShown && typeShown, typeMarginEnd);
        }
        if (pill != null) {
            changed |= applyClearanceMargin(pill, roamShown && pillShown, pillMarginEnd);
        }

        if (smallRoam != null) {
            if (smallRoam.getTranslationX() != shiftX) {
                smallRoam.setTranslationX(shiftX);
            }

            float shiftY = 0f;
            if (roamShown && smallRoam.getHeight() > 0) {
                float roamScaleY = smallRoam.getHeight() / SMALL_ROAM_HEIGHT_DP;
                if (typeShown && type instanceof ImageView && type.getHeight() > 0) {
                    float baseline = labelBaseline((ImageView) type);
                    if (!Float.isNaN(baseline)) {
                        shiftY = Math.round(baseline
                                - (smallRoam.getTop() + SMALL_ROAM_GLYPH_BOTTOM_DP * roamScaleY));
                    }
                } else if (pillShown && pill.getHeight() > 0) {
                    float pillScaleY = pill.getHeight() / SPECIAL_5G_HEIGHT_DP;
                    float pillCenter = pill.getTop()
                            + (SPECIAL_5G_PILL_TOP_DP + SPECIAL_5G_PILL_BOTTOM_DP) / 2f * pillScaleY;
                    float glyphCenter = smallRoam.getTop()
                            + (SMALL_ROAM_GLYPH_TOP_DP + SMALL_ROAM_GLYPH_BOTTOM_DP) / 2f * roamScaleY;
                    shiftY = Math.round(pillCenter - glyphCenter);
                }
            }
            if (smallRoam.getTranslationY() != shiftY) {
                smallRoam.setTranslationY(shiftY);
            }
        }
        return changed;
    }

    private static final Paint sCapsuleMetricsPaint = new Paint();

    // Baseline of the label drawn in mobile_type, in its parent's coordinates, or NaN.
    private static float labelBaseline(ImageView type) {
        Drawable d = type.getDrawable();
        if (d == null) return Float.NaN;
        try {
            // Flat text: SystemUI sizes the box to the label and draws it on the bottom edge.
            int textHeight = Reflect.getIntField(d, "mHeight");
            if (textHeight > 0 && textHeight == type.getHeight()) return type.getBottom();
            // Capsule: drawCapsuleBreath centres the text in the 0.6dp to 8.6dp pill.
            IconConfig cfg = getConfigForSlot(getSlotIdForView(type));
            if ("korean".equals(cfg.capsule) && isCapsuleTargetText(getDrawableMobileType(d), cfg)) {
                sCapsuleMetricsPaint.set((Paint) Reflect.getObjectField(d, "mMobileTypeTextPaint"));
                sCapsuleMetricsPaint.setTextSize(dpToPx(6.1f));
                Paint.FontMetrics fm = sCapsuleMetricsPaint.getFontMetrics();
                float pillCenter = (dpToPx(0.6f) + dpToPx(8.6f)) / 2f;
                return type.getTop() + pillCenter - (fm.descent + fm.ascent) / 2f;
            }
        } catch (Throwable ignored) {}
        return Float.NaN;
    }

    private static boolean applyClearanceMargin(View view, boolean active, int marginEnd) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (!(lp instanceof ViewGroup.MarginLayoutParams)) return false;
        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
        int current = mlp.getMarginEnd();
        Object applied = Reflect.getAdditionalInstanceField(view, "hiRoamMarginEnd");
        Object saved = Reflect.getAdditionalInstanceField(view, "hiNativeMarginEnd");
        // A margin that differs from the one we last set was written by SystemUI.
        int nativeMarginEnd = (applied instanceof Integer && saved instanceof Integer
                && current == ((Integer) applied).intValue()) ? ((Integer) saved).intValue() : current;
        int target = active ? marginEnd : nativeMarginEnd;
        Reflect.setAdditionalInstanceField(view, "hiNativeMarginEnd", Integer.valueOf(nativeMarginEnd));
        Reflect.setAdditionalInstanceField(view, "hiRoamMarginEnd", Integer.valueOf(target));
        if (target == current) return false;
        mlp.setMarginEnd(target);
        view.setLayoutParams(mlp);
        return true;
    }

    private static final List<WeakReference<ImageView>> sTrackedViews = new ArrayList<WeakReference<ImageView>>();

    private static synchronized void trackActiveView(ImageView view) {
        if (view == null) return;
        for (int i = sTrackedViews.size() - 1; i >= 0; i--) {
            ImageView v = sTrackedViews.get(i).get();
            if (v == null) {
                sTrackedViews.remove(i);
            } else if (v == view) {
                return;
            }
        }
        sTrackedViews.add(new WeakReference<ImageView>(view));
    }

    private static synchronized void trackVolteView(ImageView view) {
        if (view == null) return;
        for (int i = sTrackedVolteViews.size() - 1; i >= 0; i--) {
            ImageView v = sTrackedVolteViews.get(i).get();
            if (v == null) {
                sTrackedVolteViews.remove(i);
            } else if (v == view) {
                return;
            }
        }
        sTrackedVolteViews.add(new WeakReference<ImageView>(view));
        while (sTrackedVolteViews.size() > 8) {
            sTrackedVolteViews.remove(0);
        }
    }

    /**
     * Finds a view by id in the closest ancestor of {@code anchor} that owns it.
     * On current HyperOS the mobile_type view sits inside mobile_signal_container,
     * while mobile_volte/mobile_vowifi are siblings of that container inside the
     * per-SIM mobile_group, so a plain getParent().findViewById() misses them.
     */
    private static ImageView findViewInAncestors(View anchor, int id) {
        if (anchor == null || id == 0) return null;
        View curr = anchor;
        while (curr != null) {
            View found = curr.findViewById(id);
            if (found instanceof ImageView) return (ImageView) found;
            if (curr.getParent() instanceof View) {
                curr = (View) curr.getParent();
            } else {
                break;
            }
        }
        return null;
    }

    private static IconConfig orGlobal(IconConfig cfg) {
        return cfg != null ? cfg : getConfigForSlot(-1);
    }

    private static String caCustom(IconConfig cfg) {
        return IconPolicy.custom5gCaText(cfg.ca, cfg.caText);
    }

    private static String basicCustom(IconConfig cfg) {
        return IconPolicy.custom5gBasicText(cfg.basic, cfg.basicText, cfg.ca, cfg.caText);
    }

    private static String getTarget5gCaText(IconConfig cfg) {
        IconConfig c = orGlobal(cfg);
        return IconPolicy.target5gCaText(c.ca, c.caText);
    }

    private static String getTarget5gCaText() {
        return getTarget5gCaText(null);
    }

    private static String getEffective5gCaText(IconConfig cfg) {
        IconConfig c = orGlobal(cfg);
        return IconPolicy.effective5gCaText(c.ca, c.caText);
    }

    private static String getEffective5gCaText() {
        return getEffective5gCaText(null);
    }

    private static String getTarget5gBasicText(IconConfig cfg) {
        IconConfig c = orGlobal(cfg);
        return IconPolicy.target5gBasicText(c.basic, c.basicText, c.ca, c.caText);
    }

    private static String getTarget5gBasicText() {
        return getTarget5gBasicText(null);
    }

    private static String getEffective5gBasicText(IconConfig cfg) {
        return getTarget5gBasicText(cfg);
    }

    private static String getEffective5gBasicText() {
        return getEffective5gBasicText(null);
    }

    private static boolean is5gCaText(String text, IconConfig cfg) {
        IconConfig c = orGlobal(cfg);
        return IconPolicy.is5gCaText(text, caCustom(c), basicCustom(c));
    }

    private static int radioTypeForText(String text, IconConfig cfg) {
        IconConfig c = orGlobal(cfg);
        return IconPolicy.radioTypeForText(text, caCustom(c), basicCustom(c));
    }

    private static boolean is4gCaText(String text) {
        return IconPolicy.is4gCaText(text);
    }

    private static boolean is5gText(String text) {
        return IconPolicy.is5gText(text);
    }

    private static boolean is4gText(String text) {
        return IconPolicy.is4gText(text);
    }

    private static boolean isCapsuleTargetText(String text, IconConfig cfg) {
        IconConfig c = orGlobal(cfg);
        return IconPolicy.isCapsuleTargetText(text, caCustom(c), basicCustom(c));
    }

    private static boolean isTargetBreathText(String text) {
        return IconPolicy.isTargetBreathText(text);
    }

    private static String getDrawableMobileType(Drawable drawable) {
        if (drawable == null) return "";
        try {
            boolean isDoublePlus = false;
            try {
                isDoublePlus = Reflect.getBooleanField(drawable, "mShowMobileTypeDoublePlus");
            } catch (Throwable ignored) {}
            if (isDoublePlus) {
                return "5G++";
            }
            Object textObj = Reflect.getObjectField(drawable, "mMobileType");
            if (textObj instanceof String) {
                String str = (String) textObj;
                if ("5G++".equals(str)) return "5G++";
                if (!TextUtils.isEmpty(str)) return str;
            }
            Object actual = Reflect.getAdditionalInstanceField(drawable, "actualMobileType");
            if (actual instanceof String && !TextUtils.isEmpty((String) actual)) {
                return (String) actual;
            }
            Object viewObj = Reflect.getAdditionalInstanceField(drawable, "view");
            if (viewObj instanceof View) {
                Object tag = ((View) viewObj).getTag();
                if (tag instanceof String && !TextUtils.isEmpty((String) tag)) {
                    return (String) tag;
                }
            }
        } catch (Throwable ignored) {}
        return "";
    }

    private static float dpToPx(float dp) {
        float density = 0f;
        if (sContext != null) {
            try {
                density = sContext.getResources().getDisplayMetrics().density;
            } catch (Throwable ignored) {}
        }
        if (density <= 0f) {
            try {
                density = android.content.res.Resources.getSystem().getDisplayMetrics().density;
            } catch (Throwable ignored) {}
        }
        if (density <= 0f) {
            density = 3.0f;
        }
        return dp * density;
    }

    private static boolean isChinaRom() {
        try {
            Class<?> spClz = Reflect.findClassIfExists("android.os.SystemProperties", null);
            if (spClz != null) {
                String modDevice = (String) Reflect.callStaticMethod(spClz, "get", "ro.product.mod_device", "");
                if (modDevice != null && (modDevice.contains("_global") || modDevice.contains("_eea") || modDevice.contains("_in")
                        || modDevice.contains("_ru") || modDevice.contains("_id") || modDevice.contains("_tr") || modDevice.contains("_tw") || modDevice.contains("_jp"))) {
                    return false;
                }
                String incremental = (String) Reflect.callStaticMethod(spClz, "get", "ro.build.version.incremental", "");
                if (incremental != null) {
                    if (incremental.toLowerCase().contains("xiaomi.eu") || incremental.toLowerCase().contains("eu")) {
                        return false;
                    }
                    if (incremental.contains("CN")) {
                        return true;
                    }
                    if (incremental.contains("MI") || incremental.contains("EU") || incremental.contains("RU")
                            || incremental.contains("IN") || incremental.contains("ID") || incremental.contains("TR")
                            || incremental.contains("TW") || incremental.contains("JP")) {
                        return false;
                    }
                }
                String region = (String) Reflect.callStaticMethod(spClz, "get", "ro.miui.region", "");
                if (!TextUtils.isEmpty(region)) {
                    if ("CN".equalsIgnoreCase(region)) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        } catch (Throwable ignored) {}
        try {
            ClassLoader cl = MainHook.class.getClassLoader();
            if (sContext != null) cl = sContext.getClassLoader();
            Class<?> buildClz = Reflect.findClassIfExists("miui.os.Build", cl);
            if (buildClz != null) {
                return !Reflect.getStaticBooleanField(buildClz, "IS_INTERNATIONAL_BUILD");
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isInternationalRom() {
        return !isChinaRom();
    }

    private static boolean shouldShowRat(View view, Context context, IconConfig cfg) {
        String mode = cfg != null ? cfg.alwaysRat : pref(KEY_ALWAYS_SHOW_RAT);
        boolean intl = IconPolicy.ratRuleUsesRomRegion(mode) && isInternationalRom();
        int rule = IconPolicy.ratRule(mode, intl);
        if (rule == IconPolicy.RAT_SHOW) return true;
        boolean isWifi = rule == IconPolicy.RAT_HIDE_ON_WIFI && isWifiConnected(context);
        if (isWifi) return false;
        return IconPolicy.shouldShowRat(rule, isWifi, isDataSimCard(view));
    }

    private static boolean shouldShowRat(View view, Context context) {
        int slotId = getSlotIdForView(view);
        return shouldShowRat(view, context, getConfigForSlot(slotId));
    }

    private static int getSubIdForView(View view) {
        if (view == null) return -1;
        try {
            Object direct = Reflect.getAdditionalInstanceField(view, "subId");
            if (direct instanceof Integer && ((Integer) direct).intValue() >= 0) {
                return ((Integer) direct).intValue();
            }
        } catch (Throwable ignored) {}
        View curr = view;
        while (curr != null) {
            try {
                int subId = Reflect.getIntField(curr, "subId");
                if (subId >= 0) return subId;
            } catch (Throwable ignored) {}
            try {
                int subId = Reflect.getIntField(curr, "mSubId");
                if (subId >= 0) return subId;
            } catch (Throwable ignored) {}
            try {
                int slotId = Reflect.getIntField(curr, "mSlotId");
                if (slotId >= 0) {
                    int subId = getSubIdForSlotId(slotId);
                    if (subId >= 0) return subId;
                }
            } catch (Throwable ignored) {}
            try {
                int slotId = Reflect.getIntField(curr, "slotId");
                if (slotId >= 0) {
                    int subId = getSubIdForSlotId(slotId);
                    if (subId >= 0) return subId;
                }
            } catch (Throwable ignored) {}
            try {
                Object res = Reflect.callMethod(curr, "getSubId");
                if (res instanceof Integer && ((Integer) res) >= 0) {
                    return (Integer) res;
                }
            } catch (Throwable ignored) {}
            if (curr.getParent() instanceof View) {
                curr = (View) curr.getParent();
            } else {
                break;
            }
        }
        return -1;
    }

    private static int getRadioTypeForView(View view, String text, IconConfig cfg) {
        if (view != null) {
            int subId = getSubIdForView(view);
            if (subId >= 0) {
                Integer r = sSubIdRadioType.get(subId);
                if (r != null && r.intValue() != RADIO_TYPE_UNKNOWN) {
                    return r.intValue();
                }
            }
            Object viewR = Reflect.getAdditionalInstanceField(view, "radioType");
            if (viewR instanceof Integer && ((Integer) viewR).intValue() != RADIO_TYPE_UNKNOWN) {
                return ((Integer) viewR).intValue();
            }
        }
        return radioTypeForText(text, cfg);
    }

    private static int getDefaultDataSubId() {
        try {
            int subId = SubscriptionManager.getDefaultDataSubscriptionId();
            if (subId >= 0 && subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) return subId;
        } catch (Throwable ignored) {}
        if (sContext != null) {
            try {
                int subId = Settings.Global.getInt(sContext.getContentResolver(), "multi_sim_data_call", -1);
                if (subId >= 0 && subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) return subId;
            } catch (Throwable ignored) {}
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                int activeSubId = SubscriptionManager.getActiveDataSubscriptionId();
                if (activeSubId >= 0 && activeSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) return activeSubId;
            } catch (Throwable ignored) {}
        }
        try {
            Class<?> smClz = Reflect.findClassIfExists("android.telephony.SubscriptionManager", null);
            if (smClz != null) {
                Object res = Reflect.callStaticMethod(smClz, "getDefaultDataSubscriptionId");
                if (res instanceof Integer && ((Integer) res) >= 0 && ((Integer) res) != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    return (Integer) res;
                }
            }
        } catch (Throwable ignored) {}
        return -1;
    }

    private static boolean isDataSimCard(View view) {
        if (view == null) return true;
        int viewSubId = getSubIdForView(view);
        int viewSlotId = getSlotIdForView(view);
        int defaultDataSubId = getDefaultDataSubId();

        if (defaultDataSubId < 0) {
            return true;
        }

        if (viewSubId >= 0 && viewSubId == defaultDataSubId) {
            return true;
        }

        int dataSlotId = getSlotIdForSubId(defaultDataSubId);
        if (viewSlotId >= 0 && dataSlotId >= 0) {
            return viewSlotId == dataSlotId;
        }

        if (viewSubId >= 0 && viewSubId != defaultDataSubId) {
            return false;
        }

        if (viewSlotId >= 0 && dataSlotId >= 0 && viewSlotId != dataSlotId) {
            return false;
        }

        return true;
    }

    private static boolean isWifiConnected(Context context) {
        if (context == null) context = sContext;
        if (context == null) return false;
        try {
            // Fast short-circuit if Wi-Fi hardware is disabled
            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wm != null && !wm.isWifiEnabled()) {
                return false;
            }

            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;

            // 1. Primary check: Active default network capabilities (most accurate for internet data path)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network activeNet = cm.getActiveNetwork();
                if (activeNet != null) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(activeNet);
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        boolean isLocal = false;
                        if (Build.VERSION.SDK_INT >= 34) {
                            try {
                                isLocal = caps.hasCapability(36); // NET_CAPABILITY_LOCAL_NETWORK
                            } catch (Throwable ignored) {}
                        }
                        if (!isLocal && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                            return true;
                        }
                    }
                }
            }

            // 2. Direct NetworkInfo check for TYPE_WIFI (infrastructure STA mode only, excludes TYPE_WIFI_P2P)
            NetworkInfo wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (wifiInfo != null && wifiInfo.isConnected()) {
                // Confirm true AP association via WifiInfo if available
                if (wm != null) {
                    try {
                        android.net.wifi.WifiInfo winfo = wm.getConnectionInfo();
                        if (winfo != null) {
                            int netId = winfo.getNetworkId();
                            String bssid = winfo.getBSSID();
                            android.net.wifi.SupplicantState state = winfo.getSupplicantState();
                            if (netId != -1 && bssid != null && !"<none>".equals(bssid) && !"02:00:00:00:00:00".equals(bssid)
                                && state == android.net.wifi.SupplicantState.COMPLETED) {
                                return true;
                            }
                        }
                    } catch (Throwable ignored) {}
                } else {
                    return true;
                }
            }

            // 3. Fallback for dual Wi-Fi / Wi-Fi acceleration: check all networks
            // STRICTLY isolate against Wi-Fi Direct (P2P), Quick Share, Nearby Share, Aware, and local-only networks!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Network[] networks = cm.getAllNetworks();
                if (networks != null) {
                    for (Network net : networks) {
                        NetworkCapabilities caps = cm.getNetworkCapabilities(net);
                        if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            // A. Must have internet capability
                            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                                continue;
                            }
                            // B. Must NOT be NET_CAPABILITY_LOCAL_NETWORK
                            if (Build.VERSION.SDK_INT >= 34) {
                                try {
                                    if (caps.hasCapability(36)) continue;
                                } catch (Throwable ignored) {}
                            }
                            // C. Must NOT be a P2P / Aware / NAN virtual interface
                            try {
                                android.net.LinkProperties lp = cm.getLinkProperties(net);
                                if (lp != null && lp.getInterfaceName() != null) {
                                    String iface = lp.getInterfaceName().toLowerCase();
                                    if (iface.startsWith("p2p") || iface.startsWith("aware") || iface.startsWith("nan") || iface.startsWith("swlan")) {
                                        continue;
                                    }
                                }
                            } catch (Throwable ignored) {}

                            NetworkInfo ni = cm.getNetworkInfo(net);
                            if (ni != null && ni.isConnected() && ni.getType() == ConnectivityManager.TYPE_WIFI) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static synchronized void registerNetworkCallback(Context context) {
        if (sNetworkCallbackRegistered || context == null) return;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return;
            NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
            cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    triggerStatusReload();
                }
                @Override
                public void onLost(Network network) {
                    triggerStatusReload();
                }
                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                    triggerStatusReload();
                }
            });
            sNetworkCallbackRegistered = true;
            HookEnv.log(TAG + ": [v" + VERSION + "] Wi-Fi NetworkCallback registered successfully.");
        } catch (Throwable t) {
            HookEnv.log(TAG + ": Failed to register NetworkCallback: " + t);
        }
    }

    public static synchronized void updateContext(Context ctx) {
        if (ctx == null) return;
        if (sContext == null) {
            sContext = ctx.getApplicationContext() != null ? ctx.getApplicationContext() : ctx;
            registerReceiver(sContext);
            registerContentObserver(sContext);
            registerNetworkCallback(sContext);
            registerDataSwitchListener(sContext);
            reportHooksLater(3000);
        } else if (!sDataSwitchListenerRegistered) {
            registerDataSwitchListener(sContext);
        }

        if (!sConfigLoaded) {
            if (loadConfigFromProvider(sContext)) {
                triggerStatusReload();
            } else {
                scheduleColdBootRetry();
            }
        }
    }

    private static void scheduleColdBootRetry() {
        if (sRetryScheduled || sConfigLoaded) return;
        sRetryScheduled = true;

        final Handler handler = getMainHandler();
        if (handler == null) {
            sRetryScheduled = false;
            return;
        }

        final int[] delays = new int[]{ 500, 1500, 3000, 5000, 8000, 12000 };
        for (int i = 0; i < delays.length; i++) {
            final int delay = delays[i];
            final boolean isLast = (i == delays.length - 1);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (sConfigLoaded) {
                        sRetryScheduled = false;
                        return;
                    }
                    if (sContext != null && loadConfigFromProvider(sContext)) {
                        sRetryScheduled = false;
                        triggerStatusReload();
                        HookEnv.log(TAG + ": [v" + VERSION + "] Cold boot config loaded successfully on retry (" + delay + "ms).");
                    } else if (isLast) {
                        sRetryScheduled = false;
                    }
                }
            }, delay);
        }
    }

    private static boolean loadConfigFromProvider(Context context) {
        if (context == null) return false;
        try {
            Bundle bundle = context.getContentResolver().call(PROVIDER_URI, "getConfig", null, null);
            if (bundle != null) {
                final Bundle loaded = bundle;
                sConfig = ConfigKeys.resolve(new ConfigKeys.Source() {
                    @Override
                    public String get(String key, String def) {
                        return loaded.getString(key, def);
                    }
                });
                sConfigLoaded = true;
                HookEnv.log(TAG + ": [v" + VERSION + "] Config loaded from Provider -> " + sConfig);
                return true;
            }
        } catch (Throwable t) {
            HookEnv.log(TAG + ": Error querying Provider: " + t.getMessage());
        }
        return false;
    }

    private static void loadConfig() {
        if (sContext != null && loadConfigFromProvider(sContext)) {
            return;
        }

        loadConfigFromXSharedPrefs();
    }

    private static void loadConfigFromXSharedPrefs() {
        try {
            java.io.File deFile = new java.io.File("/data/user_de/0/" + PREF_PACKAGE + "/shared_prefs/" + PREF_FILE + ".xml");
            if (deFile.exists() && deFile.canRead()) {
                PrefsFile dePrefs = new PrefsFile(deFile);
                if (parsePrefs(dePrefs, "DE-File")) return;
            }

            java.io.File ceFile = new java.io.File("/data/data/" + PREF_PACKAGE + "/shared_prefs/" + PREF_FILE + ".xml");
            if (ceFile.exists() && ceFile.canRead()) {
                PrefsFile cePrefs = new PrefsFile(ceFile);
                if (parsePrefs(cePrefs, "CE-File")) return;
            }

            if (sPrefs == null) {
                sPrefs = new PrefsFile(PREF_PACKAGE, PREF_FILE);
            } else {
                sPrefs.reload();
            }
            parsePrefs(sPrefs, "Package");
        } catch (Throwable t) {
            HookEnv.log(TAG + ": Failed to load config from PrefsFile: " + t.getMessage());
        }
    }

    private static boolean parsePrefs(PrefsFile prefs, String source) {
        if (prefs == null) return false;
        try {
            prefs.reload();
            if (prefs.getAll() != null && !prefs.getAll().isEmpty()) {
                final PrefsFile loaded = prefs;
                sConfig = ConfigKeys.resolve(new ConfigKeys.Source() {
                    @Override
                    public String get(String key, String def) {
                        return loaded.getString(key, def);
                    }
                });
                sConfigLoaded = true;
                HookEnv.log(TAG + ": [v" + VERSION + "] Config loaded from PrefsFile (" + source + ") -> " + sConfig);
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static synchronized void registerContentObserver(final Context context) {
        if (sContentObserver != null || context == null) return;
        try {
            Handler handler = getMainHandler();
            Looper looper = Looper.getMainLooper();
            Handler targetHandler = handler != null ? handler : (looper != null ? new Handler(looper) : new Handler());
            sContentObserver = new ContentObserver(targetHandler) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    HookEnv.log(TAG + ": [v" + VERSION + "] ContentObserver notified change: " + uri);
                    loadConfigFromProvider(context);
                    triggerStatusReload();
                }
            };
            context.getContentResolver().registerContentObserver(CONFIG_URI, true, sContentObserver);
            HookEnv.log(TAG + ": [v" + VERSION + "] ContentObserver registered for " + CONFIG_URI);
        } catch (Throwable t) {
            HookEnv.log(TAG + ": Failed to register ContentObserver: " + t);
        }
    }

    private static synchronized void registerReceiver(Context context) {
        if (sReceiverRegistered || context == null) return;
        try {
            // 1. Module reload / report requests, only accepted from the HyperIcons app.
            //    Extras are ignored; values are always re-read from the provider.
            BroadcastReceiver reloadReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    if (intent == null) return;
                    if (HookEnv.ACTION_REQUEST_REPORT.equals(intent.getAction())) {
                        reportHooksLater(0);
                        return;
                    }
                    if (!ACTION_RELOAD.equals(intent.getAction())) return;
                    loadConfig();
                    triggerStatusReload();
                    reportHooksLater(300);
                }
            };
            IntentFilter reloadFilter = new IntentFilter(ACTION_RELOAD);
            reloadFilter.addAction(HookEnv.ACTION_REQUEST_REPORT);
            HookEnv.registerTrustedReceiver(context, reloadReceiver, reloadFilter);

            // 2. System boot & unlock lifecycle broadcasts
            BroadcastReceiver lifecycleReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    if (intent == null) return;
                    String action = intent.getAction();
                    HookEnv.log(TAG + ": [v" + VERSION + "] System lifecycle broadcast received: " + action + ", refreshing config...");
                    if (loadConfigFromProvider(sContext)) {
                        triggerStatusReload();
                    }
                }
            };
            IntentFilter lifecycleFilter = new IntentFilter();
            lifecycleFilter.addAction(Intent.ACTION_USER_PRESENT);
            lifecycleFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
            lifecycleFilter.addAction("android.intent.action.LOCKED_BOOT_COMPLETED");
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(lifecycleReceiver, lifecycleFilter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                context.registerReceiver(lifecycleReceiver, lifecycleFilter);
            }

            sReceiverRegistered = true;
            HookEnv.log(TAG + ": [v" + VERSION + "] Live reload and boot/unlock broadcast receivers registered.");
        } catch (Throwable t) {
            HookEnv.log(TAG + ": Failed to register reload receiver: " + t);
        }
    }

    private static synchronized void registerDataSwitchListener(final Context context) {
        if (sDataSwitchListenerRegistered || context == null) return;
        try {
            Handler handler = getMainHandler();
            Looper looper = Looper.getMainLooper();
            Handler targetHandler = handler != null ? handler : (looper != null ? new Handler(looper) : new Handler());

            // 1. ContentObserver for rapid database changes
            sDataSwitchObserver = new ContentObserver(targetHandler) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    HookEnv.log(TAG + ": [v" + VERSION + "] Data switch ContentObserver fired: " + uri);
                    scheduleDataSwitchReload();
                }
            };
            ContentResolver resolver = context.getContentResolver();
            if (resolver != null) {
                try {
                    resolver.registerContentObserver(Settings.Global.getUriFor("multi_sim_data_call"), false, sDataSwitchObserver);
                } catch (Throwable ignored) {}
                try {
                    resolver.registerContentObserver(Settings.Global.getUriFor("mobile_data"), false, sDataSwitchObserver);
                } catch (Throwable ignored) {}
                try {
                    resolver.registerContentObserver(Settings.Global.getUriFor("mobile_data1"), false, sDataSwitchObserver);
                } catch (Throwable ignored) {}
                try {
                    resolver.registerContentObserver(Settings.Global.getUriFor("mobile_data2"), false, sDataSwitchObserver);
                } catch (Throwable ignored) {}
            }

            // 2. BroadcastReceiver for Telephony & Network events
            sDataSwitchReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    if (intent == null) return;
                    String action = intent.getAction();
                    HookEnv.log(TAG + ": [v" + VERSION + "] Data switch Broadcast received: " + action);
                    scheduleDataSwitchReload();
                }
            };
            IntentFilter dataFilter = new IntentFilter();
            dataFilter.addAction("android.telephony.action.DEFAULT_DATA_SUBSCRIPTION_CHANGED");
            dataFilter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
            dataFilter.addAction("android.telephony.action.DEFAULT_SUBSCRIPTION_CHANGED");
            dataFilter.addAction("android.intent.action.ANY_DATA_STATE_CHANGED");
            dataFilter.addAction("android.intent.action.SUBINFO_RECORD_UPDATED");
            dataFilter.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
            dataFilter.addAction("android.intent.action.ACTION_SUBINFO_CONTENT_CHANGE");
            dataFilter.addAction("miui.intent.action.ACTION_DEFAULT_DATA_SLOT_CHANGED");
            dataFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
            dataFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(sDataSwitchReceiver, dataFilter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(sDataSwitchReceiver, dataFilter);
            }

            // 3. SubscriptionManager.OnSubscriptionsChangedListener
            try {
                SubscriptionManager sm = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                if (sm != null) {
                    sSubChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
                        @Override
                        public void onSubscriptionsChanged() {
                            HookEnv.log(TAG + ": [v" + VERSION + "] OnSubscriptionsChangedListener fired");
                            scheduleDataSwitchReload();
                        }
                    };
                    sm.addOnSubscriptionsChangedListener(sSubChangedListener);
                }
            } catch (Throwable t) {
                HookEnv.log(TAG + ": Failed to register OnSubscriptionsChangedListener: " + t);
            }

            sDataSwitchListenerRegistered = true;
            HookEnv.log(TAG + ": [v" + VERSION + "] Data switch listeners successfully registered (ContentObserver + BroadcastReceiver + SubscriptionListener).");
        } catch (Throwable t) {
            HookEnv.log(TAG + ": Failed to register data switch listener: " + t);
        }
    }

    private static void syncViewVisibility(ImageView mobileTypeView, ImageView special5gView, String text, IconConfig cfg) {
        if (mobileTypeView == null && special5gView == null) return;

        Context ctx = mobileTypeView != null ? mobileTypeView.getContext() : (special5gView != null ? special5gView.getContext() : sContext);
        View refView = mobileTypeView != null ? mobileTypeView : special5gView;

        if (cfg == null) {
            int slotId = getSlotIdForView(refView);
            cfg = getConfigForSlot(slotId);
        }

        if (!shouldShowRat(refView, ctx, cfg)) {
            if (special5gView != null) special5gView.setVisibility(View.GONE);
            if (mobileTypeView != null) mobileTypeView.setVisibility(View.GONE);
            return;
        }

        boolean isCapsuleMode = "korean".equals(cfg.capsule);

        String currentText = text;
        if (TextUtils.isEmpty(currentText)) {
            if (mobileTypeView != null && mobileTypeView.getTag() instanceof String) {
                currentText = (String) mobileTypeView.getTag();
            }
        }
        if (TextUtils.isEmpty(currentText) && mobileTypeView != null) {
            Drawable d = mobileTypeView.getDrawable();
            if (d != null) {
                currentText = getDrawableMobileType(d);
            }
        }
        if (TextUtils.isEmpty(currentText) && mobileTypeView != null && mobileTypeView.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) mobileTypeView.getParent();
            int singleId = parent.getResources().getIdentifier("mobile_type_single", "id", "com.android.systemui");
            if (singleId != 0) {
                TextView singleTv = (TextView) parent.findViewById(singleId);
                if (singleTv != null && singleTv.getText() != null) {
                    currentText = singleTv.getText().toString();
                }
            }
        }

        int radioType = getRadioTypeForView(refView, currentText, cfg);
        boolean is5g = (radioType == RADIO_TYPE_5G || radioType == RADIO_TYPE_5G_CA);
        boolean is5gCa = (radioType == RADIO_TYPE_5G_CA) || is5gCaText(currentText, cfg);
        boolean isBase5g = is5g && !is5gCa;
        if (!is5g && (radioTypeForText(currentText, cfg) == RADIO_TYPE_5G || "5G".equals(text))) {
            isBase5g = true;
        }
        boolean shouldShowSpecial5G = isCapsuleMode && isBase5g
                && IconPolicy.usesNativeBase5gPill(cfg.basic, basicCustom(cfg));

        boolean targetSpecialVisible = shouldShowSpecial5G;
        boolean targetMobileVisible = !targetSpecialVisible;

        if (special5gView != null) {
            special5gView.setVisibility(targetSpecialVisible ? View.VISIBLE : View.GONE);
        }
        if (mobileTypeView != null) {
            mobileTypeView.setVisibility(targetMobileVisible ? View.VISIBLE : View.GONE);
            trackActiveView(mobileTypeView);

            if (targetMobileVisible) {
                Drawable d = mobileTypeView.getDrawable();
                if (isCapsuleMode && isCapsuleTargetText(currentText, cfg)) {
                    ViewGroup.LayoutParams lp = mobileTypeView.getLayoutParams();
                    if (lp instanceof ViewGroup.MarginLayoutParams) {
                        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                        mlp.topMargin = 0;
                        mlp.height = (int) Math.ceil(dpToPx(9.0f));
                        if (d != null) {
                            mlp.width = (int) Math.ceil(d.getIntrinsicWidth());
                        }
                        mobileTypeView.setLayoutParams(mlp);
                    }
                    mobileTypeView.setScaleType(ImageView.ScaleType.FIT_XY);
                } else {
                    mobileTypeView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    restoreNativeTypeLayout(mobileTypeView, d, currentText);
                }
                if (d != null) d.invalidateSelf();
            }
        }
    }

    private static Class<?> sBinderClz = null;
    private static boolean sRestoringTypeLayout = false;

    // SystemUI sizes mobile_type to the drawable only when its own label changes. A capsule
    // left behind after switching to flat text, or a rewritten label, leaves a box that
    // FIT_CENTER scales the text into, so re-run SystemUI's sizing when the box is stale.
    private static void restoreNativeTypeLayout(ImageView view, Drawable d, String text) {
        if (d == null || sBinderClz == null || sRestoringTypeLayout) return;
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp == null) return;
        int height;
        try {
            height = Reflect.getIntField(d, "mHeight");
        } catch (Throwable t) {
            return;
        }
        if (lp.width == d.getIntrinsicWidth() && lp.height == height) return;
        sRestoringTypeLayout = true;
        try {
            Reflect.callStaticMethod(sBinderClz, "updateMobileTypeLayoutParams",
                    d, text != null ? text : "", view);
        } catch (Throwable t) {
            HookEnv.log(TAG + ": restoreNativeTypeLayout error: " + t);
        } finally {
            sRestoringTypeLayout = false;
        }
    }

    private static void syncViewVisibility(ImageView mobileTypeView, ImageView special5gView, String text) {
        syncViewVisibility(mobileTypeView, special5gView, text, null);
    }

    /**
     * Immediately push the VoNR carrier-config override into every live
     * MiuiMobileIconInteractorImpl. The interactor recomputes its showVonr
     * state from this flow, so the status bar updates without a reboot or a
     * CARRIER_CONFIG_CHANGED round trip through the phone process.
     */
    private static void refreshVonrVisibilityOnInteractors() {
        synchronized (sMobileIconInteractors) {
            for (int i = sMobileIconInteractors.size() - 1; i >= 0; i--) {
                Object interactor = sMobileIconInteractors.get(i).get();
                if (interactor == null) {
                    sMobileIconInteractors.remove(i);
                    continue;
                }
                try {
                    int subId = Reflect.getIntField(interactor, "subId");
                    int slotId = getSlotIdForSubId(subId);
                    IconConfig cfg = getConfigForSlot(slotId);

                    Boolean desired;
                    if ("show".equals(cfg.vonrVisibility)) {
                        desired = Boolean.TRUE;
                    } else if ("hide".equals(cfg.vonrVisibility)) {
                        desired = Boolean.FALSE;
                    } else {
                        desired = readStockVonrCarrierFlag(subId);
                    }
                    if (desired == null) continue;

                    Object readonlyFlow = Reflect.getObjectField(interactor, "carrierConfigShowVonr");
                    Object stateFlow = findStateFlowDelegate(readonlyFlow);
                    if (stateFlow == null) {
                        HookEnv.log(TAG + ": VoNR state flow delegate not found on " + readonlyFlow.getClass().getName());
                        continue;
                    }

                    Object current = Reflect.callMethod(stateFlow, "getValue");
                    if (desired.equals(current)) continue;

                    Reflect.callMethod(stateFlow, "setValue", desired);
                    HookEnv.log(TAG + ": [v" + VERSION + "] [VoNR] Pushed carrierConfigShowVonr="
                            + desired + " for subId=" + subId + " (slot=" + slotId + ")");
                } catch (Throwable t) {
                    HookEnv.log(TAG + ": refreshVonrVisibilityOnInteractors failed: " + t);
                }
            }
        }
    }

    /** Unwrap a kotlinx ReadonlyStateFlow to its mutable StateFlowImpl. */
    private static Object findStateFlowDelegate(Object readonlyFlow) {
        if (readonlyFlow == null) return null;
        Object stateFlow = null;
        try {
            stateFlow = Reflect.getObjectField(readonlyFlow, "$$delegate_0");
        } catch (Throwable ignored) {}
        if (stateFlow == null) {
            for (java.lang.reflect.Field f : readonlyFlow.getClass().getDeclaredFields()) {
                String fieldName = f.getName().toLowerCase();
                String fieldType = f.getType().getName();
                if (fieldName.contains("delegate") || fieldType.contains("StateFlow")) {
                    try {
                        f.setAccessible(true);
                        stateFlow = f.get(readonlyFlow);
                        break;
                    } catch (Throwable ignored) {}
                }
            }
        }
        return stateFlow;
    }

    /** Read the unmodified carrier value by temporarily bypassing our hook. */
    private static Boolean readStockVonrCarrierFlag(int subId) {
        Context ctx = sContext;
        if (ctx == null) return null;
        try {
            CarrierConfigManager ccm = (CarrierConfigManager) ctx.getSystemService(Context.CARRIER_CONFIG_SERVICE);
            if (ccm == null) return null;
            sVonrStockRead.set(Boolean.TRUE);
            try {
                PersistableBundle bundle = ccm.getConfigForSubId(subId);
                if (bundle == null) return null;
                return Boolean.valueOf(bundle.getBoolean("config_show_vonr_icon_in_statusbar", false));
            } finally {
                sVonrStockRead.remove();
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean isWifiConnected() {
        Context ctx = sContext;
        if (ctx == null) return false;
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            Network active = cm.getActiveNetwork();
            if (active == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(active);
            return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Register a cellular icon VM and its interactor. HyperOS 4.0 builds these
     * objects without constructors, so they are captured from
     * MiuiMobileIconVMImpl.getCellProvider() instead.
     */
    private static void trackCellularVm(Object vm, Object interactor) {
        if (vm == null || interactor == null) return;
        int subId;
        try {
            subId = Reflect.getIntField(interactor, "subId");
        } catch (Throwable t) {
            trackInteractor(interactor);
            return;
        }
        boolean added = false;
        synchronized (sCellularIconVmSubIds) {
            Integer prev = sCellularIconVmSubIds.get(vm);
            if (prev == null || prev.intValue() != subId) {
                sCellularIconVmSubIds.put(vm, Integer.valueOf(subId));
                added = true;
            }
        }
        if (added) {
            HookEnv.log(TAG + ": [v" + VERSION + "] [WiFiVoLTE] tracking VM subId=" + subId);
        }
        trackInteractor(interactor);
    }

    private static void trackInteractor(Object interactor) {
        if (interactor == null) return;
        synchronized (sMobileIconInteractors) {
            for (int i = sMobileIconInteractors.size() - 1; i >= 0; i--) {
                Object known = sMobileIconInteractors.get(i).get();
                if (known == null) {
                    sMobileIconInteractors.remove(i);
                } else if (known == interactor) {
                    return;
                }
            }
            sMobileIconInteractors.add(new WeakReference<Object>(interactor));
            while (sMobileIconInteractors.size() > 8) {
                sMobileIconInteractors.remove(0);
            }
        }
    }

    private static Boolean readBooleanFlow(Object owner, String field) {
        try {
            Object flow = Reflect.getObjectField(owner, field);
            Object value = Reflect.callMethod(flow, "getValue");
            if (value instanceof Boolean) return (Boolean) value;
        } catch (Throwable ignored) {}
        return null;
    }

    /** Read an OperatorConfig flag from the VM (miuiConfig on HyperOS 4.0, miuiOpConfig elsewhere). */
    private static boolean readVmOperatorFlag(Object vm, String flag) {
        String[] fields = {"miuiConfig", "miuiOpConfig"};
        for (String field : fields) {
            try {
                Object flow = Reflect.getObjectField(vm, field);
                Object opConfig = Reflect.callMethod(flow, "getValue");
                if (opConfig != null) return Reflect.getBooleanField(opConfig, flag);
            } catch (Throwable ignored) {}
        }
        return false;
    }

    /**
     * The volteVisibleGlobal lambda carries no subscription. Use the per-SIM value
     * only when it is unambiguous; otherwise leave the decision to the per-view paths.
     */
    private static String getLambdaWifiVoltePref(int subId) {
        if (subId >= 0) return getConfigForSlot(getSlotIdForSubId(subId)).wifiVolte;
        return IconPolicy.lambdaWifiVoltePref(pref(KEY_SIM_MODE), pref(KEY_WIFI_VOLTE_VISIBILITY),
                pref(ConfigKeys.slotKey(KEY_WIFI_VOLTE_VISIBILITY, 0)),
                pref(ConfigKeys.slotKey(KEY_WIFI_VOLTE_VISIBILITY, 1)));
    }

    /** Current VoLTE/VoNR-active state for a subscription, from the tracked interactor. */
    private static Boolean getVolteActiveForSubId(int subId) {
        Boolean fallback = null;
        int count = 0;
        synchronized (sMobileIconInteractors) {
            for (int i = sMobileIconInteractors.size() - 1; i >= 0; i--) {
                Object interactor = sMobileIconInteractors.get(i).get();
                if (interactor == null) continue;
                try {
                    int interactorSubId = Reflect.getIntField(interactor, "subId");
                    Object volteFlow = Reflect.getObjectField(interactor, "volte");
                    Object value = Reflect.callMethod(volteFlow, "getValue");
                    if (!(value instanceof Boolean)) continue;
                    count++;
                    if (interactorSubId == subId) return (Boolean) value;
                    if (fallback == null) fallback = (Boolean) value;
                } catch (Throwable ignored) {}
            }
        }
        // Older/newer ROMs occasionally leave subId as -1 until the first
        // telephony update; with a single live interactor its value is unambiguous.
        return (subId < 0 || count == 1) ? fallback : null;
    }

    /** Whether the operator policy asks SystemUI to hide the VoLTE/VoNR icon. */
    private static boolean isVoltePolicyHidden(int subId) {
        Boolean fallback = null;
        int count = 0;
        synchronized (sMobileIconInteractors) {
            for (int i = sMobileIconInteractors.size() - 1; i >= 0; i--) {
                Object interactor = sMobileIconInteractors.get(i).get();
                if (interactor == null) continue;
                try {
                    int interactorSubId = Reflect.getIntField(interactor, "subId");
                    Object opFlow = Reflect.getObjectField(interactor, "miuiOpConfig");
                    Object opConfig = Reflect.callMethod(opFlow, "getValue");
                    if (opConfig == null) continue;
                    boolean hidden = Reflect.getBooleanField(opConfig, "hideVolte");
                    count++;
                    if (interactorSubId == subId) return hidden;
                    if (fallback == null) fallback = Boolean.valueOf(hidden);
                } catch (Throwable ignored) {}
            }
        }
        return (subId < 0 || count == 1) && Boolean.TRUE.equals(fallback);
    }

    /** Whether the view model considers Wi-Fi the default connection for a subscription. */
    private static Boolean getWifiAvailableForSubId(int subId) {
        java.util.Map<Object, Integer> snapshot;
        synchronized (sCellularIconVmSubIds) {
            snapshot = new java.util.HashMap<Object, Integer>(sCellularIconVmSubIds);
        }
        Boolean fallback = null;
        int count = 0;
        for (java.util.Map.Entry<Object, Integer> entry : snapshot.entrySet()) {
            try {
                Object wifiFlow = Reflect.getObjectField(entry.getKey(), "wifiAvailable");
                Object value = Reflect.callMethod(wifiFlow, "getValue");
                if (!(value instanceof Boolean)) continue;
                count++;
                if (entry.getValue().intValue() == subId) return (Boolean) value;
                if (fallback == null) fallback = (Boolean) value;
            } catch (Throwable ignored) {}
        }
        return (subId < 0 || count == 1) ? fallback : null;
    }

    /** Current volteVisibleGlobal second flag for a subscription (volte && !hideVolte). */
    private static Boolean getVolteVisibleSecondForSubId(int subId) {
        java.util.Map<Object, Integer> snapshot;
        synchronized (sCellularIconVmSubIds) {
            snapshot = new java.util.HashMap<Object, Integer>(sCellularIconVmSubIds);
        }
        for (java.util.Map.Entry<Object, Integer> entry : snapshot.entrySet()) {
            if (entry.getValue().intValue() != subId) continue;
            try {
                Object stateFlow = findStateFlowDelegate(
                        Reflect.getObjectField(entry.getKey(), "volteVisibleGlobal"));
                if (stateFlow == null) continue;
                Object current = Reflect.callMethod(stateFlow, "getValue");
                Object second = Reflect.callMethod(current, "getSecond");
                if (second instanceof Boolean) return (Boolean) second;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static int getSubIdForSlot(int slot) {
        if (slot < 0) return -1;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return SubscriptionManager.getSubscriptionId(slot);
            }
            int[] subIds = (int[]) Reflect.callStaticMethod(
                    SubscriptionManager.class, "getSubId", Integer.valueOf(slot));
            return subIds != null && subIds.length > 0 ? subIds[0] : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    private static void applyVolteChildVisible(ImageView view, boolean visible) {
        if (view == null) return;
        ViewGroup parent = view.getParent() instanceof ViewGroup ? (ViewGroup) view.getParent() : null;
        while (parent != null) {
            try {
                if (parent.getClass().getName().endsWith("MobileSignalAnimatorContainer")) {
                    Reflect.callMethod(parent, "setChildVisible", view, Boolean.valueOf(visible));
                    return;
                }
            } catch (Throwable ignored) {}
            parent = parent.getParent() instanceof ViewGroup ? (ViewGroup) parent.getParent() : null;
        }
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Immediately apply the per-SIM Wi-Fi preference to a live mobile_volte view.
     * This is independent of the state-flow push, so a preference change is visible
     * even when no upstream telephony flow re-emits.
     */
    private static void applyWifiVolteVisibility(ImageView volteView, IconConfig cfg, int subId) {
        if (volteView == null || cfg == null) return;
        String wifiPref = cfg.wifiVolte;
        if (!IconPolicy.overridesVolteOnWifi(cfg.volte, wifiPref)) return;

        if (subId < 0) subId = getSubIdForView(volteView);
        if (subId < 0) subId = getSubIdForSlot(getSlotIdForView(volteView));
        Boolean wifiState = getWifiAvailableForSubId(subId);
        boolean wifiOn = wifiState != null ? wifiState.booleanValue() : isWifiConnected();
        boolean currentlyVisible = volteView.getVisibility() == View.VISIBLE;
        if (!wifiOn) {
            // Leaving Wi-Fi: undo a previous "hide on Wi-Fi" so the icon returns
            // to the stock mobile-network visibility immediately.
            if ("hide".equals(wifiPref)) {
                Boolean active = getVolteActiveForSubId(subId);
                if (active != null) {
                    boolean visible = active.booleanValue() && !isVoltePolicyHidden(subId);
                    if (visible != currentlyVisible) {
                        HookEnv.log(TAG + ": [v" + VERSION + "] [WiFiVoLTE] restore subId=" + subId
                                + " active=" + active + " -> visible=" + visible);
                    }
                    applyVolteChildVisible(volteView, visible);
                }
            }
            return;
        }

        if ("hide".equals(wifiPref)) {
            if (currentlyVisible) {
                HookEnv.log(TAG + ": [v" + VERSION + "] [WiFiVoLTE] hide on Wi-Fi subId=" + subId);
            }
            applyVolteChildVisible(volteView, false);
            return;
        }

        Boolean active = getVolteActiveForSubId(subId);
        if (active == null) active = getVolteVisibleSecondForSubId(subId);
        if (active != null) {
            boolean visible = active.booleanValue() && !isVoltePolicyHidden(subId);
            if (visible != currentlyVisible) {
                HookEnv.log(TAG + ": [v" + VERSION + "] [WiFiVoLTE] show on Wi-Fi subId=" + subId
                        + " active=" + active + " -> visible=" + visible);
            }
            applyVolteChildVisible(volteView, visible);
        }
    }

    /**
     * Push the per-SIM Wi-Fi preference into each live MiuiCellularIconVM's
     * volteVisibleGlobal state flow so it applies without waiting for the next
     * upstream emission. System Default leaves the flow untouched, apart from
     * restoring the stock pair once after an override.
     */
    private static void refreshWifiVolteVisibilityOnViewModels() {
        java.util.Map<Object, Integer> snapshot;
        synchronized (sCellularIconVmSubIds) {
            snapshot = new java.util.HashMap<Object, Integer>(sCellularIconVmSubIds);
        }
        for (java.util.Map.Entry<Object, Integer> entry : snapshot.entrySet()) {
            Object vm = entry.getKey();
            int subId = entry.getValue().intValue();
            try {
                IconConfig cfg = getConfigForSlot(getSlotIdForSubId(subId));
                String wifiPref = cfg.wifiVolte;
                boolean restoring = wifiPref == null || "default".equals(wifiPref);
                if (restoring && !sWifiVolteOverriddenVms.contains(vm)) continue;

                Boolean wifiState = readBooleanFlow(vm, "wifiAvailable");
                boolean wifi = wifiState != null ? wifiState.booleanValue() : isWifiConnected();
                Boolean active = getVolteActiveForSubId(subId);
                if (active == null && !"hide".equals(wifiPref)) {
                    // If this VM was never overridden, its second flag is still stock.
                    if (!sWifiVolteOverriddenVms.contains(vm)) {
                        active = getVolteVisibleSecondForSubId(subId);
                    }
                }
                if (active == null && "hide".equals(wifiPref)) {
                    active = Boolean.FALSE;
                }
                if (active == null) continue;
                boolean showWhenWifi = readVmOperatorFlag(vm, "showVolteWhenWifiOnAndDripEnd");
                Boolean hideState = readBooleanFlow(vm, "hideVolte");
                boolean hideVolte = hideState != null ? hideState.booleanValue() : readVmOperatorFlag(vm, "hideVolte");

                // Stock MiuiCellularIconVM$volteVisibleGlobal$2:
                // Pair(wifiAvailable && !showVolteWhenWifiOnAndDripEnd, volte && !hideVolte)
                boolean[] target = IconPolicy.volteVisiblePair(wifi && !showWhenWifi,
                        active.booleanValue() && !hideVolte, cfg.volte, wifiPref, wifi);
                boolean first = target[0];
                boolean second = target[1];

                Object stateFlow = findStateFlowDelegate(Reflect.getObjectField(vm, "volteVisibleGlobal"));
                if (stateFlow == null) {
                    HookEnv.log(TAG + ": [WiFiVoLTE] no state-flow delegate for subId=" + subId);
                    continue;
                }

                Object current = Reflect.callMethod(stateFlow, "getValue");
                Object currentFirst = null;
                Object currentSecond = null;
                try {
                    currentFirst = Reflect.callMethod(current, "getFirst");
                    currentSecond = Reflect.callMethod(current, "getSecond");
                } catch (Throwable ignored) {}
                if (!Boolean.valueOf(first).equals(currentFirst)
                        || !Boolean.valueOf(second).equals(currentSecond)) {
                    Class<?> pairClz = Reflect.findClass("kotlin.Pair", vm.getClass().getClassLoader());
                    Object pair = Reflect.newInstance(pairClz,
                            Boolean.valueOf(first), Boolean.valueOf(second));
                    Reflect.callMethod(stateFlow, "setValue", pair);
                    HookEnv.log(TAG + ": [v" + VERSION + "] [WiFiVoLTE] Pushed volteVisibleGlobal=("
                            + first + "," + second + ") for subId=" + subId
                            + " pref=" + wifiPref + " wifi=" + wifi
                            + (restoring ? " (restored)" : ""));
                }
                if (restoring) {
                    sWifiVolteOverriddenVms.remove(vm);
                } else {
                    sWifiVolteOverriddenVms.add(vm);
                }
            } catch (Throwable t) {
                HookEnv.log(TAG + ": refreshWifiVolteVisibilityOnViewModels failed: " + t);
            }
        }
    }

    static void refreshOperatorConfig(final int slotId) {
        final Object policy = sPolicyInstance;
        Handler handler = getMainHandler();
        if (policy == null || handler == null || slotId < 0) return;
        handler.post(new Runnable() {
            @Override
            public void run() {
                Reflect.callMethod(policy, "updateMiuiOperatorConfig", slotId);
            }
        });
    }

    private static void triggerStatusReload() {
        Handler handler = getMainHandler();
        if (handler != null) {
            handler.removeCallbacks(sReloadRunnable);
            handler.postDelayed(sReloadRunnable, 150);
        } else {
            performStatusReload();
        }
    }

    private static void performStatusReload() {
        Nsa5gHook.onConfigChanged();
        if (sPolicyInstance != null) {
            try {
                Reflect.callMethod(sPolicyInstance, "initResource", "reload", null);
                HookEnv.log(TAG + ": [v" + VERSION + "] Triggered initResource(reload, null)");
            } catch (Throwable ignored) {}

            try {
                Reflect.callMethod(sPolicyInstance, "updateMiuiOperatorConfig", -1);
                Reflect.callMethod(sPolicyInstance, "updateMiuiOperatorConfig", 0);
                Reflect.callMethod(sPolicyInstance, "updateMiuiOperatorConfig", 1);
                HookEnv.log(TAG + ": [v" + VERSION + "] Triggered updateMiuiOperatorConfig on slots -1, 0, 1");
            } catch (Throwable ignored) {}
        }

        // Push the VoNR carrier-config value into the live interactor flows so the
        // status bar re-evaluates immediately, without waiting for a broadcast.
        refreshVonrVisibilityOnInteractors();
        refreshWifiVolteVisibilityOnViewModels();

        // Live Direct Punch for tracked views on UI thread
        Handler handler = getMainHandler();
        Runnable punchRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (sTrackedViews) {
                        for (int i = sTrackedViews.size() - 1; i >= 0; i--) {
                            ImageView mobileTypeView = sTrackedViews.get(i).get();
                            if (mobileTypeView == null) {
                                sTrackedViews.remove(i);
                                continue;
                            }
                            ViewGroup parent = (ViewGroup) mobileTypeView.getParent();
                            if (parent == null) continue;

                            int slotId = getSlotIdForView(mobileTypeView);
                            IconConfig cfg = getConfigForSlot(slotId);

                            int volteViewId = parent.getResources().getIdentifier("mobile_volte", "id", "com.android.systemui");
                            ImageView volteView = findViewInAncestors(mobileTypeView, volteViewId);
                            if (volteView != null) {
                                trackVolteView(volteView);
                                if ("hide".equals(cfg.volte)) {
                                    volteView.setVisibility(View.GONE);
                                }
                                // Never overwrite the VoLTE/VoNR drawable or force the
                                // icon visible here: the operator-config update and the
                                // volteId flow hook already pick the correct VoNR/VoLTE
                                // resource, and forcing VISIBLE makes a stale icon
                                // survive a Wi-Fi/IMS state change.

                                // VoLTE/VoNR on Wi-Fi override (instant, no modem event needed)
                                applyWifiVolteVisibility(volteView, cfg, getSubIdForView(mobileTypeView));
                            }

                            int vowifiViewId = parent.getResources().getIdentifier("mobile_vowifi", "id", "com.android.systemui");
                            ImageView vowifiView = findViewInAncestors(mobileTypeView, vowifiViewId);
                            if (vowifiView != null && "hide".equals(cfg.vowifi)) {
                                // Style/visibility is driven by the operator config and
                                // the native vowifiVisible flow. Only "hide" is forced.
                                vowifiView.setVisibility(View.GONE);
                            }

                            int roamingViewId = parent.getResources().getIdentifier("mobile_roaming", "id", "com.android.systemui");
                            ImageView roamingView = roamingViewId != 0 ? (ImageView) parent.findViewById(roamingViewId) : null;
                            if (roamingView == null && roamingViewId != 0) {
                                if (parent.getParent() instanceof ViewGroup) {
                                    roamingView = (ImageView) ((ViewGroup) parent.getParent()).findViewById(roamingViewId);
                                }
                                if (roamingView == null && parent.getRootView() != null) {
                                    roamingView = (ImageView) parent.getRootView().findViewById(roamingViewId);
                                }
                            }
                            int smallRoamViewId = parent.getResources().getIdentifier("mobile_small_roam", "id", "com.android.systemui");
                            ImageView smallRoamView = smallRoamViewId != 0 ? (ImageView) parent.findViewById(smallRoamViewId) : null;
                            if (smallRoamView == null && smallRoamViewId != 0) {
                                if (parent.getParent() instanceof ViewGroup) {
                                    smallRoamView = (ImageView) ((ViewGroup) parent.getParent()).findViewById(smallRoamViewId);
                                }
                                if (smallRoamView == null && parent.getRootView() != null) {
                                    smallRoamView = (ImageView) parent.getRootView().findViewById(smallRoamViewId);
                                }
                            }

                            installSmallRoamClearance(parent);

                            int subId = getSubIdForView(mobileTypeView);
                            boolean isRoaming = isSubIdRoaming(subId, parent.getContext());
                            if (!isRoaming && roamingView != null && roamingView.getVisibility() == View.VISIBLE) {
                                isRoaming = true;
                                if (subId >= 0) sSubIdIsRoaming.put(subId, Boolean.TRUE);
                            }

                            if ("hide".equals(cfg.roaming)) {
                                if (roamingView != null) {
                                    roamingView.setVisibility(View.GONE);
                                    if (parent.getParent() instanceof ViewGroup) {
                                        try {
                                            Reflect.callMethod(parent.getParent(), "setChildVisible", roamingView, false);
                                        } catch (Throwable ignored) {}
                                    }
                                }
                                if (smallRoamView != null) smallRoamView.setVisibility(View.GONE);
                            } else if ("small".equals(cfg.roaming)) {
                                if (roamingView != null) {
                                    roamingView.setVisibility(View.GONE);
                                    if (parent.getParent() instanceof ViewGroup) {
                                        try {
                                            Reflect.callMethod(parent.getParent(), "setChildVisible", roamingView, false);
                                        } catch (Throwable ignored) {}
                                    }
                                }
                                if (smallRoamView != null) {
                                    if (isRoaming) {
                                        initRoamingResourceIds(parent.getContext());
                                        if (sResRoamSmall != 0) {
                                            smallRoamView.setImageResource(sResRoamSmall);
                                        }
                                        smallRoamView.setVisibility(View.VISIBLE);
                                        smallRoamView.invalidate();
                                        smallRoamView.requestLayout();
                                    } else {
                                        smallRoamView.setVisibility(View.GONE);
                                    }
                                }
                            } else {
                                if (isRoaming) {
                                    if (roamingView != null) {
                                        roamingView.setVisibility(View.VISIBLE);
                                        if (parent.getParent() instanceof ViewGroup) {
                                            try {
                                                Reflect.callMethod(parent.getParent(), "setChildVisible", roamingView, true);
                                            } catch (Throwable ignored) {}
                                        }
                                    }
                                    if (smallRoamView != null) smallRoamView.setVisibility(View.GONE);
                                } else {
                                    if (roamingView != null) roamingView.setVisibility(View.GONE);
                                    if (smallRoamView != null) smallRoamView.setVisibility(View.GONE);
                                }
                            }

                            int special5gId = parent.getResources().getIdentifier("mobile_special_5G", "id", "com.android.systemui");
                            ImageView special5gView = special5gId != 0 ? (ImageView) parent.findViewById(special5gId) : null;

                            Drawable d = mobileTypeView.getDrawable();
                            String text = "";
                            if (mobileTypeView.getTag() instanceof String) {
                                text = (String) mobileTypeView.getTag();
                            }
                            if (TextUtils.isEmpty(text) && d != null) {
                                text = getDrawableMobileType(d);
                            }

                            int radioType = getRadioTypeForView(mobileTypeView, text, cfg);

                            if (radioType == RADIO_TYPE_5G || radioType == RADIO_TYPE_5G_CA) {
                                boolean isCa = (radioType == RADIO_TYPE_5G_CA) || is5gCaText(text, cfg);
                                Object viewCaObj = Reflect.getAdditionalInstanceField(mobileTypeView, "isCaActive");
                                if (viewCaObj instanceof Boolean) {
                                    isCa = isCa || ((Boolean) viewCaObj).booleanValue();
                                }

                                String targetText;
                                if ("follow_ca".equals(cfg.basic)) {
                                    targetText = getEffective5gCaText(cfg);
                                    isCa = true;
                                } else {
                                    targetText = isCa ? getEffective5gCaText(cfg) : getEffective5gBasicText(cfg);
                                }

                                text = targetText;
                                mobileTypeView.setTag(text);
                                if (d != null) {
                                    try {
                                        Reflect.setObjectField(d, "mMobileType", text);
                                        Reflect.setAdditionalInstanceField(d, "actualMobileType", text);
                                        Reflect.callMethod(d, "measure");
                                    } catch (Throwable ignored) {}
                                }
                            } else if (radioType == RADIO_TYPE_4G || radioType == RADIO_TYPE_4G_CA || text.contains("4G") || text.contains("LTE") || text.contains("4.5G")) {
                                // 4G Network text reload
                                boolean is4gCa = (radioType == RADIO_TYPE_4G_CA) || text.contains("+") || text.contains("-A");
                                String targetText = text;
                                if (is4gCa) {
                                    if ("force_4g".equals(cfg.rat)) targetText = "4G+";
                                    else if ("force_lte".equals(cfg.rat)) targetText = "LTE+";
                                    else if ("force_4g_lte".equals(cfg.rat)) targetText = "4G LTE+";
                                    else if ("force_45g".equals(cfg.rat)) targetText = "4.5G+";
                                    else if ("force_ltea".equals(cfg.rat)) targetText = "LTE-A";
                                    else targetText = "4G+"; // "default"
                                } else {
                                    if ("force_4g".equals(cfg.rat)) targetText = "4G";
                                    else if ("force_lte".equals(cfg.rat)) targetText = "LTE";
                                    else if ("force_4g_lte".equals(cfg.rat)) targetText = "4G LTE";
                                    else if ("force_45g".equals(cfg.rat)) targetText = "4.5G";
                                    else if ("force_ltea".equals(cfg.rat)) targetText = "LTE";
                                    else targetText = "4G"; // "default"
                                }
                                text = targetText;
                                mobileTypeView.setTag(text);
                                if (d != null) {
                                    try {
                                        Reflect.setObjectField(d, "mMobileType", text);
                                        Reflect.setAdditionalInstanceField(d, "actualMobileType", text);
                                        Reflect.callMethod(d, "measure");
                                    } catch (Throwable ignored) {}
                                }
                            }

                            syncViewVisibility(mobileTypeView, special5gView, text, cfg);

                            mobileTypeView.requestLayout();
                            if (special5gView != null) special5gView.requestLayout();
                            if (parent != null) {
                                parent.requestLayout();
                                parent.invalidate();
                            }
                        }
                    }

                    // Independent pass over the live mobile_volte views: this
                    // applies the Wi-Fi preference immediately even when the
                    // mobile_type views could not be tracked on this ROM.
                    synchronized (sTrackedVolteViews) {
                        for (int i = sTrackedVolteViews.size() - 1; i >= 0; i--) {
                            ImageView volteView = sTrackedVolteViews.get(i).get();
                            if (volteView == null) {
                                sTrackedVolteViews.remove(i);
                                continue;
                            }
                            if (volteView.getParent() == null) continue;
                            int slotId = getSlotIdForView(volteView);
                            IconConfig cfg = getConfigForSlot(slotId);
                            applyWifiVolteVisibility(volteView, cfg, getSubIdForView(volteView));
                        }
                    }
                } catch (Throwable t) {
                    HookEnv.log(TAG + ": Direct punch live reload error: " + t);
                }
            }
        };
        if (handler != null) {
            handler.post(punchRunnable);
        } else {
            punchRunnable.run();
        }
    }

    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if ("com.android.phone".equals(lpparam.packageName)) {
            CellularFeatureHook.init(lpparam);
            return;
        }

        if (!"com.android.systemui".equals(lpparam.packageName)) {
            return;
        }

        HookEnv.log(TAG + ": [v" + VERSION + "] Initializing HyperIcons hook for com.android.systemui");
        loadConfig();

        HookEnv.group("SystemUI application", () -> hookSystemUIApplication(lpparam));
        HookEnv.group("Operator policy", () -> hookOperatorPolicy(lpparam));
        HookEnv.group("Mobile icon interactor", () -> hookMobileIconInteractor(lpparam));
        HookEnv.group("5G icon view model", () -> hookSpecial5gViewModel(lpparam));
        HookEnv.group("VoLTE view model", () -> hookVolteViewModel(lpparam));
        HookEnv.group("Mobile icon binder", () -> hookMobileIconBinder(lpparam));
        HookEnv.group("VoNR carrier config", () -> hookCarrierConfigVonr(lpparam));
        HookEnv.group("Ignore ULI", () -> Nsa5gHook.hookIgnoreUli(lpparam));
        HookEnv.group("NSA NR CA", () -> Nsa5gHook.hookNsaNrCa(lpparam));
    }

    private static void reportHooksLater(long delayMs) {
        Runnable report = () -> HookEnv.report(sContext, "systemui");
        Handler handler = getMainHandler();
        if (handler != null) {
            handler.postDelayed(report, delayMs);
        } else {
            report.run();
        }
    }

    /**
     * Override config_show_vonr_icon_in_statusbar inside SystemUI only.
     *
     * SystemUI reads the CarrierConfig bundle via CarrierConfigManager.getConfigForSubId(subId)
     * on every CARRIER_CONFIG_CHANGED emission. Returning a modified copy keeps the stock
     * cached bundle untouched and applies Default/Show/Hide without rebooting.
     */
    private void hookCarrierConfigVonr(LoadPackageParam lpparam) {
        try {
            Class<?> ccmClz = Reflect.findClassIfExists(
                    "android.telephony.CarrierConfigManager", lpparam.classLoader);
            if (ccmClz == null) {
                HookEnv.log(TAG + ": CarrierConfigManager not found, VoNR override inactive");
                return;
            }

            MethodHook overrideHook = new MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        if (Boolean.TRUE.equals(sVonrStockRead.get())) return;
                        Object result = param.getResult();
                        if (!(result instanceof PersistableBundle)) return;

                        int subId = -1;
                        if (param.args.length > 0 && param.args[0] instanceof Integer) {
                            subId = ((Integer) param.args[0]).intValue();
                        }
                        int slotId = getSlotIdForSubId(subId);
                        IconConfig cfg = getConfigForSlot(slotId);
                        if (cfg.vonrVisibility == null || "default".equals(cfg.vonrVisibility)) {
                            return;
                        }

                        boolean show = "show".equals(cfg.vonrVisibility);
                        PersistableBundle copy = new PersistableBundle((PersistableBundle) result);
                        copy.putBoolean("config_show_vonr_icon_in_statusbar", show);
                        param.setResult(copy);
                    } catch (Throwable t) {
                        HookEnv.log(TAG + ": VoNR carrier-config override failed: " + t);
                    }
                }
            };

            try {
                HookEnv.findAndHookMethod(ccmClz, "getConfigForSubId", int.class, overrideHook);
            } catch (Throwable t) {
                HookEnv.log(TAG + ": Failed to hook getConfigForSubId(int): " + t);
            }
            // Newer frameworks also expose a key-filtered overload; hook it defensively.
            try {
                HookEnv.findAndHookMethod(ccmClz, "getConfigForSubId",
                        int.class, String[].class, overrideHook);
            } catch (Throwable ignored) {}

            HookEnv.log(TAG + ": [v" + VERSION + "] CarrierConfig VoNR override hook installed");
        } catch (Throwable t) {
            HookEnv.log(TAG + ": hookCarrierConfigVonr error: " + t);
        }
    }

    private void hookSystemUIApplication(LoadPackageParam lpparam) {
        // HyperOS 4.0 uses application.impl.SystemUIApplicationImpl; AOSP uses SystemUIApplication.
        Class<?> appClz = null;
        String[] candidates = {
            lpparam.appClassName,
            "com.android.systemui.application.impl.SystemUIApplicationImpl",
            "com.android.systemui.SystemUIApplication",
        };
        for (String name : candidates) {
            if (name == null) continue;
            appClz = Reflect.findClassIfExists(name, lpparam.classLoader);
            if (appClz != null) break;
        }
        if (appClz != null) {
            // 1. Hook attachBaseContext for earliest context capture on cold boot
            HookEnv.findAndHookMethod(appClz, "attachBaseContext", Context.class, new MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context ctx = (Context) param.args[0];
                    if (ctx != null) {
                        updateContext(ctx);
                    }
                }
            });

            // 2. Hook onCreate
            HookEnv.findAndHookMethod(appClz, "onCreate", new MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context ctx = (Context) param.thisObject;
                    if (ctx != null) {
                        updateContext(ctx);
                        if (!sConfigLoaded && loadConfigFromProvider(ctx)) {
                            triggerStatusReload();
                        }
                    }
                }
            });
        }
    }

    private void hookOperatorPolicy(LoadPackageParam lpparam) {
        Class<?> policyClz = Reflect.findClassIfExists("com.android.systemui.MiuiOperatorCustomizedPolicy", lpparam.classLoader);
        if (policyClz == null) {
            HookEnv.log(TAG + ": MiuiOperatorCustomizedPolicy not found");
            return;
        }

        HookEnv.findAndHookMethod(policyClz, "start", new MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                sPolicyInstance = param.thisObject;
                try {
                    Context ctx = (Context) Reflect.getObjectField(param.thisObject, "mContext");
                    if (ctx != null) {
                        updateContext(ctx);
                        if (!sConfigLoaded && loadConfigFromProvider(ctx)) {
                            triggerStatusReload();
                        }
                    }
                } catch (Throwable t) {
                    HookEnv.log(TAG + ": Error getting mContext from policy: " + t);
                }
            }
        });

        HookEnv.findAndHookMethod(policyClz, "getStatusBarVolteType", int.class, new MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int slotId = (Integer) param.args[0];
                IconConfig cfg = getConfigForSlot(slotId);
                if ("intl_solid".equals(cfg.volte)) {
                    param.setResult(Integer.valueOf(0)); // Type 0: 实心胶囊
                } else if ("intl_4g".equals(cfg.volte)) {
                    param.setResult(Integer.valueOf(1)); // Type 1: 4G 标识组合
                } else if ("intl_hollow".equals(cfg.volte)) {
                    param.setResult(Integer.valueOf(2)); // Type 2: 无框纯字
                } else if ("intl_hd_voice".equals(cfg.volte)) {
                    param.setResult(Integer.valueOf(3)); // Type 3: HD Voice
                } else if ("china_hd".equals(cfg.volte) || "china_hd_plus".equals(cfg.volte)) {
                    param.setResult(Integer.valueOf(4)); // Type 4: 大 HD
                } else if ("intl_vo4g".equals(cfg.volte)) {
                    param.setResult(Integer.valueOf(5)); // Type 5: Vo4G
                }
            }
        });

        HookEnv.findAndHookMethod(policyClz, "getMiuiOperatorConfig", int.class, new MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object opConfig = param.getResult();
                if (opConfig == null) return;
                int slotId = (Integer) param.args[0];
                IconConfig cfg = getConfigForSlot(slotId);

                // 1. Universal 5G-A detection & display support across all carriers
                try {
                    Reflect.setBooleanField(opConfig, "support5GADisplay", true);
                } catch (Throwable ignored) {}

                // 2. Korean dynamic breathing 5G icon mode
                try {
                    if ("korean".equals(cfg.capsule)) {
                        Reflect.setBooleanField(opConfig, "showSpecial5GIcon", true);
                    } else {
                        Reflect.setBooleanField(opConfig, "showSpecial5GIcon", false);
                    }
                } catch (Throwable ignored) {}

                // 3. Customize mobileTypeName list
                try {
                    Object listObj = Reflect.getObjectField(opConfig, "mobileTypeName");
                    if (listObj instanceof List) {
                        List<String> list = (List<String>) listObj;
                        if (list.size() >= 14) {
                            ArrayList<String> modified = new ArrayList<String>(list);
                            applyConfigToList(modified, cfg);
                            // The config flow only re-emits when its content changes, so the NSA
                            // entry has to differ for a carrier count change to reach the label.
                            if (Nsa5gHook.isNrCaActive(getSubIdForSlotId(slotId))) {
                                modified.set(10, modified.get(12));
                            }
                            Reflect.setObjectField(opConfig, "mobileTypeName", modified);
                        }
                    }
                } catch (Throwable ignored) {}

                // 4. Always show RAT icon (showDataTypeDataDisconnected)
                try {
                    boolean isIntl = isInternationalRom();
                    if ("always".equals(cfg.alwaysRat) || "data_sim".equals(cfg.alwaysRat)) {
                        Reflect.setBooleanField(opConfig, "showDataTypeDataDisconnected", true);
                    } else if ("wifi_hide".equals(cfg.alwaysRat)) {
                        Reflect.setBooleanField(opConfig, "showDataTypeDataDisconnected", false);
                    } else {
                        // "default": restore system default (!isIntl)
                        Reflect.setBooleanField(opConfig, "showDataTypeDataDisconnected", !isIntl);
                    }
                } catch (Throwable ignored) {}

                // 5. VoLTE / VoNR style customization
                try {
                    initVolteResourceIds(sContext);
                    if ("hide".equals(cfg.volte)) {
                        Reflect.setBooleanField(opConfig, "hideVolte", true);
                        Reflect.setIntField(opConfig, "volteResId", 0);
                        Reflect.setIntField(opConfig, "vonrResId", 0);
                    } else if (!"default".equals(cfg.volte)) {
                        Reflect.setBooleanField(opConfig, "hideVolte", false);
                        int volteRes = getVolteResForStyle(cfg.volte, false);
                        int vonrRes = getVolteResForStyle(cfg.volte, true);
                        if (volteRes != 0) Reflect.setIntField(opConfig, "volteResId", volteRes);
                        if (vonrRes != 0) Reflect.setIntField(opConfig, "vonrResId", vonrRes);
                    }
                } catch (Throwable ignored) {}

                // 6. VoWiFi style customization
                try {
                    initVowifiResourceIds(sContext);
                    if ("hide".equals(cfg.vowifi)) {
                        Reflect.setBooleanField(opConfig, "hideVowifi", true);
                        Reflect.setIntField(opConfig, "vowifiResId", 0);
                    } else if (!"default".equals(cfg.vowifi)) {
                        Reflect.setBooleanField(opConfig, "hideVowifi", false);
                        int vowifiRes = getVowifiResForStyle(cfg.vowifi, slotId);
                        if (vowifiRes != 0) {
                            Reflect.setIntField(opConfig, "vowifiResId", vowifiRes);
                        }
                    }
                } catch (Throwable ignored) {}

                // 7. Roaming icon customization
                try {
                    if ("hide".equals(cfg.roaming) || "small".equals(cfg.roaming)) {
                        Reflect.setBooleanField(opConfig, "hideNationalRoaming", true);
                    }
                } catch (Throwable ignored) {}
            }
        });
    }

    private static void applyConfigToList(List<String> list, IconConfig cfg) {
        IconConfig c = orGlobal(cfg);
        IconPolicy.applyMobileTypeNames(list, c.ca, c.caText, c.basic, c.basicText, c.rat);
    }

    private static void applyConfigToList(List<String> list) {
        applyConfigToList(list, null);
    }

    private void hookMobileIconInteractor(LoadPackageParam lpparam) {
        Class<?> interactorClz = Reflect.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.domain.interactor.MiuiMobileIconInteractorImpl",
            lpparam.classLoader
        );
        if (interactorClz == null) {
            HookEnv.log(TAG + ": MiuiMobileIconInteractorImpl not found");
            return;
        }

        // Track live interactors so the VoNR preference can be pushed instantly.
        try {
            HookEnv.hookAllConstructors(interactorClz, new MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    synchronized (sMobileIconInteractors) {
                        sMobileIconInteractors.add(new WeakReference<Object>(param.thisObject));
                        while (sMobileIconInteractors.size() > 8) {
                            sMobileIconInteractors.remove(0);
                        }
                    }
                }
            });
        } catch (Throwable t) {
            HookEnv.log(TAG + ": Failed to hook MiuiMobileIconInteractorImpl constructor: " + t);
        }

        HookEnv.findAndHookMethod(interactorClz, "getMobileTypeName", int.class, new MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int index = (Integer) param.args[0];
                int subId = -1;
                try {
                    subId = Reflect.getIntField(param.thisObject, "subId");
                } catch (Throwable ignored) {}
                index = IconPolicy.mobileTypeIndexForNsa(index, Nsa5gHook.isNrCaActive(subId));
                int slotId = getSlotIdForSubId(subId);
                IconConfig cfg = getConfigForSlot(slotId);

                int radioType = RADIO_TYPE_UNKNOWN;
                if (index >= 12 && index <= 13) {
                    radioType = RADIO_TYPE_5G_CA;
                    sIs5GConnected = true;
                    sIs5GCaConnected = true;
                    if (subId >= 0) {
                        sSubIdIs5G.put(subId, Boolean.TRUE);
                        sSubIdIs5GCa.put(subId, Boolean.TRUE);
                        sSubIdRadioType.put(subId, Integer.valueOf(radioType));
                    }
                } else if (index >= 10 && index <= 11) {
                    radioType = RADIO_TYPE_5G;
                    sIs5GConnected = true;
                    if (subId >= 0) {
                        sSubIdIs5G.put(subId, Boolean.TRUE);
                        sSubIdIs5GCa.put(subId, Boolean.FALSE);
                        sSubIdRadioType.put(subId, Integer.valueOf(radioType));
                    }
                } else if (index == 7 || index == 8) {
                    radioType = RADIO_TYPE_4G_CA;
                    if (subId >= 0) {
                        sSubIdIs5G.put(subId, Boolean.FALSE);
                        sSubIdIs5GCa.put(subId, Boolean.FALSE);
                        sSubIdRadioType.put(subId, Integer.valueOf(radioType));
                    }
                } else if (index == 6 || index == 9) {
                    radioType = RADIO_TYPE_4G;
                    if (subId >= 0) {
                        sSubIdIs5G.put(subId, Boolean.FALSE);
                        sSubIdIs5GCa.put(subId, Boolean.FALSE);
                        sSubIdRadioType.put(subId, Integer.valueOf(radioType));
                    }
                } else if (index < 6) {
                    radioType = RADIO_TYPE_2G_3G;
                    if (subId >= 0) {
                        sSubIdIs5G.put(subId, Boolean.FALSE);
                        sSubIdIs5GCa.put(subId, Boolean.FALSE);
                        sSubIdRadioType.put(subId, Integer.valueOf(radioType));
                    }
                }

                // 1. 5G CA (Index 12: 5G+, Index 13: 5GA)
                if (index == 12 || index == 13) {
                    String ca = getTarget5gCaText(cfg);
                    if (!TextUtils.isEmpty(ca)) {
                        param.setResult(ca);
                        return;
                    } else {
                        param.setResult(index == 13 ? "5GA" : "5G+");
                        return;
                    }
                }

                // 2. Base 5G (Index 10: NSA, Index 11: SA)
                if (index == 10 || index == 11) {
                    param.setResult(getEffective5gBasicText(cfg));
                    return;
                }

                // 3. 4G Single Carrier (Index 6, 9)
                if (index == 6 || index == 9) {
                    if ("force_4g".equals(cfg.rat)) {
                        param.setResult("4G");
                        return;
                    } else if ("force_lte".equals(cfg.rat)) {
                        param.setResult("LTE");
                        return;
                    } else if ("force_4g_lte".equals(cfg.rat)) {
                        param.setResult("4G LTE");
                        return;
                    } else if ("force_45g".equals(cfg.rat)) {
                        param.setResult("4.5G");
                        return;
                    } else if ("force_ltea".equals(cfg.rat)) {
                        param.setResult("LTE");
                        return;
                    }
                }

                // 4. 4G CA (Index 7, 8)
                if (index == 7 || index == 8) {
                    if ("force_4g".equals(cfg.rat)) {
                        param.setResult("4G+");
                        return;
                    } else if ("force_lte".equals(cfg.rat)) {
                        param.setResult("LTE+");
                        return;
                    } else if ("force_4g_lte".equals(cfg.rat)) {
                        param.setResult("4G LTE+");
                        return;
                    } else if ("force_45g".equals(cfg.rat)) {
                        param.setResult("4.5G+");
                        return;
                    } else if ("force_ltea".equals(cfg.rat)) {
                        param.setResult("LTE-A");
                        return;
                    }
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object resObj = param.getResult();
                if (!(resObj instanceof String)) return;
                String text = (String) resObj;
                if (TextUtils.isEmpty(text)) return;

                int subId = -1;
                try {
                    subId = Reflect.getIntField(param.thisObject, "subId");
                } catch (Throwable ignored) {}
                int slotId = getSlotIdForSubId(subId);
                IconConfig cfg = getConfigForSlot(slotId);

                int textType = radioTypeForText(text, cfg);
                if (textType == RADIO_TYPE_5G_CA) {
                    String ca = getTarget5gCaText(cfg);
                    if (!TextUtils.isEmpty(ca)) {
                        param.setResult(ca);
                    }
                } else if (textType == RADIO_TYPE_5G) {
                    param.setResult(getEffective5gBasicText(cfg));
                } else if ("4G".equals(text) || "LTE".equals(text) || "4G LTE".equals(text) || "4.5G".equals(text)) {
                    if ("force_4g".equals(cfg.rat)) param.setResult("4G");
                    else if ("force_lte".equals(cfg.rat)) param.setResult("LTE");
                    else if ("force_4g_lte".equals(cfg.rat)) param.setResult("4G LTE");
                    else if ("force_45g".equals(cfg.rat)) param.setResult("4.5G");
                    else if ("force_ltea".equals(cfg.rat)) param.setResult("LTE");
                } else if ("4G+".equals(text) || "LTE+".equals(text) || "4G LTE+".equals(text) || "4.5G+".equals(text) || "LTE-A".equals(text)) {
                    if ("force_4g".equals(cfg.rat)) param.setResult("4G+");
                    else if ("force_lte".equals(cfg.rat)) param.setResult("LTE+");
                    else if ("force_4g_lte".equals(cfg.rat)) param.setResult("4G LTE+");
                    else if ("force_45g".equals(cfg.rat)) param.setResult("4.5G+");
                    else if ("force_ltea".equals(cfg.rat)) param.setResult("LTE-A");
                }
            }
        });
    }

    private void hookSpecial5gViewModel(LoadPackageParam lpparam) {
        Class<?> special5gLambda = Reflect.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiCellularIconVM$showSpecial5GIcon$2",
            lpparam.classLoader
        );
        if (special5gLambda != null) {
            HookEnv.findAndHookMethod(special5gLambda, "invokeSuspend", Object.class, new MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        // Native Xiaomi logic: return Boolean.valueOf(this.Z$0 && this.Z$1);
                        // this.Z$0: showSpecial5GIcon opConfig flag
                        // this.Z$1: fiveGConnected (true if truly on 5G network; false on 4G/3G/2G)
                        boolean is5GConnected = Reflect.getBooleanField(param.thisObject, "Z$1");
                        sIs5GConnected = is5GConnected;

                        if (!is5GConnected) {
                            // If NOT connected to 5G: never show special 5G icon! (System will show 4G/LTE naturally)
                            param.setResult(Boolean.FALSE);
                            return;
                        }

                        // Connected to 5G:
                        int subId = -1;
                        try {
                            subId = Reflect.getIntField(param.thisObject, "subId");
                        } catch (Throwable ignored) {}
                        int slotId = getSlotIdForSubId(subId);
                        IconConfig cfg = getConfigForSlot(slotId);
                        if (!"korean".equals(cfg.capsule)) {
                            // Non-Capsule mode: never show special 5G icon
                            param.setResult(Boolean.FALSE);
                            return;
                        }

                        // Capsule Mode on 5G: allow special 5G icon flow
                        param.setResult(Boolean.TRUE);
                    } catch (Throwable t) {
                        HookEnv.log(TAG + ": hookSpecial5gViewModel error: " + t);
                    }
                }
            });
        }
    }

    private void hookVolteViewModel(final LoadPackageParam lpparam) {
        // Track each MiuiCellularIconVM with its subscription so the VoLTE/VoNR
        // Wi-Fi preference can be pushed into its live state flow.
        Class<?> cellularVmClz = Reflect.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiCellularIconVM",
            lpparam.classLoader
        );
        if (cellularVmClz != null) {
            try {
                HookEnv.hookAllConstructors(cellularVmClz, new MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            for (Object arg : param.args) {
                                if (arg == null) continue;
                                if (arg.getClass().getName().endsWith("MiuiMobileIconInteractorImpl")) {
                                    trackCellularVm(param.thisObject, arg);
                                    break;
                                }
                            }
                        } catch (Throwable ignored) {}
                    }
                });
            } catch (Throwable t) {
                HookEnv.log(TAG + ": Failed to hook MiuiCellularIconVM constructor: " + t);
            }
        }

        Class<?> vmImplClz = Reflect.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiMobileIconVMImpl",
            lpparam.classLoader
        );
        if (vmImplClz != null) {
            try {
                HookEnv.hookAllMethods(vmImplClz, "getCellProvider", new MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            trackCellularVm(param.getResult(),
                                    Reflect.getObjectField(param.thisObject, "iconInteractor"));
                        } catch (Throwable ignored) {}
                    }
                });
            } catch (Throwable t) {
                HookEnv.log(TAG + ": Failed to hook MiuiMobileIconVMImpl.getCellProvider: " + t);
            }
        }

        // 1. Hook volteVisibleGlobal to cleanly hide when pref is 'hide'
        Class<?> volteVisibleLambda = Reflect.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiCellularIconVM$volteVisibleGlobal$2",
            lpparam.classLoader
        );
        if (volteVisibleLambda != null) {
            HookEnv.findAndHookMethod(volteVisibleLambda, "invokeSuspend", Object.class, new MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    int subId = -1;
                    try {
                        subId = Reflect.getIntField(param.thisObject, "subId");
                    } catch (Throwable ignored) {}
                    int slotId = getSlotIdForSubId(subId);
                    IconConfig cfg = getConfigForSlot(slotId);

                    Object res = param.getResult();
                    if (res == null) return;

                    Object first;
                    Object second;
                    try {
                        first = Reflect.callMethod(res, "getFirst");
                        second = Reflect.callMethod(res, "getSecond");
                    } catch (Throwable t) {
                        return;
                    }

                    String wifiPref = getLambdaWifiVoltePref(subId);
                    boolean wifi = false;
                    if ("hide".equals(wifiPref)) {
                        try {
                            wifi = Reflect.getBooleanField(param.thisObject, "Z$0");
                        } catch (Throwable t) {
                            wifi = isWifiConnected();
                        }
                    }
                    boolean stockFirst = Boolean.TRUE.equals(first);
                    boolean stockSecond = Boolean.TRUE.equals(second);
                    boolean[] pair = IconPolicy.volteVisiblePair(stockFirst, stockSecond, cfg.volte, wifiPref, wifi);
                    boolean changed = pair[0] != stockFirst || pair[1] != stockSecond;
                    first = Boolean.valueOf(pair[0]);
                    second = Boolean.valueOf(pair[1]);

                    if (changed) {
                        try {
                            Class<?> pairClz = Reflect.findClass("kotlin.Pair", lpparam.classLoader);
                            param.setResult(Reflect.newInstance(pairClz, first, second));
                        } catch (Throwable t) {
                            HookEnv.log(TAG + ": Error modifying volteVisibleGlobal: " + t);
                        }
                    }
                }
            });
        }

        // 2. Hook volteId stateflow combiner to dynamically switch drawable id
        Class<?> volteIdLambda = Reflect.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiCellularIconVM$volteId$3",
            lpparam.classLoader
        );
        if (volteIdLambda != null) {
            HookEnv.findAndHookMethod(volteIdLambda, "invokeSuspend", Object.class, new MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    int subId = -1;
                    try {
                        subId = Reflect.getIntField(param.thisObject, "subId");
                    } catch (Throwable ignored) {}
                    int slotId = getSlotIdForSubId(subId);
                    IconConfig cfg = getConfigForSlot(slotId);

                    if ("default".equals(cfg.volte)) {
                        return;
                    }

                    initVolteResourceIds(sContext);

                    if ("hide".equals(cfg.volte)) {
                        param.setResult(Integer.valueOf(0));
                        return;
                    }

                    Object res = param.getResult();
                    boolean isVonr = false;
                    if (res instanceof Number) {
                        int cur = ((Number) res).intValue();
                        isVonr = (cur == sResVonrSolid || cur == sResVonrNoFrame);
                    }

                    int targetRes = getVolteResForStyle(cfg.volte, isVonr);
                    if (targetRes != 0) {
                        param.setResult(Integer.valueOf(targetRes));
                    }
                }
            });
        }
    }

    private static Class<?> findEmitCollectorClass(ClassLoader cl) {
        if (cl == null) return null;
        // 1. Direct match on standard HyperOS class name
        Class<?> direct = Reflect.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder$bind$1$1$13$1",
            cl
        );
        if (direct != null && hasCollectorFields(direct)) {
            return direct;
        }

        // 2. Proactive heuristic scan across potential R8 numbered inner classes
        for (int i = 1; i <= 30; i++) {
            String name = "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder$bind$1$1$" + i + "$1";
            Class<?> candidate = Reflect.findClassIfExists(name, cl);
            if (candidate != null && hasCollectorFields(candidate)) {
                HookEnv.log(TAG + ": [v" + VERSION + "] Dynamically discovered collector class: " + name);
                return candidate;
            }
        }
        return direct;
    }

    private static boolean hasCollectorFields(Class<?> clz) {
        if (clz == null) return false;
        boolean hasSatellite = false;
        boolean hasClassId = false;
        try {
            for (java.lang.reflect.Field f : clz.getDeclaredFields()) {
                if ("$satellite".equals(f.getName())) hasSatellite = true;
                if (f.getName().contains("classId")) hasClassId = true;
            }
        } catch (Throwable ignored) {}
        return hasSatellite && hasClassId;
    }

    private static Class<?> findMobileGroupCollectorClass(ClassLoader cl) {
        if (cl == null) return null;
        Class<?> direct = Reflect.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder$bind$1$1$11$1",
            cl
        );
        if (direct != null && hasMobileGroupFields(direct)) {
            return direct;
        }
        for (int i = 1; i <= 30; i++) {
            String name = "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder$bind$1$1$" + i + "$1";
            Class<?> candidate = Reflect.findClassIfExists(name, cl);
            if (candidate != null && hasMobileGroupFields(candidate)) {
                HookEnv.log(TAG + ": [v" + VERSION + "] Dynamically discovered mobileGroup collector class: " + name);
                return candidate;
            }
        }
        return direct;
    }

    private static boolean hasMobileGroupFields(Class<?> clz) {
        if (clz == null) return false;
        boolean hasGroup = false;
        boolean hasClassId = false;
        try {
            for (java.lang.reflect.Field f : clz.getDeclaredFields()) {
                if (f.getName().contains("mobileGroup") || f.getName().contains("MobileGroup") || "$mobileGroup".equals(f.getName())) hasGroup = true;
                if (f.getName().contains("classId")) hasClassId = true;
            }
        } catch (Throwable ignored) {}
        return hasGroup && hasClassId;
    }

    private void hookMobileIconBinder(LoadPackageParam lpparam) {
        Class<?> binderClz = Reflect.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder",
            lpparam.classLoader
        );
        if (binderClz == null) return;
        sBinderClz = binderClz;

        // Early SubId tagging on bind
        Class<?> locVMClz = Reflect.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.LocationBasedMobileViewModel",
            lpparam.classLoader
        );
        Class<?> miuiVMClz = Reflect.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiMobileIconViewModel",
            lpparam.classLoader
        );
        Class<?> loggerClz = Reflect.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.MobileViewLogger",
            lpparam.classLoader
        );

        if (locVMClz != null && miuiVMClz != null && loggerClz != null) {
            HookEnv.findAndHookMethod(
                binderClz,
                "bind",
                ViewGroup.class,
                locVMClz,
                miuiVMClz,
                loggerClz,
                new MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            ViewGroup viewGroup = (ViewGroup) param.args[0];
                            Object locationVM = param.args[1];
                            if (viewGroup == null || locationVM == null) return;

                            if (!sConfigLoaded && viewGroup.getContext() != null) {
                                updateContext(viewGroup.getContext());
                            }

                            // Track the VoLTE child even if the subscription id is not
                            // resolved yet; the direct Wi-Fi pass can still resolve it
                            // from the view tags on the next reload.
                            int earlyVolteId = viewGroup.getResources().getIdentifier(
                                    "mobile_volte", "id", "com.android.systemui");
                            if (earlyVolteId != 0) {
                                View maybeVolte = viewGroup.findViewById(earlyVolteId);
                                if (maybeVolte instanceof ImageView) {
                                    trackVolteView((ImageView) maybeVolte);
                                }
                            }

                            int subId = -1;
                            try {
                                Object commonImpl = Reflect.getObjectField(locationVM, "commonImpl");
                                if (commonImpl != null) {
                                    subId = Reflect.getIntField(commonImpl, "subscriptionId");
                                }
                            } catch (Throwable ignored) {}
                            if (subId < 0) {
                                try {
                                    subId = Reflect.getIntField(viewGroup, "subId");
                                } catch (Throwable ignored) {}
                            }
                            if (subId < 0) return;

                            int slotId = getSlotIdForSubId(subId);
                            if (slotId < 0) {
                                try {
                                    slotId = Reflect.getIntField(viewGroup, "slotId");
                                } catch (Throwable ignored) {}
                            }
                            if (slotId < 0) {
                                try {
                                    slotId = Reflect.getIntField(viewGroup, "mSlotId");
                                } catch (Throwable ignored) {}
                            }

                            Reflect.setAdditionalInstanceField(viewGroup, "subId", Integer.valueOf(subId));
                            if (slotId >= 0) {
                                Reflect.setAdditionalInstanceField(viewGroup, "slotId", Integer.valueOf(slotId));
                            }

                            // Bind gives us the exact wrapper VM and interactor for
                            // this subscription. Track both here so the instant
                            // Wi-Fi/VoLTE push works even when the VM and interactor
                            // constructors were removed by the ROM optimizer.
                            Object miuiVm = param.args.length > 2 ? param.args[2] : null;
                            if (miuiVm != null) {
                                Object interactor = null;
                                try {
                                    interactor = Reflect.getObjectField(miuiVm, "iconInteractor");
                                } catch (Throwable ignored) {}
                                Object cellVm = null;
                                try {
                                    cellVm = Reflect.callMethod(miuiVm, "getCellProvider");
                                } catch (Throwable ignored) {}
                                if (interactor != null) {
                                    if (cellVm != null) {
                                        trackCellularVm(cellVm, interactor);
                                    } else {
                                        trackInteractor(interactor);
                                    }
                                }
                            }

                            int containerId = viewGroup.getResources().getIdentifier("mobile_signal_container", "id", "com.android.systemui");
                            ViewGroup container = containerId != 0 ? (ViewGroup) viewGroup.findViewById(containerId) : viewGroup;
                            if (container != null) {
                                int typeId = viewGroup.getResources().getIdentifier("mobile_type", "id", "com.android.systemui");
                                ImageView mobileType = typeId != 0 ? (ImageView) container.findViewById(typeId) : null;
                                if (mobileType != null) {
                                    Reflect.setAdditionalInstanceField(mobileType, "subId", Integer.valueOf(subId));
                                    if (slotId >= 0) {
                                        Reflect.setAdditionalInstanceField(mobileType, "slotId", Integer.valueOf(slotId));
                                    }
                                    trackActiveView(mobileType);
                                }

                                int special5gId = viewGroup.getResources().getIdentifier("mobile_special_5G", "id", "com.android.systemui");
                                ImageView special5G = special5gId != 0 ? (ImageView) container.findViewById(special5gId) : null;
                                if (special5G != null) {
                                    Reflect.setAdditionalInstanceField(special5G, "subId", Integer.valueOf(subId));
                                    if (slotId >= 0) {
                                        Reflect.setAdditionalInstanceField(special5G, "slotId", Integer.valueOf(slotId));
                                    }
                                }
                            }

                            int volteId = viewGroup.getResources().getIdentifier("mobile_volte", "id", "com.android.systemui");
                            ImageView volteView = volteId != 0 ? (ImageView) viewGroup.findViewById(volteId) : null;
                            if (volteView != null) {
                                Reflect.setAdditionalInstanceField(volteView, "subId", Integer.valueOf(subId));
                                if (slotId >= 0) {
                                    Reflect.setAdditionalInstanceField(volteView, "slotId", Integer.valueOf(slotId));
                                }
                                trackVolteView(volteView);
                            }

                            int vowifiId = viewGroup.getResources().getIdentifier("mobile_vowifi", "id", "com.android.systemui");
                            ImageView vowifiView = vowifiId != 0 ? (ImageView) viewGroup.findViewById(vowifiId) : null;
                            if (vowifiView != null) {
                                Reflect.setAdditionalInstanceField(vowifiView, "subId", Integer.valueOf(subId));
                                if (slotId >= 0) {
                                    Reflect.setAdditionalInstanceField(vowifiView, "slotId", Integer.valueOf(slotId));
                                }
                            }

                            int roamingId = viewGroup.getResources().getIdentifier("mobile_roaming", "id", "com.android.systemui");
                            ImageView roamingView = roamingId != 0 ? (ImageView) viewGroup.findViewById(roamingId) : null;
                            if (roamingView != null) {
                                Reflect.setAdditionalInstanceField(roamingView, "subId", Integer.valueOf(subId));
                                if (slotId >= 0) {
                                    Reflect.setAdditionalInstanceField(roamingView, "slotId", Integer.valueOf(slotId));
                                }
                            }

                            int smallRoamId = viewGroup.getResources().getIdentifier("mobile_small_roam", "id", "com.android.systemui");
                            ImageView smallRoamView = smallRoamId != 0 ? (ImageView) viewGroup.findViewById(smallRoamId) : null;
                            if (smallRoamView != null) {
                                Reflect.setAdditionalInstanceField(smallRoamView, "subId", Integer.valueOf(subId));
                                if (slotId >= 0) {
                                    Reflect.setAdditionalInstanceField(smallRoamView, "slotId", Integer.valueOf(slotId));
                                }
                                if (smallRoamView.getParent() instanceof ViewGroup) {
                                    installSmallRoamClearance((ViewGroup) smallRoamView.getParent());
                                }
                            }
                        } catch (Throwable t) {
                            HookEnv.log(TAG + ": bind hook error: " + t);
                        }
                    }
                }
            );
        }
        Class<?> drawableClz = Reflect.findClassIfExists(
            "com.miui.systemui.statusbar.views.MobileTypeDrawable",
            lpparam.classLoader
        );
        if (drawableClz == null) return;

        // 0. Hook getIntrinsicHeight and getIntrinsicWidth to eliminate ImageView fitCenter matrix translation offset
        HookEnv.findAndHookMethod(drawableClz, "getIntrinsicHeight", new MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Drawable drawable = (Drawable) param.thisObject;
                String mobileType = getDrawableMobileType(drawable);
                View v = (View) Reflect.getAdditionalInstanceField(drawable, "view");
                int slotId = getSlotIdForView(v);
                IconConfig cfg = getConfigForSlot(slotId);
                if ("korean".equals(cfg.capsule) && isCapsuleTargetText(mobileType, cfg)) {
                    param.setResult((int) Math.ceil(dpToPx(9.0f)));
                }
            }
        });

        HookEnv.findAndHookMethod(drawableClz, "getIntrinsicWidth", new MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Drawable drawable = (Drawable) param.thisObject;
                String mobileType = getDrawableMobileType(drawable);
                View v = (View) Reflect.getAdditionalInstanceField(drawable, "view");
                int slotId = getSlotIdForView(v);
                IconConfig cfg = getConfigForSlot(slotId);
                if ("korean".equals(cfg.capsule) && isCapsuleTargetText(mobileType, cfg)) {
                    Paint textPaint = (Paint) Reflect.getObjectField(drawable, "mMobileTypeTextPaint");
                    textPaint.setTextSize(dpToPx(6.1f));
                    float measuredW = textPaint.measureText(mobileType);
                    float padX = dpToPx(2.5f);
                    param.setResult((int) Math.ceil(measuredW + padX * 2f));
                }
            }
        });

        HookEnv.findAndHookMethod(drawableClz, "measure", new MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Drawable drawable = (Drawable) param.thisObject;
                String mobileType = getDrawableMobileType(drawable);
                View v = (View) Reflect.getAdditionalInstanceField(drawable, "view");
                int slotId = getSlotIdForView(v);
                IconConfig cfg = getConfigForSlot(slotId);
                Paint textPaint = (Paint) Reflect.getObjectField(drawable, "mMobileTypeTextPaint");
                int mMobileTypeSize = Reflect.getIntField(drawable, "mMobileTypeSize");
                if ("korean".equals(cfg.capsule) && isCapsuleTargetText(mobileType, cfg)) {
                    textPaint.setTextSize(dpToPx(6.1f));
                } else if (mMobileTypeSize > 0) {
                    textPaint.setTextSize(mMobileTypeSize);
                }
            }
        });

        // 1. View layout params update hook (Per-slot isolated mutual exclusion & Vertical alignment calibration)
        HookEnv.findAndHookMethod(
            binderClz,
            "updateMobileTypeLayoutParams",
            drawableClz,
            String.class,
            ImageView.class,
            new MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        String str = (String) param.args[1];
                        ImageView mobileTypeView = (ImageView) param.args[2];
                        Drawable drawable = (Drawable) param.args[0];
                        if (mobileTypeView == null || str == null) return;

                        // Save str to tag for reliable immediate retrieval in emit
                        mobileTypeView.setTag(str);

                        // Associate mobileTypeView with drawable
                        if (drawable != null) {
                            Reflect.setAdditionalInstanceField(drawable, "view", mobileTypeView);
                            Reflect.setAdditionalInstanceField(drawable, "actualMobileType", str);
                        }

                        int subId = getSubIdForView(mobileTypeView);
                        int slotId = getSlotIdForView(mobileTypeView);
                        IconConfig cfg = getConfigForSlot(slotId);
                        int rType = RADIO_TYPE_UNKNOWN;

                        int textType = radioTypeForText(str, cfg);
                        if (textType == RADIO_TYPE_5G_CA) {
                            rType = RADIO_TYPE_5G_CA;
                            sIs5GConnected = true;
                            sIs5GCaConnected = true;
                            if (subId >= 0) {
                                sSubIdIs5G.put(subId, Boolean.TRUE);
                                sSubIdIs5GCa.put(subId, Boolean.TRUE);
                                sSubIdRadioType.put(subId, Integer.valueOf(rType));
                            }
                            Reflect.setAdditionalInstanceField(mobileTypeView, "isCaActive", Boolean.TRUE);
                        } else if (textType == RADIO_TYPE_5G) {
                            rType = RADIO_TYPE_5G;
                            sIs5GConnected = true;
                            if (subId >= 0) {
                                sSubIdIs5G.put(subId, Boolean.TRUE);
                                sSubIdRadioType.put(subId, Integer.valueOf(rType));
                            }
                            if (!"follow_ca".equals(cfg.basic)) {
                                sIs5GCaConnected = false;
                                if (subId >= 0) {
                                    sSubIdIs5GCa.put(subId, Boolean.FALSE);
                                }
                                Reflect.setAdditionalInstanceField(mobileTypeView, "isCaActive", Boolean.FALSE);
                            }
                        } else if (textType == RADIO_TYPE_4G_CA) {
                            rType = RADIO_TYPE_4G_CA;
                            if (subId >= 0) {
                                sSubIdIs5G.put(subId, Boolean.FALSE);
                                sSubIdIs5GCa.put(subId, Boolean.FALSE);
                                sSubIdRadioType.put(subId, Integer.valueOf(rType));
                            }
                            Reflect.setAdditionalInstanceField(mobileTypeView, "isCaActive", Boolean.FALSE);
                        } else if (textType == RADIO_TYPE_4G) {
                            rType = RADIO_TYPE_4G;
                            if (subId >= 0) {
                                sSubIdIs5G.put(subId, Boolean.FALSE);
                                sSubIdIs5GCa.put(subId, Boolean.FALSE);
                                sSubIdRadioType.put(subId, Integer.valueOf(rType));
                            }
                            Reflect.setAdditionalInstanceField(mobileTypeView, "isCaActive", Boolean.FALSE);
                        } else if (textType == RADIO_TYPE_2G_3G) {
                            rType = RADIO_TYPE_2G_3G;
                            if (subId >= 0) {
                                sSubIdIs5G.put(subId, Boolean.FALSE);
                                sSubIdIs5GCa.put(subId, Boolean.FALSE);
                                sSubIdRadioType.put(subId, Integer.valueOf(rType));
                            }
                            Reflect.setAdditionalInstanceField(mobileTypeView, "isCaActive", Boolean.FALSE);
                        } else if (TextUtils.isEmpty(str)) {
                            // SystemUI clears the label when the SIM is out of service. Forget the
                            // last network too, or the next reload writes its label back.
                            if (subId >= 0) {
                                sSubIdIs5G.put(subId, Boolean.FALSE);
                                sSubIdIs5GCa.put(subId, Boolean.FALSE);
                                sSubIdRadioType.remove(subId);
                            }
                            Reflect.setAdditionalInstanceField(mobileTypeView, "isCaActive", Boolean.FALSE);
                            Reflect.setAdditionalInstanceField(mobileTypeView, "radioType", Integer.valueOf(RADIO_TYPE_UNKNOWN));
                        }

                        if (rType != RADIO_TYPE_UNKNOWN) {
                            Reflect.setAdditionalInstanceField(mobileTypeView, "radioType", Integer.valueOf(rType));
                        }

                        trackActiveView(mobileTypeView);

                        ViewGroup parent = (ViewGroup) mobileTypeView.getParent();
                        if (parent == null) return;

                        int special5gId = parent.getResources().getIdentifier("mobile_special_5G", "id", "com.android.systemui");
                        ImageView special5gView = special5gId != 0 ? (ImageView) parent.findViewById(special5gId) : null;

                        syncViewVisibility(mobileTypeView, special5gView, str, cfg);
                    } catch (Throwable t) {
                        HookEnv.log(TAG + ": updateMobileTypeLayoutParams hook error: " + t);
                    }
                }
            }
        );

        // 2. Coroutine visibility emission hook (Per-slot atomic mutual exclusion, NEVER kill views on uninitialized empty text)
        Class<?> emitCollectorClz = findEmitCollectorClass(lpparam.classLoader);
        if (emitCollectorClz != null) {
            HookEnv.log(TAG + ": [v" + VERSION + "] Hooking emitCollector on " + emitCollectorClz.getName());
            HookEnv.findAndHookMethod(
                emitCollectorClz,
                "emit",
                Object.class,
                "kotlin.coroutines.Continuation",
                new MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            int classId = Reflect.getIntField(param.thisObject, "$r8$classId");
                            ImageView targetView = (ImageView) Reflect.getObjectField(param.thisObject, "$satellite");
                            if (targetView == null) return;

                            Context ctx = targetView.getContext();
                            int viewId = targetView.getId();
                            int smallRoamId = ctx != null ? ctx.getResources().getIdentifier("mobile_small_roam", "id", "com.android.systemui") : 0;
                            if ((smallRoamId != 0 && viewId == smallRoamId) || classId == 3) {
                                int slotId = getSlotIdForView(targetView);
                                int subId = getSubIdForView(targetView);
                                IconConfig cfg = getConfigForSlot(slotId);

                                boolean systemRoaming = Boolean.TRUE.equals(param.args[0]);
                                if (subId >= 0 && systemRoaming) {
                                    sSubIdIsRoaming.put(subId, Boolean.TRUE);
                                }
                                boolean isRoaming = isSubIdRoaming(subId, ctx) || systemRoaming;

                                if ("hide".equals(cfg.roaming)) {
                                    param.args[0] = Boolean.FALSE;
                                    targetView.setVisibility(View.GONE);
                                    return;
                                } else if ("small".equals(cfg.roaming)) {
                                    if (ctx != null) initRoamingResourceIds(ctx);
                                    if (sResRoamSmall != 0) {
                                        targetView.setImageResource(sResRoamSmall);
                                    }
                                    boolean show = isRoaming;
                                    param.args[0] = Boolean.valueOf(show);
                                    targetView.setVisibility(show ? View.VISIBLE : View.GONE);
                                    return;
                                } else {
                                    return;
                                }
                            }

                            if (classId != 1 && classId != 2) return;

                            if (!shouldShowRat(targetView, ctx)) {
                                targetView.setVisibility(View.GONE);
                                param.args[0] = Boolean.FALSE;
                                return;
                            }

                            ViewGroup parent = (ViewGroup) targetView.getParent();
                            if (parent == null) return;

                            ImageView special5gView = null;
                            ImageView mobileTypeView = null;

                            if (classId == 1) {
                                special5gView = targetView;
                                int typeId = parent.getResources().getIdentifier("mobile_type", "id", "com.android.systemui");
                                if (typeId != 0) mobileTypeView = (ImageView) parent.findViewById(typeId);
                            } else {
                                mobileTypeView = targetView;
                                int special5gId = parent.getResources().getIdentifier("mobile_special_5G", "id", "com.android.systemui");
                                if (special5gId != 0) special5gView = (ImageView) parent.findViewById(special5gId);
                            }

                            syncViewVisibility(mobileTypeView, special5gView, null);

                            if (classId == 1) {
                                boolean isVisible = (special5gView != null && special5gView.getVisibility() == View.VISIBLE);
                                param.args[0] = Boolean.valueOf(isVisible);
                            } else {
                                boolean isVisible = (mobileTypeView != null && mobileTypeView.getVisibility() == View.VISIBLE);
                                param.args[0] = Boolean.valueOf(isVisible);
                            }
                        } catch (Throwable t) {
                            HookEnv.log(TAG + ": emit collector hook error: " + t);
                        }
                    }
                }
            );
        }

        // 2b. MobileGroup roaming/vowifi visibility collector hook
        Class<?> groupCollectorClz = findMobileGroupCollectorClass(lpparam.classLoader);
        if (groupCollectorClz != null) {
            HookEnv.log(TAG + ": [v" + VERSION + "] Hooking groupCollector on " + groupCollectorClz.getName());
            HookEnv.findAndHookMethod(
                groupCollectorClz,
                "emit",
                Object.class,
                "kotlin.coroutines.Continuation",
                new MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            ImageView targetView = (ImageView) Reflect.getObjectField(param.thisObject, "$voWifi");
                            if (targetView == null) return;
                            Context ctx = targetView.getContext();
                            if (ctx == null) return;

                            int roamingId = ctx.getResources().getIdentifier("mobile_roaming", "id", "com.android.systemui");
                            if (roamingId != 0 && targetView.getId() == roamingId) {
                                int slotId = getSlotIdForView(targetView);
                                int subId = getSubIdForView(targetView);
                                IconConfig cfg = getConfigForSlot(slotId);

                                boolean isRoam = false;
                                if (param.args[0] instanceof Boolean) {
                                    isRoam = ((Boolean) param.args[0]).booleanValue();
                                } else if (param.args[0] != null) {
                                    try {
                                        Object sec = Reflect.callMethod(param.args[0], "getSecond");
                                        if (sec instanceof Boolean) isRoam = ((Boolean) sec).booleanValue();
                                    } catch (Throwable ignored) {}
                                }
                                if (isRoam && subId >= 0) {
                                    sSubIdIsRoaming.put(subId, Boolean.TRUE);
                                }

                                if ("hide".equals(cfg.roaming) || "small".equals(cfg.roaming)) {
                                    if (param.args[0] instanceof Boolean) {
                                        param.args[0] = Boolean.FALSE;
                                    } else if (param.args[0] != null) {
                                        try {
                                            Class<?> pClz = param.args[0].getClass();
                                            param.args[0] = Reflect.newInstance(pClz, Boolean.FALSE, Boolean.FALSE);
                                        } catch (Throwable ignored) {}
                                    }
                                    targetView.setVisibility(View.GONE);
                                }
                            }

                            int vowifiId = ctx.getResources().getIdentifier("mobile_vowifi", "id", "com.android.systemui");
                            if (vowifiId != 0 && targetView.getId() == vowifiId) {
                                int slotId = getSlotIdForView(targetView);
                                IconConfig cfg = getConfigForSlot(slotId);
                                if ("hide".equals(cfg.vowifi)) {
                                    if (param.args[0] instanceof Boolean) {
                                        param.args[0] = Boolean.FALSE;
                                    } else if (param.args[0] != null) {
                                        try {
                                            Class<?> pClz = param.args[0].getClass();
                                            param.args[0] = Reflect.newInstance(pClz, Boolean.FALSE, Boolean.FALSE);
                                        } catch (Throwable ignored) {}
                                    }
                                    targetView.setVisibility(View.GONE);
                                }
                            }
                        } catch (Throwable t) {
                            HookEnv.log(TAG + ": groupCollector emit before hook error: " + t);
                        }
                    }
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            ImageView targetView = (ImageView) Reflect.getObjectField(param.thisObject, "$voWifi");
                            if (targetView == null) return;
                            Context ctx = targetView.getContext();
                            if (ctx == null) return;

                            int roamingId = ctx.getResources().getIdentifier("mobile_roaming", "id", "com.android.systemui");
                            if (roamingId != 0 && targetView.getId() == roamingId) {
                                int slotId = getSlotIdForView(targetView);
                                IconConfig cfg = getConfigForSlot(slotId);
                                if ("hide".equals(cfg.roaming) || "small".equals(cfg.roaming)) {
                                    targetView.setVisibility(View.GONE);
                                    Object group = Reflect.getObjectField(param.thisObject, "$mobileGroup");
                                    if (group != null) {
                                        Reflect.callMethod(group, "setChildVisible", targetView, false);
                                    }
                                }
                            }

                            int vowifiId = ctx.getResources().getIdentifier("mobile_vowifi", "id", "com.android.systemui");
                            if (vowifiId != 0 && targetView.getId() == vowifiId) {
                                int slotId = getSlotIdForView(targetView);
                                IconConfig cfg = getConfigForSlot(slotId);
                                if ("hide".equals(cfg.vowifi)) {
                                    targetView.setVisibility(View.GONE);
                                    Object group = Reflect.getObjectField(param.thisObject, "$mobileGroup");
                                    if (group != null) {
                                        Reflect.callMethod(group, "setChildVisible", targetView, false);
                                    }
                                }
                            }
                        } catch (Throwable t) {
                            HookEnv.log(TAG + ": groupCollector emit after hook error: " + t);
                        }
                    }
                }
            );
        }

        // 2c. MobileSignalAnimatorContainer child visibility enforcement hook
        Class<?> animatorContainerClz = Reflect.findClassIfExists(
            "com.android.systemui.statusbar.views.MobileSignalAnimatorContainer",
            lpparam.classLoader
        );
        if (animatorContainerClz != null) {
            HookEnv.findAndHookMethod(
                animatorContainerClz,
                "setChildVisible",
                View.class,
                boolean.class,
                new MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            View child = (View) param.args[0];
                            if (child == null) return;
                            Context ctx = child.getContext();
                            if (ctx == null) return;

                            int roamingId = ctx.getResources().getIdentifier("mobile_roaming", "id", "com.android.systemui");
                            if (roamingId != 0 && child.getId() == roamingId) {
                                int slotId = getSlotIdForView(child);
                                int subId = getSubIdForView(child);
                                boolean visible = Boolean.TRUE.equals(param.args[1]);
                                if (visible && subId >= 0) {
                                    sSubIdIsRoaming.put(subId, Boolean.TRUE);
                                }
                                IconConfig cfg = getConfigForSlot(slotId);
                                if ("hide".equals(cfg.roaming) || "small".equals(cfg.roaming)) {
                                    param.args[1] = Boolean.FALSE;
                                    child.setVisibility(View.GONE);
                                }
                            }

                            int vowifiId = ctx.getResources().getIdentifier("mobile_vowifi", "id", "com.android.systemui");
                            if (vowifiId != 0 && child.getId() == vowifiId) {
                                int slotId = getSlotIdForView(child);
                                IconConfig cfg = getConfigForSlot(slotId);
                                if ("hide".equals(cfg.vowifi)) {
                                    param.args[1] = Boolean.FALSE;
                                    child.setVisibility(View.GONE);
                                }
                            }

                            // VoLTE/VoNR on Wi-Fi enforcement
                            int volteId = ctx.getResources().getIdentifier("mobile_volte", "id", "com.android.systemui");
                            if (volteId != 0 && child.getId() == volteId) {
                                int slotId = getSlotIdForView(child);
                                IconConfig cfg = getConfigForSlot(slotId);
                                String wifiPref = cfg.wifiVolte;
                                if (IconPolicy.overridesVolteOnWifi(cfg.volte, wifiPref)) {
                                    int subId = getSubIdForView(child);
                                    if (subId < 0 && child.getParent() instanceof ViewGroup) {
                                        ViewGroup childParent = (ViewGroup) child.getParent();
                                        int mobileTypeId = childParent.getResources().getIdentifier(
                                                "mobile_type", "id", "com.android.systemui");
                                        if (mobileTypeId != 0) {
                                            View mobileTypeView = childParent.findViewById(mobileTypeId);
                                            if (mobileTypeView != null) subId = getSubIdForView(mobileTypeView);
                                        }
                                    }
                                    if (subId < 0) subId = getSubIdForSlot(slotId);
                                    Boolean wifi = getWifiAvailableForSubId(subId);
                                    boolean wifiOn = wifi != null ? wifi.booleanValue() : isWifiConnected();
                                    boolean active = false;
                                    boolean policyHidden = false;
                                    if (wifiOn && "show".equals(wifiPref)) {
                                        active = Boolean.TRUE.equals(getVolteActiveForSubId(subId));
                                        policyHidden = active && isVoltePolicyHidden(subId);
                                    }
                                    Boolean forced = IconPolicy.volteChildVisibleOverride(
                                            cfg.volte, wifiPref, wifiOn, active, policyHidden);
                                    if (forced != null) {
                                        param.args[1] = forced;
                                        if (!forced.booleanValue()) child.setVisibility(View.GONE);
                                    }
                                }
                            }
                        } catch (Throwable t) {
                            HookEnv.log(TAG + ": setChildVisible before hook error: " + t);
                        }
                    }
                }
            );
        }

        // 3. Network activity flow collector hook (Drives real-time breathing for 5G CA)
        Class<?> special5gResCollectorClz = Reflect.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder$bind$1$1$9$1",
            lpparam.classLoader
        );
        if (special5gResCollectorClz != null) {
            HookEnv.findAndHookMethod(
                special5gResCollectorClz,
                "invokeSuspend",
                Object.class,
                new MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            ImageView special5gView = (ImageView) Reflect.getObjectField(param.thisObject, "$special5G");
                            if (special5gView == null) return;

                            int resId = Reflect.getIntField(param.thisObject, "I$0");
                            boolean isDataActive = false;
                            try {
                                String name = special5gView.getResources().getResourceEntryName(resId);
                                if (name != null && name.contains("5g_on")) {
                                    isDataActive = true;
                                }
                            } catch (Throwable ignored) {}

                            ViewGroup parent = (ViewGroup) special5gView.getParent();
                            if (parent == null) return;

                            int typeId = parent.getResources().getIdentifier("mobile_type", "id", "com.android.systemui");
                            if (typeId == 0) return;

                            ImageView mobileTypeView = (ImageView) parent.findViewById(typeId);
                            if (mobileTypeView == null) return;

                            Drawable d = mobileTypeView.getDrawable();
                            if (d != null) {
                                Reflect.setAdditionalInstanceField(d, "isDataActive", Boolean.valueOf(isDataActive));
                                d.invalidateSelf();
                            }
                        } catch (Throwable t) {
                            HookEnv.log(TAG + ": special5gResCollector hook error: " + t);
                        }
                    }
                }
            );
        }

        // 4. Hook MobileTypeDrawable.draw for Option A Korean Capsule Breathing (Exclusively for 5GA & 5G+)
        HookEnv.findAndHookMethod(
            drawableClz,
            "draw",
            Canvas.class,
            new MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Drawable drawable = (Drawable) param.thisObject;
                        String mobileType = getDrawableMobileType(drawable);
                        if (TextUtils.isEmpty(mobileType)) return;

                        View v = (View) Reflect.getAdditionalInstanceField(drawable, "view");
                        int slotId = getSlotIdForView(v);
                        IconConfig cfg = getConfigForSlot(slotId);
                        boolean isCapsuleMode = "korean".equals(cfg.capsule);
                        if (!isCapsuleMode || !isCapsuleTargetText(mobileType, cfg)) {
                            // Non-capsule mode or non-target text: use default native system drawing
                            return;
                        }

                        // Intercept default draw and render Option A Capsule Breathing
                        param.setResult(null);

                        Canvas canvas = (Canvas) param.args[0];
                        drawCapsuleBreath(canvas, drawable, mobileType);
                    } catch (Throwable t) {
                        HookEnv.log(TAG + ": draw hook error: " + t);
                    }
                }
            }
        );
    }

    private static void drawCapsuleBreath(Canvas canvas, Drawable drawable, String mobileType) {
        try {
            Paint textPaint = (Paint) Reflect.getObjectField(drawable, "mMobileTypeTextPaint");
            int baseColor = Reflect.getIntField(drawable, "mMobileTypeColor");

            // Strictly align font size with native Korean 5G pill (cap-height 4.40dp / 8.0dp = 55% ratio)
            textPaint.setTextSize(dpToPx(6.1f));
            float measuredW = textPaint.measureText(mobileType);

            // Resolve real-time data active state
            boolean isDataActive = false;
            Object activeObj = Reflect.getAdditionalInstanceField(drawable, "isDataActive");
            if (activeObj instanceof Boolean) {
                isDataActive = ((Boolean) activeObj).booleanValue();
            } else {
                // Fallback: check tag of special5GView in same parent
                ImageView mobileTypeView = (ImageView) Reflect.getAdditionalInstanceField(drawable, "view");
                if (mobileTypeView != null && mobileTypeView.getParent() instanceof ViewGroup) {
                    ViewGroup parent = (ViewGroup) mobileTypeView.getParent();
                    int special5gId = parent.getResources().getIdentifier("mobile_special_5G", "id", "com.android.systemui");
                    if (special5gId != 0) {
                        ImageView special5gView = (ImageView) parent.findViewById(special5gId);
                        if (special5gView != null && special5gView.getTag() instanceof Integer) {
                            try {
                                int tagId = ((Integer) special5gView.getTag()).intValue();
                                String name = special5gView.getResources().getResourceEntryName(tagId);
                                if (name != null && name.contains("5g_on")) {
                                    isDataActive = true;
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            }

            // Exact parity with Korean 5G pill (height = 8.0dp, top = 0.6dp, bottom = 8.6dp, radius = 2.0dp)
            float pillTop = dpToPx(0.6f);
            float pillBottom = dpToPx(8.6f);
            float padX = dpToPx(2.5f);

            // Capsule starts at X = 0f to guarantee zero clipping by ImageView bounds
            RectF box = new RectF(0f, pillTop, measuredW + padX * 2f, pillBottom);
            float radius = dpToPx(2.0f);

            // Canonical vertical & horizontal text centering in rectangle
            float textX = padX;
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float textY = box.centerY() - (fm.descent + fm.ascent) / 2f;

            if (isDataActive) {
                // === Active (ON): Solid capsule background + inverted hollow text ===
                Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                bgPaint.setStyle(Paint.Style.FILL);
                bgPaint.setColor(baseColor);
                canvas.drawRoundRect(box, radius, radius, bgPaint);

                // Invert text color against capsule background
                int r = (baseColor >> 16) & 0xFF;
                int g = (baseColor >> 8) & 0xFF;
                int b = baseColor & 0xFF;
                double luminance = 0.299 * r + 0.587 * g + 0.114 * b;
                int textColor = luminance > 128 ? 0xFF111111 : 0xFFFFFFFF;

                int oldTextColor = textPaint.getColor();
                textPaint.setColor(textColor);
                canvas.drawText(mobileType, textX, textY, textPaint);
                textPaint.setColor(oldTextColor);
            } else {
                // === Idle (OFF): Thin-line hollow capsule border (1.0dp, 90% alpha) + normal text ===
                Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                borderPaint.setStyle(Paint.Style.STROKE);
                float strokeWidth = dpToPx(1.0f);
                borderPaint.setStrokeWidth(strokeWidth);
                borderPaint.setColor(baseColor);
                borderPaint.setAlpha(230); // ~90% opacity (strokeAlpha="0.9" in signal_5g_off.xml)

                float halfStroke = strokeWidth / 2f;
                RectF strokeBox = new RectF(box.left + halfStroke, box.top + halfStroke, box.right - halfStroke, box.bottom - halfStroke);
                canvas.drawRoundRect(strokeBox, radius, radius, borderPaint);

                canvas.drawText(mobileType, textX, textY, textPaint);
            }
        } catch (Throwable t) {
            HookEnv.log(TAG + ": drawCapsuleBreath error: " + t);
        }
    }
}

