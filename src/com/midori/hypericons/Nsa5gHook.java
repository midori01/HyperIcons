package com.midori.hypericons;

import android.os.Handler;
import android.os.SystemClock;
import android.telephony.ServiceState;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

/** Experimental NSA 5G rules for the status bar. Everything here is off by default. */
final class Nsa5gHook {
    private Nsa5gHook() {}

    private static final String TAG = "HyperIconsNsa";
    private static final String CLIENT_CLASS = "com.android.systemui.statusbar.policy.MiuiFiveGServiceClient";
    private static final String ICONS_CLASS = "com.miui.systemui.statusbar.mobile.TelephonyIcons";
    private static final String FIVE_G_CONNECTED_CLASS =
            "com.android.systemui.statusbar.pipeline.mobile.domain.interactor.MiuiMobileIconInteractorImpl$fiveGConnected$3";

    // Stock SystemUI waits this long before dropping 5G on its Korean carrier path.
    private static final long BEARER_HOLD_MS = 11000L;
    private static final int NR_STATE_RESTRICTED = 1;
    private static final int NR_STATE_NOT_RESTRICTED = 2;

    private static volatile WeakReference<Object> sClient;
    private static volatile Object sUnknownIconGroup;
    private static volatile boolean sIgnoreUliApplied;
    private static final ConcurrentHashMap<Integer, Boolean> sBearerUp = new ConcurrentHashMap<Integer, Boolean>();
    private static final ConcurrentHashMap<Integer, Long> sBearerReleasedAt = new ConcurrentHashMap<Integer, Long>();
    private static final ThreadLocal<Boolean> sEvaluatingFiveG = new ThreadLocal<Boolean>();
    private static final ConcurrentHashMap<Integer, Runnable> sHoldExpiries = new ConcurrentHashMap<Integer, Runnable>();

    static boolean ignoreUli() {
        return ConfigKeys.VALUE_ON.equals(MainHook.pref(ConfigKeys.KEY_IGNORE_ULI));
    }

    static void hookIgnoreUli(LoadPackageParam lpparam) {
        ClassLoader loader = lpparam.classLoader;
        Class<?> clientClz = Reflect.findClassIfExists(CLIENT_CLASS, loader);
        Class<?> iconsClz = Reflect.findClassIfExists(ICONS_CLASS, loader);
        if (clientClz != null && iconsClz != null) {
            sUnknownIconGroup = Reflect.getStaticObjectField(iconsClz, "UNKNOWN");
            HookEnv.hookAllConstructors(clientClz, new MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    sClient = new WeakReference<Object>(param.thisObject);
                }
            });
            // Runs before the icon group is compared and published, so a fake 5G never reaches the status bar.
            HookEnv.findAndHookMethod(clientClz, "notifyListenersIfNecessary", int.class, new MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    applyIgnoreUli(param.thisObject, (Integer) param.args[0]);
                }
            });
        }
        hookNrStatePath(loader);
    }

    private static void applyIgnoreUli(Object client, int slot) {
        Object state = Reflect.callMethod(client, "getCurrentServiceState", slot);
        long now = SystemClock.elapsedRealtime();
        int bearer = Reflect.getIntField(state, "mBearerAllocationStatus");
        // The modem only reports changes, so the hold has to start when the bearer goes away.
        Boolean wasUp = sBearerUp.put(slot, bearer > 0);
        if (bearer > 0) {
            sBearerReleasedAt.remove(slot);
        } else if (Boolean.TRUE.equals(wasUp)) {
            sBearerReleasedAt.put(slot, now);
        }
        if (!ignoreUli() || Reflect.getObjectField(state, "mIconGroup") == sUnknownIconGroup) return;

        Long released = sBearerReleasedAt.get(slot);
        long sinceBearer = released == null ? Long.MAX_VALUE : now - released;
        int action = IconPolicy.nsaIconAction(Reflect.getIntField(state, "mNrConfigType"), bearer,
                dataNetworkType(client, slot), sinceBearer, BEARER_HOLD_MS);
        if (action == IconPolicy.NSA_ICON_CLEAR) {
            Reflect.setObjectField(state, "mIconGroup", sUnknownIconGroup);
        } else if (action == IconPolicy.NSA_ICON_HOLD) {
            Handler handler = (Handler) Reflect.getObjectField(client, "mHandler");
            Runnable expiry = sHoldExpiries.get(slot);
            if (expiry == null) {
                expiry = new Runnable() {
                    @Override
                    public void run() {
                        recomputeIcons();
                    }
                };
                sHoldExpiries.put(slot, expiry);
            }
            handler.removeCallbacks(expiry);
            handler.postDelayed(expiry, BEARER_HOLD_MS - sinceBearer);
        }
    }

    private static int dataNetworkType(Object client, int slot) {
        int subId = MainHook.getSubIdForSlotId(slot);
        if (subId < 0) return -1;
        try {
            Object repo = Reflect.callMethod(Reflect.getObjectField(client, "mMobileRepos"), "getRepoForSubId", subId);
            Object serviceState = Reflect.callMethod(Reflect.callMethod(repo, "getServiceStateMiui"), "getValue");
            return serviceState == null ? -1 : (Integer) Reflect.callMethod(serviceState, "getDataNetworkType");
        } catch (Throwable t) {
            HookEnv.log(TAG + ": reading the data network type failed: " + t);
            return -1;
        }
    }

    /** MediaTek and Xiaomi XRING builds trust ServiceState.getNrState() instead of the Qualcomm client. */
    private static void hookNrStatePath(ClassLoader loader) {
        Class<?> configs = Reflect.findClassIfExists("com.miui.utils.configs.MiuiConfigs", loader);
        Class<?> fiveGConnected = Reflect.findClassIfExists(FIVE_G_CONNECTED_CLASS, loader);
        if (configs == null || fiveGConnected == null) return;
        if (!Reflect.getStaticBooleanField(configs, "IS_MEDIATEK")
                && !Reflect.getStaticBooleanField(configs, "IS_XRING")) {
            return;
        }
        HookEnv.findAndHookMethod(fiveGConnected, "invokeSuspend", Object.class, new MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                sEvaluatingFiveG.set(Boolean.TRUE);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                sEvaluatingFiveG.remove();
            }
        });
        HookEnv.findAndHookMethod(ServiceState.class, "getNrState", new MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (sEvaluatingFiveG.get() != null && ignoreUli()
                        && Integer.valueOf(NR_STATE_NOT_RESTRICTED).equals(param.getResult())) {
                    param.setResult(NR_STATE_RESTRICTED);
                }
            }
        });
    }

    static void onConfigChanged() {
        boolean ignoreUli = ignoreUli();
        if (ignoreUli == sIgnoreUliApplied) return;
        sIgnoreUliApplied = ignoreUli;
        recomputeIcons();
    }

    private static void recomputeIcons() {
        WeakReference<Object> ref = sClient;
        final Object client = ref != null ? ref.get() : null;
        if (client == null) return;
        ((Handler) Reflect.getObjectField(client, "mHandler")).post(new Runnable() {
            @Override
            public void run() {
                Reflect.callMethod(client, "update5GIcon");
            }
        });
    }
}
