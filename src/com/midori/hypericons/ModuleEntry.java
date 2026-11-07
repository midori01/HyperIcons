package com.midori.hypericons;

import io.github.libxposed.api.XposedModule;

/**
 * libxposed API 102 entry point (META-INF/xposed/java_init.list). Dispatches
 * SystemUI and the phone process to the existing hook classes once their
 * class loader is ready, before the Application is created.
 */
public class ModuleEntry extends XposedModule {

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        HookEnv.attach(this);
        HookEnv.log("[v" + MainHook.VERSION + "] Loaded in " + param.getProcessName()
                + " on " + getFrameworkName() + " " + getFrameworkVersion()
                + " (API " + getApiVersion() + ")");
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        String pkg = param.getPackageName();
        if (!"com.android.systemui".equals(pkg) && !"com.android.phone".equals(pkg)) return;
        HookEnv.attach(this);
        try {
            String appClass = param.getApplicationInfo() != null ? param.getApplicationInfo().className : null;
            new MainHook().handleLoadPackage(new LoadPackageParam(pkg, param.getClassLoader(), appClass));
        } catch (Throwable t) {
            HookEnv.log("Hook setup for " + pkg + " stopped: " + t);
        }
    }
}
