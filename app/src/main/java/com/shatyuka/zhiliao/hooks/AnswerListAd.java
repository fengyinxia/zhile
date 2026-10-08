package com.shatyuka.zhiliao.hooks;

import android.content.Context;
import com.shatyuka.zhiliao.AdModels;
import com.shatyuka.zhiliao.Compat;
import com.shatyuka.zhiliao.Helper;
import java.lang.reflect.Method;
import java.util.ArrayList;
import de.robv.android.xposed.XC_MethodHook;

public class AnswerListAd implements IHook {
    private Method resolve, insert;
    public String getName() { return "去回答列表广告"; }
    public void init(ClassLoader loader) throws Throwable {
        AdModels.install(loader);
        Class<?> ad = loader.loadClass("com.zhihu.android.adbase.model.AnswerListAd");
        resolve = Compat.method(Helper.MorphAdHelper, "resolveAnswerAdParam", boolean.class,
                Context.class, ad, Boolean.class);
        insert = Compat.method(loader.loadClass("com.zhihu.android.question.api.model.AnswerListWrapper"),
                "insertAdBrandToList", void.class, ArrayList.class, boolean.class);
    }
    public void hook() {
        Compat.hook(resolve, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (Compat.enabled("switch_answerlistad", true)) p.setResult(false);
            }
        });
        Compat.hook(insert, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (Compat.enabled("switch_answerlistad", true)) p.setResult(null);
            }
        });
    }
}
