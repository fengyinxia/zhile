package com.shatyuka.zhiliao.hooks;

import android.view.View;
import android.view.ViewGroup;
import com.shatyuka.zhiliao.Compat;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class VIPBanner implements IHook {
    private Class<?> banner;
    private final List<Method> binds = new ArrayList<>();
    public String getName() { return "隐藏会员卡片"; }
    public void init(ClassLoader loader) throws Throwable {
        banner = loader.loadClass("com.zhihu.android.premium.view.VipEntranceView");
        for (Method m : banner.getDeclaredMethods()) if (m.getName().equals("setData")) binds.add(m);
        if (binds.isEmpty()) throw new NoSuchMethodException("VipEntranceView.setData");
    }
    private void hide(Object value) {
        if (!Compat.enabled("switch_vipbanner", false)) return;
        View view = (View) value;
        view.setVisibility(View.GONE);
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp != null) { lp.height = 0; view.setLayoutParams(lp); }
    }
    public void hook() {
        XposedBridge.hookAllConstructors(banner, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) { hide(p.thisObject); }
        });
        for (Method bind : binds) Compat.hook(bind, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) { hide(p.thisObject); }
        });
        // Keep host view initialization and MoreVipData legality/membership state unchanged.
    }
}
