package com.ecommerce.crawler.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Set;

public class SubmitCrawlJobRequest {

    @NotEmpty(message = "At least one seed URL must be provided")
    private List<String> seedUrls;

    private Set<String> allowedDomains;

    @Min(value = 1, message = "Max pages must be at least 1")
    @Max(value = 500, message = "Max pages cannot exceed 500")
    private int maxPages = 50;

    @Min(value = 0, message = "Max depth must be at least 0")
    @Max(value = 5, message = "Max depth cannot exceed 5")
    private int maxDepth = 2;

    @Min(value = 0, message = "Request delay cannot be negative")
    @Max(value = 5000, message = "Request delay cannot exceed 5000 ms")
    private long requestDelayMs = 50;

    public SubmitCrawlJobRequest() {
    }

    public SubmitCrawlJobRequest(List<String> seedUrls, Set<String> allowedDomains, int maxPages, int maxDepth, long requestDelayMs) {
        this.seedUrls = seedUrls;
        this.allowedDomains = allowedDomains;
        this.maxPages = maxPages;
        this.maxDepth = maxDepth;
        this.requestDelayMs = requestDelayMs;
    }

    public List<String> getSeedUrls() {
        return seedUrls;
    }

    public void setSeedUrls(List<String> seedUrls) {
        this.seedUrls = seedUrls;
    }

    public Set<String> getAllowedDomains() {
        return allowedDomains;
    }

    public void setAllowedDomains(Set<String> allowedDomains) {
        this.allowedDomains = allowedDomains;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public long getRequestDelayMs() {
        return requestDelayMs;
    }

    public void setRequestDelayMs(long requestDelayMs) {
        this.requestDelayMs = requestDelayMs;
    }
}
