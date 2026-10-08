package com.shatyuka.zhiliao.hooks;

import android.view.View;
import android.view.ViewGroup;
import com.shatyuka.zhiliao.AdModels;
import com.shatyuka.zhiliao.Compat;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class CommentAd implements IHook {
    private final List<Method> binds = new ArrayList<>();
    public String getName() { return "去评论广告"; }
    public void init(ClassLoader loader) throws Throwable {
        AdModels.install(loader);
        Class<?> ad = loader.loadClass("com.zhihu.android.adbase.model.CommentListAd");
        for (String name : new String[]{"com.zhihu.android.comment.holder.BaseCommentDynamicAdViewHolder",
                "com.zhihu.android.comment.holder.CommentDynamicAdViewHolderV70"}) {
            // JADX renders this as onBindData; J1 is the actual DEX method name.
            binds.add(Compat.method(loader.loadClass(name), "a", void.class, ad));
        }
    }
    public void hook() {
        for (Method m : binds) Compat.hook(m, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (!Compat.enabled("switch_commentad", true)) return;
                View view = (View) XposedHelpers.getObjectField(p.thisObject, "itemView");
                if (view != null) {
                    ViewGroup.LayoutParams lp = view.getLayoutParams();
                    if (lp != null) { lp.height = 0; view.setLayoutParams(lp); }
                    view.setVisibility(View.GONE);
                }
                p.setResult(null);
            }
        });
    }
}
