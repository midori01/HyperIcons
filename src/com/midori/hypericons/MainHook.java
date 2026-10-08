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
import android.provider.Settings;
import android.text.TextUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    private static final String TAG = "HyperIconsHook";
    public static final String VERSION = "5.3.2";
    public static final String ACTION_RELOAD = "com.midori.hypericons.ACTION_RELOAD";
    public static final String PREF_PACKAGE = "com.midori.hypericons";
    public static final String PREF_FILE = "hyper_icons_config";
    public static final String PROVIDER_AUTHORITY = "com.midori.hypericons.provider";
    public static final Uri PROVIDER_URI = Uri.parse("content://" + PROVIDER_AUTHORITY);
    public static final Uri CONFIG_URI = Uri.parse("content://" + PROVIDER_AUTHORITY + "/config");

    // Mode and Tab Keys
    public static final String KEY_SIM_MODE = "pref_sim_mode";           // "both" | "separate"
    public static final String KEY_TARGET_TAB = "pref_target_tab";       // "sim1" | "sim2"

    // Global / Both SIMs Keys
    public static final String KEY_5G_CA = "pref_5g_ca";
    public static final String KEY_5G_BASIC = "pref_5g_basic";
    public static final String KEY_CAPSULE_STYLE = "pref_capsule_style";
    public static final String KEY_4G_RAT = "pref_4g_rat";
    public static final String KEY_ALWAYS_SHOW_RAT = "pref_always_show_rat";
    public static final String KEY_VOLTE_STYLE = "pref_volte_style";
    public static final String KEY_VOWIFI_STYLE = "pref_vowifi_style";
    public static final String KEY_ROAMING_STYLE = "pref_roaming_style";

    // SIM 1 (Slot 0) Keys
    public static final String KEY_SIM1_5G_CA = "pref_sim1_5g_ca";
    public static final String KEY_SIM1_5G_BASIC = "pref_sim1_5g_basic";
    public static final String KEY_SIM1_CAPSULE_STYLE = "pref_sim1_capsule_style";
    public static final String KEY_SIM1_4G_RAT = "pref_sim1_4g_rat";
    public static final String KEY_SIM1_ALWAYS_SHOW_RAT = "pref_sim1_always_show_rat";
    public static final String KEY_SIM1_VOLTE_STYLE = "pref_sim1_volte_style";
    public static final String KEY_SIM1_VOWIFI_STYLE = "pref_sim1_vowifi_style";
    public static final String KEY_SIM1_ROAMING_STYLE = "pref_sim1_roaming_style";

    // SIM 2 / eSIM (Slot 1) Keys
    public static final String KEY_SIM2_5G_CA = "pref_sim2_5g_ca";
    public static final String KEY_SIM2_5G_BASIC = "pref_sim2_5g_basic";
    public static final String KEY_SIM2_CAPSULE_STYLE = "pref_sim2_capsule_style";
    public static final String KEY_SIM2_4G_RAT = "pref_sim2_4g_rat";
    public static final String KEY_SIM2_ALWAYS_SHOW_RAT = "pref_sim2_always_show_rat";
    public static final String KEY_SIM2_VOLTE_STYLE = "pref_sim2_volte_style";
    public static final String KEY_SIM2_VOWIFI_STYLE = "pref_sim2_vowifi_style";
    public static final String KEY_SIM2_ROAMING_STYLE = "pref_sim2_roaming_style";

    private static volatile String sPrefSimMode = "both";
    private static volatile String sPrefTargetTab = "sim1";

    // Global / Both SIMs preferences
    private static volatile String sPref5gCa = "default";
    private static volatile String sPref5gBasic = "default";
    private static volatile String sPrefCapsuleStyle = "default";
    private static volatile String sPref4gRat = "default";
    private static volatile String sPrefAlwaysShowRat = "default";
    private static volatile String sPrefVolteStyle = "default";
    private static volatile String sPrefVowifiStyle = "default";
    private static volatile String sPrefRoamingStyle = "default";

    // SIM 1 (Slot 0) preferences
    private static volatile String sPrefSim15gCa = "default";
    private static volatile String sPrefSim15gBasic = "default";
    private static volatile String sPrefSim1CapsuleStyle = "default";
    private static volatile String sPrefSim14gRat = "default";
    private static volatile String sPrefSim1AlwaysShowRat = "default";
    private static volatile String sPrefSim1VolteStyle = "default";
    private static volatile String sPrefSim1VowifiStyle = "default";
    private static volatile String sPrefSim1RoamingStyle = "default";

    // SIM 2 / eSIM (Slot 1) preferences
    private static volatile String sPrefSim25gCa = "default";
    private static volatile String sPrefSim25gBasic = "default";
    private static volatile String sPrefSim2CapsuleStyle = "default";
    private static volatile String sPrefSim24gRat = "default";
    private static volatile String sPrefSim2AlwaysShowRat = "default";
    private static volatile String sPrefSim2VolteStyle = "default";
    private static volatile String sPrefSim2VowifiStyle = "default";
    private static volatile String sPrefSim2RoamingStyle = "default";

    public static class IconConfig {
        public String ca = "default";
        public String basic = "default";
        public String capsule = "default";
        public String rat = "default";
        public String alwaysRat = "default";
        public String volte = "default";
        public String vowifi = "default";
        public String roaming = "default";
    }

    public static IconConfig getConfigForSlot(int slotId) {
        IconConfig cfg = new IconConfig();
        if ("separate".equals(sPrefSimMode)) {
            if (slotId == 0) {
                cfg.ca = sPrefSim15gCa;
                cfg.basic = sPrefSim15gBasic;
                cfg.capsule = sPrefSim1CapsuleStyle;
                cfg.rat = sPrefSim14gRat;
                cfg.alwaysRat = sPrefSim1AlwaysShowRat;
                cfg.volte = sPrefSim1VolteStyle;
                cfg.vowifi = sPrefSim1VowifiStyle;
                cfg.roaming = sPrefSim1RoamingStyle;
                return cfg;
            } else if (slotId == 1) {
                cfg.ca = sPrefSim25gCa;
                cfg.basic = sPrefSim25gBasic;
                cfg.capsule = sPrefSim2CapsuleStyle;
                cfg.rat = sPrefSim24gRat;
                cfg.alwaysRat = sPrefSim2AlwaysShowRat;
                cfg.volte = sPrefSim2VolteStyle;
                cfg.vowifi = sPrefSim2VowifiStyle;
                cfg.roaming = sPrefSim2RoamingStyle;
                return cfg;
            }
        }
        cfg.ca = sPref5gCa;
        cfg.basic = sPref5gBasic;
        cfg.capsule = sPrefCapsuleStyle;
        cfg.rat = sPref4gRat;
        cfg.alwaysRat = sPrefAlwaysShowRat;
        cfg.volte = sPrefVolteStyle;
        cfg.vowifi = sPrefVowifiStyle;
        cfg.roaming = sPrefRoamingStyle;
        return cfg;
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
            Class<?> smClz = XposedHelpers.findClassIfExists("android.telephony.SubscriptionManager", null);
            if (smClz != null) {
                Object res = XposedHelpers.callStaticMethod(smClz, "getSlotIndex", subId);
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
            Class<?> smClz = XposedHelpers.findClassIfExists("android.telephony.SubscriptionManager", null);
            if (smClz != null) {
                Object res = XposedHelpers.callStaticMethod(smClz, "getSubscriptionId", slotId);
                if (res instanceof Integer && ((Integer) res).intValue() >= 0 && ((Integer) res).intValue() != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    return (Integer) res;
                }
                Object ids = XposedHelpers.callStaticMethod(smClz, "getSubId", slotId);
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
            Object direct = XposedHelpers.getAdditionalInstanceField(view, "slotId");
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
                int slotId = XposedHelpers.getIntField(curr, "slotId");
                if (slotId >= 0) return slotId;
            } catch (Throwable ignored) {}
            try {
                int slotId = XposedHelpers.getIntField(curr, "mSlotId");
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

    public static final int RADIO_TYPE_UNKNOWN = 0;
    public static final int RADIO_TYPE_2G_3G = 1;
    public static final int RADIO_TYPE_4G = 2;
    public static final int RADIO_TYPE_4G_CA = 3;
    public static final int RADIO_TYPE_5G = 4;
    public static final int RADIO_TYPE_5G_CA = 5;

    private static volatile boolean sIs5GConnected = false;
    private static volatile boolean sIs5GCaConnected = false;
    private static final java.util.concurrent.ConcurrentHashMap<Integer, Boolean> sSubIdIs5G = new java.util.concurrent.ConcurrentHashMap<Integer, Boolean>();
    private static final java.util.concurrent.ConcurrentHashMap<Integer, Boolean> sSubIdIs5GCa = new java.util.concurrent.ConcurrentHashMap<Integer, Boolean>();
    private static final java.util.concurrent.ConcurrentHashMap<Integer, Integer> sSubIdRadioType = new java.util.concurrent.ConcurrentHashMap<Integer, Integer>();
    private static volatile boolean sNetworkCallbackRegistered = false;
    private static volatile Handler sMainHandler = null;
    private static final Runnable sReloadRunnable = new Runnable() {
        @Override
        public void run() {
            performStatusReload();
        }
    };

    private static Handler getMainHandler() {
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
                    XposedBridge.log(TAG + ": [v" + VERSION + "] Multi-phase data switch reload pass (" + delay + "ms)");
                    performStatusReload();
                }
            };
            sDataSwitchRunnables.add(r);
            handler.postDelayed(r, delay);
        }
    }

    private static XSharedPreferences sPrefs = null;
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
            XposedBridge.log(TAG + ": initVolteResourceIds error: " + t);
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
            XposedBridge.log(TAG + ": initVowifiResourceIds error: " + t);
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
            XposedBridge.log(TAG + ": initRoamingResourceIds error: " + t);
        }
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

    private static String getTarget5gCaText(IconConfig cfg) {
        String ca = cfg != null ? cfg.ca : sPref5gCa;
        if ("5ga".equals(ca)) return "5GA";
        if ("5g_plus".equals(ca)) return "5G+";
        if ("5g_plus_plus".equals(ca)) return "5G++";
        if ("5guwb".equals(ca)) return "5GUWB";
        if ("5ge".equals(ca)) return "5Ge";
        if ("5g_6rx".equals(ca)) return "5G 6Rx";
        if ("5g_plus_plus_6rx".equals(ca)) return "5G++ 6Rx";
        if ("5guwb_6rx".equals(ca)) return "5GUWB 6Rx";
        return "";
    }

    private static String getTarget5gCaText() {
        return getTarget5gCaText(null);
    }

    private static String getEffective5gCaText(IconConfig cfg) {
        String ca = getTarget5gCaText(cfg);
        return TextUtils.isEmpty(ca) ? "5G+" : ca;
    }

    private static String getEffective5gCaText() {
        return getEffective5gCaText(null);
    }

    private static String getTarget5gBasicText(IconConfig cfg) {
        String basic = cfg != null ? cfg.basic : sPref5gBasic;
        if ("follow_ca".equals(basic)) {
            return getEffective5gCaText(cfg);
        }
        return "5G";
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

    private static boolean is5gCaText(String text) {
        if (TextUtils.isEmpty(text)) return false;
        return "5GA".equals(text) || "5G+".equals(text) || "5G++".equals(text) || "5GUWB".equals(text) || "5Ge".equals(text)
                || "5G 6Rx".equals(text) || "5G++ 6Rx".equals(text) || "5GUWB 6Rx".equals(text);
    }

    private static boolean is4gCaText(String text) {
        if (TextUtils.isEmpty(text)) return false;
        return text.contains("+") || text.contains("-A") || "4.5G+".equals(text);
    }

    private static boolean is5gText(String text) {
        if (TextUtils.isEmpty(text)) return false;
        return text.startsWith("5G");
    }

    private static boolean is4gText(String text) {
        if (TextUtils.isEmpty(text)) return false;
        return text.contains("4G") || text.contains("LTE") || text.contains("4.5G");
    }

    private static boolean isCapsuleTargetText(String text) {
        if (TextUtils.isEmpty(text)) return false;
        return is5gText(text) || is4gText(text);
    }

    private static boolean isTargetBreathText(String text) {
        if (TextUtils.isEmpty(text)) return false;
        return is5gCaText(text) || is4gCaText(text);
    }

    private static String getDrawableMobileType(Drawable drawable) {
        if (drawable == null) return "";
        try {
            boolean isDoublePlus = false;
            try {
                isDoublePlus = XposedHelpers.getBooleanField(drawable, "mShowMobileTypeDoublePlus");
            } catch (Throwable ignored) {}
            if (isDoublePlus) {
                return "5G++";
            }
            Object textObj = XposedHelpers.getObjectField(drawable, "mMobileType");
            if (textObj instanceof String) {
                String str = (String) textObj;
                if ("5G++".equals(str)) return "5G++";
                if (!TextUtils.isEmpty(str)) return str;
            }
            Object actual = XposedHelpers.getAdditionalInstanceField(drawable, "actualMobileType");
            if (actual instanceof String && !TextUtils.isEmpty((String) actual)) {
                return (String) actual;
            }
            Object viewObj = XposedHelpers.getAdditionalInstanceField(drawable, "view");
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
            Class<?> spClz = XposedHelpers.findClassIfExists("android.os.SystemProperties", null);
            if (spClz != null) {
                String modDevice = (String) XposedHelpers.callStaticMethod(spClz, "get", "ro.product.mod_device", "");
                if (modDevice != null && (modDevice.contains("_global") || modDevice.contains("_eea") || modDevice.contains("_in")
                        || modDevice.contains("_ru") || modDevice.contains("_id") || modDevice.contains("_tr") || modDevice.contains("_tw") || modDevice.contains("_jp"))) {
                    return false;
                }
                String incremental = (String) XposedHelpers.callStaticMethod(spClz, "get", "ro.build.version.incremental", "");
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
                String region = (String) XposedHelpers.callStaticMethod(spClz, "get", "ro.miui.region", "");
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
            Class<?> buildClz = XposedHelpers.findClassIfExists("miui.os.Build", cl);
            if (buildClz != null) {
                return !XposedHelpers.getStaticBooleanField(buildClz, "IS_INTERNATIONAL_BUILD");
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isInternationalRom() {
        return !isChinaRom();
    }

    private static boolean shouldShowRat(View view, Context context, IconConfig cfg) {
        String alwaysRat = cfg != null ? cfg.alwaysRat : sPrefAlwaysShowRat;
        if ("always".equals(alwaysRat)) {
            // "always": regardless of ROM version or data SIM status, display on BOTH SIMs!
            return true;
        }

        boolean isWifi = isWifiConnected(context);
        if ("wifi_hide".equals(alwaysRat)) {
            if (isWifi) return false;
            // On cellular, only active data SIM shows RAT
            return isDataSimCard(view);
        }

        // "default": respect native ROM behavior
        if (isInternationalRom()) {
            // International ROM (Global, EU, RU, ID, TR, TW, etc.):
            // 1. Hide on Wi-Fi
            if (isWifi) return false;
            // 2. On cellular, only active data SIM shows RAT
            return isDataSimCard(view);
        } else {
            // China ROM: follow system default behavior
            return true;
        }
    }

    private static boolean shouldShowRat(View view, Context context) {
        int slotId = getSlotIdForView(view);
        return shouldShowRat(view, context, getConfigForSlot(slotId));
    }

    private static int getSubIdForView(View view) {
        if (view == null) return -1;
        try {
            Object direct = XposedHelpers.getAdditionalInstanceField(view, "subId");
            if (direct instanceof Integer && ((Integer) direct).intValue() >= 0) {
                return ((Integer) direct).intValue();
            }
        } catch (Throwable ignored) {}
        View curr = view;
        while (curr != null) {
            try {
                int subId = XposedHelpers.getIntField(curr, "subId");
                if (subId >= 0) return subId;
            } catch (Throwable ignored) {}
            try {
                int subId = XposedHelpers.getIntField(curr, "mSubId");
                if (subId >= 0) return subId;
            } catch (Throwable ignored) {}
            try {
                int slotId = XposedHelpers.getIntField(curr, "mSlotId");
                if (slotId >= 0) {
                    int subId = getSubIdForSlotId(slotId);
                    if (subId >= 0) return subId;
                }
            } catch (Throwable ignored) {}
            try {
                int slotId = XposedHelpers.getIntField(curr, "slotId");
                if (slotId >= 0) {
                    int subId = getSubIdForSlotId(slotId);
                    if (subId >= 0) return subId;
                }
            } catch (Throwable ignored) {}
            try {
                Object res = XposedHelpers.callMethod(curr, "getSubId");
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

    private static int getRadioTypeForView(View view, String text) {
        if (view != null) {
            int subId = getSubIdForView(view);
            if (subId >= 0) {
                Integer r = sSubIdRadioType.get(subId);
                if (r != null && r.intValue() != RADIO_TYPE_UNKNOWN) {
                    return r.intValue();
                }
            }
            Object viewR = XposedHelpers.getAdditionalInstanceField(view, "radioType");
            if (viewR instanceof Integer && ((Integer) viewR).intValue() != RADIO_TYPE_UNKNOWN) {
                return ((Integer) viewR).intValue();
            }
        }
        if (is5gCaText(text)) return RADIO_TYPE_5G_CA;
        if ("5G".equals(text)) return RADIO_TYPE_5G;
        if (text != null && (text.contains("+") || text.contains("-A"))) return RADIO_TYPE_4G_CA;
        if (text != null && (text.contains("4G") || text.contains("LTE") || text.contains("4.5G"))) return RADIO_TYPE_4G;
        if (!TextUtils.isEmpty(text)) return RADIO_TYPE_2G_3G;
        return RADIO_TYPE_UNKNOWN;
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
            Class<?> smClz = XposedHelpers.findClassIfExists("android.telephony.SubscriptionManager", null);
            if (smClz != null) {
                Object res = XposedHelpers.callStaticMethod(smClz, "getDefaultDataSubscriptionId");
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

    private static boolean isRatHiddenOnWifi(Context context) {
        if ("always".equals(sPrefAlwaysShowRat)) {
            return false;
        }
        boolean isWifi = isWifiConnected(context);
        if (!isWifi) {
            return false;
        }
        if ("wifi_hide".equals(sPrefAlwaysShowRat)) {
            return true;
        }
        // "default": follow ROM default (International ROM hides on Wi-Fi; China ROM shows on Wi-Fi)
        return isInternationalRom();
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
            XposedBridge.log(TAG + ": [v" + VERSION + "] Wi-Fi NetworkCallback registered successfully.");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to register NetworkCallback: " + t);
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
                        XposedBridge.log(TAG + ": [v" + VERSION + "] Cold boot config loaded successfully on retry (" + delay + "ms).");
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
                sPrefSimMode = bundle.getString(KEY_SIM_MODE, "both");
                sPrefTargetTab = bundle.getString(KEY_TARGET_TAB, "both");

                sPref5gCa = bundle.getString(KEY_5G_CA, "default");
                sPref5gBasic = bundle.getString(KEY_5G_BASIC, "default");
                sPrefCapsuleStyle = bundle.getString(KEY_CAPSULE_STYLE, "default");
                sPref4gRat = bundle.getString(KEY_4G_RAT, "default");
                sPrefAlwaysShowRat = bundle.getString(KEY_ALWAYS_SHOW_RAT, "default");
                sPrefVolteStyle = bundle.getString(KEY_VOLTE_STYLE, "default");
                sPrefVowifiStyle = bundle.getString(KEY_VOWIFI_STYLE, "default");
                sPrefRoamingStyle = bundle.getString(KEY_ROAMING_STYLE, "default");

                sPrefSim15gCa = bundle.getString(KEY_SIM1_5G_CA, sPref5gCa);
                sPrefSim15gBasic = bundle.getString(KEY_SIM1_5G_BASIC, sPref5gBasic);
                sPrefSim1CapsuleStyle = bundle.getString(KEY_SIM1_CAPSULE_STYLE, sPrefCapsuleStyle);
                sPrefSim14gRat = bundle.getString(KEY_SIM1_4G_RAT, sPref4gRat);
                sPrefSim1AlwaysShowRat = bundle.getString(KEY_SIM1_ALWAYS_SHOW_RAT, sPrefAlwaysShowRat);
                sPrefSim1VolteStyle = bundle.getString(KEY_SIM1_VOLTE_STYLE, sPrefVolteStyle);
                sPrefSim1VowifiStyle = bundle.getString(KEY_SIM1_VOWIFI_STYLE, sPrefVowifiStyle);
                sPrefSim1RoamingStyle = bundle.getString(KEY_SIM1_ROAMING_STYLE, sPrefRoamingStyle);

                sPrefSim25gCa = bundle.getString(KEY_SIM2_5G_CA, sPref5gCa);
                sPrefSim25gBasic = bundle.getString(KEY_SIM2_5G_BASIC, sPref5gBasic);
                sPrefSim2CapsuleStyle = bundle.getString(KEY_SIM2_CAPSULE_STYLE, sPrefCapsuleStyle);
                sPrefSim24gRat = bundle.getString(KEY_SIM2_4G_RAT, sPref4gRat);
                sPrefSim2AlwaysShowRat = bundle.getString(KEY_SIM2_ALWAYS_SHOW_RAT, sPrefAlwaysShowRat);
                sPrefSim2VolteStyle = bundle.getString(KEY_SIM2_VOLTE_STYLE, sPrefVolteStyle);
                sPrefSim2VowifiStyle = bundle.getString(KEY_SIM2_VOWIFI_STYLE, sPrefVowifiStyle);
                sPrefSim2RoamingStyle = bundle.getString(KEY_SIM2_ROAMING_STYLE, sPrefRoamingStyle);

                sConfigLoaded = true;
                XposedBridge.log(TAG + ": [v" + VERSION + "] Config loaded from Provider -> Mode: " + sPrefSimMode + ", Global: [CA=" + sPref5gCa + ", Basic=" + sPref5gBasic + ", Cap=" + sPrefCapsuleStyle + ", 4G=" + sPref4gRat + ", RAT=" + sPrefAlwaysShowRat + ", VoLTE=" + sPrefVolteStyle + ", VoWiFi=" + sPrefVowifiStyle + ", Roam=" + sPrefRoamingStyle + "], SIM1: [CA=" + sPrefSim15gCa + "], SIM2: [CA=" + sPrefSim25gCa + "]");
                return true;
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Error querying Provider: " + t.getMessage());
        }
        return false;
    }

    private static void loadConfig(Intent intent) {
        if (intent != null) {
            if (intent.hasExtra(KEY_SIM_MODE)) sPrefSimMode = intent.getStringExtra(KEY_SIM_MODE);
            if (intent.hasExtra(KEY_TARGET_TAB)) sPrefTargetTab = intent.getStringExtra(KEY_TARGET_TAB);

            if (intent.hasExtra(KEY_5G_CA)) sPref5gCa = intent.getStringExtra(KEY_5G_CA);
            if (intent.hasExtra(KEY_5G_BASIC)) sPref5gBasic = intent.getStringExtra(KEY_5G_BASIC);
            if (intent.hasExtra(KEY_CAPSULE_STYLE)) sPrefCapsuleStyle = intent.getStringExtra(KEY_CAPSULE_STYLE);
            if (intent.hasExtra(KEY_4G_RAT)) sPref4gRat = intent.getStringExtra(KEY_4G_RAT);
            if (intent.hasExtra(KEY_ALWAYS_SHOW_RAT)) sPrefAlwaysShowRat = intent.getStringExtra(KEY_ALWAYS_SHOW_RAT);
            if (intent.hasExtra(KEY_VOLTE_STYLE)) sPrefVolteStyle = intent.getStringExtra(KEY_VOLTE_STYLE);
            if (intent.hasExtra(KEY_VOWIFI_STYLE)) sPrefVowifiStyle = intent.getStringExtra(KEY_VOWIFI_STYLE);
            if (intent.hasExtra(KEY_ROAMING_STYLE)) sPrefRoamingStyle = intent.getStringExtra(KEY_ROAMING_STYLE);

            if (intent.hasExtra(KEY_SIM1_5G_CA)) sPrefSim15gCa = intent.getStringExtra(KEY_SIM1_5G_CA);
            if (intent.hasExtra(KEY_SIM1_5G_BASIC)) sPrefSim15gBasic = intent.getStringExtra(KEY_SIM1_5G_BASIC);
            if (intent.hasExtra(KEY_SIM1_CAPSULE_STYLE)) sPrefSim1CapsuleStyle = intent.getStringExtra(KEY_SIM1_CAPSULE_STYLE);
            if (intent.hasExtra(KEY_SIM1_4G_RAT)) sPrefSim14gRat = intent.getStringExtra(KEY_SIM1_4G_RAT);
            if (intent.hasExtra(KEY_SIM1_ALWAYS_SHOW_RAT)) sPrefSim1AlwaysShowRat = intent.getStringExtra(KEY_SIM1_ALWAYS_SHOW_RAT);
            if (intent.hasExtra(KEY_SIM1_VOLTE_STYLE)) sPrefSim1VolteStyle = intent.getStringExtra(KEY_SIM1_VOLTE_STYLE);
            if (intent.hasExtra(KEY_SIM1_VOWIFI_STYLE)) sPrefSim1VowifiStyle = intent.getStringExtra(KEY_SIM1_VOWIFI_STYLE);
            if (intent.hasExtra(KEY_SIM1_ROAMING_STYLE)) sPrefSim1RoamingStyle = intent.getStringExtra(KEY_SIM1_ROAMING_STYLE);

            if (intent.hasExtra(KEY_SIM2_5G_CA)) sPrefSim25gCa = intent.getStringExtra(KEY_SIM2_5G_CA);
            if (intent.hasExtra(KEY_SIM2_5G_BASIC)) sPrefSim25gBasic = intent.getStringExtra(KEY_SIM2_5G_BASIC);
            if (intent.hasExtra(KEY_SIM2_CAPSULE_STYLE)) sPrefSim2CapsuleStyle = intent.getStringExtra(KEY_SIM2_CAPSULE_STYLE);
            if (intent.hasExtra(KEY_SIM2_4G_RAT)) sPrefSim24gRat = intent.getStringExtra(KEY_SIM2_4G_RAT);
            if (intent.hasExtra(KEY_SIM2_ALWAYS_SHOW_RAT)) sPrefSim2AlwaysShowRat = intent.getStringExtra(KEY_SIM2_ALWAYS_SHOW_RAT);
            if (intent.hasExtra(KEY_SIM2_VOLTE_STYLE)) sPrefSim2VolteStyle = intent.getStringExtra(KEY_SIM2_VOLTE_STYLE);
            if (intent.hasExtra(KEY_SIM2_VOWIFI_STYLE)) sPrefSim2VowifiStyle = intent.getStringExtra(KEY_SIM2_VOWIFI_STYLE);
            if (intent.hasExtra(KEY_SIM2_ROAMING_STYLE)) sPrefSim2RoamingStyle = intent.getStringExtra(KEY_SIM2_ROAMING_STYLE);

            sConfigLoaded = true;
            XposedBridge.log(TAG + ": [v" + VERSION + "] Config updated via Intent -> Mode: " + sPrefSimMode + ", Global CA: " + sPref5gCa + ", SIM1 CA: " + sPrefSim15gCa + ", SIM2 CA: " + sPrefSim25gCa);
            return;
        }

        if (sContext != null && loadConfigFromProvider(sContext)) {
            return;
        }

        loadConfigFromXSharedPrefs();
    }

    private static void loadConfigFromXSharedPrefs() {
        try {
            java.io.File deFile = new java.io.File("/data/user_de/0/" + PREF_PACKAGE + "/shared_prefs/" + PREF_FILE + ".xml");
            if (deFile.exists() && deFile.canRead()) {
                XSharedPreferences dePrefs = new XSharedPreferences(deFile);
                if (parsePrefs(dePrefs, "DE-File")) return;
            }

            java.io.File ceFile = new java.io.File("/data/data/" + PREF_PACKAGE + "/shared_prefs/" + PREF_FILE + ".xml");
            if (ceFile.exists() && ceFile.canRead()) {
                XSharedPreferences cePrefs = new XSharedPreferences(ceFile);
                if (parsePrefs(cePrefs, "CE-File")) return;
            }

            if (sPrefs == null) {
                sPrefs = new XSharedPreferences(PREF_PACKAGE, PREF_FILE);
            } else {
                sPrefs.reload();
            }
            parsePrefs(sPrefs, "Package");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to load config from XSharedPreferences: " + t.getMessage());
        }
    }

    private static boolean parsePrefs(XSharedPreferences prefs, String source) {
        if (prefs == null) return false;
        try {
            prefs.reload();
            if (prefs.getAll() != null && !prefs.getAll().isEmpty()) {
                sPrefSimMode = prefs.getString(KEY_SIM_MODE, "both");
                sPrefTargetTab = prefs.getString(KEY_TARGET_TAB, "both");

                sPref5gCa = prefs.getString(KEY_5G_CA, "default");
                sPref5gBasic = prefs.getString(KEY_5G_BASIC, "default");
                sPrefCapsuleStyle = prefs.getString(KEY_CAPSULE_STYLE, "default");
                sPref4gRat = prefs.getString(KEY_4G_RAT, "default");
                sPrefAlwaysShowRat = prefs.getString(KEY_ALWAYS_SHOW_RAT, "default");
                sPrefVolteStyle = prefs.getString(KEY_VOLTE_STYLE, "default");
                sPrefVowifiStyle = prefs.getString(KEY_VOWIFI_STYLE, "default");
                sPrefRoamingStyle = prefs.getString(KEY_ROAMING_STYLE, "default");

                sPrefSim15gCa = prefs.getString(KEY_SIM1_5G_CA, sPref5gCa);
                sPrefSim15gBasic = prefs.getString(KEY_SIM1_5G_BASIC, sPref5gBasic);
                sPrefSim1CapsuleStyle = prefs.getString(KEY_SIM1_CAPSULE_STYLE, sPrefCapsuleStyle);
                sPrefSim14gRat = prefs.getString(KEY_SIM1_4G_RAT, sPref4gRat);
                sPrefSim1AlwaysShowRat = prefs.getString(KEY_SIM1_ALWAYS_SHOW_RAT, sPrefAlwaysShowRat);
                sPrefSim1VolteStyle = prefs.getString(KEY_SIM1_VOLTE_STYLE, sPrefVolteStyle);
                sPrefSim1VowifiStyle = prefs.getString(KEY_SIM1_VOWIFI_STYLE, sPrefVowifiStyle);
                sPrefSim1RoamingStyle = prefs.getString(KEY_SIM1_ROAMING_STYLE, sPrefRoamingStyle);

                sPrefSim25gCa = prefs.getString(KEY_SIM2_5G_CA, sPref5gCa);
                sPrefSim25gBasic = prefs.getString(KEY_SIM2_5G_BASIC, sPref5gBasic);
                sPrefSim2CapsuleStyle = prefs.getString(KEY_SIM2_CAPSULE_STYLE, sPrefCapsuleStyle);
                sPrefSim24gRat = prefs.getString(KEY_SIM2_4G_RAT, sPref4gRat);
                sPrefSim2AlwaysShowRat = prefs.getString(KEY_SIM2_ALWAYS_SHOW_RAT, sPrefAlwaysShowRat);
                sPrefSim2VolteStyle = prefs.getString(KEY_SIM2_VOLTE_STYLE, sPrefVolteStyle);
                sPrefSim2VowifiStyle = prefs.getString(KEY_SIM2_VOWIFI_STYLE, sPrefVowifiStyle);
                sPrefSim2RoamingStyle = prefs.getString(KEY_SIM2_ROAMING_STYLE, sPrefRoamingStyle);

                sConfigLoaded = true;
                XposedBridge.log(TAG + ": [v" + VERSION + "] Config loaded from XSharedPreferences (" + source + ") -> Mode: " + sPrefSimMode + ", Global CA: " + sPref5gCa + ", SIM1 CA: " + sPrefSim15gCa + ", SIM2 CA: " + sPrefSim25gCa);
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
                    XposedBridge.log(TAG + ": [v" + VERSION + "] ContentObserver notified change: " + uri);
                    loadConfigFromProvider(context);
                    triggerStatusReload();
                }
            };
            context.getContentResolver().registerContentObserver(CONFIG_URI, true, sContentObserver);
            XposedBridge.log(TAG + ": [v" + VERSION + "] ContentObserver registered for " + CONFIG_URI);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to register ContentObserver: " + t);
        }
    }

    private static synchronized void registerReceiver(Context context) {
        if (sReceiverRegistered || context == null) return;
        try {
            // 1. Module reload broadcast (exported for HyperIcons app)
            BroadcastReceiver reloadReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    if (intent == null || !ACTION_RELOAD.equals(intent.getAction())) return;
                    loadConfig(intent);
                    triggerStatusReload();
                }
            };
            IntentFilter reloadFilter = new IntentFilter(ACTION_RELOAD);
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(reloadReceiver, reloadFilter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(reloadReceiver, reloadFilter);
            }

            // 2. System boot & unlock lifecycle broadcasts
            BroadcastReceiver lifecycleReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    if (intent == null) return;
                    String action = intent.getAction();
                    XposedBridge.log(TAG + ": [v" + VERSION + "] System lifecycle broadcast received: " + action + ", refreshing config...");
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
            XposedBridge.log(TAG + ": [v" + VERSION + "] Live reload and boot/unlock broadcast receivers registered.");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to register reload receiver: " + t);
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
                    XposedBridge.log(TAG + ": [v" + VERSION + "] Data switch ContentObserver fired: " + uri);
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
                    XposedBridge.log(TAG + ": [v" + VERSION + "] Data switch Broadcast received: " + action);
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
                            XposedBridge.log(TAG + ": [v" + VERSION + "] OnSubscriptionsChangedListener fired");
                            scheduleDataSwitchReload();
                        }
                    };
                    sm.addOnSubscriptionsChangedListener(sSubChangedListener);
                }
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Failed to register OnSubscriptionsChangedListener: " + t);
            }

            sDataSwitchListenerRegistered = true;
            XposedBridge.log(TAG + ": [v" + VERSION + "] Data switch listeners successfully registered (ContentObserver + BroadcastReceiver + SubscriptionListener).");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to register data switch listener: " + t);
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

        int radioType = getRadioTypeForView(refView, currentText);
        boolean is5g = (radioType == RADIO_TYPE_5G || radioType == RADIO_TYPE_5G_CA);
        boolean is5gCa = (radioType == RADIO_TYPE_5G_CA) || is5gCaText(currentText);
        boolean isBase5g = is5g && !is5gCa;
        if (!is5g && ("5G".equals(currentText) || "5G".equals(text))) {
            isBase5g = true;
        }
        boolean shouldShowSpecial5G = isCapsuleMode && isBase5g && !"follow_ca".equals(cfg.basic);

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
                if (isCapsuleMode && isCapsuleTargetText(currentText)) {
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
                }
                if (d != null) d.invalidateSelf();
            }
        }
    }

    private static void syncViewVisibility(ImageView mobileTypeView, ImageView special5gView, String text) {
        syncViewVisibility(mobileTypeView, special5gView, text, null);
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
        if (sPolicyInstance != null) {
            try {
                XposedHelpers.callMethod(sPolicyInstance, "initResource", "reload", null);
                XposedBridge.log(TAG + ": [v" + VERSION + "] Triggered initResource(reload, null)");
            } catch (Throwable ignored) {}

            try {
                XposedHelpers.callMethod(sPolicyInstance, "updateMiuiOperatorConfig", -1);
                XposedHelpers.callMethod(sPolicyInstance, "updateMiuiOperatorConfig", 0);
                XposedHelpers.callMethod(sPolicyInstance, "updateMiuiOperatorConfig", 1);
                XposedBridge.log(TAG + ": [v" + VERSION + "] Triggered updateMiuiOperatorConfig on slots -1, 0, 1");
            } catch (Throwable ignored) {}
        }

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
                            ImageView volteView = volteViewId != 0 ? (ImageView) parent.findViewById(volteViewId) : null;
                            if (volteView != null) {
                                if ("hide".equals(cfg.volte)) {
                                    volteView.setVisibility(View.GONE);
                                } else if (!"default".equals(cfg.volte)) {
                                    initVolteResourceIds(parent.getContext());
                                    int targetRes = getVolteResForStyle(cfg.volte, false);
                                    if (targetRes != 0) {
                                        volteView.setImageResource(targetRes);
                                    }
                                    volteView.setVisibility(View.VISIBLE);
                                    volteView.invalidate();
                                    volteView.requestLayout();
                                }
                            }

                            int vowifiViewId = parent.getResources().getIdentifier("mobile_vowifi", "id", "com.android.systemui");
                            ImageView vowifiView = vowifiViewId != 0 ? (ImageView) parent.findViewById(vowifiViewId) : null;
                            if (vowifiView != null) {
                                if ("hide".equals(cfg.vowifi)) {
                                    vowifiView.setVisibility(View.GONE);
                                } else if (!"default".equals(cfg.vowifi)) {
                                    initVowifiResourceIds(parent.getContext());
                                    int targetRes = getVowifiResForStyle(cfg.vowifi, slotId);
                                    if (targetRes != 0) {
                                        vowifiView.setImageResource(targetRes);
                                    }
                                    vowifiView.setVisibility(View.VISIBLE);
                                    vowifiView.invalidate();
                                    vowifiView.requestLayout();
                                }
                            }

                            int roamingViewId = parent.getResources().getIdentifier("mobile_roaming", "id", "com.android.systemui");
                            ImageView roamingView = roamingViewId != 0 ? (ImageView) parent.findViewById(roamingViewId) : null;
                            int smallRoamViewId = parent.getResources().getIdentifier("mobile_small_roam", "id", "com.android.systemui");
                            ImageView smallRoamView = smallRoamViewId != 0 ? (ImageView) parent.findViewById(smallRoamViewId) : null;
                            if ("hide".equals(cfg.roaming)) {
                                if (roamingView != null) roamingView.setVisibility(View.GONE);
                                if (smallRoamView != null) smallRoamView.setVisibility(View.GONE);
                            } else if ("small".equals(cfg.roaming)) {
                                if (roamingView != null) roamingView.setVisibility(View.GONE);
                                if (smallRoamView != null) {
                                    initRoamingResourceIds(parent.getContext());
                                    if (sResRoamSmall != 0) {
                                        smallRoamView.setImageResource(sResRoamSmall);
                                    }
                                    smallRoamView.setVisibility(View.VISIBLE);
                                    smallRoamView.invalidate();
                                    smallRoamView.requestLayout();
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

                            int radioType = getRadioTypeForView(mobileTypeView, text);

                            if (radioType == RADIO_TYPE_5G || radioType == RADIO_TYPE_5G_CA) {
                                boolean isCa = (radioType == RADIO_TYPE_5G_CA) || is5gCaText(text);
                                Object viewCaObj = XposedHelpers.getAdditionalInstanceField(mobileTypeView, "isCaActive");
                                if (viewCaObj instanceof Boolean) {
                                    isCa = isCa || ((Boolean) viewCaObj).booleanValue();
                                }

                                String targetText;
                                if ("follow_ca".equals(cfg.basic)) {
                                    targetText = getEffective5gCaText(cfg);
                                    isCa = true;
                                } else {
                                    // "default"
                                    targetText = isCa ? getEffective5gCaText(cfg) : "5G";
                                }

                                text = targetText;
                                mobileTypeView.setTag(text);
                                if (d != null) {
                                    try {
                                        XposedHelpers.setObjectField(d, "mMobileType", text);
                                        XposedHelpers.setAdditionalInstanceField(d, "actualMobileType", text);
                                        XposedHelpers.callMethod(d, "measure");
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
                                        XposedHelpers.setObjectField(d, "mMobileType", text);
                                        XposedHelpers.setAdditionalInstanceField(d, "actualMobileType", text);
                                        XposedHelpers.callMethod(d, "measure");
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
                } catch (Throwable t) {
                    XposedBridge.log(TAG + ": Direct punch live reload error: " + t);
                }
            }
        };
        if (handler != null) {
            handler.post(punchRunnable);
        } else {
            punchRunnable.run();
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if ("com.android.phone".equals(lpparam.packageName)) {
            CellularFeatureHook.init(lpparam);
            return;
        }

        if (!"com.android.systemui".equals(lpparam.packageName)) {
            return;
        }

        XposedBridge.log(TAG + ": [v" + VERSION + "] Initializing HyperIcons hook for com.android.systemui");
        loadConfig(null);

        hookSystemUIApplication(lpparam);
        hookOperatorPolicy(lpparam);
        hookMobileIconInteractor(lpparam);
        hookSpecial5gViewModel(lpparam);
        hookVolteViewModel(lpparam);
        hookMobileIconBinder(lpparam);
    }

    private void hookSystemUIApplication(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> appClz = XposedHelpers.findClassIfExists("com.android.systemui.SystemUIApplication", lpparam.classLoader);
        if (appClz != null) {
            // 1. Hook attachBaseContext for earliest context capture on cold boot
            XposedHelpers.findAndHookMethod(appClz, "attachBaseContext", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context ctx = (Context) param.args[0];
                    if (ctx != null) {
                        updateContext(ctx);
                    }
                }
            });

            // 2. Hook onCreate
            XposedHelpers.findAndHookMethod(appClz, "onCreate", new XC_MethodHook() {
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

    private void hookOperatorPolicy(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> policyClz = XposedHelpers.findClassIfExists("com.android.systemui.MiuiOperatorCustomizedPolicy", lpparam.classLoader);
        if (policyClz == null) {
            XposedBridge.log(TAG + ": MiuiOperatorCustomizedPolicy not found");
            return;
        }

        XposedHelpers.findAndHookMethod(policyClz, "start", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                sPolicyInstance = param.thisObject;
                try {
                    Context ctx = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    if (ctx != null) {
                        updateContext(ctx);
                        if (!sConfigLoaded && loadConfigFromProvider(ctx)) {
                            triggerStatusReload();
                        }
                    }
                } catch (Throwable t) {
                    XposedBridge.log(TAG + ": Error getting mContext from policy: " + t);
                }
            }
        });

        XposedHelpers.findAndHookMethod(policyClz, "getStatusBarVolteType", int.class, new XC_MethodHook() {
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

        XposedHelpers.findAndHookMethod(policyClz, "getMiuiOperatorConfig", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object opConfig = param.getResult();
                if (opConfig == null) return;
                int slotId = (Integer) param.args[0];
                IconConfig cfg = getConfigForSlot(slotId);

                // 1. Universal 5G-A detection & display support across all carriers
                try {
                    XposedHelpers.setBooleanField(opConfig, "support5GADisplay", true);
                } catch (Throwable ignored) {}

                // 2. Korean dynamic breathing 5G icon mode
                try {
                    if ("korean".equals(cfg.capsule)) {
                        XposedHelpers.setBooleanField(opConfig, "showSpecial5GIcon", true);
                    } else {
                        XposedHelpers.setBooleanField(opConfig, "showSpecial5GIcon", false);
                    }
                } catch (Throwable ignored) {}

                // 3. Customize mobileTypeName list
                try {
                    Object listObj = XposedHelpers.getObjectField(opConfig, "mobileTypeName");
                    if (listObj instanceof List) {
                        List<String> list = (List<String>) listObj;
                        if (list.size() >= 14) {
                            ArrayList<String> modified = new ArrayList<String>(list);
                            applyConfigToList(modified, cfg);
                            XposedHelpers.setObjectField(opConfig, "mobileTypeName", modified);
                        }
                    }
                } catch (Throwable ignored) {}

                // 4. Always show RAT icon (showDataTypeDataDisconnected)
                try {
                    boolean isIntl = isInternationalRom();
                    if ("always".equals(cfg.alwaysRat)) {
                        XposedHelpers.setBooleanField(opConfig, "showDataTypeDataDisconnected", true);
                    } else if ("wifi_hide".equals(cfg.alwaysRat)) {
                        XposedHelpers.setBooleanField(opConfig, "showDataTypeDataDisconnected", false);
                    } else {
                        // "default": restore system default (!isIntl)
                        XposedHelpers.setBooleanField(opConfig, "showDataTypeDataDisconnected", !isIntl);
                    }
                } catch (Throwable ignored) {}

                // 5. VoLTE / VoNR style customization
                try {
                    initVolteResourceIds(sContext);
                    if ("hide".equals(cfg.volte)) {
                        XposedHelpers.setBooleanField(opConfig, "hideVolte", true);
                        XposedHelpers.setIntField(opConfig, "volteResId", 0);
                        XposedHelpers.setIntField(opConfig, "vonrResId", 0);
                    } else if (!"default".equals(cfg.volte)) {
                        XposedHelpers.setBooleanField(opConfig, "hideVolte", false);
                        int volteRes = getVolteResForStyle(cfg.volte, false);
                        int vonrRes = getVolteResForStyle(cfg.volte, true);
                        if (volteRes != 0) XposedHelpers.setIntField(opConfig, "volteResId", volteRes);
                        if (vonrRes != 0) XposedHelpers.setIntField(opConfig, "vonrResId", vonrRes);
                    }
                } catch (Throwable ignored) {}

                // 6. VoWiFi style customization
                try {
                    initVowifiResourceIds(sContext);
                    if ("hide".equals(cfg.vowifi)) {
                        XposedHelpers.setBooleanField(opConfig, "hideVowifi", true);
                        XposedHelpers.setIntField(opConfig, "vowifiResId", 0);
                    } else if (!"default".equals(cfg.vowifi)) {
                        XposedHelpers.setBooleanField(opConfig, "hideVowifi", false);
                        int vowifiRes = getVowifiResForStyle(cfg.vowifi, slotId);
                        if (vowifiRes != 0) {
                            XposedHelpers.setIntField(opConfig, "vowifiResId", vowifiRes);
                        }
                    }
                } catch (Throwable ignored) {}

                // 7. Roaming icon customization
                try {
                    if ("hide".equals(cfg.roaming) || "small".equals(cfg.roaming)) {
                        XposedHelpers.setBooleanField(opConfig, "hideNationalRoaming", true);
                    }
                } catch (Throwable ignored) {}
            }
        });
    }

    private static void applyConfigToList(List<String> list, IconConfig cfg) {
        String caText = getTarget5gCaText(cfg);
        if (!TextUtils.isEmpty(caText)) {
            list.set(12, caText);
            list.set(13, caText);
        } else {
            list.set(12, "5G+");
            list.set(13, "5GA");
        }

        String basic = cfg != null ? cfg.basic : sPref5gBasic;
        if ("follow_ca".equals(basic)) {
            String basicText = getEffective5gBasicText(cfg);
            list.set(10, basicText);
            list.set(11, basicText);
        } else {
            list.set(10, "5G");
            list.set(11, "5G");
        }

        String rat = cfg != null ? cfg.rat : sPref4gRat;
        if ("force_4g".equals(rat)) {
            list.set(6, "4G");
            list.set(9, "4G");
            list.set(7, "4G+");
            list.set(8, "4G+");
        } else if ("force_lte".equals(rat)) {
            list.set(6, "LTE");
            list.set(9, "LTE");
            list.set(7, "LTE+");
            list.set(8, "LTE+");
        } else if ("force_4g_lte".equals(rat)) {
            list.set(6, "4G LTE");
            list.set(9, "4G LTE");
            list.set(7, "4G LTE+");
            list.set(8, "4G LTE+");
        } else if ("force_45g".equals(rat)) {
            list.set(6, "4.5G");
            list.set(9, "4.5G");
            list.set(7, "4.5G+");
            list.set(8, "4.5G+");
        } else if ("force_ltea".equals(rat)) {
            list.set(6, "LTE");
            list.set(9, "LTE");
            list.set(7, "LTE-A");
            list.set(8, "LTE-A");
        } else {
            list.set(6, "4G");
            list.set(9, "LTE");
            list.set(7, "4G+");
            list.set(8, "LTE+");
        }
    }

    private static void applyConfigToList(List<String> list) {
        applyConfigToList(list, null);
    }

    private void hookMobileIconInteractor(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> interactorClz = XposedHelpers.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.domain.interactor.MiuiMobileIconInteractorImpl",
            lpparam.classLoader
        );
        if (interactorClz == null) {
            XposedBridge.log(TAG + ": MiuiMobileIconInteractorImpl not found");
            return;
        }

        XposedHelpers.findAndHookMethod(interactorClz, "getMobileTypeName", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                int index = (Integer) param.args[0];
                int subId = -1;
                try {
                    subId = XposedHelpers.getIntField(param.thisObject, "subId");
                } catch (Throwable ignored) {}
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
                    subId = XposedHelpers.getIntField(param.thisObject, "subId");
                } catch (Throwable ignored) {}
                int slotId = getSlotIdForSubId(subId);
                IconConfig cfg = getConfigForSlot(slotId);

                if (is5gCaText(text)) {
                    String ca = getTarget5gCaText(cfg);
                    if (!TextUtils.isEmpty(ca)) {
                        param.setResult(ca);
                    }
                } else if ("5G".equals(text)) {
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

    private void hookSpecial5gViewModel(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> special5gLambda = XposedHelpers.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiCellularIconVM$showSpecial5GIcon$2",
            lpparam.classLoader
        );
        if (special5gLambda != null) {
            XposedHelpers.findAndHookMethod(special5gLambda, "invokeSuspend", Object.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        // Native Xiaomi logic: return Boolean.valueOf(this.Z$0 && this.Z$1);
                        // this.Z$0: showSpecial5GIcon opConfig flag
                        // this.Z$1: fiveGConnected (true if truly on 5G network; false on 4G/3G/2G)
                        boolean is5GConnected = XposedHelpers.getBooleanField(param.thisObject, "Z$1");
                        sIs5GConnected = is5GConnected;

                        if (!is5GConnected) {
                            // If NOT connected to 5G: never show special 5G icon! (System will show 4G/LTE naturally)
                            param.setResult(Boolean.FALSE);
                            return;
                        }

                        // Connected to 5G:
                        int subId = -1;
                        try {
                            subId = XposedHelpers.getIntField(param.thisObject, "subId");
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
                        XposedBridge.log(TAG + ": hookSpecial5gViewModel error: " + t);
                    }
                }
            });
        }
    }

    private void hookVolteViewModel(final XC_LoadPackage.LoadPackageParam lpparam) {
        // 1. Hook volteVisibleGlobal to cleanly hide when pref is 'hide'
        Class<?> volteVisibleLambda = XposedHelpers.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiCellularIconVM$volteVisibleGlobal$2",
            lpparam.classLoader
        );
        if (volteVisibleLambda != null) {
            XposedHelpers.findAndHookMethod(volteVisibleLambda, "invokeSuspend", Object.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    int subId = -1;
                    try {
                        subId = XposedHelpers.getIntField(param.thisObject, "subId");
                    } catch (Throwable ignored) {}
                    int slotId = getSlotIdForSubId(subId);
                    IconConfig cfg = getConfigForSlot(slotId);

                    if ("hide".equals(cfg.volte)) {
                        Object res = param.getResult();
                        if (res != null) {
                            try {
                                Object first = XposedHelpers.callMethod(res, "getFirst");
                                Class<?> pairClz = XposedHelpers.findClass("kotlin.Pair", lpparam.classLoader);
                                Object newPair = XposedHelpers.newInstance(pairClz, first, Boolean.FALSE);
                                param.setResult(newPair);
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + ": Error modifying volteVisibleGlobal: " + t);
                            }
                        }
                    }
                }
            });
        }

        // 2. Hook volteId stateflow combiner to dynamically switch drawable id
        Class<?> volteIdLambda = XposedHelpers.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiCellularIconVM$volteId$3",
            lpparam.classLoader
        );
        if (volteIdLambda != null) {
            XposedHelpers.findAndHookMethod(volteIdLambda, "invokeSuspend", Object.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    int subId = -1;
                    try {
                        subId = XposedHelpers.getIntField(param.thisObject, "subId");
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
        Class<?> direct = XposedHelpers.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder$bind$1$1$13$1",
            cl
        );
        if (direct != null && hasCollectorFields(direct)) {
            return direct;
        }

        // 2. Proactive heuristic scan across potential R8 numbered inner classes
        for (int i = 1; i <= 30; i++) {
            String name = "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder$bind$1$1$" + i + "$1";
            Class<?> candidate = XposedHelpers.findClassIfExists(name, cl);
            if (candidate != null && hasCollectorFields(candidate)) {
                XposedBridge.log(TAG + ": [v" + VERSION + "] Dynamically discovered collector class: " + name);
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

    private void hookMobileIconBinder(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> binderClz = XposedHelpers.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder",
            lpparam.classLoader
        );
        if (binderClz == null) return;

        // Early SubId tagging on bind
        Class<?> locVMClz = XposedHelpers.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.LocationBasedMobileViewModel",
            lpparam.classLoader
        );
        Class<?> miuiVMClz = XposedHelpers.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiMobileIconViewModel",
            lpparam.classLoader
        );
        Class<?> loggerClz = XposedHelpers.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.MobileViewLogger",
            lpparam.classLoader
        );

        if (locVMClz != null && miuiVMClz != null && loggerClz != null) {
            XposedHelpers.findAndHookMethod(
                binderClz,
                "bind",
                ViewGroup.class,
                locVMClz,
                miuiVMClz,
                loggerClz,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            ViewGroup viewGroup = (ViewGroup) param.args[0];
                            Object locationVM = param.args[1];
                            if (viewGroup == null || locationVM == null) return;

                            if (!sConfigLoaded && viewGroup.getContext() != null) {
                                updateContext(viewGroup.getContext());
                            }

                            int subId = -1;
                            try {
                                Object commonImpl = XposedHelpers.getObjectField(locationVM, "commonImpl");
                                if (commonImpl != null) {
                                    subId = XposedHelpers.getIntField(commonImpl, "subscriptionId");
                                }
                            } catch (Throwable ignored) {}
                            if (subId < 0) {
                                try {
                                    subId = XposedHelpers.getIntField(viewGroup, "subId");
                                } catch (Throwable ignored) {}
                            }
                            if (subId < 0) return;

                            int slotId = getSlotIdForSubId(subId);
                            if (slotId < 0) {
                                try {
                                    slotId = XposedHelpers.getIntField(viewGroup, "slotId");
                                } catch (Throwable ignored) {}
                            }
                            if (slotId < 0) {
                                try {
                                    slotId = XposedHelpers.getIntField(viewGroup, "mSlotId");
                                } catch (Throwable ignored) {}
                            }

                            XposedHelpers.setAdditionalInstanceField(viewGroup, "subId", Integer.valueOf(subId));
                            if (slotId >= 0) {
                                XposedHelpers.setAdditionalInstanceField(viewGroup, "slotId", Integer.valueOf(slotId));
                            }

                            int containerId = viewGroup.getResources().getIdentifier("mobile_signal_container", "id", "com.android.systemui");
                            ViewGroup container = containerId != 0 ? (ViewGroup) viewGroup.findViewById(containerId) : viewGroup;
                            if (container != null) {
                                int typeId = viewGroup.getResources().getIdentifier("mobile_type", "id", "com.android.systemui");
                                ImageView mobileType = typeId != 0 ? (ImageView) container.findViewById(typeId) : null;
                                if (mobileType != null) {
                                    XposedHelpers.setAdditionalInstanceField(mobileType, "subId", Integer.valueOf(subId));
                                    if (slotId >= 0) {
                                        XposedHelpers.setAdditionalInstanceField(mobileType, "slotId", Integer.valueOf(slotId));
                                    }
                                    trackActiveView(mobileType);
                                }

                                int special5gId = viewGroup.getResources().getIdentifier("mobile_special_5G", "id", "com.android.systemui");
                                ImageView special5G = special5gId != 0 ? (ImageView) container.findViewById(special5gId) : null;
                                if (special5G != null) {
                                    XposedHelpers.setAdditionalInstanceField(special5G, "subId", Integer.valueOf(subId));
                                    if (slotId >= 0) {
                                        XposedHelpers.setAdditionalInstanceField(special5G, "slotId", Integer.valueOf(slotId));
                                    }
                                }
                            }

                            int volteId = viewGroup.getResources().getIdentifier("mobile_volte", "id", "com.android.systemui");
                            ImageView volteView = volteId != 0 ? (ImageView) viewGroup.findViewById(volteId) : null;
                            if (volteView != null) {
                                XposedHelpers.setAdditionalInstanceField(volteView, "subId", Integer.valueOf(subId));
                                if (slotId >= 0) {
                                    XposedHelpers.setAdditionalInstanceField(volteView, "slotId", Integer.valueOf(slotId));
                                }
                            }

                            int vowifiId = viewGroup.getResources().getIdentifier("mobile_vowifi", "id", "com.android.systemui");
                            ImageView vowifiView = vowifiId != 0 ? (ImageView) viewGroup.findViewById(vowifiId) : null;
                            if (vowifiView != null) {
                                XposedHelpers.setAdditionalInstanceField(vowifiView, "subId", Integer.valueOf(subId));
                                if (slotId >= 0) {
                                    XposedHelpers.setAdditionalInstanceField(vowifiView, "slotId", Integer.valueOf(slotId));
                                }
                            }

                            int roamingId = viewGroup.getResources().getIdentifier("mobile_roaming", "id", "com.android.systemui");
                            ImageView roamingView = roamingId != 0 ? (ImageView) viewGroup.findViewById(roamingId) : null;
                            if (roamingView != null) {
                                XposedHelpers.setAdditionalInstanceField(roamingView, "subId", Integer.valueOf(subId));
                                if (slotId >= 0) {
                                    XposedHelpers.setAdditionalInstanceField(roamingView, "slotId", Integer.valueOf(slotId));
                                }
                            }

                            int smallRoamId = viewGroup.getResources().getIdentifier("mobile_small_roam", "id", "com.android.systemui");
                            ImageView smallRoamView = smallRoamId != 0 ? (ImageView) viewGroup.findViewById(smallRoamId) : null;
                            if (smallRoamView != null) {
                                XposedHelpers.setAdditionalInstanceField(smallRoamView, "subId", Integer.valueOf(subId));
                                if (slotId >= 0) {
                                    XposedHelpers.setAdditionalInstanceField(smallRoamView, "slotId", Integer.valueOf(slotId));
                                }
                            }
                        } catch (Throwable t) {
                            XposedBridge.log(TAG + ": bind hook error: " + t);
                        }
                    }
                }
            );
        }
        Class<?> drawableClz = XposedHelpers.findClassIfExists(
            "com.miui.systemui.statusbar.views.MobileTypeDrawable",
            lpparam.classLoader
        );
        if (drawableClz == null) return;

        // 0. Hook getIntrinsicHeight and getIntrinsicWidth to eliminate ImageView fitCenter matrix translation offset
        XposedHelpers.findAndHookMethod(drawableClz, "getIntrinsicHeight", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Drawable drawable = (Drawable) param.thisObject;
                String mobileType = getDrawableMobileType(drawable);
                View v = (View) XposedHelpers.getAdditionalInstanceField(drawable, "view");
                int slotId = getSlotIdForView(v);
                IconConfig cfg = getConfigForSlot(slotId);
                if ("korean".equals(cfg.capsule) && isCapsuleTargetText(mobileType)) {
                    param.setResult((int) Math.ceil(dpToPx(9.0f)));
                }
            }
        });

        XposedHelpers.findAndHookMethod(drawableClz, "getIntrinsicWidth", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Drawable drawable = (Drawable) param.thisObject;
                String mobileType = getDrawableMobileType(drawable);
                View v = (View) XposedHelpers.getAdditionalInstanceField(drawable, "view");
                int slotId = getSlotIdForView(v);
                IconConfig cfg = getConfigForSlot(slotId);
                if ("korean".equals(cfg.capsule) && isCapsuleTargetText(mobileType)) {
                    Paint textPaint = (Paint) XposedHelpers.getObjectField(drawable, "mMobileTypeTextPaint");
                    textPaint.setTextSize(dpToPx(6.1f));
                    float measuredW = textPaint.measureText(mobileType);
                    float padX = dpToPx(2.5f);
                    param.setResult((int) Math.ceil(measuredW + padX * 2f));
                }
            }
        });

        XposedHelpers.findAndHookMethod(drawableClz, "measure", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Drawable drawable = (Drawable) param.thisObject;
                String mobileType = getDrawableMobileType(drawable);
                View v = (View) XposedHelpers.getAdditionalInstanceField(drawable, "view");
                int slotId = getSlotIdForView(v);
                IconConfig cfg = getConfigForSlot(slotId);
                Paint textPaint = (Paint) XposedHelpers.getObjectField(drawable, "mMobileTypeTextPaint");
                int mMobileTypeSize = XposedHelpers.getIntField(drawable, "mMobileTypeSize");
                if ("korean".equals(cfg.capsule) && isCapsuleTargetText(mobileType)) {
                    textPaint.setTextSize(dpToPx(6.1f));
                } else if (mMobileTypeSize > 0) {
                    textPaint.setTextSize(mMobileTypeSize);
                }
            }
        });

        // 1. View layout params update hook (Per-slot isolated mutual exclusion & Vertical alignment calibration)
        XposedHelpers.findAndHookMethod(
            binderClz,
            "updateMobileTypeLayoutParams",
            drawableClz,
            String.class,
            ImageView.class,
            new XC_MethodHook() {
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
                            XposedHelpers.setAdditionalInstanceField(drawable, "view", mobileTypeView);
                            XposedHelpers.setAdditionalInstanceField(drawable, "actualMobileType", str);
                        }

                        int subId = getSubIdForView(mobileTypeView);
                        int slotId = getSlotIdForView(mobileTypeView);
                        IconConfig cfg = getConfigForSlot(slotId);
                        int rType = RADIO_TYPE_UNKNOWN;

                        if (is5gCaText(str)) {
                            rType = RADIO_TYPE_5G_CA;
                            sIs5GConnected = true;
                            sIs5GCaConnected = true;
                            if (subId >= 0) {
                                sSubIdIs5G.put(subId, Boolean.TRUE);
                                sSubIdIs5GCa.put(subId, Boolean.TRUE);
                                sSubIdRadioType.put(subId, Integer.valueOf(rType));
                            }
                            XposedHelpers.setAdditionalInstanceField(mobileTypeView, "isCaActive", Boolean.TRUE);
                        } else if ("5G".equals(str)) {
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
                                XposedHelpers.setAdditionalInstanceField(mobileTypeView, "isCaActive", Boolean.FALSE);
                            }
                        } else if (str.contains("+") || str.contains("-A")) {
                            rType = RADIO_TYPE_4G_CA;
                            if (subId >= 0) {
                                sSubIdIs5G.put(subId, Boolean.FALSE);
                                sSubIdIs5GCa.put(subId, Boolean.FALSE);
                                sSubIdRadioType.put(subId, Integer.valueOf(rType));
                            }
                            XposedHelpers.setAdditionalInstanceField(mobileTypeView, "isCaActive", Boolean.FALSE);
                        } else if (str.contains("4G") || str.contains("LTE") || str.contains("4.5G")) {
                            rType = RADIO_TYPE_4G;
                            if (subId >= 0) {
                                sSubIdIs5G.put(subId, Boolean.FALSE);
                                sSubIdIs5GCa.put(subId, Boolean.FALSE);
                                sSubIdRadioType.put(subId, Integer.valueOf(rType));
                            }
                            XposedHelpers.setAdditionalInstanceField(mobileTypeView, "isCaActive", Boolean.FALSE);
                        } else if (!TextUtils.isEmpty(str)) {
                            rType = RADIO_TYPE_2G_3G;
                            if (subId >= 0) {
                                sSubIdIs5G.put(subId, Boolean.FALSE);
                                sSubIdIs5GCa.put(subId, Boolean.FALSE);
                                sSubIdRadioType.put(subId, Integer.valueOf(rType));
                            }
                            XposedHelpers.setAdditionalInstanceField(mobileTypeView, "isCaActive", Boolean.FALSE);
                        }

                        if (rType != RADIO_TYPE_UNKNOWN) {
                            XposedHelpers.setAdditionalInstanceField(mobileTypeView, "radioType", Integer.valueOf(rType));
                        }

                        trackActiveView(mobileTypeView);

                        ViewGroup parent = (ViewGroup) mobileTypeView.getParent();
                        if (parent == null) return;

                        int special5gId = parent.getResources().getIdentifier("mobile_special_5G", "id", "com.android.systemui");
                        ImageView special5gView = special5gId != 0 ? (ImageView) parent.findViewById(special5gId) : null;

                        syncViewVisibility(mobileTypeView, special5gView, str, cfg);
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + ": updateMobileTypeLayoutParams hook error: " + t);
                    }
                }
            }
        );

        // 2. Coroutine visibility emission hook (Per-slot atomic mutual exclusion, NEVER kill views on uninitialized empty text)
        Class<?> emitCollectorClz = findEmitCollectorClass(lpparam.classLoader);
        if (emitCollectorClz != null) {
            XposedBridge.log(TAG + ": [v" + VERSION + "] Hooking emitCollector on " + emitCollectorClz.getName());
            XposedHelpers.findAndHookMethod(
                emitCollectorClz,
                "emit",
                Object.class,
                "kotlin.coroutines.Continuation",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            int classId = XposedHelpers.getIntField(param.thisObject, "$r8$classId");
                            if (classId != 1 && classId != 2) return;

                            ImageView targetView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "$satellite");
                            if (targetView == null) return;

                            Context ctx = targetView.getContext();
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
                            XposedBridge.log(TAG + ": emit collector hook error: " + t);
                        }
                    }
                }
            );
        }

        // 3. Network activity flow collector hook (Drives real-time breathing for 5G CA)
        Class<?> special5gResCollectorClz = XposedHelpers.findClassIfExists(
            "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder$bind$1$1$9$1",
            lpparam.classLoader
        );
        if (special5gResCollectorClz != null) {
            XposedHelpers.findAndHookMethod(
                special5gResCollectorClz,
                "invokeSuspend",
                Object.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            ImageView special5gView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "$special5G");
                            if (special5gView == null) return;

                            int resId = XposedHelpers.getIntField(param.thisObject, "I$0");
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
                                XposedHelpers.setAdditionalInstanceField(d, "isDataActive", Boolean.valueOf(isDataActive));
                                d.invalidateSelf();
                            }
                        } catch (Throwable t) {
                            XposedBridge.log(TAG + ": special5gResCollector hook error: " + t);
                        }
                    }
                }
            );
        }

        // 4. Hook MobileTypeDrawable.draw for Option A Korean Capsule Breathing (Exclusively for 5GA & 5G+)
        XposedHelpers.findAndHookMethod(
            drawableClz,
            "draw",
            Canvas.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Drawable drawable = (Drawable) param.thisObject;
                        String mobileType = getDrawableMobileType(drawable);
                        if (TextUtils.isEmpty(mobileType)) return;

                        View v = (View) XposedHelpers.getAdditionalInstanceField(drawable, "view");
                        int slotId = getSlotIdForView(v);
                        IconConfig cfg = getConfigForSlot(slotId);
                        boolean isCapsuleMode = "korean".equals(cfg.capsule);
                        if (!isCapsuleMode || !isCapsuleTargetText(mobileType)) {
                            // Non-capsule mode or non-target text: use default native system drawing
                            return;
                        }

                        // Intercept default draw and render Option A Capsule Breathing
                        param.setResult(null);

                        Canvas canvas = (Canvas) param.args[0];
                        drawCapsuleBreath(canvas, drawable, mobileType);
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + ": draw hook error: " + t);
                    }
                }
            }
        );
    }

    private static void drawCapsuleBreath(Canvas canvas, Drawable drawable, String mobileType) {
        try {
            Paint textPaint = (Paint) XposedHelpers.getObjectField(drawable, "mMobileTypeTextPaint");
            int baseColor = XposedHelpers.getIntField(drawable, "mMobileTypeColor");

            // Strictly align font size with native Korean 5G pill (cap-height 4.40dp / 8.0dp = 55% ratio)
            textPaint.setTextSize(dpToPx(6.1f));
            float measuredW = textPaint.measureText(mobileType);

            // Resolve real-time data active state
            boolean isDataActive = false;
            Object activeObj = XposedHelpers.getAdditionalInstanceField(drawable, "isDataActive");
            if (activeObj instanceof Boolean) {
                isDataActive = ((Boolean) activeObj).booleanValue();
            } else {
                // Fallback: check tag of special5GView in same parent
                ImageView mobileTypeView = (ImageView) XposedHelpers.getAdditionalInstanceField(drawable, "view");
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
            XposedBridge.log(TAG + ": drawCapsuleBreath error: " + t);
        }
    }
}

