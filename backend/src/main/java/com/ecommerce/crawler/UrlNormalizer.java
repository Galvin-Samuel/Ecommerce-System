package com.ecommerce.crawler;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Normalizes URLs for accurate deduplication in the crawler frontier.
 */
public final class UrlNormalizer {

    private UrlNormalizer() {
    }

    /**
     * Normalizes a URL string by lowercasing scheme/host, resolving default ports, and stripping fragments.
     */
    public static String normalize(String urlStr) {
        if (urlStr == null || urlStr.trim().isEmpty()) {
            return null;
        }

        try {
            URI uri = new URI(urlStr.trim());
            return normalize(uri);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Resolves a potentially relative link against a base URI and returns the normalized absolute URL.
     */
    public static String resolveAndNormalize(String baseUriStr, String relativeOrAbsoluteLink) {
        if (relativeOrAbsoluteLink == null || relativeOrAbsoluteLink.trim().isEmpty()) {
            return null;
        }

        try {
            URI baseUri = new URI(baseUriStr.trim());
            URI resolved = baseUri.resolve(relativeOrAbsoluteLink.trim());
            return normalize(resolved);
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalize(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null) {
            return null;
        }
        scheme = scheme.toLowerCase();

        String host = uri.getHost();
        if (host == null) {
            return null;
        }
        host = host.toLowerCase();

        int port = uri.getPort();
        if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
            port = -1; // Strip default port
        }

        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }

        String query = uri.getQuery(); // Keep query params intact
        try {
            URI clean = new URI(scheme, uri.getUserInfo(), host, port, path, query, null);
            return clean.toASCIIString();
        } catch (URISyntaxException e) {
            return uri.toString();
        }
    }
}
