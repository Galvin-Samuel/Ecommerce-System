package com.ecommerce.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Robust HTML parser utilizing Jsoup for extracting metadata, summaries, and links.
 */
public class HtmlPageParser {

    private static final int MAX_SUMMARY_LENGTH = 500;

    public ParsedPage parse(String html, String baseUri) {
        if (html == null || html.trim().isEmpty()) {
            return new ParsedPage("", null, "", List.of());
        }

        Document doc = Jsoup.parse(html, baseUri != null ? baseUri : "");

        // 1. Title
        String title = doc.title();
        if (title == null) {
            title = "";
        }

        // 2. Canonical URL
        String canonicalUrl = null;
        Element canonicalEl = doc.selectFirst("link[rel=canonical]");
        if (canonicalEl != null && canonicalEl.hasAttr("href")) {
            canonicalUrl = canonicalEl.attr("abs:href");
        }

        // 3. Text summary
        String bodyText = doc.body() != null ? doc.body().text() : "";
        String summary = bodyText.length() > MAX_SUMMARY_LENGTH
                ? bodyText.substring(0, MAX_SUMMARY_LENGTH) + "..."
                : bodyText;

        // 4. Outgoing links
        Set<String> links = new LinkedHashSet<>();
        Elements anchorElements = doc.select("a[href]");
        for (Element anchor : anchorElements) {
            String href = anchor.attr("href");
            if (href.startsWith("javascript:") || href.startsWith("mailto:") || href.startsWith("#")) {
                continue;
            }
            String resolved = UrlNormalizer.resolveAndNormalize(baseUri, href);
            if (resolved != null) {
                links.add(resolved);
            }
        }

        return new ParsedPage(title, canonicalUrl, summary, new ArrayList<>(links));
    }
}
