package com.midori.hypericons;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection helpers for SystemUI and phone internals, replacing the legacy
 * XposedHelpers calls (which API 102 modules must not use) with the same
 * lookup rules: fields and best-matching methods are searched up the class
 * hierarchy, made accessible and cached.
 */
public final class Reflect {
    private Reflect() {}

    private static final Map<String, Field> sFieldCache = new ConcurrentHashMap<String, Field>();
    private static final Map<String, Method> sMethodCache = new ConcurrentHashMap<String, Method>();
    private static final Map<String, Constructor<?>> sCtorCache = new ConcurrentHashMap<String, Constructor<?>>();
    private static final WeakHashMap<Object, HashMap<String, Object>> sExtraFields =
            new WeakHashMap<Object, HashMap<String, Object>>();

    // ---- Classes ----

    /** Loads a class without initializing it; a null loader means the boot class path. */
    public static Class<?> findClass(String name, ClassLoader loader) {
        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(name);
        }
    }

    public static Class<?> findClassIfExists(String name, ClassLoader loader) {
        try {
            return Class.forName(name, false, loader);
        } catch (Throwable t) {
            return null;
        }
    }

    // ---- Fields ----

    public static Field findField(Class<?> clazz, String name) {
        String key = clazz.getName() + '#' + name + '@' + System.identityHashCode(clazz);
        Field cached = sFieldCache.get(key);
        if (cached != null) return cached;
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                sFieldCache.put(key, f);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        throw new NoSuchFieldError(clazz.getName() + "#" + name);
    }

    public static Object getObjectField(Object obj, String name) {
        try {
            return findField(obj.getClass(), name).get(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setObjectField(Object obj, String name, Object value) {
        try {
            findField(obj.getClass(), name).set(obj, value);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static int getIntField(Object obj, String name) {
        try {
            return findField(obj.getClass(), name).getInt(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setIntField(Object obj, String name, int value) {
        try {
            findField(obj.getClass(), name).setInt(obj, value);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static boolean getBooleanField(Object obj, String name) {
        try {
            return findField(obj.getClass(), name).getBoolean(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static void setBooleanField(Object obj, String name, boolean value) {
        try {
            findField(obj.getClass(), name).setBoolean(obj, value);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static Object getStaticObjectField(Class<?> clazz, String name) {
        try {
            return findField(clazz, name).get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    public static boolean getStaticBooleanField(Class<?> clazz, String name) {
        try {
            return findField(clazz, name).getBoolean(null);
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    // ---- Methods and constructors ----

    public static Object callMethod(Object obj, String name, Object... args) {
        return invoke(findMethodBestMatch(obj.getClass(), name, args), obj, args);
    }

    public static Object callStaticMethod(Class<?> clazz, String name, Object... args) {
        return invoke(findMethodBestMatch(clazz, name, args), null, args);
    }

    public static Object newInstance(Class<?> clazz, Object... args) {
        Constructor<?> ctor = findConstructorBestMatch(clazz, args);
        try {
            return ctor.newInstance(args);
        } catch (InvocationTargetException e) {
            throw sneaky(e.getCause());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Exact declared method of {@code clazz} (superclasses are not searched). */
    public static Method findMethodExact(Class<?> clazz, String name, Class<?>... types) {
        try {
            Method m = clazz.getDeclaredMethod(name, types);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodError(clazz.getName() + "#" + name);
        }
    }

    public static Method findMethodBestMatch(Class<?> clazz, String name, Object... args) {
        StringBuilder key = new StringBuilder(clazz.getName()).append('@')
                .append(System.identityHashCode(clazz)).append('#').append(name);
        for (Object arg : args) key.append(',').append(arg == null ? "null" : arg.getClass().getName());
        String cacheKey = key.toString();
        Method cached = sMethodCache.get(cacheKey);
        if (cached != null) return cached;

        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            Method fallback = null;
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(name) || !matches(m.getParameterTypes(), args)) continue;
                if (exactly(m.getParameterTypes(), args)) {
                    fallback = m;
                    break;
                }
                if (fallback == null) fallback = m;
            }
            if (fallback != null) {
                fallback.setAccessible(true);
                sMethodCache.put(cacheKey, fallback);
                return fallback;
            }
        }
        for (Class<?> itf : clazz.getInterfaces()) {
            for (Method m : itf.getMethods()) {
                if (m.getName().equals(name) && matches(m.getParameterTypes(), args)) {
                    m.setAccessible(true);
                    sMethodCache.put(cacheKey, m);
                    return m;
                }
            }
        }
        throw new NoSuchMethodError(clazz.getName() + "#" + name + " with " + args.length + " args");
    }

    private static Constructor<?> findConstructorBestMatch(Class<?> clazz, Object... args) {
        StringBuilder key = new StringBuilder(clazz.getName()).append('@').append(System.identityHashCode(clazz));
        for (Object arg : args) key.append(',').append(arg == null ? "null" : arg.getClass().getName());
        String cacheKey = key.toString();
        Constructor<?> cached = sCtorCache.get(cacheKey);
        if (cached != null) return cached;
        for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
            if (matches(ctor.getParameterTypes(), args)) {
                ctor.setAccessible(true);
                sCtorCache.put(cacheKey, ctor);
                return ctor;
            }
        }
        throw new NoSuchMethodError(clazz.getName() + "#<init> with " + args.length + " args");
    }

    private static Object invoke(Method m, Object target, Object[] args) {
        try {
            return m.invoke(Modifier.isStatic(m.getModifiers()) ? null : target, args);
        } catch (InvocationTargetException e) {
            throw sneaky(e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    private static boolean matches(Class<?>[] types, Object[] args) {
        if (types.length != args.length) return false;
        for (int i = 0; i < types.length; i++) {
            if (!isAssignable(types[i], args[i])) return false;
        }
        return true;
    }

    private static boolean exactly(Class<?>[] types, Object[] args) {
        for (int i = 0; i < types.length; i++) {
            if (args[i] == null) continue;
            Class<?> t = types[i].isPrimitive() ? box(types[i]) : types[i];
            if (t != args[i].getClass()) return false;
        }
        return true;
    }

    private static boolean isAssignable(Class<?> type, Object arg) {
        if (arg == null) return !type.isPrimitive();
        if (!type.isPrimitive()) return type.isInstance(arg);
        Class<?> a = arg.getClass();
        if (box(type) == a) return true;
        // Primitive widening, as Method.invoke performs after unboxing.
        if (type == long.class) return a == Integer.class || a == Short.class || a == Byte.class || a == Character.class;
        if (type == int.class) return a == Short.class || a == Byte.class || a == Character.class;
        if (type == float.class || type == double.class) {
            return a == Integer.class || a == Long.class || a == Short.class || a == Byte.class
                    || a == Character.class || (type == double.class && a == Float.class);
        }
        if (type == short.class) return a == Byte.class;
        return false;
    }

    private static Class<?> box(Class<?> primitive) {
        if (primitive == boolean.class) return Boolean.class;
        if (primitive == int.class) return Integer.class;
        if (primitive == long.class) return Long.class;
        if (primitive == float.class) return Float.class;
        if (primitive == double.class) return Double.class;
        if (primitive == short.class) return Short.class;
        if (primitive == byte.class) return Byte.class;
        if (primitive == char.class) return Character.class;
        return Void.class;
    }

    private static RuntimeException sneaky(Throwable t) {
        Reflect.<RuntimeException>throwUnchecked(t);
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable t) throws T {
        throw (T) t;
    }

    // ---- Extra per-instance fields ----

    public static Object getAdditionalInstanceField(Object obj, String key) {
        synchronized (sExtraFields) {
            HashMap<String, Object> fields = sExtraFields.get(obj);
            return fields != null ? fields.get(key) : null;
        }
    }

    public static Object setAdditionalInstanceField(Object obj, String key, Object value) {
        synchronized (sExtraFields) {
            HashMap<String, Object> fields = sExtraFields.get(obj);
            if (fields == null) {
                fields = new HashMap<String, Object>();
                sExtraFields.put(obj, fields);
            }
            return fields.put(key, value);
        }
    }
}
