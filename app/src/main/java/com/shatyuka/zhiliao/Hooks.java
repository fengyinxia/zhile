package com.shatyuka.zhiliao;

import android.widget.Toast;

import com.shatyuka.zhiliao.hooks.AnswerAd;
import com.shatyuka.zhiliao.hooks.AnswerListAd;
import com.shatyuka.zhiliao.hooks.Article;
import com.shatyuka.zhiliao.hooks.AutoRefresh;
import com.shatyuka.zhiliao.hooks.Cleaner;
import com.shatyuka.zhiliao.hooks.ColorMode;
import com.shatyuka.zhiliao.hooks.CommentAd;
import com.shatyuka.zhiliao.hooks.CustomFilter;
import com.shatyuka.zhiliao.hooks.ExternLink;
import com.shatyuka.zhiliao.hooks.FeedAd;
import com.shatyuka.zhiliao.hooks.FeedTopHotBanner;
import com.shatyuka.zhiliao.hooks.FollowButton;
import com.shatyuka.zhiliao.hooks.HeadZoneBanner;
import com.shatyuka.zhiliao.hooks.HotBanner;
import com.shatyuka.zhiliao.hooks.IHook;
import com.shatyuka.zhiliao.hooks.LaunchAd;
import com.shatyuka.zhiliao.hooks.LiveButton;
import com.shatyuka.zhiliao.hooks.MineHybridView;
import com.shatyuka.zhiliao.hooks.NavButton;
import com.shatyuka.zhiliao.hooks.NavRes;
import com.shatyuka.zhiliao.hooks.NextAnswer;
import com.shatyuka.zhiliao.hooks.FullScreen;
import com.shatyuka.zhiliao.hooks.RedDot;
import com.shatyuka.zhiliao.hooks.SearchAd;
import com.shatyuka.zhiliao.hooks.ShareAd;
import com.shatyuka.zhiliao.hooks.StatusBar;
import com.shatyuka.zhiliao.hooks.Tag;
import com.shatyuka.zhiliao.hooks.ThirdPartyLogin;
import com.shatyuka.zhiliao.hooks.VIPBanner;
import com.shatyuka.zhiliao.hooks.WebView;
import com.shatyuka.zhiliao.hooks.ZhihuPreference;

import de.robv.android.xposed.XposedBridge;

public class Hooks {
    static final IHook[] hooks = {
            new ZhihuPreference(),
            new LaunchAd(),
            new CustomFilter(),
            new FeedAd(),
            new AnswerListAd(),
            new CommentAd(),
            new AnswerAd(),
            new ShareAd(),
            new LiveButton(),
            new NextAnswer(),
            new RedDot(),
            new ExternLink(),
            new VIPBanner(),
            new NavButton(),
            new HotBanner(),
            new ColorMode(),
            new Article(),
            new Tag(),
            new SearchAd(),
            new StatusBar(),
            new ThirdPartyLogin(),
            new NavRes(),
            new WebView(),
            new Cleaner(),
            new FeedTopHotBanner(),
            new HeadZoneBanner(),
            new MineHybridView(),
            new FollowButton(),
            new FullScreen(),
            new AutoRefresh(),
    };

    public static void init(final ClassLoader classLoader) {
        int initialized = 0;
        for (IHook hook : hooks) {
            try {
                hook.init(classLoader);
                hook.hook();
                initialized++;
                Compat.note(hook.getClass().getSimpleName() + ": initialized (runtime effect NOT verified)");
            } catch (Throwable e) {
                if (!Helper.prefs.getBoolean("switch_hidetoast", false))
                    Helper.toast(hook.getName() + "功能加载失败，可能不支持当前版本知乎: " + Helper.packageInfo.versionName, Toast.LENGTH_LONG);
                Compat.note(hook.getClass().getSimpleName() + ": FAILED " + e);
                XposedBridge.log(e);
            }
        }
        Compat.note("Initialization returned for " + initialized + "/" + hooks.length
                + " features; this is not proof of ad removal or UI compatibility");
    }
}
