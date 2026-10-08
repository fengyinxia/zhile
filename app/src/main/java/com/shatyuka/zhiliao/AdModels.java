package com.shatyuka.zhiliao;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import de.robv.android.xposed.XC_MethodHook;

/** Typed filtering shared by advertising hooks; never walks arbitrary object graphs. */
public final class AdModels {
    private static Class<?> listAd, answerAd, commentAd, commentInfo, searchCommercial, searchResult;
    private static Constructor<?> searchGuessConstructor;
    private static boolean installed;
    private AdModels() {}
    public static synchronized void install(ClassLoader loader) throws Throwable {
        if (installed) return;
        listAd = loader.loadClass("com.zhihu.android.adbase.model.ListAd");
        answerAd = loader.loadClass("com.zhihu.android.adbase.model.AnswerListAd");
        commentAd = loader.loadClass("com.zhihu.android.adbase.model.CommentListAd");
        commentInfo = loader.loadClass("com.zhihu.android.api.model.CommentAd");
        searchCommercial = loader.loadClass("com.zhihu.android.api.model.SearchHotCommercialData");
        searchResult = loader.loadClass("com.zhihu.android.api.model.SearchResult");
        searchGuessConstructor = loader.loadClass("com.zhihu.android.api.model.SearchGuessQueries").getDeclaredConstructor();
        searchGuessConstructor.setAccessible(true);
        Class<?> converter = loader.loadClass("com.zhihu.android.net.c.b");
        Method convert = Compat.method(converter, "convert", Object.class, Object.class);
        Compat.hook(convert, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!param.hasThrowable()) filterModel(param.getResult());
            }
        });
        installed = true;
    }
    public static boolean blocked(Object item) throws IllegalAccessException, NoSuchFieldException {
        if (item == null) return Helper.prefs.getBoolean("switch_mainswitch", false);
        // CommentListAd derives from FeedAdvert: test the more specific classes FIRST.
        if (commentAd.isInstance(item) || commentInfo.isInstance(item))
            return Compat.enabled("switch_commentad", true);
        if (answerAd.isInstance(item)) return Compat.enabled("switch_answerlistad", true);
        if (searchCommercial.isInstance(item)) return Compat.enabled("switch_searchad", true);
        if (searchResult.isInstance(item)) {
            Object nested = Compat.field(searchResult, "data").get(item);
            return Compat.enabled("switch_searchad", true) && isAdModel(nested);
        }
        return listAd.isInstance(item) && Compat.enabled("switch_feedad", true);
    }
    private static boolean isAdModel(Object item) {
        return item != null && (listAd.isInstance(item) || commentInfo.isInstance(item)
                || searchCommercial.isInstance(item));
    }
    public static List<?> filter(List<?> source) throws IllegalAccessException, NoSuchFieldException {
        ArrayList<Object> copy = null;
        for (int i = 0; i < source.size(); i++) {
            Object item = source.get(i);
            if (blocked(item)) {
                if (copy == null) copy = new ArrayList<>(source.subList(0, i));
            } else if (copy != null) copy.add(item);
        }
        return copy == null ? source : copy;
    }
    public static void filterModel(Object result) throws Throwable {
        if (result == null || !Helper.prefs.getBoolean("switch_mainswitch", false)) return;
        String name = result.getClass().getName();
        if (!name.startsWith("com.zhihu.android.")) return;
        // Only known list-shaped fields, one level deep. Preserve paging/cursors/normal content.
        filterField(result, "data");
        if (name.equals("com.zhihu.android.api.model.SearchRecommendQuery")) {
            if (Compat.enabled("switch_searchad", true)) {
                Compat.field(result.getClass(), "commercialData").set(result, new ArrayList<>());
                // A null wrapper is treated as a request failure and leaves cached content in play.
                // Publish a successful empty recommendation list; preserve other response metadata.
                Field recommendations = Compat.field(result.getClass(), "recommendQueries");
                Object queries = recommendations.get(result);
                if (queries == null) {
                    queries = searchGuessConstructor.newInstance();
                    recommendations.set(result, queries);
                }
                Compat.field(queries.getClass(), "queries").set(queries, new ArrayList<>());
            }
        } else if (name.equals("com.zhihu.android.api.model.PresetWords")
                || name.equals("com.zhihu.android.service.search_service.model.PresetWords")) {
            if (Compat.enabled("switch_searchad", true))
                Compat.field(result.getClass(), "preset").set(result, null);
        } else if (name.equals("com.zhihu.android.api.model.HotSearchBean")) {
            if (Compat.enabled("switch_searchad", true)) {
                Compat.field(result.getClass(), "searchHotList").set(result, new ArrayList<>());
                Compat.field(result.getClass(), "queries").set(result, new ArrayList<>());
            }
            filterField(result, "queries");
        }
    }
    private static void filterField(Object model, String name) throws Throwable {
        Field f;
        try { f = Compat.field(model.getClass(), name); }
        catch (NoSuchFieldException ignored) { return; }
        Object data = f.get(model);
        if (!(data instanceof List)) return;
        List<?> source = (List<?>) data;
        List<?> filtered = filter(source);
        if (source != filtered) f.set(model, filtered);
    }
}
