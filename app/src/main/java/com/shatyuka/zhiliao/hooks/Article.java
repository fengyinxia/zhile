package com.shatyuka.zhiliao.hooks;

import com.shatyuka.zhiliao.Compat;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;

public class Article implements IHook {
    private Class<?> adapter, pager;
    private Method count, type;
    private Field fragment;
    public String getName() { return "精简文章页面"; }
    public void init(ClassLoader loader) throws Throwable {
        adapter = loader.loadClass("com.zhihu.android.mix.adapter.a");
        pager = loader.loadClass("com.zhihu.android.mix.fragment.ContentMixPagerFragment");
        count = Compat.method(adapter, "getItemCount", int.class);
        type = Compat.method(pager, "p", String.class);
        fragment = Compat.field(adapter, "g");
    }
    public void hook() {
        Compat.hook(count, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                if (!Compat.enabled("switch_article", false) || !adapter.isInstance(p.thisObject)
                        || p.hasThrowable()) return;
                Object owner = fragment.get(p.thisObject);
                // t is now the content ID; getType() reads s. Use value equality, not String identity.
                if (pager.isInstance(owner) && "article".equals(type.invoke(owner))
                        && p.getResult() instanceof Integer && (Integer) p.getResult() > 1) p.setResult(1);
            }
        });
    }
}
