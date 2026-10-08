package com.shatyuka.zhiliao.hooks;

import android.view.View;
import com.shatyuka.zhiliao.Compat;
import com.shatyuka.zhiliao.FeedKeywords;
import com.shatyuka.zhiliao.Helper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

public class CustomFilter implements IHook {
    private FeedKeywords keywords;
    private Class<?> paging;
    private Method parse, refresh, insertRange, insertItem, resume, hidden, visible;
    private Method getData, getHeaderCount, notifyChanged, computingLayout;
    private Field responseData, pageAdapter, recycler;
    private final Map<Object, Rules> checkedPages = Collections.synchronizedMap(new WeakHashMap<>());
    private Class<?> deserializer;
    private static volatile CustomFilter installed;
    private final Set<Object> queuedPages = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<Object, Boolean>()));

    public String getName() { return "自定义过滤"; }

    public void init(ClassLoader loader) throws Throwable {
        keywords = new FeedKeywords(loader);
        deserializer = loader.loadClass("com.zhihu.android.api.util.ZHObjectRegistryCenter$InnerDeserializer");
        Class<?> apiRoot = loader.loadClass("com.zhihu.android.api.model.template.api.ApiTemplateRoot");
        Class<?> template = loader.loadClass("com.zhihu.android.api.model.template.TemplateRoot");
        parse = Compat.method(apiRoot, "parse", template);
        paging = loader.loadClass("com.zhihu.android.app.ui.fragment.paging.BasePagingFragment");
        Class<?> packet = loader.loadClass("com.zhihu.android.api.model.ZHObjectList");
        responseData = Compat.field(packet, "data");
        refresh = Compat.method(paging, "postRefreshSucceed", void.class, packet);
        insertRange = Compat.method(paging, "insertDataRangeToList", void.class, int.class, List.class);
        insertItem = Compat.method(paging, "insertDataItemToList", void.class, int.class, Object.class);
        getData = Compat.method(paging, "getDataList", List.class);
        getHeaderCount = Compat.method(paging, "getHeaderCount", int.class);
        pageAdapter = Compat.field(paging, "mAdapter");
        recycler = Compat.field(paging, "mRecyclerView");
        notifyChanged = Compat.method(pageAdapter.getType(), "notifyDataSetChanged", void.class);
        computingLayout = Compat.method(loader.loadClass("androidx.recyclerview.widget.RecyclerView"), "isComputingLayout", boolean.class);
        // These are inherited from BaseFragment: callbacks must be scoped to paging instances.
        resume = Compat.method(paging, "onResume", void.class);
        hidden = Compat.method(paging, "onHiddenChanged", void.class, boolean.class);
        visible = Compat.method(paging, "setUserVisibleHint", void.class, boolean.class);
    }

    public void hook() {
        XposedBridge.hookAllMethods(deserializer, "deserialize", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                if (!p.hasThrowable() && blocked(p.getResult(), new Rules())) p.setResult(null);
            }
        });
        Compat.hook(parse, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                if (!p.hasThrowable() && p.getResult() != null) keywords.rememberParsed(p.getResult(), p.thisObject);
            }
        });
        Compat.hook(refresh, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                Rules rules = new Rules();
                if (!rules.active() || p.args[0] == null) return;
                Object data = responseData.get(p.args[0]);
                if (data instanceof List) responseData.set(p.args[0], keywords.filter((List<?>) data, rules));
            }
        });
        Compat.hook(insertRange, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                Rules rules = new Rules();
                if (rules.active() && p.args[1] instanceof List) p.args[1] = keywords.filter((List<?>) p.args[1], rules);
            }
        });
        Compat.hook(insertItem, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                if (blocked(p.args[1], new Rules())) p.setResult(null);
            }
        });
        XC_MethodHook recheck = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                if (!paging.isInstance(p.thisObject)) return;
                if (hidden.equals(p.method) && Boolean.TRUE.equals(p.args[0])) return;
                if (visible.equals(p.method) && Boolean.FALSE.equals(p.args[0])) return;
                recheckPage(p.thisObject);
            }
        };
        Compat.hook(resume, recheck);
        Compat.hook(hidden, recheck);
        Compat.hook(visible, recheck);
        installed = this;
    }

    public static void onKeywordsChanged() {
        CustomFilter filter = installed;
        if (filter == null) return;
        List<Object> pages;
        synchronized (filter.checkedPages) { pages = new ArrayList<>(filter.checkedPages.keySet()); }
        for (Object page : pages) {
            try {
                Object view = filter.recycler.get(page);
                if (view instanceof View) filter.postRecheck(page, (View) view);
            } catch (Throwable error) { Compat.note("CustomFilter: rule-change recheck failed: " + error.getClass().getSimpleName()); }
        }
    }

    private void postRecheck(Object fragment, View view) {
        if (!queuedPages.add(fragment)) return;
        final WeakReference<Object> target = new WeakReference<>(fragment);
        boolean accepted = view.post(new Runnable() {
            public void run() {
                Object page = target.get();
                if (page == null) return;
                queuedPages.remove(page);
                try { recheckPage(page); }
                catch (Throwable error) { Compat.note("CustomFilter: deferred recheck failed: " + error.getClass().getSimpleName()); }
            }
        });
        if (!accepted) queuedPages.remove(fragment);
    }

    private boolean blocked(Object item, Rules rules) throws IllegalAccessException {
        if (!rules.active()) return false;
        FeedKeywords.Details details = keywords.read(item);
        return details != null && rules.matches(details);
    }

    private void recheckPage(Object fragment) throws Throwable {
        Rules rules = new Rules();
        Rules previous = checkedPages.get(fragment);
        if (rules.same(previous)) return;
        if (!rules.active()) { checkedPages.put(fragment, rules); return; }
        Object adapter = pageAdapter.get(fragment);
        Object data = getData.invoke(fragment);
        if (adapter == null || !(data instanceof List)) return;
        Object view = recycler.get(fragment);
        if (view instanceof View && (boolean) computingLayout.invoke(view)) {
            postRecheck(fragment, (View) view);
            return;
        }
        synchronized (fragment) {
            List<?> list = (List<?>) data;
            int headers = Math.max(0, Math.min((int) getHeaderCount.invoke(fragment), list.size()));
            List<?> tail = new ArrayList<>(list.subList(headers, list.size()));
            List<?> kept = keywords.filter(tail, rules);
            if (kept != tail) {
                list.subList(headers, list.size()).clear();
                @SuppressWarnings("unchecked") List<Object> mutable = (List<Object>) list;
                mutable.addAll(kept);
                notifyChanged.invoke(adapter);
                Compat.note("CustomFilter: rechecked existing feed rows; removed=" + (tail.size() - kept.size()));
            }
        }
        checkedPages.put(fragment, rules);
    }

    private static final class Rules implements FeedKeywords.Matcher {
        final Pattern title = Helper.regex_title, author = Helper.regex_author, content = Helper.regex_content;
        final boolean main = Helper.prefs.getBoolean("switch_mainswitch", false);
        final boolean video = Helper.prefs.getBoolean("switch_video", false);
        final boolean article = Helper.prefs.getBoolean("switch_removearticle", false);
        final boolean pin = Helper.prefs.getBoolean("switch_pin", false);
        final boolean ads = Helper.prefs.getBoolean("switch_feedad", true);
        final boolean market = Helper.prefs.getBoolean("switch_marketcard", false);
        boolean active() { return main && (title != null || author != null || content != null || video || article || pin || ads || market); }
        public boolean matches(FeedKeywords.Details d) {
            if (!main) return false;
            if (d.market) return market;
            if (video && (d.video || "zvideo".equals(d.type) || "drama".equals(d.type))) return true;
            if (article && ("article".equals(d.type) || "Post".equals(d.type))) return true;
            if (pin && "pin".equals(d.type)) return true;
            if (ads && "SvipActivity".equals(d.type)) return true;
            return d.matches(title, author, content);
        }
        boolean same(Rules r) {
            return r != null && title == r.title && author == r.author && content == r.content
                    && main == r.main && video == r.video && article == r.article && pin == r.pin && ads == r.ads && market == r.market;
        }
    }
}
