package com.ecommerce.crawler;

/**
 * Strategy interface for retrieving page content from URLs.
 */
public interface PageFetcher {

    /**
     * Fetches raw content from the given URL.
     *
     * @param url target URL
     * @return raw page content
     * @throws Exception on fetch error or timeout
     */
    PageContent fetch(String url) throws Exception;
}
