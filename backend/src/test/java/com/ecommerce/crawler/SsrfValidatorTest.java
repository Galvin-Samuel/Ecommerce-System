package com.ecommerce.crawler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SsrfValidatorTest {

    private final SsrfValidator validator = new SsrfValidator();

    @Test
    @DisplayName("Should block loopback and private IP addresses")
    void testBlockPrivateAndLoopback() {
        assertThat(validator.isSafe(URI.create("http://127.0.0.1:8080/admin"), null)).isFalse();
        assertThat(validator.isSafe(URI.create("http://localhost:8080/secret"), null)).isFalse();
        assertThat(validator.isSafe(URI.create("http://192.168.1.1/router"), null)).isFalse();
        assertThat(validator.isSafe(URI.create("http://10.0.0.1/internal"), null)).isFalse();
    }

    @Test
    @DisplayName("Should block non-http schemes")
    void testBlockNonHttpSchemes() {
        assertThat(validator.isSafe(URI.create("file:///etc/passwd"), null)).isFalse();
        assertThat(validator.isSafe(URI.create("ftp://ftp.example.com"), null)).isFalse();
    }

    @Test
    @DisplayName("Should enforce allowed domain policy")
    void testEnforceAllowedDomains() {
        Set<String> allowed = Set.of("shop.example.com", "partner.org");

        // Allowed host and subdomains
        assertThat(validator.isSafe(URI.create("https://shop.example.com/item/1"), allowed)).isTrue();
        assertThat(validator.isSafe(URI.create("https://deal.shop.example.com/item/1"), allowed)).isTrue();
        assertThat(validator.isSafe(URI.create("https://partner.org/catalog"), allowed)).isTrue();

        // Disallowed hosts
        assertThat(validator.isSafe(URI.create("https://attacker.com/malicious"), allowed)).isFalse();
        assertThat(validator.isSafe(URI.create("https://example.com/other"), allowed)).isFalse();
    }
}
