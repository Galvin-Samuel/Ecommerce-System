package com.ecommerce.crawler;

public record CrawlTask(
        String url,
        int depth
) {
}
