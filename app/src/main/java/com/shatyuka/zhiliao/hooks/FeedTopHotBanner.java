package com.shatyuka.zhiliao.hooks;

import com.shatyuka.zhiliao.Compat;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;

public class FeedTopHotBanner implements IHook {
    private Class<?> top;
    private Method deserialize;
    public String getName() { return "隐藏推荐页置顶热门"; }
    public void init(ClassLoader loader) throws Throwable {
        top = loader.loadClass("com.zhihu.android.api.model.FeedTopHot");
        Class<?> decoder = loader.loadClass("com.zhihu.android.api.model.FeedTopHotAutoJacksonDeserializer");
        deserialize = Compat.method(decoder, "deserialize", Object.class,
                loader.loadClass("com.fasterxml.jackson.b.j"), loader.loadClass("com.fasterxml.jackson.databind.g"));
    }
    public void hook() {
        // deserialize is inherited in this version. Consume the JSON before removing its result.
        Compat.hook(deserialize, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (!p.hasThrowable() && top.isInstance(p.getResult()) && Compat.enabled("switch_feedtophot", false))
                    p.setResult(null);
            }
        });
    }
}
