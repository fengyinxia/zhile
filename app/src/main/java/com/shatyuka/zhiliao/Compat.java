package com.shatyuka.zhiliao;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Exact mappings for the extracted arm64 11.10.0 (41012) APK. */
public final class Compat {
    private Compat() {}
    public static boolean enabled(String key, boolean defaultValue) {
        return Helper.prefs.getBoolean("switch_mainswitch", false)
                && Helper.prefs.getBoolean(key, defaultValue);
    }
    public static Method method(Class<?> cls, String name, Class<?> result, Class<?>... params)
            throws NoSuchMethodException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getReturnType() == result
                        && Arrays.equals(m.getParameterTypes(), params)) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        throw new NoSuchMethodException(cls.getName() + "." + name + Arrays.toString(params));
    }
    public static Field field(Class<?> cls, String name) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(cls.getName() + "." + name);
    }
    public static void hook(Method m, XC_MethodHook callback) {
        XposedBridge.hookMethod(m, callback);
        XposedBridge.log("[ZhiliaoCompat] installed " + m.toGenericString());
    }
    public static void note(String text) { XposedBridge.log("[ZhiliaoCompat] " + text); }
}
