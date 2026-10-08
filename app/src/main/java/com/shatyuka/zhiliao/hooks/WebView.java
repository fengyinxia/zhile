package com.shatyuka.zhiliao.hooks;

import com.shatyuka.zhiliao.Compat;
import com.shatyuka.zhiliao.Helper;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class WebView implements IHook {
    private Method finished;
    private Class<?> answerAppView;
    public String getName() { return "WebView修改"; }
    public void init(ClassLoader loader) throws Throwable {
        answerAppView = loader.loadClass("com.zhihu.android.answer.module.content.appview.AnswerAppView");
        finished = Compat.method(Helper.WebViewClientWrapper, "onPageFinished", void.class,
                android.webkit.WebView.class, String.class);
    }
    public void hook() throws Throwable {
        if (Compat.enabled("switch_subscribe", false)) {
            if (XposedBridge.hookAllMethods(answerAppView, "canPreRender", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (Compat.enabled("switch_subscribe", false)) p.setResult(false);
                }
            }).isEmpty()) Compat.note("AnswerAppView.canPreRender not declared; native follow-button hooks retained");
        }
        XposedBridge.hookAllConstructors(android.webkit.WebView.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (Compat.enabled("switch_webview_debug", false))
                    android.webkit.WebView.setWebContentsDebuggingEnabled(true);
            }
        });
        Compat.hook(finished, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (!Helper.prefs.getBoolean("switch_mainswitch", false)) return;
                android.webkit.WebView web = (android.webkit.WebView) p.args[0];
                if (web == null) return;
                String builtin = getScript(Helper.prefs.getBoolean("switch_watermark", false),
                        Helper.prefs.getBoolean("switch_subscribe", false));
                if (!builtin.isEmpty()) web.evaluateJavascript(builtin, null);
                String custom = Helper.prefs.getString("edit_js", "");
                if (custom != null && !custom.trim().isEmpty()) web.evaluateJavascript(custom, null);
            }
        });
    }
    static String getScript(boolean watermark, boolean subscribe) {
        if (!watermark && !subscribe) return "";
        StringBuilder js = new StringBuilder("(function(){function apply(){");
        if (watermark) js.append("if(!document.getElementById('zhiliao-watermark')){var s=document.createElement('style');s.id='zhiliao-watermark';s.textContent='.App{background-image:none!important}';(document.head||document.documentElement).appendChild(s);}");
        if (subscribe) js.append("var a=document.getElementsByClassName('Toolbar-functionButtons')[0];var b=a&&a.lastElementChild;if(b&&b.querySelector('.Avatar')&&b.style.display!=='none')b.style.display='none';var c=document.getElementsByClassName('UserLine AuthorCard')[0];var d=c&&c.lastElementChild;if(d&&d.tagName==='BUTTON'&&d.style.display!=='none')d.style.display='none';");
        js.append("}apply();if(!window.__zhiliaoObserver&&window.MutationObserver){window.__zhiliaoObserver=new MutationObserver(apply);window.__zhiliaoObserver.observe(document.documentElement,{childList:true,subtree:true});}})();");
        return js.toString();
    }
}
