package com.shatyuka.zhiliao.hooks;

import android.view.View;

import com.shatyuka.zhiliao.Helper;

import java.lang.reflect.Field;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class NavRes implements IHook {
    static Class<?> BottomNav;
    static Class<?> BottomNavBgViewExploreA;
    static Class<?> FeedConfigManager;

    static Field BottomNavBgViewExploreA_right;
    static Field BottomNavBgViewExploreA_center;

    @Override
    public String getName() {
        return "导航栏样式";
    }

    @Override
    public void init(ClassLoader classLoader) throws Throwable {
        BottomNav = classLoader.loadClass("com.zhihu.android.bottomnav.b");
        BottomNavBgViewExploreA = classLoader.loadClass("com.zhihu.android.bottomnav.core.explore.BottomNavBgViewExploreA");
        BottomNavBgViewExploreA_right = com.shatyuka.zhiliao.Compat.field(BottomNavBgViewExploreA, "b");
        BottomNavBgViewExploreA_center = com.shatyuka.zhiliao.Compat.field(BottomNavBgViewExploreA, "c");
        FeedConfigManager = classLoader.loadClass("com.zhihu.android.ad");
    }

    @Override
    public void hook() throws Throwable {
        if (Helper.prefs.getBoolean("switch_mainswitch", false) && Helper.prefs.getBoolean("switch_navres", false)) {
            XposedHelpers.findAndHookMethod(BottomNav, "b", XC_MethodReplacement.returnConstant(null));
            XposedBridge.hookAllConstructors(FeedConfigManager, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    param.args[3] = 0L;
                }
            });
        }

        if (Helper.prefs.getBoolean("switch_mainswitch", false) && Helper.prefs.getBoolean("switch_nipple", false)) {
            XposedHelpers.findAndHookMethod(BottomNavBgViewExploreA, "setBackground", boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View right = (View) BottomNavBgViewExploreA_right.get(param.thisObject);
                    View center = (View) BottomNavBgViewExploreA_center.get(param.thisObject);
                    if (right != null && center != null) {
                        center.setBackground(right.getBackground());
                    }
                }
            });
        }
    }
}
