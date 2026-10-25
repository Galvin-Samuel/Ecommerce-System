package com.ecommerce.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Standard HTTP page fetcher using Java 11+ HttpClient with timeouts and response size caps.
 */
@Component
public class HttpPageFetcher implements PageFetcher {

    private static final Logger log = LoggerFactory.getLogger(HttpPageFetcher.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 5;
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024; // 1 MB cap

    private final HttpClient httpClient;

    public HttpPageFetcher() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NEVER) // Manually handled to prevent SSRF bypass
                .build();
    }

    @Override
    public PageContent fetch(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "EcommerceCrawler/1.0 (+https://ecommerce.example.com/bot)")
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();
        String contentType = response.headers().firstValue("Content-Type").orElse("text/html");
        String body = response.body();

        if (body != null && body.length() > MAX_RESPONSE_BYTES) {
            log.warn("Truncating oversized response from '{}' ({} chars)", url, body.length());
            body = body.substring(0, MAX_RESPONSE_BYTES);
        }

        return new PageContent(url, statusCode, contentType, body != null ? body : "");
    }
}
