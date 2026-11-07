package com.midori.hypericons;

/** The package being hooked and its class loader. */
public final class LoadPackageParam {
    public final String packageName;
    public final ClassLoader classLoader;
    /** The package's Application class from its manifest, or null if unknown. */
    public final String appClassName;

    LoadPackageParam(String packageName, ClassLoader classLoader, String appClassName) {
        this.packageName = packageName;
        this.classLoader = classLoader;
        this.appClassName = appClassName;
    }
}
