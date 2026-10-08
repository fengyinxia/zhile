package com.shatyuka.zhiliao.hooks;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.shatyuka.zhiliao.Helper;
import com.shatyuka.zhiliao.Compat;
import com.shatyuka.zhiliao.R;

import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

@SuppressWarnings("deprecation")
public class ZhihuPreference implements IHook {
    final static String modulePackage = "com.shatyuka.zhiliao";

    private static Object preference_zhiliao;

    private static int version_click = 0;
    private static int author_click = 0;

    private static int settings_res_id = 0;
    private static int debug_res_id = 0;

    static Class<?> SettingsFragment;
    static Class<?> DebugFragment;
    static Class<?> Preference;
    static Class<?> SwitchPreference;
    static Class<?> OnPreferenceChangeListener;
    static Class<?> OnPreferenceClickListener;
    static Class<?> PreferenceFragmentCompat;
    static Class<?> PreferenceManager;
    static Class<?> PreferenceInflater;
    static Class<?> PageInfoType;
    static Class<?> ZHIntent;
    static Class<?> MainActivity;
    static Class<?> BasePreferenceFragment;
    static Class<?> PreferenceGroup;
    static Class<?> EditTextPreference;
    static Class<?> ListPreference;
    static Class<?> TooltipCompat;

    static Method findPreference;
    static Method setSummary;
    static Method setIcon;
    static Method setVisible;
    static Method getKey;
    static Method setChecked;
    static Method setOnPreferenceChangeListener;
    static Method setOnPreferenceClickListener;
    static Method setSharedPreferencesName;
    static Method getContext;
    static Method getText;
    static Method addPreferencesFromResource;
    static Method inflate;
    static Method setTooltipText;
    static Method startFragment;

    static Field ListPreference_mEntries;
    static Field ListPreference_mEntryValues;

    static Constructor<?> ZHIntent_ctor;

    static String getLayoutResId_MethodName;
    static String onCreate_MethodName;
    static String onPreferenceClick_MethodName;

    @Override
    public String getName() {
        return "设置页面";
    }

    @Override
    public void init(ClassLoader loader) throws Throwable {
        SettingsFragment = loader.loadClass("com.zhihu.android.app.ui.fragment.preference.SettingsFragment");
        DebugFragment = loader.loadClass("com.zhihu.android.app.ui.fragment.DebugFragment");
        Preference = loader.loadClass("androidx.preference.Preference");
        SwitchPreference = loader.loadClass("com.zhihu.android.app.ui.widget.SwitchPreference");
        OnPreferenceChangeListener = loader.loadClass("androidx.preference.Preference$c");
        OnPreferenceClickListener = loader.loadClass("androidx.preference.Preference$d");
        PreferenceFragmentCompat = loader.loadClass("androidx.preference.f");
        PreferenceManager = loader.loadClass("androidx.preference.i");
        PreferenceInflater = loader.loadClass("androidx.preference.h");
        PageInfoType = loader.loadClass("com.zhihu.android.data.analytics.PageInfoType");
        ZHIntent = loader.loadClass("com.zhihu.android.app.util.ZHIntent");
        MainActivity = loader.loadClass("com.zhihu.android.app.ui.activity.MainActivity");
        BasePreferenceFragment = loader.loadClass("com.zhihu.android.app.ui.fragment.BasePreferenceFragment");
        PreferenceGroup = loader.loadClass("androidx.preference.PreferenceGroup");
        EditTextPreference = loader.loadClass("androidx.preference.EditTextPreference");
        ListPreference = loader.loadClass("androidx.preference.ListPreference");
        TooltipCompat = loader.loadClass("androidx.appcompat.widget.TooltipCompat");
        findPreference = Compat.method(PreferenceFragmentCompat, "a", Preference, CharSequence.class);
        setSummary = Compat.method(Preference, "a", void.class, CharSequence.class);
        setIcon = Compat.method(Preference, "a", void.class, Drawable.class);
        setVisible = Compat.method(Preference, "c", void.class, boolean.class);
        getKey = Compat.method(Preference, "C", String.class);
        setChecked = Compat.method(SwitchPreference, "g", void.class, boolean.class);
        getText = Compat.method(EditTextPreference, "i", String.class);
        getLayoutResId_MethodName = "j";
        onCreate_MethodName = "i";
        setOnPreferenceChangeListener = Compat.method(Preference, "a", void.class, OnPreferenceChangeListener);
        setOnPreferenceClickListener = Compat.method(Preference, "a", void.class, OnPreferenceClickListener);
        setSharedPreferencesName = Compat.method(PreferenceManager, "a", void.class, String.class);
        addPreferencesFromResource = Compat.method(PreferenceFragmentCompat, "b", void.class, int.class);
        inflate = Compat.method(PreferenceInflater, "a", Preference, XmlPullParser.class, PreferenceGroup);
        getContext = BasePreferenceFragment.getMethod("getContext");
        setTooltipText = TooltipCompat.getMethod("setTooltipText", View.class, CharSequence.class);
        startFragment = Compat.method(BasePreferenceFragment, "a", void.class, ZHIntent);
        ListPreference_mEntries = Compat.field(ListPreference, "a");
        ListPreference_mEntryValues = Compat.field(ListPreference, "b");
        ZHIntent_ctor = ZHIntent.getConstructor(Class.class, Bundle.class, String.class,
                Array.newInstance(PageInfoType, 0).getClass());
        onPreferenceClick_MethodName = "onPreferenceClick";
        Compat.note("Settings mapping resolved for extracted arm64 11.10.0 (41012)");
    }

