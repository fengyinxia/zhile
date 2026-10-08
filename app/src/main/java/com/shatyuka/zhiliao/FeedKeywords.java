package com.shatyuka.zhiliao;

import java.lang.reflect.Field;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Reads only known, visible feed text roles; never searches arbitrary model fields. */
public final class FeedKeywords {
    enum Schema {
        API_ROOT("com.zhihu.android.api.model.template.api.ApiTemplateRoot", "extra", "common_card"),
        API_CARD("com.zhihu.android.api.model.template.api.ApiFeedCard", "feed_content", "media_card_content"),
        API_FEED("com.zhihu.android.api.model.template.api.ApiFeedContent", "title", "content", "sourceLine", "video"),
        API_MEDIA("com.zhihu.android.api.model.template.api.ApiMediaContent", "title", "content"),
        API_TEXT("com.zhihu.android.api.model.template.api.ApiText", "panel_text", "action_url"),
        API_ACTION("com.zhihu.android.api.model.template.api.ApiAction", "intent_url"),
        API_LINE("com.zhihu.android.api.model.template.api.ApiLine", "elements", "secondElements"),
        API_ELEMENT("com.zhihu.android.api.model.template.api.ApiElement", "text"),
        TEMPLATE("com.zhihu.android.api.model.template.TemplateFeed", "unique", "content"),
        FEED("com.zhihu.android.api.model.template.FeedContent", "title", "content", "author", "videoInfo"),
        MEDIA("com.zhihu.android.api.model.template.MediaContent", "contents"),
        TEMPLATE_TEXT("com.zhihu.android.api.model.template.TemplateText", "text"),
        TEMPLATE_AUTHOR("com.zhihu.android.api.model.template.TemplateAuthor", "line1"),
        UNIQUE("com.zhihu.android.api.model.template.DataUnique", "type"),
        CARD("com.zhihu.android.ui.shared.sdui.model.Card", "elements", "extra"),
        EXTRA("com.zhihu.android.ui.shared.sdui.model.Card$Extra", "contentType"),
        ELEMENT("com.zhihu.android.ui.shared.sdui.model.Element", "id"),
        TEXT("com.zhihu.android.ui.shared.sdui.model.TextElement", "text", "richText"),
        LINE("com.zhihu.android.ui.shared.sdui.model.Line", "elements"),
        RICH("com.zhihu.android.ui.shared.sdui.model.RichText", "text", "elements"),
        AUTHOR("com.zhihu.android.ui.shared.sdui.model.Author", "fistLine"),
        COVER("com.zhihu.android.ui.shared.sdui.model.CoverContent", "title", "abstract", "top", "mid", "footer"),
        CONTENT("com.zhihu.android.ui.shared.sdui.model.Content", "leftElements", "rightElement"),
        AVATAR("com.zhihu.android.ui.shared.sdui.model.Avatar"),
        VIDEO("com.zhihu.android.ui.shared.sdui.model.Video"),
        MARKET("com.zhihu.android.api.model.MarketCard");

        final String host;
        final String[] fields;
        Schema(String host, String... fields) { this.host = host; this.fields = fields; }
    }

