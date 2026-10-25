package com.ecommerce.crawler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlNormalizerTest {

    @Test
    @DisplayName("Should normalize host, scheme, and default ports while retaining query params")
    void testNormalizeUrl() {
        String raw = "HTTP://EXAMPLE.COM:80/shop/items?category=books&sort=price#reviews";
        String normalized = UrlNormalizer.normalize(raw);

        assertThat(normalized).isEqualTo("http://example.com/shop/items?category=books&sort=price");
    }

    @Test
    @DisplayName("Should strip default port 443 for https")
    void testStripDefaultHttpsPort() {
        String raw = "HTTPS://Shop.Example.com:443/cart";
        String normalized = UrlNormalizer.normalize(raw);

        assertThat(normalized).isEqualTo("https://shop.example.com/cart");
    }

    @Test
    @DisplayName("Should resolve relative links against parent base URL")
    void testResolveRelativeLinks() {
        String base = "https://example.com/products/electronics/index.html";

        assertThat(UrlNormalizer.resolveAndNormalize(base, "/about"))
                .isEqualTo("https://example.com/about");

        assertThat(UrlNormalizer.resolveAndNormalize(base, "laptops/deal.html"))
                .isEqualTo("https://example.com/products/electronics/laptops/deal.html");

        assertThat(UrlNormalizer.resolveAndNormalize(base, "../home.html"))
                .isEqualTo("https://example.com/products/home.html");
    }
}
