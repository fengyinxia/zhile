package com.shatyuka.zhiliao.hooks;

import com.shatyuka.zhiliao.Helper;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

public class ThirdPartyLogin implements IHook {
    static Class<?> UiConfigImpl;

    @Override
    public String getName() {
        return "第三方登录";
    }

    @Override
    public void init(ClassLoader classLoader) throws Throwable {
        UiConfigImpl = classLoader.loadClass("com.zhihu.android.account.provider.UiConfigImpl");
    }

    @Override
    public void hook() throws Throwable {
        if (Helper.prefs.getBoolean("switch_mainswitch", false) && Helper.prefs.getBoolean("switch_thirdpartylogin", false)) {
            XposedHelpers.findAndHookMethod(UiConfigImpl, "showQQ", XC_MethodReplacement.returnConstant(true));
            XposedHelpers.findAndHookMethod(UiConfigImpl, "showSina", XC_MethodReplacement.returnConstant(true));
            XposedHelpers.findAndHookMethod(UiConfigImpl, "showWeChat", XC_MethodReplacement.returnConstant(true));
        }
    }
}
