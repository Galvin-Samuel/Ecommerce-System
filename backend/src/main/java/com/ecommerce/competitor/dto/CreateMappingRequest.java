package com.ecommerce.competitor.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateMappingRequest {

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "Source ID is required")
    private String sourceId;

    @NotBlank(message = "Competitor URL is required")
    private String competitorUrl;

    public CreateMappingRequest() {
    }

    public CreateMappingRequest(String productId, String sourceId, String competitorUrl) {
        this.productId = productId;
        this.sourceId = sourceId;
        this.competitorUrl = competitorUrl;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getCompetitorUrl() {
        return competitorUrl;
    }

    public void setCompetitorUrl(String competitorUrl) {
        this.competitorUrl = competitorUrl;
    }
}
