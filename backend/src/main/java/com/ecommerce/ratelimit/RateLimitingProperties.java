package com.ecommerce.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for distributed Redis rate limiting.
 */
@Component
@ConfigurationProperties(prefix = "app.rate-limiting")
public class RateLimitingProperties {

    public enum FallbackPolicy {
        FAIL_OPEN,
        FAIL_CLOSED
    }

    private boolean enabled = true;
    private boolean trustProxy = false;
    private FallbackPolicy fallbackPolicy = FallbackPolicy.FAIL_OPEN;
    private Map<String, RuleConfig> rules = new HashMap<>();

    public RateLimitingProperties() {
        // Initialize default rules
        rules.put("auth", new RuleConfig(10, 60));       // 10 requests / 60 seconds
        rules.put("orders", new RuleConfig(15, 60));     // 15 requests / 60 seconds
        rules.put("api-default", new RuleConfig(60, 60)); // 60 requests / 60 seconds
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isTrustProxy() {
        return trustProxy;
    }

    public void setTrustProxy(boolean trustProxy) {
        this.trustProxy = trustProxy;
    }

    public FallbackPolicy getFallbackPolicy() {
        return fallbackPolicy;
    }

    public void setFallbackPolicy(FallbackPolicy fallbackPolicy) {
        this.fallbackPolicy = fallbackPolicy;
    }

    public Map<String, RuleConfig> getRules() {
        return rules;
    }

    public void setRules(Map<String, RuleConfig> rules) {
        this.rules = rules;
    }

    public RuleConfig getRuleForCategory(String category) {
        RuleConfig rule = rules.get(category);
        if (rule == null) {
            rule = rules.get("api-default");
        }
        if (rule == null) {
            rule = new RuleConfig(60, 60);
        }
        return rule;
    }

    public static class RuleConfig {
        private int limit = 60;
        private int windowSeconds = 60;

        public RuleConfig() {
        }

        public RuleConfig(int limit, int windowSeconds) {
            this.limit = limit;
            this.windowSeconds = windowSeconds;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }
}
