package com.ecommerce.crawler;

public record PageContent(
        String url,
        int statusCode,
        String contentType,
        String htmlBody
) {
}
