package com.ecommerce.crawler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlPageParserTest {

    private final HtmlPageParser parser = new HtmlPageParser();

    @Test
    @DisplayName("Should extract title, canonical URL, text summary, and outgoing links from HTML")
    void testParseHtml() {
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Summer Sale 2026 - Best Deals</title>
                    <link rel="canonical" href="https://example.com/summer-sale" />
                </head>
                <body>
                    <header><h1>Welcome to our Store</h1></header>
                    <p>Discover our premier selection of electronics, clothing, and home accessories at half price!</p>
                    <nav>
                        <a href="/products/laptops">Laptops</a>
                        <a href="https://example.com/products/phones">Smartphones</a>
                        <a href="javascript:void(0)">Do Nothing</a>
                        <a href="#section2">Jump</a>
                    </nav>
                </body>
                </html>
                """;

        ParsedPage parsed = parser.parse(html, "https://example.com/summer-sale");

        assertThat(parsed.title()).isEqualTo("Summer Sale 2026 - Best Deals");
        assertThat(parsed.canonicalUrl()).isEqualTo("https://example.com/summer-sale");
        assertThat(parsed.summary()).contains("Discover our premier selection");
        assertThat(parsed.outgoingLinks()).containsExactlyInAnyOrder(
                "https://example.com/products/laptops",
                "https://example.com/products/phones"
        );
    }

    @Test
    @DisplayName("Should handle malformed or empty HTML gracefully")
    void testMalformedHtml() {
        ParsedPage empty = parser.parse("", "https://example.com");
        assertThat(empty.title()).isEmpty();
        assertThat(empty.outgoingLinks()).isEmpty();

        ParsedPage malformed = parser.parse("<div><p>Unclosed paragraph <a href='/page'>Next", "https://example.com");
        assertThat(malformed.summary()).contains("Unclosed paragraph Next");
        assertThat(malformed.outgoingLinks()).contains("https://example.com/page");
    }
}
