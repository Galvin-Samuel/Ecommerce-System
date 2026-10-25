package com.ecommerce.crawler;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "crawler_results")
@CompoundIndexes({
        @CompoundIndex(name = "jobId_url_idx", def = "{'jobId': 1, 'url': 1}", unique = true)
})
public class CrawlResult {

    @Id
    private String id;

    @Indexed
    private String jobId;

    private String url;
    private int depth;
    private String title;
    private String canonicalUrl;
    private String summary;
    private List<String> outgoingLinks = new ArrayList<>();
    private int statusCode;
    private String contentType;
    private Instant crawledAt;

    public CrawlResult() {
    }

    public CrawlResult(String jobId, String url, int depth, String title, String canonicalUrl,
                       String summary, List<String> outgoingLinks, int statusCode, String contentType, Instant crawledAt) {
        this.jobId = jobId;
        this.url = url;
        this.depth = depth;
        this.title = title;
        this.canonicalUrl = canonicalUrl;
        this.summary = summary;
        this.outgoingLinks = outgoingLinks != null ? outgoingLinks : new ArrayList<>();
        this.statusCode = statusCode;
        this.contentType = contentType;
        this.crawledAt = crawledAt != null ? crawledAt : Instant.now();
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    public void setCanonicalUrl(String canonicalUrl) {
        this.canonicalUrl = canonicalUrl;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getOutgoingLinks() {
        return outgoingLinks;
    }

    public void setOutgoingLinks(List<String> outgoingLinks) {
        this.outgoingLinks = outgoingLinks;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Instant getCrawledAt() {
        return crawledAt;
    }

    public void setCrawledAt(Instant crawledAt) {
        this.crawledAt = crawledAt;
    }
}
