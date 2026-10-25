package com.ecommerce.competitor.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class CreateSourceRequest {

    @NotBlank(message = "Source name is required")
    private String name;

    @NotBlank(message = "Allowed domain is required")
    private String allowedDomain;

    @NotBlank(message = "Price CSS selector is required")
    private String priceCssSelector;

    private String currency = "USD";

    @Min(value = 15, message = "Polling interval must be at least 15 minutes")
    private int pollingIntervalMinutes = 60;

    public CreateSourceRequest() {
    }

    public CreateSourceRequest(String name, String allowedDomain, String priceCssSelector, String currency, int pollingIntervalMinutes) {
        this.name = name;
        this.allowedDomain = allowedDomain;
        this.priceCssSelector = priceCssSelector;
        this.currency = currency;
        this.pollingIntervalMinutes = pollingIntervalMinutes;
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

    public int getPollingIntervalMinutes() {
        return pollingIntervalMinutes;
    }

    public void setPollingIntervalMinutes(int pollingIntervalMinutes) {
        this.pollingIntervalMinutes = pollingIntervalMinutes;
    }
}
