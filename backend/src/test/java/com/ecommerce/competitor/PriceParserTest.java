package com.ecommerce.competitor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PriceParserTest {

    @Test
    @DisplayName("Should extract standard decimal prices with currency prefixes and commas")
    void testExtractStandardPrices() {
        assertThat(PriceParser.extractPrice("$49.99"))
                .contains(new BigDecimal("49.99"));

        assertThat(PriceParser.extractPrice("$1,299.50"))
                .contains(new BigDecimal("1299.50"));

        assertThat(PriceParser.extractPrice("USD 89.00"))
                .contains(new BigDecimal("89.00"));

        assertThat(PriceParser.extractPrice("Now only $19.95 for a limited time!"))
                .contains(new BigDecimal("19.95"));
    }

    @Test
    @DisplayName("Should reject empty, zero, negative, or unparseable input")
    void testRejectInvalidPrices() {
        assertThat(PriceParser.extractPrice(null)).isEmpty();
        assertThat(PriceParser.extractPrice("")).isEmpty();
        assertThat(PriceParser.extractPrice("No price available")).isEmpty();
        assertThat(PriceParser.extractPrice("$0.00")).isEmpty();
    }
}
