package com.midori.hypericons;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.widget.Toast;

import java.io.File;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CellularFeatureHook {
    private static final String TAG = "HyperIconsPhoneHook";
    public static final String KEY_ENABLE_5GA_KEEPALIVE = "pref_enable_5ga_keepalive";
    private static final String PREF_PACKAGE = "com.midori.hypericons";
    private static final String PREF_FILE = "hyper_icons_config";
    private static final String PREF_KEEPALIVE_LOCAL = "hyper_cellular_keepalive";
    private static final Uri PROVIDER_URI = Uri.parse("content://com.midori.hypericons.provider/config");
    public static final String ACTION_RELOAD = "com.midori.hypericons.ACTION_RELOAD";

    private static volatile Handler sHandler = null;
    private static volatile Context sContext = null;
    private static volatile Boolean sFeatureEnabledCache = null;
    private static volatile long sLastCheckTime = 0;
    private static final long CACHE_TTL_MS = 2000; // 2 seconds cache TTL
    private static volatile long sLastAutoRestoreTime = 0;

    private static Handler getHandler() {
        if (sHandler == null) {
            synchronized (CellularFeatureHook.class) {
                if (sHandler == null) {
                    Looper looper = Looper.getMainLooper();
                    if (looper != null) {
                        sHandler = new Handler(looper);
                    }
                }
            }
        }
        return sHandler;
    }

    public static void invalidateFeatureEnabledCache() {
        sFeatureEnabledCache = null;
        sLastCheckTime = 0;
    }

    public static boolean isFeatureEnabled(ClassLoader cl) {
        long now = System.currentTimeMillis();
        Boolean cached = sFeatureEnabledCache;
        if (cached != null && (now - sLastCheckTime < CACHE_TTL_MS)) {
            return cached.booleanValue();
        }

        boolean result = checkFeatureEnabledDirect(cl);
        sFeatureEnabledCache = result;
        sLastCheckTime = now;
        return result;
    }

    private static boolean checkFeatureEnabledDirect(ClassLoader cl) {
        // 1. Query ContentProvider if Context is available
        Context ctx = sContext;
        if (ctx == null && cl != null) {
            try {
                Class<?> f5gClz = XposedHelpers.findClassIfExists("com.android.phone.FiveGManagerBase", cl);
                if (f5gClz != null) {
                    ctx = (Context) XposedHelpers.getStaticObjectField(f5gClz, "mContext");
                }
            } catch (Throwable ignored) {}
        }

        if (ctx != null) {
            try {
                Bundle b = ctx.getContentResolver().call(PROVIDER_URI, "getConfig", null, null);
                if (b != null && b.containsKey(KEY_ENABLE_5GA_KEEPALIVE)) {
                    return b.getBoolean(KEY_ENABLE_5GA_KEEPALIVE, false);
                }
            } catch (Throwable ignored) {}
        }

        // 2. Query Direct Boot aware Device Protected storage
        try {
            File deFile = new File("/data/user_de/0/" + PREF_PACKAGE + "/shared_prefs/" + PREF_FILE + ".xml");
            if (deFile.exists() && deFile.canRead()) {
                XSharedPreferences dePrefs = new XSharedPreferences(deFile);
                dePrefs.reload();
                return dePrefs.getBoolean(KEY_ENABLE_5GA_KEEPALIVE, false);
            }
        } catch (Throwable ignored) {}

        // 3. Query Credential Encrypted storage
        try {
            File ceFile = new File("/data/data/" + PREF_PACKAGE + "/shared_prefs/" + PREF_FILE + ".xml");
            if (ceFile.exists() && ceFile.canRead()) {
                XSharedPreferences cePrefs = new XSharedPreferences(ceFile);
                cePrefs.reload();
                return cePrefs.getBoolean(KEY_ENABLE_5GA_KEEPALIVE, false);
            }
        } catch (Throwable ignored) {}

        // 4. Query package prefs fallback
        try {
            XSharedPreferences pkgPrefs = new XSharedPreferences(PREF_PACKAGE, PREF_FILE);
            pkgPrefs.reload();
            return pkgPrefs.getBoolean(KEY_ENABLE_5GA_KEEPALIVE, false);
        } catch (Throwable ignored) {}

        return false;
    }

    // Local snapshot preference helpers for respecting user configuration
    private static SharedPreferences getKeepAlivePrefs(Context ctx) {
        if (ctx == null) return null;
        try {
            return ctx.getSharedPreferences(PREF_KEEPALIVE_LOCAL, Context.MODE_PRIVATE);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean getSavedVoNRSetting(Context ctx, int slotId) {
        SharedPreferences sp = getKeepAlivePrefs(ctx);
        if (sp != null && sp.contains("saved_vonr_slot_" + slotId)) {
            return sp.getBoolean("saved_vonr_slot_" + slotId, true);
        }
        return true;
    }

    private static void saveVoNRSetting(Context ctx, int slotId, boolean enabled) {
        SharedPreferences sp = getKeepAlivePrefs(ctx);
        if (sp != null) {
            sp.edit().putBoolean("saved_vonr_slot_" + slotId, enabled).apply();
            XposedBridge.log(TAG + ": User configured VoNR for slot " + slotId + " = " + enabled);
        }
    }

    private static boolean getSavedDsdaSetting(Context ctx) {
        SharedPreferences sp = getKeepAlivePrefs(ctx);
        if (sp != null && sp.contains("saved_dsda_enabled")) {
            return sp.getBoolean("saved_dsda_enabled", true);
        }
        if (ctx != null) {
            try {
                int dbVal = Settings.Global.getInt(ctx.getContentResolver(), "hybrid_dsda_mode_enabled", -1);
                if (dbVal != -1) return dbVal == 1;
            } catch (Throwable ignored) {}
        }
        return true;
    }

    private static void saveDsdaSetting(Context ctx, boolean enabled) {
        SharedPreferences sp = getKeepAlivePrefs(ctx);
        if (sp != null) {
            sp.edit().putBoolean("saved_dsda_enabled", enabled).apply();
            XposedBridge.log(TAG + ": User configured DSDA = " + enabled);
        }
    }

    private static int getSavedApnSetting(Context ctx) {
        SharedPreferences sp = getKeepAlivePrefs(ctx);
        if (sp != null && sp.contains("saved_apn_modify_flag")) {
            return sp.getInt("saved_apn_modify_flag", 1);
        }
        if (ctx != null) {
            try {
                return Settings.System.getInt(ctx.getContentResolver(), "apn_modify_flag", 1);
            } catch (Throwable ignored) {}
        }
        return 1;
    }

    private static void saveApnSetting(Context ctx, int val) {
        SharedPreferences sp = getKeepAlivePrefs(ctx);
        if (sp != null) {
            sp.edit().putInt("saved_apn_modify_flag", val).apply();
            XposedBridge.log(TAG + ": User configured APN modify flag = " + val);
        }
    }

    public static void init(final XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.log(TAG + ": Initializing CellularFeatureHook for com.android.phone");

        // 1. Hook PhoneApp onCreate to register SIM, CarrierConfig broadcast listeners & APN observer
        hookApplication(lpparam);

        // 2. Hook FiveGManagerBase lifecycle, CarrierConfig changes, data slot transitions & VoNR
        hookFiveGManager(lpparam);

        // 3. Hook MiuiDsdaManager lifecycle & DSDA state tracking
        hookDsdaManager(lpparam);

        // 4. Hook status getters to prevent UI regression and self-heal modem states
        hookStatusGetters(lpparam);
    }

    private static void hookApplication(final XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> appClz = XposedHelpers.findClassIfExists("com.android.phone.PhoneApp", lpparam.classLoader);
        if (appClz == null) {
            appClz = XposedHelpers.findClassIfExists("com.android.phone.MiuiPhoneApp", lpparam.classLoader);
        }
        if (appClz == null) {
            appClz = XposedHelpers.findClassIfExists("android.app.Application", lpparam.classLoader);
        }
        if (appClz != null) {
            XposedHelpers.findAndHookMethod(appClz, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    sContext = (Context) param.thisObject;
                    registerSimReceiver(sContext, lpparam.classLoader);
                    registerApnObserver(sContext);
                }
            });
        }
    }

    private static void registerApnObserver(final Context context) {
        if (context == null) return;
        try {
            Handler h = getHandler();
            if (h != null) {
                // Initialize default APN setting cache from existing system state
                int initialApn = Settings.System.getInt(context.getContentResolver(), "apn_modify_flag", 1);
                saveApnSetting(context, initialApn);

                context.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor("apn_modify_flag"),
                    false,
                    new ContentObserver(h) {
                        @Override
                        public void onChange(boolean selfChange) {
                            try {
                                int val = Settings.System.getInt(context.getContentResolver(), "apn_modify_flag", 0);
                                saveApnSetting(context, val);
                            } catch (Throwable ignored) {}
                        }
                    }
                );
                XposedBridge.log(TAG + ": ContentObserver registered for apn_modify_flag (initial=" + initialApn + ")");
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to register APN observer: " + t);
        }
    }

    private static void registerSimReceiver(final Context context, final ClassLoader cl) {
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
            filter.addAction("android.intent.action.SIM_STATE_CHANGED");
            filter.addAction(ACTION_RELOAD);
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    if (intent == null) return;
                    String action = intent.getAction();

                    if (ACTION_RELOAD.equals(action)) {
                        invalidateFeatureEnabledCache();
                        boolean enabled = isFeatureEnabled(cl);
                        XposedBridge.log(TAG + ": ACTION_RELOAD received, 5G+ keepalive enabled=" + enabled);
                        if (enabled) {
                            scheduleRestoreMultiPhase(cl, 0);
                            scheduleRestoreMultiPhase(cl, 1);
                        }
                        return;
                    }

                    if (!isFeatureEnabled(cl)) {
                        XposedBridge.log(TAG + ": Event " + action + " ignored (feature disabled)");
                        return;
                    }

                    int slot = intent.getIntExtra("android.telephony.extra.SLOT_INDEX", -1);
                    if (slot < 0) slot = intent.getIntExtra("slot", -1);
                    if (slot < 0) slot = intent.getIntExtra("phone", -1);

                    XposedBridge.log(TAG + ": Broadcast received: " + action + ", slot=" + slot);
                    if (slot >= 0 && slot <= 1) {
                        scheduleRestoreMultiPhase(cl, slot);
                    } else {
                        scheduleRestoreMultiPhase(cl, 0);
                        scheduleRestoreMultiPhase(cl, 1);
                    }
                }
            }, filter);
            XposedBridge.log(TAG + ": BroadcastReceiver registered for CARRIER_CONFIG_CHANGED, SIM_STATE_CHANGED & ACTION_RELOAD");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": registerSimReceiver error: " + t);
        }
    }

    private static void hookFiveGManager(final XC_LoadPackage.LoadPackageParam lpparam) {
        final Class<?> f5gClz = XposedHelpers.findClassIfExists("com.android.phone.FiveGManagerBase", lpparam.classLoader);
        if (f5gClz != null) {
            // Hook onHandleCarrierConfigChanged
            try {
                XposedHelpers.findAndHookMethod(f5gClz, "onHandleCarrierConfigChanged", Message.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!isFeatureEnabled(lpparam.classLoader)) return;
                        Message msg = (Message) param.args[0];
                        int slotId = (msg != null) ? msg.arg1 : -1;
                        XposedBridge.log(TAG + ": onHandleCarrierConfigChanged slotId=" + slotId);
                        if (slotId >= 0 && slotId <= 1) {
                            scheduleRestoreMultiPhase(lpparam.classLoader, slotId);
                        } else {
                            scheduleRestoreMultiPhase(lpparam.classLoader, 0);
                            scheduleRestoreMultiPhase(lpparam.classLoader, 1);
                        }
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Failed to hook onHandleCarrierConfigChanged: " + t);
            }

            // Hook onDataSlotReady
            try {
                XposedHelpers.findAndHookMethod(f5gClz, "onDataSlotReady", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!isFeatureEnabled(lpparam.classLoader)) return;
                        XposedBridge.log(TAG + ": onDataSlotReady triggered");
                        scheduleRestoreMultiPhase(lpparam.classLoader, 0);
                        scheduleRestoreMultiPhase(lpparam.classLoader, 1);
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Failed to hook onDataSlotReady: " + t);
            }

            // Hook onInitFiveGData
            try {
                XposedHelpers.findAndHookMethod(f5gClz, "onInitFiveGData", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!isFeatureEnabled(lpparam.classLoader)) return;
                        XposedBridge.log(TAG + ": onInitFiveGData triggered");
                        scheduleRestoreMultiPhase(lpparam.classLoader, 0);
                        scheduleRestoreMultiPhase(lpparam.classLoader, 1);
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Failed to hook onInitFiveGData: " + t);
            }

            // Hook setUserVoNREnabled to capture user preference changes
            try {
                XposedHelpers.findAndHookMethod(f5gClz, "setUserVoNREnabled", int.class, boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        int slotId = (Integer) param.args[0];
                        boolean enabled = (Boolean) param.args[1];
                        Context ctx = (Context) XposedHelpers.getStaticObjectField(f5gClz, "mContext");
                        saveVoNRSetting(ctx != null ? ctx : sContext, slotId, enabled);
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Failed to hook setUserVoNREnabled on FiveGManagerBase: " + t);
            }
        }
    }

    private static void hookDsdaManager(final XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> dsdaClz = XposedHelpers.findClassIfExists("com.android.phone.dsda.MiuiDsdaManager", lpparam.classLoader);
        if (dsdaClz != null) {
            try {
                XposedHelpers.findAndHookMethod(dsdaClz, "setDsdaEnabled", boolean.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        boolean enabled = (Boolean) param.args[0];
                        saveDsdaSetting(sContext, enabled);
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Failed to hook setDsdaEnabled on MiuiDsdaManager: " + t);
            }
        }

        // Suppress "双卡双通开启 重启生效" Toast spam completely when keepalive feature is enabled
        Class<?> handlerClz = XposedHelpers.findClassIfExists("com.android.phone.dsda.MiuiDsdaManager$DsdaHandler", lpparam.classLoader);
        if (handlerClz != null) {
            try {
                XposedHelpers.findAndHookMethod(handlerClz, "handleMessage", Message.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Message msg = (Message) param.args[0];
                        if (msg != null && msg.what == 3) {
                            if (isFeatureEnabled(param.thisObject.getClass().getClassLoader())) {
                                // DsdaHandler case 3 only pops Toast. Silently suppress it.
                                param.setResult(null);
                            }
                        }
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Failed to hook DsdaHandler.handleMessage: " + t);
            }
        }

        // Global Toast interceptor in com.android.phone as ultimate safety fallback
        try {
            XposedHelpers.findAndHookMethod(Toast.class, "show", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!isFeatureEnabled(param.thisObject.getClass().getClassLoader())) return;
                    CharSequence text = null;
                    try {
                        text = (CharSequence) XposedHelpers.callMethod(param.thisObject, "getText");
                    } catch (Throwable ignored) {}
                    if (text == null) {
                        try {
                            Object t = XposedHelpers.getObjectField(param.thisObject, "mText");
                            if (t != null) text = t.toString();
                        } catch (Throwable ignored) {}
                    }
                    if (text != null) {
                        String s = text.toString();
                        if (s.contains("双卡双通") || s.contains("Dual active") || s.contains("雙卡雙待") || s.contains("重启") || s.contains("Reboot")) {
                            param.setResult(null);
                        }
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook Toast.show: " + t);
        }
    }

    private static void hookStatusGetters(final XC_LoadPackage.LoadPackageParam lpparam) {
        final Class<?> qcomClz = XposedHelpers.findClassIfExists("com.android.phone.FiveGManager", lpparam.classLoader);
        final Class<?> baseClz = XposedHelpers.findClassIfExists("com.android.phone.FiveGManagerBase", lpparam.classLoader);

        if (qcomClz != null) {
            // 1. get3GPPVersion
            try {
                XposedHelpers.findAndHookMethod(qcomClz, "get3GPPVersion", int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!isFeatureEnabled(param.thisObject.getClass().getClassLoader())) return;
                        int slotId = (Integer) param.args[0];
                        if (slotId < 0 || slotId > 1) return;
                        int result = (Integer) param.getResult();
                        int target = getTarget3GPPVersion(param.thisObject);
                        if (result < target) {
                            param.setResult(target);
                            applySingleMethod(param.thisObject, "set3GPPVersion", target, slotId);
                        }
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Failed to hook get3GPPVersion: " + t);
            }

            // 2. 5G-A and enhanced features getters
            hookBooleanGetter(qcomClz, "isFiveGAEnabled", "setUserFiveGAEnabled", "5ga_enable", true);
            hookBooleanGetter(qcomClz, "isFiveGA4CCEnabled", "setUserFiveGA4CCEnabled", "5ga_4cc_enable", true);
            hookBooleanGetter(qcomClz, "isHOMEnabled", "setUserHOMEnabled", "hom_enable", true);
            hookBooleanGetter(qcomClz, "isSwulEnabled", "setUserSwulEnabled", "swul_enable", true);
            hookBooleanGetter(qcomClz, "isR16PowerEnabled", "setUserR16PowerEnabled", "r16_power_enable", true);
            hookBooleanGetter(qcomClz, "isFullPowerModeEnabled", "setFullPowerModeEnabled", "full_power_mode", true);
            hookBooleanGetter(qcomClz, "isConditionalHandoverEnabled", "setConditionalHandoverEnabled", "conditional_handover_enable", true);
            hookBooleanGetter(qcomClz, "isUlCaEnabled", "setUlCaEnabled", "ulca_enable", true);

            // 3. VoNR getter hook on FiveGManager
            hookVoNRGetter(qcomClz, baseClz);
        }

        // 4. DSDA getter hook on MiuiDsdaManager
        Class<?> dsdaClz = XposedHelpers.findClassIfExists("com.android.phone.dsda.MiuiDsdaManager", lpparam.classLoader);
        if (dsdaClz != null) {
            try {
                XposedHelpers.findAndHookMethod(dsdaClz, "isDsdaEnabled", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!isFeatureEnabled(param.thisObject.getClass().getClassLoader())) return;
                        boolean target = getSavedDsdaSetting(sContext);
                        boolean result = (Boolean) param.getResult();
                        if (!result && target) {
                            param.setResult(true);
                        }
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": Failed to hook isDsdaEnabled on MiuiDsdaManager: " + t);
            }
        }
    }

    private static void hookVoNRGetter(Class<?> qcomClz, final Class<?> baseClz) {
        try {
            XposedHelpers.findAndHookMethod(qcomClz, "isVoNREnabled", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!isFeatureEnabled(param.thisObject.getClass().getClassLoader())) return;
                    int slotId = (Integer) param.args[0];
                    if (slotId < 0 || slotId > 1) return;
                    Context ctx = baseClz != null ? (Context) XposedHelpers.getStaticObjectField(baseClz, "mContext") : null;
                    boolean target = getSavedVoNRSetting(ctx != null ? ctx : sContext, slotId);
                    boolean result = (Boolean) param.getResult();
                    if (!result && target) {
                        param.setResult(true);
                        applySingleMethod(param.thisObject, "setUserVoNREnabled", slotId, true);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook isVoNREnabled: " + t);
        }
    }

    private static void hookBooleanGetter(Class<?> clz, final String getterName, final String setterName, final String prefKey, final boolean defValue) {
        try {
            XposedHelpers.findAndHookMethod(clz, getterName, int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!isFeatureEnabled(param.thisObject.getClass().getClassLoader())) return;
                    int slotId = (Integer) param.args[0];
                    if (slotId < 0 || slotId > 1) return;
                    boolean result = (Boolean) param.getResult();
                    boolean target = getTargetFeatureEnabled(param.thisObject, prefKey, defValue);
                    if (!result && target) {
                        param.setResult(true);
                        applySingleMethod(param.thisObject, setterName, 1, slotId);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook " + getterName + ": " + t);
        }
    }

    public static void scheduleRestoreMultiPhase(final ClassLoader cl, final int slotId) {
        if (!isFeatureEnabled(cl)) return;
        Handler h = getHandler();
        if (h == null) {
            restoreFeatures(cl, slotId);
            return;
        }

        h.post(new Runnable() {
            @Override
            public void run() {
                restoreFeatures(cl, slotId);
            }
        });

        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                restoreFeatures(cl, slotId);
            }
        }, 1200);

        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                restoreFeatures(cl, slotId);
            }
        }, 3000);
    }

    public static void restoreFeatures(ClassLoader cl, int slotId) {
        if (!isFeatureEnabled(cl)) return;
        if (slotId < 0 || slotId > 1) return;
        sLastAutoRestoreTime = System.currentTimeMillis();
        try {
            Class<?> f5gClz = XposedHelpers.findClassIfExists("com.android.phone.FiveGManagerBase", cl);
            if (f5gClz == null) return;
            Object manager = XposedHelpers.callStaticMethod(f5gClz, "getInstance");
            if (manager == null) {
                Class<?> qcomClz = XposedHelpers.findClassIfExists("com.android.phone.FiveGManager", cl);
                if (qcomClz != null) {
                    manager = XposedHelpers.callStaticMethod(qcomClz, "internalInit");
                }
            }
            if (manager == null) return;

            Context ctx = (Context) XposedHelpers.getStaticObjectField(f5gClz, "mContext");
            Context activeCtx = ctx != null ? ctx : sContext;
            SharedPreferences sp = ctx != null ? PreferenceManager.getDefaultSharedPreferences(ctx) : null;

            // 1. 3GPP R17 & 5G+ features
            int targetVersion = sp != null ? sp.getInt("set_3gpp_version", 17) : 17;
            if (targetVersion < 15 || targetVersion > 18) targetVersion = 17;
            applySingleMethod(manager, "set3GPPVersion", targetVersion, slotId);

            int fivega = sp != null ? sp.getInt("5ga_enable", 1) : 1;
            applySingleMethod(manager, "setUserFiveGAEnabled", fivega, slotId);

            int fivega4cc = sp != null ? sp.getInt("5ga_4cc_enable", 1) : 1;
            applySingleMethod(manager, "setUserFiveGA4CCEnabled", fivega4cc, slotId);

            int hom = sp != null ? sp.getInt("hom_enable", 1) : 1;
            applySingleMethod(manager, "setUserHOMEnabled", hom, slotId);

            int swul = sp != null ? sp.getInt("swul_enable", 1) : 1;
            applySingleMethod(manager, "setUserSwulEnabled", swul, slotId);

            int r16power = sp != null ? sp.getInt("r16_power_enable", 1) : 1;
            applySingleMethod(manager, "setUserR16PowerEnabled", r16power, slotId);

            int fullpower = sp != null ? sp.getInt("full_power_mode", 1) : 1;
            applySingleMethod(manager, "setFullPowerModeEnabled", fullpower, slotId);

            int cho = sp != null ? sp.getInt("conditional_handover_enable", 1) : 1;
            applySingleMethod(manager, "setConditionalHandoverEnabled", cho, slotId);

            int ulca = sp != null ? sp.getInt("ulca_enable", 1) : 1;
            applySingleMethod(manager, "setUlCaEnabled", ulca, slotId);

            int sul = sp != null ? sp.getInt("sul_enable", 0) : 0;
            applySingleMethod(manager, "setSulEnabled", sul, slotId);

            // 2. VoNR auto keepalive
            boolean targetVoNR = getSavedVoNRSetting(activeCtx, slotId);
            applySingleMethod(manager, "setUserVoNREnabled", slotId, targetVoNR);
            XposedBridge.log(TAG + ": [Slot " + slotId + "] VoNR auto-restored to " + targetVoNR);

            // 3. DSDA (双卡双通) auto keepalive - Silent, zero toast spam (Only for Slot 1: SIM 2 / eSIM)
            boolean targetDsda = getSavedDsdaSetting(activeCtx);
            if (slotId == 1 && activeCtx != null) {
                try {
                    int curDb = Settings.Global.getInt(activeCtx.getContentResolver(), "hybrid_dsda_mode_enabled", -1);
                    if (curDb != (targetDsda ? 1 : 0)) {
                        Settings.Global.putInt(activeCtx.getContentResolver(), "hybrid_dsda_mode_enabled", targetDsda ? 1 : 0);
                    }
                    Class<?> dsdaClz = XposedHelpers.findClassIfExists("com.android.phone.dsda.MiuiDsdaManager", cl);
                    if (dsdaClz != null) {
                        Object dsdaMgr = XposedHelpers.callStaticMethod(dsdaClz, "getInstance");
                        if (dsdaMgr != null) {
                            boolean isResetHwMbn = false;
                            try {
                                isResetHwMbn = (Boolean) XposedHelpers.callMethod(dsdaMgr, "isSwitchDsdaModeByResetHwMbn");
                            } catch (Throwable ignored) {}

                            if (isResetHwMbn) {
                                // Directly update persistent property without posting Toast Message 3
                                Object modemUtils = XposedHelpers.getObjectField(dsdaMgr, "mModemDsdaUtils");
                                if (modemUtils != null) {
                                    XposedHelpers.callMethod(modemUtils, "setDsdaCapPropertyEnabled", targetDsda);
                                }
                            } else {
                                XposedHelpers.callMethod(dsdaMgr, "setDsdaEnabledByQmiMsimPreference", targetDsda);
                            }
                        }
                    }
                    XposedBridge.log(TAG + ": DSDA auto-restored (silent) to " + targetDsda);
                } catch (Throwable t) {
                    XposedBridge.log(TAG + ": Failed to restore DSDA: " + t);
                }
            }

            // 4. 强制APN编辑 (apn_modify_flag) auto keepalive
            int targetApn = getSavedApnSetting(activeCtx);
            if (activeCtx != null) {
                try {
                    Settings.System.putInt(activeCtx.getContentResolver(), "apn_modify_flag", targetApn);
                    XposedBridge.log(TAG + ": apn_modify_flag auto-restored to " + targetApn);
                } catch (Throwable t) {
                    XposedBridge.log(TAG + ": Failed to restore apn_modify_flag: " + t);
                }
            }

            XposedBridge.log(TAG + ": [SIM 2 / eSIM (Slot 1)] Full features locked & restored: R" + targetVersion + ", 5GA=" + fivega + ", VoNR=" + targetVoNR + ", DSDA=" + targetDsda + ", APN=" + targetApn);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": restoreFeatures failed for slot " + slotId + ": " + t);
        }
    }

    private static void applySingleMethod(Object manager, String methodName, int val, int slotId) {
        if (manager == null) return;
        try {
            XposedHelpers.callMethod(manager, methodName, val, slotId);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": " + methodName + " failed: " + t);
        }
    }

    private static void applySingleMethod(Object manager, String methodName, int slotId, boolean val) {
        if (manager == null) return;
        try {
            XposedHelpers.callMethod(manager, methodName, slotId, val);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": " + methodName + " failed: " + t);
        }
    }

    private static int getTarget3GPPVersion(Object manager) {
        try {
            Context ctx = (Context) XposedHelpers.getStaticObjectField(
                XposedHelpers.findClass("com.android.phone.FiveGManagerBase", manager.getClass().getClassLoader()),
                "mContext"
            );
            if (ctx != null) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
                int ver = sp.getInt("set_3gpp_version", 17);
                if (ver >= 15 && ver <= 18) return ver;
            }
        } catch (Throwable ignored) {}
        return 17;
    }

    private static boolean getTargetFeatureEnabled(Object manager, String prefKey, boolean defVal) {
        try {
            Context ctx = (Context) XposedHelpers.getStaticObjectField(
                XposedHelpers.findClass("com.android.phone.FiveGManagerBase", manager.getClass().getClassLoader()),
                "mContext"
            );
            if (ctx != null) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
                return sp.getInt(prefKey, defVal ? 1 : 0) == 1;
            }
        } catch (Throwable ignored) {}
        return defVal;
    }
}
