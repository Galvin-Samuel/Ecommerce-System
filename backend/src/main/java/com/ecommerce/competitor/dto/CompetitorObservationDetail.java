package com.ecommerce.competitor.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CompetitorObservationDetail(
        String sourceId,
        String sourceName,
        BigDecimal competitorPrice,
        String currency,
        BigDecimal priceDifference,
        Double percentageDifference,
        String status,
        boolean isFresh,
        Instant observedAt,
        String competitorUrl
) {
}
