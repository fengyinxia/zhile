package com.shatyuka.zhiliao;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.widget.Toast;
import java.io.File;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Official-APK research variant; no native signature/repack compatibility layer. */
public class MainHook implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private boolean installed;
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!Helper.hookPackage.equals(lp.packageName) || !Helper.hookPackage.equals(lp.processName)) return;
        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class,
                new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                        if (installed || !(p.args[0] instanceof Application)) return;
                        Application app = (Application) p.args[0];
                        if (!Helper.hookPackage.equals(app.getPackageName())) return;
                        Helper.context = app.getApplicationContext();
                        if (!Helper.checkSignature(Helper.context)) {
                            Compat.note("Official-APK variant: host signature mismatch; skipping hooks");
                            return;
                        }
                        if (!Helper.init(app.getClassLoader())) {
                            Helper.toast("初始化失败或版本不匹配；仅适配 11.10.0 (41012)", Toast.LENGTH_SHORT);
                            return;
                        }
                        installed = true;
                        XposedHelpers.findAndHookMethod(File.class, "exists", new XC_MethodHook() {
                            @Override protected void beforeHookedMethod(MethodHookParam file) {
                                if (".allowXposed".equals(((File) file.thisObject).getName())) file.setResult(true);
                            }
                        });
                        Hooks.init(app.getClassLoader());
                        if (!Helper.prefs.getBoolean("switch_mainswitch", false))
                            Helper.toast("已加载静态适配版，请到知乎设置页开启知了；未经真机验证", Toast.LENGTH_LONG);
                    }
                });
    }
    @Override public void initZygote(StartupParam param) throws Throwable {
        Helper.modRes = Helper.getModuleRes(param.modulePath);
    }
}
