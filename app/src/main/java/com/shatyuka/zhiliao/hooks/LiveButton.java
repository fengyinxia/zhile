package com.shatyuka.zhiliao.hooks;

import android.os.Bundle;
import android.view.View;
import com.shatyuka.zhiliao.Compat;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class LiveButton implements IHook {
    private Field feedTab;
    private Class<?> tabLayout;
    private Method created;
    private final Map<Object, Boolean> homeTabs = Collections.synchronizedMap(new WeakHashMap<>());
    public String getName() { return "移除直播入口"; }
    public void init(ClassLoader loader) throws Throwable {
        Class<?> fragment = loader.loadClass("com.zhihu.android.app.feed.ui.fragment.FeedsTabsFragment");
        feedTab = Compat.field(fragment, "mFeedTab");
        created = Compat.method(fragment, "onViewCreated", void.class, View.class, Bundle.class);
        tabLayout = loader.loadClass("com.google.android.material.tabs.TabLayout");
    }
    private void hide(Object tabs) {
        if (!Compat.enabled("switch_livebutton", false) || tabs == null) return;
        int count = (Integer) XposedHelpers.callMethod(tabs, "getTabCount");
        for (int i = 0; i < count; i++) {
            Object tab = XposedHelpers.callMethod(tabs, "getTabAt", i);
            if (tab == null) continue;
            CharSequence title = (CharSequence) XposedHelpers.callMethod(tab, "getText");
            if ("直播".contentEquals(title == null ? "" : title)) {
                View view = (View) XposedHelpers.getObjectField(tab, "view");
                if (view != null) view.setVisibility(View.GONE);
            }
        }
    }
    public void hook() {
        Compat.hook(created, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                Object tabs = feedTab.get(p.thisObject);
                if (tabs != null) { homeTabs.put(tabs, true); hide(tabs); }
            }
        });
        if (XposedBridge.hookAllMethods(tabLayout, "addTab", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (homeTabs.containsKey(p.thisObject)) hide(p.thisObject);
            }
        }).isEmpty()) throw new IllegalStateException("TabLayout.addTab unavailable");
        Compat.note("Legacy mEntranceManger removed; live option now targets only home live tabs");
    }
}
