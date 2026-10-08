package com.shatyuka.zhiliao.hooks;

import com.shatyuka.zhiliao.AdModels;
import com.shatyuka.zhiliao.Compat;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import de.robv.android.xposed.XC_MethodHook;

/** Search landing-page sections only; history and actual search results are untouched. */
public class SearchAd implements IHook {
    private Method readCache, cacheResult, bindGuess, bindHot, notifyChanged;
    private Class<?> guessQuery;
    private Field items, adapter, guessNode, hotNode, guessQueries, hotQueries, guessAdded, hotAdded;

    public String getName() { return "去搜索广告"; }

    public void init(ClassLoader loader) throws Throwable {
        AdModels.install(loader);
        Class<?> result = loader.loadClass("com.zhihu.android.net.cache.Result");
        readCache = Compat.method(result, "fromJsonCache", result, Class.class, byte[].class);
        cacheResult = Compat.method(result, "getResult", Object.class);

        Class<?> fragment = loader.loadClass("com.zhihu.android.app.ui.fragment.search.AlphaSearchFragment");
        guessQuery = loader.loadClass("com.zhihu.android.api.model.SearchGuessQueries$Query");
        bindGuess = Compat.method(fragment, "a", void.class, List.class);
        bindHot = Compat.method(fragment, "b", void.class, List.class);
        items = Compat.field(fragment, "h");
        adapter = Compat.field(fragment, "i");
        guessNode = Compat.field(fragment, "m");
        hotNode = Compat.field(fragment, "n");
        guessQueries = Compat.field(guessNode.getType(), "queries");
        hotQueries = Compat.field(hotNode.getType(), "queries");
        guessAdded = Compat.field(fragment, "s");
        hotAdded = Compat.field(fragment, "u");
        notifyChanged = Compat.method(adapter.getType(), "notifyDataSetChanged", void.class);
    }

    public void hook() {
        // Disk cache uses its own JSON reader, not the Retrofit converter in AdModels.
        Compat.hook(readCache, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!Compat.enabled("switch_searchad", true) || param.hasThrowable()
                        || param.getResult() == null) return;
                String model = ((Class<?>) param.args[0]).getName();
                if (model.equals("com.zhihu.android.api.model.SearchRecommendQuery")
                        || model.equals("com.zhihu.android.api.model.HotSearchBean")
                        || model.equals("com.zhihu.android.api.model.PresetWords")
                        || model.equals("com.zhihu.android.service.search_service.model.PresetWords")) {
                    AdModels.filterModel(cacheResult.invoke(param.getResult()));
                    Compat.note("SearchAd: filtered search cache");
                }
            }
        });

        // Memory/preload data may bypass JSON too. Filter before it reaches the page adapter.
        Compat.hook(bindGuess, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!Compat.enabled("switch_searchad", true)) return;
                suppressSections(param.thisObject);
                param.args[0] = Collections.emptyList();
                // Keep the original successful-empty path: remove skeletons and update page state.
            }
        });
        Compat.hook(bindHot, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!Compat.enabled("switch_searchad", true)) return;
                suppressSections(param.thisObject);
                // Even an empty list adds HotSearchBean in the original renderer, retaining its title.
                param.setResult(null);
                Compat.note("SearchAd: blocked hot-search section before bind");
            }
        });
    }

    private void suppressSections(Object fragment) throws Throwable {
        Object guess = guessNode.get(fragment);
        Object hot = hotNode.get(fragment);
        if (guess != null) guessQueries.set(guess, new ArrayList<>());
        if (hot != null) hotQueries.set(hot, new ArrayList<>());
        guessAdded.setBoolean(fragment, false);
        hotAdded.setBoolean(fragment, false);

        Object data = items.get(fragment);
        boolean changed = false;
        if (data instanceof List) {
            Iterator<?> iterator = ((List<?>) data).iterator();
            while (iterator.hasNext()) {
                Object item = iterator.next();
                if (item == guess || item == hot || guessQuery.isInstance(item)) {
                    iterator.remove();
                    changed = true;
                }
            }
        }
        Object listAdapter = adapter.get(fragment);
        if (changed && listAdapter != null) notifyChanged.invoke(listAdapter);
    }
}
