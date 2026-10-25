package com.ecommerce.competitor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for extracting and parsing prices from raw text or HTML elements.
 */
public final class PriceParser {

    private static final Pattern PRICE_PATTERN = Pattern.compile("([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)");

    private PriceParser() {
    }

    /**
     * Extracts a numeric BigDecimal price from arbitrary text (e.g. "$1,299.99", "USD 89.50", "49.99").
     *
     * @param text raw input text
     * @return parsed positive BigDecimal or empty if invalid/missing
     */
    public static Optional<BigDecimal> extractPrice(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Optional.empty();
        }

        // Clean common noise
        String clean = text.trim();
        Matcher matcher = PRICE_PATTERN.matcher(clean);
        if (matcher.find()) {
            String numberStr = matcher.group(1).replace(",", "");
            try {
                BigDecimal price = new BigDecimal(numberStr).setScale(2, RoundingMode.HALF_UP);
                if (price.compareTo(BigDecimal.ZERO) > 0) {
                    return Optional.of(price);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return Optional.empty();
    }
}
