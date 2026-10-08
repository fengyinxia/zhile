package com.shatyuka.zhiliao;

import org.junit.Before;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import static org.junit.Assert.*;

/** Pure-JVM regression tests; fixtures mirror the inspected arm64 field schema, not real user data. */
public class FeedKeywordsTest {
    public static class ApiRoot { Object extra, common_card; }
    public static class ApiCard { Object feed_content, media_card_content; }
    public static class ApiFeed { Object title, content, sourceLine, video; }
    public static class ApiMedia { Object title, content; }
    public static class ApiText { String panel_text; Object action_url; ApiText(String s) { panel_text = s; } }
    public static class ApiAction { String intent_url; }
    public static class ApiLine { Object elements, secondElements; }
    public static class ApiElement { Object text; ApiElement(Object v) { text = v; } }
    public static class Template { Object unique, content; }
    public static class Feed { Object title, content, author, videoInfo; }
    public static class Media { Object contents; }
    public static class TemplateText { String text; TemplateText(String s) { text = s; } }
    public static class TemplateAuthor { Object line1; }
    public static class Unique { String type; Unique(String s) { type = s; } }
    public static class Element { String id = "0"; Object ignoredBackReference; }
    public static class Card extends Element { Object elements, extra; }
    public static class Extra { String contentType; String ignoredTitle; }
    public static class Text extends Element { String text; CharSequence richText; Text(String s, String key) { text = s; id = key; } }
    public static class Line extends Element { Object elements; }
    public static class Rich extends Element { Object text, elements; }
    public static class Author extends Element { Object fistLine; Object ignoredSecondLine; }
    public static class Cover extends Element {
        // Java cannot declare Kotlin's reserved-word field "abstract"; the fixture resolver maps it.
        Object title, snippet_, top, mid, footer;
    }
    public static class Content extends Element { Object leftElements, rightElement; }
    public static class Avatar extends Element { }
    public static class Video extends Element { }
    public static class Market { }
    public static class DerivedCard extends Card { }
    public static class EqualTemplate extends Template {
        @Override public int hashCode() { return 7; }
        @Override public boolean equals(Object other) { return other instanceof EqualTemplate; }
    }

    private FeedKeywords filter;
    @Before public void setup() throws Exception {
        final Class<?>[] fixtures = {ApiRoot.class, ApiCard.class, ApiFeed.class, ApiMedia.class,
                ApiText.class, ApiAction.class, ApiLine.class, ApiElement.class, Template.class,
                Feed.class, Media.class, TemplateText.class, TemplateAuthor.class, Unique.class,
                Card.class, Extra.class, Element.class, Text.class, Line.class, Rich.class,
                Author.class, Cover.class, Content.class, Avatar.class, Video.class, Market.class};
        assertEquals(FeedKeywords.Schema.values().length, fixtures.length);
        filter = new FeedKeywords(new ClassLoader(getClass().getClassLoader()) {
            @Override public Class<?> loadClass(String name) throws ClassNotFoundException {
                for (FeedKeywords.Schema s : FeedKeywords.Schema.values())
                    if (name.equals(s.host)) return fixtures[s.ordinal()];
                return super.loadClass(name);
            }
        }, (cls, name) -> FeedKeywords.field(cls, cls == Cover.class && name.equals("abstract") ? "snippet_" : name));
    }
    private ApiRoot api(Object feed, Object media) {
        ApiCard c = new ApiCard(); c.feed_content = feed; c.media_card_content = media;
        ApiRoot r = new ApiRoot(); r.common_card = c; r.extra = new Unique("answer"); return r;
    }
    private Card card(Object... children) { Card c = new Card(); c.elements = Arrays.asList(children); return c; }
    private boolean title(Object value, String pattern) throws Exception {
        return filter.read(value).matches(Pattern.compile(pattern), null, null);
    }