    private static final int MAX_DEPTH = 16;
    private final Map<Schema, Class<?>> types = new EnumMap<>(Schema.class);
    private final Map<Schema, Map<String, Field>> fields = new EnumMap<>(Schema.class);
    // MediaContent drops the distinction between its title row and body rows when parsed.
    // Keep semantic text, not a blocked decision, so editing a rule can re-filter existing rows.
    private final Map<IdentityKey, Details> parsedText = new java.util.HashMap<>();
    private final ReferenceQueue<Object> expiredText = new ReferenceQueue<>();
    private static final class IdentityKey extends WeakReference<Object> {
        private final int hash;
        IdentityKey(Object target, ReferenceQueue<Object> queue) { super(target, queue); hash = System.identityHashCode(target); }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) {
            return this == other || (other instanceof IdentityKey && get() != null && get() == ((IdentityKey) other).get());
        }
    }
    private void pruneText() {
        Object key;
        while ((key = expiredText.poll()) != null) parsedText.remove(key);
    }

    interface FieldLookup { Field find(Class<?> cls, String name) throws NoSuchFieldException; }
    public FeedKeywords(ClassLoader loader) throws ReflectiveOperationException {
        this(loader, FeedKeywords::field);
    }
    // Same-package JVM tests can map Kotlin's reserved-word field names to Java fixtures.
    FeedKeywords(ClassLoader loader, FieldLookup lookup) throws ReflectiveOperationException {
        for (Schema schema : Schema.values()) {
            Class<?> cls = loader.loadClass(schema.host);
            types.put(schema, cls);
            Map<String, Field> members = new java.util.HashMap<>();
            for (String name : schema.fields) members.put(name, lookup.find(cls, name));
            fields.put(schema, members);
        }
    }

    static Field field(Class<?> cls, String name) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(cls.getName() + "." + name);
    }

    private boolean is(Schema kind, Object value) { return types.get(kind).isInstance(value); }
    private Object get(Schema kind, Object value, String name) throws IllegalAccessException {
        return is(kind, value) ? fields.get(kind).get(name).get(value) : null;
    }
    private static String string(Object value) { return value instanceof CharSequence ? value.toString() : null; }
    private static Set<Object> seen() { return Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>()); }

    public static final class Details {
        public String type, title, author, content;
        public boolean video, market;
        private String fallbackTitle, fallbackAuthor;
        public boolean matches(Pattern titleRule, Pattern authorRule, Pattern contentRule) {
            return match(titleRule, title) || match(authorRule, author) || match(contentRule, content);
        }
        private static boolean match(Pattern rule, String value) {
            return rule != null && value != null && rule.matcher(value).find();
        }
    }

    public interface Matcher { boolean matches(Details details); }
    public List<?> filter(List<?> source, Matcher matcher) throws IllegalAccessException {
        ArrayList<Object> copy = null;
        for (int i = 0; i < source.size(); i++) {
            Object item = source.get(i);
            Details details = read(item);
            if (details != null && matcher.matches(details)) {
                if (copy == null) copy = new ArrayList<>(source.subList(0, i));
            } else if (copy != null) copy.add(item);
        }
        return copy == null ? source : copy;
    }

    public void rememberParsed(Object parsed, Object apiRoot) throws IllegalAccessException {
        if (!is(Schema.TEMPLATE, parsed) || !is(Schema.API_ROOT, apiRoot)) return;
        Details details = read(apiRoot);
        synchronized (parsedText) {
            pruneText();
            parsedText.put(new IdentityKey(parsed, expiredText), details);
        }
    }

    public Details read(Object value) throws IllegalAccessException {
        if (value == null) return null;
        if (is(Schema.TEMPLATE, value)) {
            synchronized (parsedText) {
                pruneText();
                Details saved = parsedText.get(new IdentityKey(value, null));
                if (saved != null) return saved;
            }
        }
        Details d = new Details();
        if (is(Schema.API_ROOT, value)) {
            d.type = string(get(Schema.UNIQUE, get(Schema.API_ROOT, value, "extra"), "type"));
            Object card = get(Schema.API_ROOT, value, "common_card");
            Object media = get(Schema.API_CARD, card, "media_card_content");
            if (media != null) {
                // Match the same branch that ApiTemplateRoot.parse() actually renders.
                d.title = text(get(Schema.API_MEDIA, media, "title"));
                d.content = text(get(Schema.API_MEDIA, media, "content"));
            } else {
                Object feed = get(Schema.API_CARD, card, "feed_content");
                d.title = text(get(Schema.API_FEED, feed, "title"));
                d.content = text(get(Schema.API_FEED, feed, "content"));
                d.video = get(Schema.API_FEED, feed, "video") != null;
                d.author = apiAuthor(get(Schema.API_FEED, feed, "sourceLine"));
            }
        } else if (is(Schema.TEMPLATE, value)) {
            d.type = string(get(Schema.UNIQUE, get(Schema.TEMPLATE, value, "unique"), "type"));
            Object feed = get(Schema.TEMPLATE, value, "content");
            if (is(Schema.FEED, feed)) {
                d.title = text(get(Schema.FEED, feed, "title"));
                d.content = text(get(Schema.FEED, feed, "content"));
                d.author = firstText(get(Schema.TEMPLATE_AUTHOR, get(Schema.FEED, feed, "author"), "line1"));
                d.video = get(Schema.FEED, feed, "videoInfo") != null;
            } else if (is(Schema.MEDIA, feed)) {
                // Without the source API metadata, never guess that the first body row is a title.
                d.content = text(get(Schema.MEDIA, feed, "contents"));
            }
        } else if (is(Schema.CARD, value)) {
            d.type = string(get(Schema.EXTRA, get(Schema.CARD, value, "extra"), "contentType"));
            walk(get(Schema.CARD, value, "elements"), d, true, 0, seen());
            if (d.title == null) d.title = d.fallbackTitle;
            if (d.author == null) d.author = d.fallbackAuthor;
        } else if (is(Schema.MARKET, value)) {
            d.market = true;
        } else return null;
        return d;
    }

    private String apiAuthor(Object line) throws IllegalAccessException {
        Object elements = get(Schema.API_LINE, line, "elements");
        if (elements instanceof List) {
            for (Object item : (List<?>) elements) {
                Object label = get(Schema.API_ELEMENT, item, "text");
                String url = string(get(Schema.API_ACTION, get(Schema.API_TEXT, label, "action_url"), "intent_url"));
                if (url != null && (url.startsWith("zhihu://people/") || url.contains("zhihu.com/people/")))
                    return text(label);
            }
        }
        // sourceLine is the input to the host's TemplateAuthor parser, not headline/footline.
        return firstText(elements);
    }

    private String firstText(Object values) throws IllegalAccessException {
        if (values instanceof Iterable) {
            for (Object item : (Iterable<?>) values) {
                String s = text(item);
                if (s != null && !s.isEmpty()) return s;
            }
            return null;
        }
        return text(values);
    }

    private String text(Object value) throws IllegalAccessException { return text(value, 0, seen()); }
    private String text(Object value, int depth, Set<Object> visited) throws IllegalAccessException {
        if (value == null || depth > MAX_DEPTH) return null;
        if (value instanceof CharSequence) return value.toString();
        if (!visited.add(value)) return null;
        if (is(Schema.API_TEXT, value)) return string(get(Schema.API_TEXT, value, "panel_text"));
        if (is(Schema.TEMPLATE_TEXT, value)) return string(get(Schema.TEMPLATE_TEXT, value, "text"));
        if (is(Schema.TEXT, value)) {
            String rich = string(get(Schema.TEXT, value, "richText"));
            return rich != null ? rich : string(get(Schema.TEXT, value, "text"));
        }
        if (is(Schema.API_ELEMENT, value)) return text(get(Schema.API_ELEMENT, value, "text"), depth + 1, visited);
        if (is(Schema.RICH, value)) {
            String primary = text(get(Schema.RICH, value, "text"), depth + 1, visited);
            String inline = text(get(Schema.RICH, value, "elements"), depth + 1, visited);
            return join(primary, inline, "");
        }
        if (is(Schema.LINE, value)) return text(get(Schema.LINE, value, "elements"), depth + 1, visited);
        if (is(Schema.API_LINE, value)) return join(text(get(Schema.API_LINE, value, "elements"), depth + 1, visited),
                text(get(Schema.API_LINE, value, "secondElements"), depth + 1, visited), "\n");
        if (value instanceof Iterable) {
            StringBuilder out = new StringBuilder();
            boolean found = false;
            for (Object item : (Iterable<?>) value) {
                String part = text(item, depth + 1, visited);
                if (part != null) {
                    if (found && (is(Schema.API_LINE, item) || item instanceof Iterable)) out.append('\n');
                    out.append(part);
                    found = true;
                }
            }
            return found ? out.toString() : null;
        }
        return null;
    }

    private static String join(String first, String second, String separator) {
        if (first == null) return second;
        return second == null ? first : first + separator + second;
    }

    private void walk(Object value, Details d, boolean legacyTitle, int depth, Set<Object> visited) throws IllegalAccessException {
        if (value == null || depth > MAX_DEPTH || !visited.add(value)) return;
        if (value instanceof Iterable) {
            for (Object item : (Iterable<?>) value) walk(item, d, legacyTitle, depth + 1, visited);
            return;
        }
        if (is(Schema.VIDEO, value)) { d.video = true; return; }
        if (is(Schema.AUTHOR, value)) {
            String name = firstText(get(Schema.LINE, get(Schema.AUTHOR, value, "fistLine"), "elements"));
            if (d.author == null) d.author = name;
            return; // Exclude biography/statistics in secondLine and badges/tailElement.
        }
        if (is(Schema.COVER, value)) {
            if (d.title == null) d.title = text(get(Schema.COVER, value, "title"));
            if (d.content == null) d.content = text(get(Schema.COVER, value, "abstract"));
            walk(get(Schema.COVER, value, "top"), d, false, depth + 1, visited);
            walk(get(Schema.COVER, value, "mid"), d, false, depth + 1, visited);
            walk(get(Schema.COVER, value, "footer"), d, false, depth + 1, visited);
            return;
        }
        String id = string(get(Schema.ELEMENT, value, "id"));
        boolean title = id != null && (id.equals("title") || (id.endsWith("_title") && !id.endsWith("sub_title")));
        boolean summary = id != null && (id.equals("summary") || id.equals("abstract") || id.endsWith("_summary"));
        if (title || summary || (legacyTitle && "Text".equals(id))) {
            String s = text(value);
            if (title && d.title == null) d.title = s;
            else if (summary && d.content == null) d.content = s;
            else if (legacyTitle && "Text".equals(id) && d.fallbackTitle == null) d.fallbackTitle = s;
            if (s != null) return;
        }
        if (is(Schema.LINE, value)) {
            Object children = get(Schema.LINE, value, "elements");
            if (legacyTitle && "0".equals(id) && children instanceof List && !((List<?>) children).isEmpty()
                    && is(Schema.AVATAR, ((List<?>) children).get(0))) {
                d.fallbackAuthor = firstText(children);
                return;
            }
            walk(children, d, false, depth + 1, visited);
        } else if (is(Schema.CONTENT, value)) {
            walk(get(Schema.CONTENT, value, "leftElements"), d, legacyTitle, depth + 1, visited);
            walk(get(Schema.CONTENT, value, "rightElement"), d, false, depth + 1, visited);
        } else if (is(Schema.RICH, value)) {
            walk(get(Schema.RICH, value, "text"), d, false, depth + 1, visited);
            walk(get(Schema.RICH, value, "elements"), d, false, depth + 1, visited);
        }
        // No traversal of actions, extra/infoMap, image captions, nested cards or back-references.
    }
}
