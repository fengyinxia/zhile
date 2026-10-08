package com.shatyuka.zhiliao.hooks;

import android.view.View;
import com.shatyuka.zhiliao.Compat;
import com.shatyuka.zhiliao.Helper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class NextAnswer implements IHook {
    private final List<Class<?>> buttons = new ArrayList<>();
    private Method setup;
    private Field answerButton;
    public String getName() { return "移除下一个回答按钮"; }
    public void init(ClassLoader loader) throws Throwable {
        for (String name : new String[]{"com.zhihu.android.answer.widget.NextAnswerAnimationView",
                "com.zhihu.android.mix.widget.NextContentAnimationView",
                "com.zhihu.android.mixshortcontainer.function.next.NextContentAnimationView",
                "com.zhihu.android.feature.short_container_feature.ui.widget.next.PopupMenuNextButton"}) {
            buttons.add(loader.loadClass(name));
        }
        setup = Compat.method(Helper.AnswerPagerFragment, "setupNextAnswerBtn", void.class);
        answerButton = Compat.field(Helper.AnswerPagerFragment, "mNextAnimationView");
    }
    private boolean target(Object value) {
        for (Class<?> type : buttons) if (type.isInstance(value)) return true;
        return false;
    }
    public void hook() throws Throwable {
        if (!Compat.enabled("switch_nextanswer", false)) return;
        // Keep initialization and valid view references; hiding must not null Kotlin lateinit fields.
        Compat.hook(setup, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                Object button = answerButton.get(p.thisObject);
                if (button instanceof View) ((View) button).setVisibility(View.GONE);
            }
        });
        for (Class<?> type : buttons) XposedBridge.hookAllConstructors(type, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                ((View) p.thisObject).setVisibility(View.GONE);
            }
        });
        Compat.hook(Compat.method(View.class, "setVisibility", void.class, int.class), new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (Compat.enabled("switch_nextanswer", false) && target(p.thisObject)) p.args[0] = View.GONE;
            }
        });
    }
}