    @Test public void ordinaryApiFieldsStillMatch() throws Exception {
        ApiFeed f = new ApiFeed(); f.title = new ApiText("ChatGPT title"); f.content = new ApiText("body word");
        assertTrue(title(api(f, null), "ChatGPT"));
        assertTrue(filter.read(api(f, null)).matches(null, null, Pattern.compile("body word")));
    }
    @Test public void mediaTitleAndBodyAreSeparate() throws Exception {
        ApiMedia m = new ApiMedia(); m.title = new ApiText("media headline");
        m.content = Arrays.asList(new ApiText("body keyword"));
        assertTrue(title(api(null, m), "headline"));
        assertFalse(title(api(null, m), "body keyword"));
        assertTrue(filter.read(api(null, m)).matches(null, null, Pattern.compile("body keyword")));
    }
    @Test public void renderedMediaWinsOverUnusedLegacyTitle() throws Exception {
        ApiFeed f = new ApiFeed(); f.title = new ApiText("blocked unused field");
        ApiMedia m = new ApiMedia(); m.title = new ApiText("visible title");
        assertFalse(title(api(f, m), "blocked"));
        assertTrue(title(api(f, m), "visible"));
    }
    @Test public void parsedMediaRetainsSourceTitleAcrossRuleChanges() throws Exception {
        ApiMedia m = new ApiMedia(); m.title = new ApiText("ChatGPT");
        Template t = new Template(); t.content = new Media(); t.unique = new Unique("answer");
        filter.rememberParsed(t, api(null, m));
        assertFalse(title(t, "unrelated"));
        assertTrue(title(t, "ChatGPT"));
    }
    @Test public void metadataIsKeyedByInstanceNotModelEquality() throws Exception {
        Template a = new EqualTemplate(), b = new EqualTemplate();
        ApiMedia first = new ApiMedia(), second = new ApiMedia(); first.title = new ApiText("first"); second.title = new ApiText("second");
        filter.rememberParsed(a, api(null, first)); filter.rememberParsed(b, api(null, second));
        assertEquals("first", filter.read(a).title); assertEquals("second", filter.read(b).title);
    }
    @Test public void titlelessMediaDoesNotGuessBodyAsTitle() throws Exception {
        ApiMedia m = new ApiMedia(); m.content = Arrays.asList(new ApiText("ChatGPT body"));
        Template t = new Template(); Media parsed = new Media();
        parsed.contents = Arrays.asList(Arrays.asList(new TemplateText("ChatGPT body"))); t.content = parsed;
        filter.rememberParsed(t, api(null, m));
        assertFalse(title(t, "ChatGPT"));
        assertTrue(filter.read(t).matches(null, null, Pattern.compile("ChatGPT")));
    }
    @Test public void parsedOrdinaryTemplateHasSemanticAuthorAndTitle() throws Exception {
        Feed f = new Feed(); f.title = new TemplateText("native title");
        TemplateAuthor a = new TemplateAuthor(); a.line1 = Arrays.asList(new TemplateText("Alice"), new TemplateText("123 likes")); f.author = a;
        Template t = new Template(); t.unique = new Unique("answer"); t.content = f;
        assertTrue(title(t, "native"));
        assertEquals("Alice", filter.read(t).author);
    }
    @Test public void coverTitleAndAbstractAreReadNotFooterStatistics() throws Exception {
        Cover c = new Cover(); c.title = new Text("headline", "0"); c.snippet_ = new Text("summary", "0");
        c.footer = new Text("1000 likes", "Text");
        FeedKeywords.Details d = filter.read(card(c));
        assertEquals("headline", d.title); assertEquals("summary", d.content);
        assertFalse(d.matches(Pattern.compile("likes"), Pattern.compile("likes"), null));
    }
    @Test public void richTitleRunsAreJoinedAndDuplicateReferenceIgnored() throws Exception {
        Rich r = new Rich(); r.id = "question_title"; Text part = new Text("Chat", "0");
        r.text = part; r.elements = Arrays.asList(part, new Text("GPT", "0"));
        assertTrue(title(card(r), "ChatGPT"));
        assertEquals("ChatGPT", filter.read(card(r)).title);
    }
    @Test public void nestedContentAndRichSummaryAreSupported() throws Exception {
        Rich r = new Rich(); r.id = "answer_summary"; r.elements = Arrays.asList(new Text("body", "0"), new Text(" word", "0"));
        Cover cover = new Cover(); cover.title = new Text("nested title", "0");
        Content box = new Content(); box.leftElements = Arrays.asList(cover, r);
        FeedKeywords.Details d = filter.read(card(box));
        assertEquals("nested title", d.title); assertEquals("body word", d.content);
    }
    @Test public void explicitAuthorIgnoresBiographyAndCounters() throws Exception {
        Author a = new Author(); Line line = new Line(); line.elements = Arrays.asList(new Text("Alice", "0"), new Text("123 likes", "0"));
        a.fistLine = line; a.ignoredSecondLine = new Text("biography", "0");
        FeedKeywords.Details d = filter.read(card(a, new Text("headline", "Text")));
        assertEquals("Alice", d.author); assertEquals("headline", d.title);
        assertFalse(d.matches(null, Pattern.compile("likes|biography"), null));
    }
    @Test public void apiPeopleLinkIsPreferredToSourceStatistics() throws Exception {
        ApiText stats = new ApiText("123 likes"), name = new ApiText("Alice");
        ApiAction link = new ApiAction(); link.intent_url = "zhihu://people/example"; name.action_url = link;
        ApiLine line = new ApiLine(); line.elements = Arrays.asList(new ApiElement(stats), new ApiElement(name));
        ApiFeed f = new ApiFeed(); f.sourceLine = line;
        assertEquals("Alice", filter.read(api(f, null)).author);
    }
    @Test public void legacyAvatarLineAndSummaryRemainSupported() throws Exception {
        Line a = new Line(); a.elements = Arrays.asList(new Avatar(), new Text("Alice", "0"));
        FeedKeywords.Details d = filter.read(card(new Text("old title", "Text"), a, new Text("old body", "x_summary")));
        assertEquals("old title", d.title); assertEquals("Alice", d.author); assertEquals("old body", d.content);
    }
    @Test public void decodedRichTextIsPreferredToMarkupSource() throws Exception {
        Text t = new Text("raw markup", "title"); t.richText = "visible title";
        assertTrue(title(card(t), "visible")); assertFalse(title(card(t), "markup"));
    }
    @Test public void englishRemainsCaseSensitiveUnlessRequested() throws Exception {
        Card c = card(new Text("ChatGPT", "Text"));
        assertFalse(title(c, "chatgpt")); assertTrue(title(c, "(?i)chatgpt"));
    }
    @Test public void missingFieldDoesNotMatchEmptyRegexButActualEmptyTextDoes() throws Exception {
        assertFalse(title(card(), "^$"));
        assertTrue(title(card(new Text("", "Text")), "^$"));
    }
    @Test public void mediaRowsDoNotJoinUnrelatedWordsAcrossNewlines() throws Exception {
        ApiLine a = new ApiLine(), b = new ApiLine();
        a.elements = Arrays.asList(new ApiElement(new ApiText("foo"))); b.elements = Arrays.asList(new ApiElement(new ApiText("bar")));
        ApiMedia m = new ApiMedia(); m.content = Arrays.asList(a, b);
        assertEquals("foo\nbar", filter.read(api(null, m)).content);
        assertFalse(filter.read(api(null, m)).matches(null, null, Pattern.compile("foobar")));
    }
    @Test public void cyclesAndUnknownMetadataAreNotScanned() throws Exception {
        Content c = new Content(); c.leftElements = Collections.emptyList(); c.rightElement = c;
        Card root = card(c); Extra e = new Extra(); e.ignoredTitle = "blocked"; root.extra = e; root.ignoredBackReference = root;
        assertFalse(title(root, "blocked")); assertNull(filter.read(new Object()));
    }
    @Test public void freshRulesFilterExistingListWithoutMutatingInputOrDroppingUnknownRows() throws Exception {
        final Card blocked = card(new Text("ChatGPT", "Text"));
        Object header = new Object(), footer = new Object();
        List<?> input = Arrays.asList(header, blocked, footer);
        assertSame(input, filter.filter(input, d -> d.matches(Pattern.compile("unrelated"), null, null)));
        List<?> output = filter.filter(input, d -> d.matches(Pattern.compile("ChatGPT"), null, null));
        assertEquals(Arrays.asList(header, footer), output); assertEquals(3, input.size());
    }
    @Test public void subclassesAndNestedVideoKeepExistingModelSemantics() throws Exception {
        DerivedCard c = new DerivedCard(); Cover cover = new Cover(); cover.title = new Text("headline", "0"); cover.mid = new Video(); c.elements = Arrays.asList(cover);
        assertTrue(title(c, "headline")); assertTrue(filter.read(c).video);
        assertTrue(filter.read(new Market()).market);
    }
}
