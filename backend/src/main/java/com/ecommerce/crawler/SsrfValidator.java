package com.ecommerce.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * Defensive URL and SSRF validator that blocks loopback, private, and link-local destinations.
 */
public class SsrfValidator {

    private static final Logger log = LoggerFactory.getLogger(SsrfValidator.class);

    private final boolean allowPrivateNetworks;

    public SsrfValidator() {
        this(false);
    }

    public SsrfValidator(boolean allowPrivateNetworks) {
        this.allowPrivateNetworks = allowPrivateNetworks;
    }

    /**
     * Validates that a URI is safe to crawl.
     *
     * @param uri            candidate URI
     * @param allowedDomains optional set of allowed domains (if non-empty, host must match or end with domain)
     * @return true if valid and safe to crawl
     */
    public boolean isSafe(URI uri, Set<String> allowedDomains) {
        if (uri == null) {
            return false;
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            log.warn("Rejected URL with disallowed scheme: {}", scheme);
            return false;
        }

        String host = uri.getHost();
        if (host == null || host.trim().isEmpty()) {
            return false;
        }

        host = host.toLowerCase().trim();

        // Enforce allowed domains if configured
        if (allowedDomains != null && !allowedDomains.isEmpty()) {
            boolean matched = false;
            for (String domain : allowedDomains) {
                String cleanDomain = domain.toLowerCase().trim();
                if (host.equals(cleanDomain) || host.endsWith("." + cleanDomain)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                log.warn("Host '{}' does not match allowed domains: {}", host, allowedDomains);
                return false;
            }
        }

        if (allowPrivateNetworks) {
            return true;
        }

        // Prevent SSRF: block localhost, private, and link-local IPs
        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                    || address.isAnyLocalAddress() || address.isMulticastAddress()) {
                log.warn("SSRF attempt blocked for host '{}' resolving to internal IP '{}'", host, address.getHostAddress());
                return false;
            }
        } catch (UnknownHostException e) {
            log.debug("Host '{}' cannot be resolved yet: {}", host, e.getMessage());
        }

        return true;
    }
}
