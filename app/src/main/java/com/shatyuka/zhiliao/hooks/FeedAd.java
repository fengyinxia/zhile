package com.shatyuka.zhiliao.hooks;

import android.content.Context;
import com.shatyuka.zhiliao.AdModels;
import com.shatyuka.zhiliao.Compat;
import com.shatyuka.zhiliao.Helper;
import java.util.List;
import de.robv.android.xposed.XC_MethodHook;

public class FeedAd implements IHook {
    private Class<?> paging, listAd;
    public String getName() { return "去信息流广告"; }
    public void init(ClassLoader loader) throws Throwable {
        AdModels.install(loader);
        paging = loader.loadClass("com.zhihu.android.app.ui.fragment.paging.BasePagingFragment");
        listAd = loader.loadClass("com.zhihu.android.adbase.model.ListAd");
    }
    public void hook() throws Throwable {
        Class<?> objectList = paging.getClassLoader().loadClass("com.zhihu.android.api.model.ZHObjectList");
        Compat.hook(Compat.method(paging, "postRefreshSucceed", void.class, objectList), new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                AdModels.filterModel(p.args[0]);
            }
        });
        Compat.hook(Compat.method(paging, "insertDataRangeToList", void.class, int.class, List.class), new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                if (p.args[1] instanceof List) p.args[1] = AdModels.filter((List<?>) p.args[1]);
            }
        });
        Compat.hook(Compat.method(Helper.MorphAdHelper, "resolve", boolean.class,
                Context.class, listAd, Boolean.class), new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                if (AdModels.blocked(p.args[1])) p.setResult(false);
            }
        });
        // isSlidingWindow/isFloatAdCard only change the presentation, not whether an ad exists.
    }
}
