package com.ecommerce.shorturl.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateShortUrlRequest {

    @NotBlank(message = "Original URL is required")
    @Size(max = 2048, message = "URL cannot exceed 2048 characters")
    private String originalUrl;

    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,32}$", message = "Custom alias must be 3-32 alphanumeric characters, hyphens or underscores")
    private String customAlias;

    @Min(value = 1, message = "Expiration must be at least 1 day")
    @Max(value = 365, message = "Expiration cannot exceed 365 days")
    private Integer expiresInDays;

    public CreateShortUrlRequest() {
    }

    public CreateShortUrlRequest(String originalUrl, String customAlias, Integer expiresInDays) {
        this.originalUrl = originalUrl;
        this.customAlias = customAlias;
        this.expiresInDays = expiresInDays;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getCustomAlias() {
        return customAlias;
    }

    public void setCustomAlias(String customAlias) {
        this.customAlias = customAlias;
    }

    public Integer getExpiresInDays() {
        return expiresInDays;
    }

    public void setExpiresInDays(Integer expiresInDays) {
        this.expiresInDays = expiresInDays;
    }
}