    @Override
    public void hook() throws Throwable {
        XposedBridge.hookMethod(PreferenceInflater.getMethod("a", int.class, PreferenceGroup), new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XmlResourceParser parser;
                int id = (int) param.args[0];
                if (id == 7355608)
                    parser = Helper.modRes.getXml(R.xml.settings);
                else if (id == debug_res_id)
                    parser = Helper.modRes.getXml(R.xml.preferences_zhihu);
                else
                    return;
                try {
                    param.setResult(inflate.invoke(param.thisObject, parser, param.args[1]));
                } finally {
                    parser.close();
                }
            }
        });

        XposedHelpers.findAndHookMethod(SettingsFragment, getLayoutResId_MethodName, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                settings_res_id = (int) param.getResult();
            }
        });
        XposedHelpers.findAndHookMethod(DebugFragment, getLayoutResId_MethodName, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                debug_res_id = (int) param.getResult();
            }
        });

        XposedBridge.hookMethod(addPreferencesFromResource, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (((int) param.args[0]) == settings_res_id) {
                    addPreferencesFromResource.invoke(param.thisObject, 7355608);
                }
            }
        });

        XposedHelpers.findAndHookMethod(SettingsFragment, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object thisObject = param.thisObject;
                preference_zhiliao = findPreference.invoke(thisObject, "preference_id_zhiliao");
                setSummary.invoke(preference_zhiliao, Helper.modRes.getString(R.string.app_version));
                setOnPreferenceClickListener.invoke(preference_zhiliao, thisObject);
            }
        });

        XposedHelpers.findAndHookMethod(SettingsFragment, onPreferenceClick_MethodName, Preference, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[0] == preference_zhiliao) {
                    Object intent = ZHIntent_ctor.newInstance(DebugFragment, null, "SCREEN_NAME_NULL", Array.newInstance(PageInfoType, 0));
                    startFragment.invoke(param.thisObject, intent);
                    param.setResult(false);
                }
            }
        });
        XposedBridge.hookMethod(BasePreferenceFragment.getMethod("a", Bundle.class, String.class), new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.thisObject.getClass() == DebugFragment) {
                    Field[] fields = PreferenceFragmentCompat.getDeclaredFields();
                    for (Field field : fields) {
                        if (field.getType() == PreferenceManager) {
                            field.setAccessible(true);
                            setSharedPreferencesName.invoke(field.get(param.thisObject), "zhiliao_preferences");
                            return;
                        }
                    }
                }
            }
        });
        XposedHelpers.findAndHookMethod(BasePreferenceFragment, "onViewCreated", View.class, Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.thisObject.getClass() == DebugFragment) {
                    Field[] fields = BasePreferenceFragment.getDeclaredFields();
                    for (Field field : fields) {
                        if (field.getType().getName().equals("com.zhihu.android.app.ui.widget.SystemBar")) {
                            field.setAccessible(true);
                            Object systemBar = field.get(param.thisObject);
                            ViewGroup toolbar = (ViewGroup) systemBar.getClass().getMethod("getToolbar").invoke(systemBar);
                            toolbar.getClass().getMethod("setTitle", CharSequence.class).invoke(toolbar, "知了");

                            ImageView restart = new ImageView(Helper.context);
                            final int _48dp = (int) (Helper.scale * 48 + 0.5);
                            restart.setLayoutParams(new ViewGroup.MarginLayoutParams(_48dp, _48dp));
                            restart.setImageDrawable(Helper.modRes.getDrawable(R.drawable.ic_restart));
                            restart.setScaleType(ImageView.ScaleType.CENTER);
                            restart.setOnClickListener(v -> Helper.doRestart(Helper.context));
                            restart.setColorFilter(Helper.getDarkMode() ? 0xffd3d3d3 : 0xff646464);
                            setTooltipText.invoke(null, restart, "重启知乎");
                            ViewGroup actionMenuView = (ViewGroup) (toolbar.getChildAt(0));
                            actionMenuView.addView(restart);
                            break;
                        }
                    }
                }
            }
        });
        XposedHelpers.findAndHookMethod(DebugFragment, onCreate_MethodName, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Object thisObject = param.thisObject;
                Object preference_version = findPreference.invoke(thisObject, "preference_version");
                Object preference_author = findPreference.invoke(thisObject, "preference_author");
                Object preference_help = findPreference.invoke(thisObject, "preference_help");
                Object preference_channel = findPreference.invoke(thisObject, "preference_channel");
                Object preference_telegram = findPreference.invoke(thisObject, "preference_telegram");
                Object preference_sourcecode = findPreference.invoke(thisObject, "preference_sourcecode");
                Object preference_donate = findPreference.invoke(thisObject, "preference_donate");
                Object preference_status = findPreference.invoke(thisObject, "preference_status");
                Object switch_externlink = findPreference.invoke(thisObject, "switch_externlink");
                Object switch_externlinkex = findPreference.invoke(thisObject, "switch_externlinkex");
                Object switch_tag = findPreference.invoke(thisObject, "switch_tag");
                Object switch_thirdpartylogin = findPreference.invoke(thisObject, "switch_thirdpartylogin");
                Object switch_autorefresh = findPreference.invoke(thisObject, "switch_autorefresh");
                Object switch_livebutton = findPreference.invoke(thisObject, "switch_livebutton");
                Object switch_reddot = findPreference.invoke(thisObject, "switch_reddot");
                Object switch_vipbanner = findPreference.invoke(thisObject, "switch_vipbanner");
                Object switch_vipnav = findPreference.invoke(thisObject, "switch_vipnav");
                Object switch_videonav = findPreference.invoke(thisObject, "switch_videonav");
                Object switch_friendnav = findPreference.invoke(thisObject, "switch_friendnav");
                Object switch_panelnav = findPreference.invoke(thisObject, "switch_panelnav");
                Object switch_findnav = findPreference.invoke(thisObject, "switch_findnav");
                Object switch_article = findPreference.invoke(thisObject, "switch_article");
                Object switch_navres = findPreference.invoke(thisObject, "switch_navres");
                Object switch_nipple = findPreference.invoke(thisObject, "switch_nipple");
                Object switch_nextanswer = findPreference.invoke(thisObject, "switch_nextanswer");
                Object preference_clean = findPreference.invoke(thisObject, "preference_clean");
                Object switch_autoclean = findPreference.invoke(thisObject, "switch_autoclean");
                Object switch_feedtophot = findPreference.invoke(thisObject, "switch_feedtophot");
                Object switch_minehybrid = findPreference.invoke(thisObject, "switch_minehybrid");

                setOnPreferenceChangeListener.invoke(findPreference.invoke(thisObject, "accept_eula"), thisObject);
                setOnPreferenceClickListener.invoke(switch_externlink, thisObject);
                setOnPreferenceClickListener.invoke(switch_externlinkex, thisObject);
                setOnPreferenceClickListener.invoke(switch_tag, thisObject);
                setOnPreferenceClickListener.invoke(switch_thirdpartylogin, thisObject);
                setOnPreferenceClickListener.invoke(switch_autorefresh, thisObject);
                setOnPreferenceClickListener.invoke(switch_livebutton, thisObject);
                setOnPreferenceClickListener.invoke(switch_reddot, thisObject);
                setOnPreferenceClickListener.invoke(switch_vipbanner, thisObject);
                setOnPreferenceClickListener.invoke(switch_vipnav, thisObject);
                setOnPreferenceClickListener.invoke(switch_videonav, thisObject);
                setOnPreferenceClickListener.invoke(switch_friendnav, thisObject);
                setOnPreferenceClickListener.invoke(switch_panelnav, thisObject);
                setOnPreferenceClickListener.invoke(switch_findnav, thisObject);
                setOnPreferenceClickListener.invoke(switch_article, thisObject);
                setOnPreferenceClickListener.invoke(switch_navres, thisObject);
                setOnPreferenceClickListener.invoke(switch_nipple, thisObject);
                setOnPreferenceClickListener.invoke(switch_nextanswer, thisObject);
                setOnPreferenceClickListener.invoke(preference_clean, thisObject);
                setOnPreferenceClickListener.invoke(switch_autoclean, thisObject);
                setOnPreferenceClickListener.invoke(preference_version, thisObject);
                setOnPreferenceClickListener.invoke(preference_author, thisObject);
                setOnPreferenceClickListener.invoke(preference_help, thisObject);
                setOnPreferenceClickListener.invoke(preference_channel, thisObject);
                setOnPreferenceClickListener.invoke(preference_telegram, thisObject);
                setOnPreferenceClickListener.invoke(preference_sourcecode, thisObject);
                setOnPreferenceClickListener.invoke(preference_donate, thisObject);
                setOnPreferenceClickListener.invoke(switch_feedtophot, thisObject);
                setOnPreferenceClickListener.invoke(switch_minehybrid, thisObject);

                String loaded_version = Helper.modRes.getString(R.string.app_version);
                setSummary.invoke(preference_version, loaded_version);
                setSummary.invoke(switch_livebutton, "旧直播按钮入口已移除；本版隐藏首页直播标签（重启生效）");

                if (Helper.officialZhihu) {
                    String real_version = null;
                    try {
                        real_version = Helper.context.getPackageManager().getResourcesForApplication(modulePackage).getString(R.string.app_version);
                    } catch (Exception ignore) {
                    }
                    if (real_version == null || loaded_version.equals(real_version)) {
                        setVisible.invoke(preference_status, false);
                    } else {
                        setOnPreferenceClickListener.invoke(preference_status, thisObject);
                        Object category_eula = findPreference.invoke(thisObject, "category_eula");
                        Object category_ads = findPreference.invoke(thisObject, "category_ads");
                        Object category_misc = findPreference.invoke(thisObject, "category_misc");
                        Object category_ui = findPreference.invoke(thisObject, "category_ui");
                        Object category_nav = findPreference.invoke(thisObject, "category_nav");
                        Object category_answer_buttons = findPreference.invoke(thisObject, "category_answer_buttons");
                        Object category_filter = findPreference.invoke(thisObject, "category_filter");
                        Object category_webview = findPreference.invoke(thisObject, "category_webview");
                        Object category_cleaner = findPreference.invoke(thisObject, "category_cleaner");
                        Object category_debug = findPreference.invoke(thisObject, "category_debug");
                        setVisible.invoke(category_eula, false);
                        setVisible.invoke(category_ads, false);
                        setVisible.invoke(category_misc, false);
                        setVisible.invoke(category_ui, false);
                        setVisible.invoke(category_nav, false);
                        setVisible.invoke(category_answer_buttons, false);
                        setVisible.invoke(category_filter, false);
                        setVisible.invoke(category_webview, false);
                        setVisible.invoke(category_cleaner, false);
                        setVisible.invoke(category_debug, false);
                        return null;
                    }
                } else {
                    setVisible.invoke(preference_status, false);
                }

                setIcon.invoke(preference_status, Helper.modRes.getDrawable(R.drawable.ic_refresh));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_mainswitch"), Helper.modRes.getDrawable(R.drawable.ic_toggle_on));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_launchad"), Helper.modRes.getDrawable(R.drawable.ic_ad_units));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_feedad"), Helper.modRes.getDrawable(R.drawable.ic_table_rows));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_answerlistad"), Helper.modRes.getDrawable(R.drawable.ic_format_list));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_commentad"), Helper.modRes.getDrawable(R.drawable.ic_comment));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_sharead"), Helper.modRes.getDrawable(R.drawable.ic_share));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_answerad"), Helper.modRes.getDrawable(R.drawable.ic_notes));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_searchad"), Helper.modRes.getDrawable(R.drawable.ic_search));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_video"), Helper.modRes.getDrawable(R.drawable.ic_play_circle));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_removearticle"), Helper.modRes.getDrawable(R.drawable.ic_article));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_pin"), Helper.modRes.getDrawable(R.drawable.ic_emoji_objects));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_marketcard"), Helper.modRes.getDrawable(R.drawable.ic_vip));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_club"), Helper.modRes.getDrawable(R.drawable.ic_group));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_goods"), Helper.modRes.getDrawable(R.drawable.ic_local_mall));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_related"), Helper.modRes.getDrawable(R.drawable.ic_search_off));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_searchwords"), Helper.modRes.getDrawable(R.drawable.ic_plagiarism));
                setIcon.invoke(switch_externlink, Helper.modRes.getDrawable(R.drawable.ic_link));
                setIcon.invoke(switch_externlinkex, Helper.modRes.getDrawable(R.drawable.ic_link));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_colormode"), Helper.modRes.getDrawable(R.drawable.ic_color));
                setIcon.invoke(switch_tag, Helper.modRes.getDrawable(R.drawable.ic_label));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_statusbar"), Helper.modRes.getDrawable(R.drawable.ic_fullscreen));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_fullscreen"), Helper.modRes.getDrawable(R.drawable.ic_fullscreen_exit));
                setIcon.invoke(switch_thirdpartylogin, Helper.modRes.getDrawable(R.drawable.ic_login));
                setIcon.invoke(switch_autorefresh, Helper.modRes.getDrawable(R.drawable.ic_refresh));
                setIcon.invoke(switch_livebutton, Helper.modRes.getDrawable(R.drawable.ic_live_tv));
                setIcon.invoke(switch_reddot, Helper.modRes.getDrawable(R.drawable.ic_mark_chat_unread));
                setIcon.invoke(switch_vipbanner, Helper.modRes.getDrawable(R.drawable.ic_vip_banner));
                setIcon.invoke(switch_vipnav, Helper.modRes.getDrawable(R.drawable.ic_vip_nav));
                setIcon.invoke(switch_videonav, Helper.modRes.getDrawable(R.drawable.ic_play_circle));
                setIcon.invoke(switch_friendnav, Helper.modRes.getDrawable(R.drawable.ic_person_add_alt));
                setIcon.invoke(switch_panelnav, Helper.modRes.getDrawable(R.drawable.ic_add_circle));
                setIcon.invoke(switch_findnav, Helper.modRes.getDrawable(R.drawable.ic_cross_star));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_hotbanner"), Helper.modRes.getDrawable(R.drawable.ic_whatshot));
                setIcon.invoke(switch_article, Helper.modRes.getDrawable(R.drawable.ic_article));
                setIcon.invoke(switch_navres, Helper.modRes.getDrawable(R.drawable.ic_event));
                setIcon.invoke(switch_nipple, Helper.modRes.getDrawable(R.drawable.ic_do_disturb_on));
                setIcon.invoke(switch_nextanswer, Helper.modRes.getDrawable(R.drawable.ic_circle_down));
                setIcon.invoke(findPreference.invoke(thisObject, "edit_title"), Helper.regex_title != null ? Helper.modRes.getDrawable(R.drawable.ic_check) : Helper.modRes.getDrawable(R.drawable.ic_close));
                setIcon.invoke(findPreference.invoke(thisObject, "edit_author"), Helper.regex_author != null ? Helper.modRes.getDrawable(R.drawable.ic_check) : Helper.modRes.getDrawable(R.drawable.ic_close));
                setIcon.invoke(findPreference.invoke(thisObject, "edit_content"), Helper.regex_content != null ? Helper.modRes.getDrawable(R.drawable.ic_check) : Helper.modRes.getDrawable(R.drawable.ic_close));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_webview_debug"), Helper.modRes.getDrawable(R.drawable.ic_code));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_watermark"), Helper.modRes.getDrawable(R.drawable.ic_layers));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_subscribe"), Helper.modRes.getDrawable(R.drawable.ic_person_add_alt));
                setIcon.invoke(findPreference.invoke(thisObject, "edit_js"), Helper.modRes.getDrawable(R.drawable.ic_javascript));
                setIcon.invoke(preference_clean, Helper.modRes.getDrawable(R.drawable.ic_delete));
                setIcon.invoke(switch_autoclean, Helper.modRes.getDrawable(R.drawable.ic_auto_delete));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_silenceclean"), Helper.modRes.getDrawable(R.drawable.ic_notifications_off));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_hidetoast"), Helper.modRes.getDrawable(R.drawable.ic_tooltip));
                setIcon.invoke(preference_version, Helper.modRes.getDrawable(R.drawable.ic_info));
                setIcon.invoke(preference_author, Helper.modRes.getDrawable(R.drawable.ic_person));
                setIcon.invoke(preference_help, Helper.modRes.getDrawable(R.drawable.ic_help));
                setIcon.invoke(preference_channel, Helper.modRes.getDrawable(R.drawable.ic_rss_feed));
                setIcon.invoke(preference_telegram, Helper.modRes.getDrawable(R.drawable.ic_telegram));
                setIcon.invoke(preference_sourcecode, Helper.modRes.getDrawable(R.drawable.ic_github));
                setIcon.invoke(preference_donate, Helper.modRes.getDrawable(R.drawable.ic_monetization));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_feedtophot"), Helper.modRes.getDrawable(R.drawable.ic_whatshot));
                setIcon.invoke(findPreference.invoke(thisObject, "switch_minehybrid"), Helper.modRes.getDrawable(R.drawable.ic_viewcard));

                if (Helper.prefs.getBoolean("accept_eula", false)) {
                    Object category_eula = findPreference.invoke(thisObject, "category_eula");
                    setVisible.invoke(category_eula, false);
                } else {
                    Object switch_main = findPreference.invoke(param.thisObject, "switch_mainswitch");
                    setChecked.invoke(switch_main, false);
                }

                setSummary.invoke(preference_clean, "广告缓存和日志文件 " + Cleaner.humanReadableByteCount(Cleaner.getFileSize(Cleaner.getCacheFiles())));

                return null;
            }
        });
        XposedHelpers.findAndHookMethod(DebugFragment, onPreferenceClick_MethodName, Preference, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                Object preference = param.args[0];
                switch ((String) getKey.invoke(preference)) {
                    case "preference_status":
                        System.exit(0);
                        break;
                    case "preference_version":
                        version_click++;
                        if (version_click == 5) {
                            Helper.toast("点我次数再多，更新也不会变快哦", Toast.LENGTH_SHORT);
                            version_click = 0;
                        }
                        break;
                    case "preference_author":
                        author_click++;
                        if (author_click == 5) {
                            Helper.toast(Helper.modRes.getStringArray(R.array.click_author)[new Random().nextInt(4)], Toast.LENGTH_SHORT);
                            author_click = 0;
                        }
                        break;
                    case "preference_help":
                        Uri uri_help = Uri.parse("https://github.com/shatyuka/Zhiliao/wiki");
                        Intent intent_help = new Intent(Intent.ACTION_VIEW, uri_help);
                        ((Context) getContext.invoke(param.thisObject)).startActivity(intent_help);
                        break;
                    case "preference_channel":
                        Uri uri_channel = Uri.parse("https://t.me/zhiliao");
                        Intent intent_channel = new Intent(Intent.ACTION_VIEW, uri_channel);
                        ((Context) getContext.invoke(param.thisObject)).startActivity(intent_channel);
                        break;
                    case "preference_telegram":
                        Uri uri_telegram = Uri.parse("https://t.me/joinchat/OibCWxbdCMkJ2fG8J1DpQQ");
                        Intent intent_telegram = new Intent(Intent.ACTION_VIEW, uri_telegram);
                        ((Context) getContext.invoke(param.thisObject)).startActivity(intent_telegram);
                        break;
                    case "preference_sourcecode":
                        Uri uri_sourcecode = Uri.parse("https://github.com/shatyuka/Zhiliao");
                        Intent intent_sourcecode = new Intent(Intent.ACTION_VIEW, uri_sourcecode);
                        ((Context) getContext.invoke(param.thisObject)).startActivity(intent_sourcecode);
                        break;
                    case "preference_donate":
                        Uri uri_donate = Uri.parse("https://github.com/shatyuka/Zhiliao/wiki/Donate");
                        Intent intent_donate = new Intent(Intent.ACTION_VIEW, uri_donate);
                        ((Context) getContext.invoke(param.thisObject)).startActivity(intent_donate);
                        break;
                    case "switch_externlink":
                        Object switch_externlinkex = findPreference.invoke(param.thisObject, "switch_externlinkex");
                        setChecked.invoke(switch_externlinkex, false);
                        break;
                    case "switch_externlinkex":
                        Object switch_externlink = findPreference.invoke(param.thisObject, "switch_externlink");
                        setChecked.invoke(switch_externlink, false);
                        break;
                    case "preference_clean":
                        String size = Cleaner.humanReadableByteCount(Cleaner.doClean());
                        if (!Helper.prefs.getBoolean("switch_silenceclean", false)) {
                            Toast.makeText(Helper.context, "共清理 " + size + " 临时文件", Toast.LENGTH_SHORT).show();
                        }
                        setSummary.invoke(preference, "广告缓存和日志文件 " + Cleaner.humanReadableByteCount(Cleaner.getFileSize(Cleaner.getCacheFiles())));
                        break;
                    case "switch_navres":
                        Helper.deleteDirectory(Helper.context.getFilesDir() + "/bottom_nav");
                    case "switch_tag":
                    case "switch_thirdpartylogin":
                    case "switch_autorefresh":
                    case "switch_livebutton":
                    case "switch_reddot":
                    case "switch_vipbanner":
                    case "switch_vipnav":
                    case "switch_videonav":
                    case "switch_friendnav":
                    case "switch_panelnav":
                    case "switch_findnav":
                    case "switch_article":
                    case "switch_nextanswer":
                    case "switch_nipple":
                    case "switch_autoclean":
                    case "switch_feedtophot":
                    case "switch_minehybrid":
                        Helper.toast("重启知乎生效", Toast.LENGTH_SHORT);
                        break;
                }
                return false;
            }
        });
        XposedBridge.hookMethod(DebugFragment.getMethod("a", Preference, Object.class), new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                if ((boolean) param.args[1]) {
                    Object switch_main = findPreference.invoke(param.thisObject, "switch_mainswitch");
                    setChecked.invoke(switch_main, true);
                    Object category_eula = findPreference.invoke(param.thisObject, "category_eula");
                    setVisible.invoke(category_eula, false);
                }
                return true;
            }
        });
        XposedBridge.hookMethod(EditTextPreference.getMethod("a", String.class), new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object thisObject = param.thisObject;
                switch ((String) getKey.invoke(thisObject)) {
                    case "edit_title":
                        Helper.regex_title = Helper.compileRegex((String) getText.invoke(thisObject));
                        setIcon.invoke(thisObject, Helper.regex_title != null ? Helper.modRes.getDrawable(R.drawable.ic_check) : Helper.modRes.getDrawable(R.drawable.ic_close));
                        break;
                    case "edit_author":
                        Helper.regex_author = Helper.compileRegex((String) getText.invoke(thisObject));
                        setIcon.invoke(thisObject, Helper.regex_author != null ? Helper.modRes.getDrawable(R.drawable.ic_check) : Helper.modRes.getDrawable(R.drawable.ic_close));
                        break;
                    case "edit_content":
                        Helper.regex_content = Helper.compileRegex((String) getText.invoke(thisObject));
                        setIcon.invoke(thisObject, Helper.regex_content != null ? Helper.modRes.getDrawable(R.drawable.ic_check) : Helper.modRes.getDrawable(R.drawable.ic_close));
                        break;
                    default:
                        return;
                }
                CustomFilter.onKeywordsChanged();
            }
        });
        XposedHelpers.findAndHookMethod(Dialog.class, "dismiss", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Dialog dialog = (Dialog) param.thisObject;
                if (dialog.isShowing()) {
                    View view = dialog.getWindow().getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) Helper.context.getSystemService(Activity.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getRootView().getWindowToken(), 0);
                    }
                }
            }
        });

        XposedHelpers.findAndHookMethod(SettingsFragment, "onViewCreated", View.class, Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Helper.settingsView = param.args[0];
            }
        });

        XposedHelpers.findAndHookMethod(BasePreferenceFragment, "onDestroyView", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (param.thisObject.getClass() == SettingsFragment) {
                    Helper.settingsView = null;
                }
            }
        });
    }
}
