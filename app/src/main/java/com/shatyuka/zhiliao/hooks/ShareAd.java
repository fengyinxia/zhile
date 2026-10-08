package com.shatyuka.zhiliao.hooks;

import android.content.Context;
import android.view.View;
import com.shatyuka.zhiliao.Compat;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;

public class ShareAd implements IHook {
    private Method fetch, show;
    public String getName() { return "去分享广告"; }
    public void init(ClassLoader loader) throws Throwable {
        fetch = Compat.method(loader.loadClass("com.zhihu.android.app.share.ShareAdFetcherImpl"),
                "fetchAd", void.class, Context.class, loader.loadClass("com.zhihu.android.library.sharecore.b"));
        show = Compat.method(loader.loadClass("com.zhihu.android.library.sharecore.fragment.ShareFragment"),
                "showShareAd", void.class, View.class);
    }
    public void hook() {
        XC_MethodHook callback = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (Compat.enabled("switch_sharead", true)) p.setResult(null);
            }
        };
        Compat.hook(fetch, callback);
        Compat.hook(show, callback);
    }
}
