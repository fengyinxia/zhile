package com.shatyuka.zhiliao.hooks;

import android.view.View;
import android.view.ViewGroup;
import com.shatyuka.zhiliao.Compat;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class RedDot implements IHook {
    private Class<?> notiMsgModel, viewModel;
    private Method navCount, iconCount, navBadge, dotCount, avatarTip, communityCount;

    public String getName() { return "不显示小红点"; }

    public void init(ClassLoader loader) throws Throwable {
        navCount = Compat.method(loader.loadClass("com.zhihu.android.bottomnav.core.BottomNavMenuItemView"), "a", void.class, int.class);
        iconCount = Compat.method(loader.loadClass("com.zhihu.android.bottomnav.core.BottomNavMenuItemViewForIconOnly"), "a", void.class, int.class);
        navBadge = Compat.method(loader.loadClass("com.zhihu.android.bottomnav.core.BaseBottomNavMenuItemView"), "a", void.class,
                loader.loadClass("com.zhihu.android.bottomnav.api.model.NavBadge"));
        communityCount = Compat.method(loader.loadClass("com.zhihu.android.community_base.view.icon.IconWithDotAndCountView"), "a", void.class,
                int.class, boolean.class, int.class);
        dotCount = Compat.method(loader.loadClass("com.zhihu.android.notification.widget.CountDotView"), "a", void.class, int.class, boolean.class);
        avatarTip = Compat.method(loader.loadClass("com.zhihu.android.recentlyviewed.ui.viewholder.BaseFeedFollowAvatarViewHolder"), "a", void.class,
                View.class, boolean.class);
        notiMsgModel = loader.loadClass("com.zhihu.android.notification.model.viewmodel.NotiMsgModel");
        viewModel = loader.loadClass("com.zhihu.android.app.feed.ui.fragment.help.tabhelp.model.ViewModel");
    }

    public void hook() {
        if (!Compat.enabled("switch_reddot", false)) return;
        XC_MethodReplacement hide = XC_MethodReplacement.returnConstant(null);
        for (Method m : new Method[]{navCount, iconCount, navBadge, avatarTip, communityCount}) {
            XposedBridge.hookMethod(m, hide);
        }
        XposedHelpers.findAndHookMethod(notiMsgModel, "getUnreadCount", XC_MethodReplacement.returnConstant(0));
        XposedBridge.hookMethod(dotCount, new XC_MethodReplacement() {
            protected Object replaceHookedMethod(MethodHookParam p) {
                ((View) p.thisObject).setVisibility(View.GONE);
                return null;
            }
        });
        XposedHelpers.findAndHookConstructor(viewModel, View.class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam p) {
                if (p.args[0] instanceof ViewGroup) {
                    ViewGroup view = (ViewGroup) p.args[0];
                    if (view.getChildCount() == 2) view.setVisibility(View.GONE);
                }
            }
        });
    }
}
