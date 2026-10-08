package com.shatyuka.zhiliao.hooks;

import com.shatyuka.zhiliao.Compat;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;

public class FullScreen implements IHook {
    private Method enter;
    public String getName() { return "禁止进入全屏模式"; }
    public void init(ClassLoader loader) throws Throwable {
        // c.d() enters clear-screen mode; c.e() exits and remains intact.
        enter = Compat.method(loader.loadClass("com.zhihu.android.feature.short_container_feature.ui.widget.toolbar.clearscreen.c"),
                "d", void.class);
    }
    public void hook() {
        Compat.hook(enter, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (Compat.enabled("switch_fullscreen", false)) p.setResult(null);
            }
        });
    }
}
