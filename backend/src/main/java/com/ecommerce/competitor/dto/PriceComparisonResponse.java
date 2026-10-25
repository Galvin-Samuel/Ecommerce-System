package com.ecommerce.competitor.dto;

import java.math.BigDecimal;
import java.util.List;

public record PriceComparisonResponse(
        String productId,
        String productName,
        BigDecimal localPrice,
        String currency,
        List<CompetitorObservationDetail> competitorPrices,
        String summary
) {
}
