package com.shatyuka.zhiliao.hooks;

import com.shatyuka.zhiliao.Compat;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;

public class AutoRefresh implements IHook {
    private Method automatic, returnTop;
    public String getName() { return "禁止自动刷新"; }
    public void init(ClassLoader loader) throws Throwable {
        automatic = Compat.method(loader.loadClass("com.zhihu.android.app.feed.util.j"),
                "a", void.class, long.class, int.class, loader.loadClass("kotlin.jvm.a.b"),
                loader.loadClass("com.zhihu.android.feed.delegate.k"));
        returnTop = Compat.method(loader.loadClass("com.zhihu.android.app.feed.ui.fragment.BaseTabChildFragment"),
                "a", int.class, boolean.class);
    }
    public void hook() {
        Compat.hook(automatic, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (Compat.enabled("switch_autorefresh", false)) p.setResult(null);
            }
        });
        Compat.hook(returnTop, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (Compat.enabled("switch_autorefresh", false) && Boolean.TRUE.equals(p.args[0])) p.setResult(1);
            }
        });
        // Do not null a coroutine/network result or drop postRefreshSucceed callbacks.
        // Pull-to-refresh and paging still execute their normal completion paths.
    }
}
