package com.shatyuka.zhiliao.hooks;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import com.shatyuka.zhiliao.Compat;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class LaunchAd implements IHook {
    private Method requestAd, hotLaunch, deepLink, isShow;
    public String getName() { return "去启动页广告"; }
    public void init(ClassLoader loader) throws Throwable {
        Class<?> callback = loader.loadClass("com.zhihu.android.launch.api.a.b");
        requestAd = Compat.method(loader.loadClass("com.zhihu.android.launch.impl.LaunchImpl"),
                "requestAd", void.class, String.class, callback, View.class);
        hotLaunch = Compat.method(loader.loadClass("com.zhihu.android.sdk.launchad.f"),
                "a", boolean.class, Context.class);
        Class<?> helper = loader.loadClass("com.zhihu.android.app.util.dk");
        deepLink = Compat.method(helper, "showLaunchAd", void.class, String.class);
        isShow = Compat.method(helper, "isShowLaunchAd", boolean.class);
    }
    public void hook() {
        Compat.hook(requestAd, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (!Compat.enabled("switch_launchad", true) || p.args[1] == null) return;
                final Object callback = p.args[1];
                boolean queued = new Handler(Looper.getMainLooper()).post(() -> {
                    try { XposedHelpers.callMethod(callback, "b"); }
                    catch (Throwable e) { Compat.note("launch onPop callback failed: " + e); }
                });
                // Do not suppress the state machine unless we scheduled its normal exit.
                if (queued) p.setResult(null);
            }
        });
        XC_MethodHook noLaunch = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (Compat.enabled("switch_launchad", true)) p.setResult(false);
            }
        };
        Compat.hook(hotLaunch, noLaunch);
        Compat.hook(isShow, noLaunch);
        Compat.hook(deepLink, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (Compat.enabled("switch_launchad", true)) p.setResult(null);
            }
        });
    }
}
