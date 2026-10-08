package com.shatyuka.zhiliao.hooks;

import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import com.shatyuka.zhiliao.Compat;
import com.shatyuka.zhiliao.Helper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.WeakHashMap;
import de.robv.android.xposed.XC_MethodHook;

public class AnswerAd implements IHook {
    private Method intercept;
    private Method[] relatedBinds, relatedVisibility;
    private Field relatedItemView;
    private final Map<View, RelatedLayout> collapsedRelated = Collections.synchronizedMap(new WeakHashMap<>());
    private final Set<Class<?>> relatedLogged = Collections.synchronizedSet(new HashSet<Class<?>>());
    private static final class RelatedLayout {
        final int visibility, width, height;
        final boolean hadParams;
        RelatedLayout(View view) {
            visibility = view.getVisibility();
            ViewGroup.LayoutParams p = view.getLayoutParams();
            hadParams = p != null;
            width = p == null ? 0 : p.width;
            height = p == null ? 0 : p.height;
        }
    }
    public String getName() { return "回答页广告与关联模块"; }
    public void init(ClassLoader loader) throws Throwable {
        intercept = Compat.method(loader.loadClass("com.zhihu.android.appview.a$j"), "a",
                WebResourceResponse.class, Helper.IZhihuWebView, WebResourceRequest.class);
        Class<?> queries = loader.loadClass("com.zhihu.android.api.model.RelatedQueries");
        Class<?> group = loader.loadClass("com.zhihu.android.app.ui.viewholder.RelatedSearchGroupHolder");
        Class<?> groupV2 = loader.loadClass("com.zhihu.android.app.ui.viewholder.RelatedSearchGroupHolderV2");
        Class<?> legacy = loader.loadClass("com.zhihu.android.mixshortcontainer.holder.RelatedSearchViewHolder");
        Class<?> legacyModel = loader.loadClass("com.zhihu.android.mixshortcontainer.model.RelatedSearchModel");
        relatedItemView = Compat.field(loader.loadClass("androidx.recyclerview.widget.RecyclerView$ViewHolder"), "itemView");
        if (relatedItemView.getType() != View.class) throw new NoSuchFieldException("Related-search itemView type mismatch");
        relatedBinds = new Method[] {
                Compat.method(group, "a", void.class, queries),
                Compat.method(groupV2, "a", void.class, queries),
                Compat.method(legacy, "a", void.class, legacyModel)
        };
        relatedVisibility = new Method[] {
                Compat.method(group, "a", void.class, boolean.class),
                Compat.method(groupV2, "a", void.class, boolean.class)
        };
    }
    private static boolean hostRequest(WebResourceRequest request) {
        if (request == null || !"GET".equals(request.getMethod()) || request.isForMainFrame()) return false;
        Uri uri = request.getUrl();
        return "https".equals(uri.getScheme()) && ("api.zhihu.com".equals(uri.getHost())
                || "www.zhihu.com".equals(uri.getHost()));
    }
    private static boolean hide(List<String> s) {
        int n = s.size();
        if (n < 3) return false;
        String end = s.get(n - 1), parent = s.get(n - 2);
        boolean answerModules = Compat.enabled("switch_answerad", true)
                && ("recommendations".equals(end) || "hotmodule".equals(parent)
                || ("brand".equals(s.get(2)) && "card".equals(end)));
        return answerModules || (Compat.enabled("switch_club", false) && "bind_club".equals(end))
                || (Compat.enabled("switch_goods", false) && "goods".equals(parent))
                || (Compat.enabled("switch_article", false) && "recommendation".equals(end))
                || (Compat.enabled("switch_related", false) && "related_queries".equals(s.get(n - 3)))
                || (Compat.enabled("switch_searchwords", false) && "entity_word".equals(end));
    }
    private void collapseRelated(Object holder) throws IllegalAccessException {
        View view = (View) relatedItemView.get(holder);
        synchronized (collapsedRelated) {
            if (!collapsedRelated.containsKey(view)) collapsedRelated.put(view, new RelatedLayout(view));
        }
        view.setVisibility(View.GONE);
        ViewGroup.LayoutParams p = view.getLayoutParams();
        if (p != null && (p.width != 0 || p.height != 0)) {
            p.width = 0;
            p.height = 0;
            view.setLayoutParams(p);
        }
    }
    private void restoreRelated(Object holder) throws IllegalAccessException {
        View view = (View) relatedItemView.get(holder);
        RelatedLayout saved = collapsedRelated.remove(view);
        if (saved == null) return;
        ViewGroup.LayoutParams p = view.getLayoutParams();
        if (saved.hadParams && p != null) {
            p.width = saved.width;
            p.height = saved.height;
            view.setLayoutParams(p);
        }
        view.setVisibility(saved.visibility);
    }
    public void hook() {
        XC_MethodHook bindRelated = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                if (!Compat.enabled("switch_related", false)) { restoreRelated(p.thisObject); return; }
                collapseRelated(p.thisObject);
                p.setResult(null); // Do not inflate rows or bind cached/preloaded queries behind a hidden title.
                if (relatedLogged.add(p.method.getDeclaringClass()))
                    Compat.note("AnswerAd: blocked native related-search bind (" + p.method.getDeclaringClass().getSimpleName() + ")");
            }
        };
        for (Method method : relatedBinds) Compat.hook(method, bindRelated);
        XC_MethodHook visibilityRelated = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                if (Compat.enabled("switch_related", false)) {
                    collapseRelated(p.thisObject);
                    p.args[0] = false;
                } else restoreRelated(p.thisObject);
            }
            @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                if (!p.hasThrowable() && Compat.enabled("switch_related", false)) collapseRelated(p.thisObject);
            }
        };
        for (Method method : relatedVisibility) Compat.hook(method, visibilityRelated);
        Compat.hook(intercept, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                WebResourceRequest request = (WebResourceRequest) p.args[1];
                if (!hostRequest(request) || !hide(request.getUrl().getPathSegments())) return;
                // Keep the upstream empty-object protocol. Endpoint response shapes still need device testing.
                WebResourceResponse response = new WebResourceResponse("application/json", "UTF-8",
                        200, "OK", Collections.singletonMap("Cache-Control", "no-store"),
                        new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));
                p.setResult(response);
            }
            @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                if (!Compat.enabled("switch_searchwords", false) || p.hasThrowable()) return;
                WebResourceRequest request = (WebResourceRequest) p.args[1];
                if (!hostRequest(request)) return;
                List<String> s = request.getUrl().getPathSegments();
                if (s.size() < 3 || !"appview".equals(s.get(0)) || !"answer".equals(s.get(s.size()-2))) return;
                Object value = p.getResult();
                if (!(value instanceof WebResourceResponse)) return;
                WebResourceResponse response = (WebResourceResponse) value;
                InputStream in = response.getData();
                if (in == null || (response.getEncoding() != null
                        && !"UTF-8".equalsIgnoreCase(response.getEncoding()))) return;
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                try {
                    int count;
                    while ((count = in.read(buffer)) != -1) {
                        bytes.write(buffer, 0, count);
                        if (bytes.size() > 4 * 1024 * 1024) {
                            response.setData(new SequenceInputStream(new ByteArrayInputStream(bytes.toByteArray()), in));
                            return;
                        }
                    }
                    String content = new String(bytes.toByteArray(), StandardCharsets.UTF_8)
                            .replace("\"searchWords\"", "\"searchWords_bak\"")
                            .replace("\"search_words\"", "\"search_words_bak\"");
                    response.setData(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
                    in.close();
                } catch (java.io.IOException e) {
                    // Restore the bytes already consumed; never truncate a response on read failure.
                    response.setData(new SequenceInputStream(new ByteArrayInputStream(bytes.toByteArray()), in));
                    Compat.note("search words response rewrite skipped: " + e.getClass().getSimpleName());
                }
            }
        });
    }
}
