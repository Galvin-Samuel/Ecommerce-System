package com.ecommerce.competitor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "competitor_sources")
public class CompetitorSource {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    @Indexed
    private String allowedDomain;

    private String priceCssSelector;
    private String currency = "USD";
    private boolean enabled = true;
    private int pollingIntervalMinutes = 60;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public CompetitorSource() {
    }

    public CompetitorSource(String id, String name, String allowedDomain, String priceCssSelector, String currency, int pollingIntervalMinutes) {
        this.id = id;
        this.name = name;
        this.allowedDomain = allowedDomain;
        this.priceCssSelector = priceCssSelector;
        this.currency = currency != null ? currency.toUpperCase() : "USD";
        this.pollingIntervalMinutes = Math.max(15, pollingIntervalMinutes);
        this.enabled = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAllowedDomain() {
        return allowedDomain;
    }

    public void setAllowedDomain(String allowedDomain) {
        this.allowedDomain = allowedDomain;
    }

    public String getPriceCssSelector() {
        return priceCssSelector;
    }

    public void setPriceCssSelector(String priceCssSelector) {
        this.priceCssSelector = priceCssSelector;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPollingIntervalMinutes() {
        return pollingIntervalMinutes;
    }

    public void setPollingIntervalMinutes(int pollingIntervalMinutes) {
        this.pollingIntervalMinutes = pollingIntervalMinutes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
