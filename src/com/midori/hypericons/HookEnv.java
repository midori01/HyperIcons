package com.midori.hypericons;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.libxposed.api.XposedInterface;

/**
 * Bridges the hooks to the libxposed API 102 module: logging, running
 * {@link MethodHook} callbacks inside interceptors, and recording which hook
 * points were installed so the app can show what works on this ROM.
 */
public final class HookEnv {
    private HookEnv() {}

    private static final String LOG_TAG = "HyperIcons";

    private static volatile XposedInterface sModule;

    /** Hook point -> null when installed, otherwise the failure reason. Insertion ordered. */
    private static final Map<String, String> sHookStatus = new LinkedHashMap<String, String>();

    static void attach(XposedInterface module) {
        sModule = module;
    }

    public static XposedInterface module() {
        return sModule;
    }

    public static void log(String msg) {
        XposedInterface module = sModule;
        if (module != null) {
            module.log(Log.INFO, LOG_TAG, msg);
        } else {
            Log.i(LOG_TAG, msg);
        }
    }

    // ---- Hook status ----

    public static void recordHook(String point, Throwable failure) {
        String reason = null;
        if (failure != null) {
            reason = failure.getClass().getSimpleName();
            if (failure.getMessage() != null) reason += ": " + failure.getMessage();
        }
        synchronized (sHookStatus) {
            sHookStatus.put(point, reason);
        }
    }

    public static void recordMissing(String point, String what) {
        synchronized (sHookStatus) {
            sHookStatus.put(point, "Not found: " + what);
        }
    }

    /** Snapshot of the hook status: point -> null (ok) or failure reason. */
    public static Map<String, String> hookStatus() {
        synchronized (sHookStatus) {
            return new LinkedHashMap<String, String>(sHookStatus);
        }
    }

    private static String pointName(Executable e) {
        String cls = e.getDeclaringClass().getName();
        int dot = cls.lastIndexOf('.');
        String simple = dot >= 0 ? cls.substring(dot + 1) : cls;
        return simple + (e instanceof Constructor ? "#<init>" : "#" + e.getName());
    }

    // ---- Feature groups ----

    public interface Section {
        void run() throws Throwable;
    }

    private static final Map<String, String> sGroupStatus = new LinkedHashMap<String, String>();
    private static int sInstalledHooks = 0;

    /**
     * Runs one feature's hook setup in isolation. The feature counts as working
     * when it installed at least one hook; a failure no longer stops the
     * features after it.
     */
    public static void group(String name, Section section) {
        int before;
        synchronized (sHookStatus) {
            before = sInstalledHooks;
        }
        String reason;
        try {
            section.run();
            synchronized (sHookStatus) {
                reason = sInstalledHooks > before ? null : "No hook point found on this ROM";
            }
        } catch (Throwable t) {
            reason = t.getClass().getSimpleName() + (t.getMessage() != null ? ": " + t.getMessage() : "");
            log("Feature " + name + " failed: " + t);
        }
        synchronized (sHookStatus) {
            sGroupStatus.put(name, reason);
        }
    }

    /** Signature permission the module's own broadcasts are sent with; other apps cannot send them. */
    public static final String PERMISSION_RELOAD = "com.midori.hypericons.permission.RELOAD";
    /** Asks the hooked processes to send a fresh hook report without reloading anything. */
    public static final String ACTION_REQUEST_REPORT = "com.midori.hypericons.ACTION_REQUEST_REPORT";

