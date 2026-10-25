package com.ecommerce.crawler;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Document(collection = "crawler_jobs")
public class CrawlJob {

    @Id
    private String id;

    @Indexed(unique = true)
    private String jobId;

    private List<String> seedUrls = new ArrayList<>();
    private Set<String> allowedDomains = new HashSet<>();

    private int maxPages = 50;
    private int maxDepth = 2;
    private long requestDelayMs = 50;

    @Indexed
    private CrawlJobStatus status = CrawlJobStatus.SUBMITTED;

    private Instant startTime;
    private Instant completionTime;

    private int pagesProcessed = 0;
    private int pagesFailed = 0;
    private String errorMessage;

    @Indexed
    private String createdByUserId;

    public CrawlJob() {
    }

    public CrawlJob(String jobId, List<String> seedUrls, Set<String> allowedDomains, int maxPages, int maxDepth, String createdByUserId) {
        this.jobId = jobId;
        this.seedUrls = seedUrls;
        this.allowedDomains = allowedDomains != null ? allowedDomains : new HashSet<>();
        this.maxPages = maxPages > 0 ? maxPages : 50;
        this.maxDepth = maxDepth > 0 ? maxDepth : 2;
        this.createdByUserId = createdByUserId;
        this.status = CrawlJobStatus.SUBMITTED;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
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

    public CrawlJobStatus getStatus() {
        return status;
    }

    public void setStatus(CrawlJobStatus status) {
        this.status = status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(Instant completionTime) {
        this.completionTime = completionTime;
    }

    public int getPagesProcessed() {
        return pagesProcessed;
    }

    public void setPagesProcessed(int pagesProcessed) {
        this.pagesProcessed = pagesProcessed;
    }

    public int getPagesFailed() {
        return pagesFailed;
    }

    public void setPagesFailed(int pagesFailed) {
        this.pagesFailed = pagesFailed;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }
}
