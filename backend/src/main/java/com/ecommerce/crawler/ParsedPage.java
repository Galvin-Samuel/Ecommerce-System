package com.ecommerce.crawler;

import java.util.List;

public record ParsedPage(
        String title,
        String canonicalUrl,
        String summary,
        List<String> outgoingLinks
) {
}