    /** Registers a receiver for the module's own broadcasts, which only the app can send. */
    public static void registerTrustedReceiver(Context context, android.content.BroadcastReceiver receiver,
                                               android.content.IntentFilter filter) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, PERMISSION_RELOAD, null, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(receiver, filter, PERMISSION_RELOAD, null);
        }
    }

    public static final String METHOD_REPORT_HOOKS = "reportHooks";
    public static final String EXTRA_PROCESS = "process";
    public static final String EXTRA_REPORT = "report";

    /**
     * Sends the hook status of this process to the app's provider:
     * a header line ("framework\tversion\tapi\tmoduleVersion\ttime"), then one
     * "G" line per feature group and one "P" line per hook point, each as
     * "kind\tname\treason" with an empty reason when it works.
     */
    public static void report(Context context, String process) {
        if (context == null) return;
        StringBuilder sb = new StringBuilder();
        XposedInterface module = sModule;
        if (module != null) {
            sb.append(module.getFrameworkName()).append('\t')
              .append(module.getFrameworkVersion()).append('\t')
              .append(module.getApiVersion());
        } else {
            sb.append("?\t?\t0");
        }
        sb.append('\t').append(MainHook.VERSION).append('\t').append(System.currentTimeMillis()).append('\n');
        synchronized (sHookStatus) {
            for (Map.Entry<String, String> e : sGroupStatus.entrySet()) {
                sb.append("G\t").append(e.getKey()).append('\t').append(clean(e.getValue())).append('\n');
            }
            for (Map.Entry<String, String> e : sHookStatus.entrySet()) {
                sb.append("P\t").append(e.getKey()).append('\t').append(clean(e.getValue())).append('\n');
            }
        }
        try {
            Bundle extras = new Bundle();
            extras.putString(EXTRA_PROCESS, process);
            extras.putString(EXTRA_REPORT, sb.toString());
            context.getContentResolver().call(
                    Uri.parse("content://com.midori.hypericons.provider/config"), METHOD_REPORT_HOOKS, null, extras);
        } catch (Throwable t) {
            log("Hook report for " + process + " not delivered: " + t);
        }
    }

    private static String clean(String s) {
        return s == null ? "" : s.replace('\t', ' ').replace('\n', ' ');
    }

    // ---- Hooking ----

    public static XposedInterface.HookHandle hook(final Executable target, final MethodHook callback) {
        String point = pointName(target);
        try {
            XposedInterface module = sModule;
            if (module == null) throw new IllegalStateException("module not attached");
            XposedInterface.HookHandle handle = module.hook(target).intercept(new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    return run(target, callback, chain);
                }
            });
            recordHook(point, null);
            synchronized (sHookStatus) {
                sInstalledHooks++;
            }
            return handle;
        } catch (Throwable t) {
            recordHook(point, t);
            throw t;
        }
    }

    private static Object run(Executable target, MethodHook callback, XposedInterface.Chain chain) throws Throwable {
        MethodHook.MethodHookParam param = new MethodHook.MethodHookParam(
                target, chain.getThisObject(), chain.getArgs().toArray());
        try {
            callback.beforeHookedMethod(param);
        } catch (Throwable t) {
            log("Error in before hook of " + pointName(target) + ": " + t);
            param.returnEarly = false;
            param.setOutcome(null, null);
        }

        if (!param.returnEarly) {
            try {
                param.setOutcome(chain.proceed(param.args), null);
            } catch (Throwable t) {
                param.setOutcome(null, t);
            }
        }

        Object lastResult = param.getResult();
        Throwable lastThrowable = param.getThrowable();
        try {
            callback.afterHookedMethod(param);
        } catch (Throwable t) {
            log("Error in after hook of " + pointName(target) + ": " + t);
            param.setOutcome(lastResult, lastThrowable);
        }

        if (param.getThrowable() != null) throw param.getThrowable();
        return param.getResult();
    }

    /**
     * Hooks the method declared by {@code clazz} itself. The trailing argument is the
     * {@link MethodHook}; the others are parameter types as classes or class names.
     */
    public static XposedInterface.HookHandle findAndHookMethod(Class<?> clazz, String name, Object... typesAndCallback) {
        if (typesAndCallback.length == 0 || !(typesAndCallback[typesAndCallback.length - 1] instanceof MethodHook)) {
            throw new IllegalArgumentException("no callback for " + clazz.getName() + "#" + name);
        }
        MethodHook callback = (MethodHook) typesAndCallback[typesAndCallback.length - 1];
        Class<?>[] types = new Class<?>[typesAndCallback.length - 1];
        Method method;
        try {
            for (int i = 0; i < types.length; i++) {
                Object t = typesAndCallback[i];
                types[i] = t instanceof Class ? (Class<?>) t : Reflect.findClass((String) t, clazz.getClassLoader());
            }
            method = Reflect.findMethodExact(clazz, name, types);
        } catch (Throwable t) {
            String cls = clazz.getName();
            recordHook(cls.substring(cls.lastIndexOf('.') + 1) + "#" + name, t);
            throw t;
        }
        return hook(method, callback);
    }

    /** Hooks every non-abstract method named {@code name} declared by {@code clazz}. */
    public static List<XposedInterface.HookHandle> hookAllMethods(Class<?> clazz, String name, MethodHook callback) {
        List<XposedInterface.HookHandle> handles = new ArrayList<XposedInterface.HookHandle>();
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name) && !Modifier.isAbstract(m.getModifiers())) {
                handles.add(hook(m, callback));
            }
        }
        return handles;
    }

    public static List<XposedInterface.HookHandle> hookAllConstructors(Class<?> clazz, MethodHook callback) {
        List<XposedInterface.HookHandle> handles = new ArrayList<XposedInterface.HookHandle>();
        for (Constructor<?> c : clazz.getDeclaredConstructors()) {
            handles.add(hook(c, callback));
        }
        return handles;
    }
}
