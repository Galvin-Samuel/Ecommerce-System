package com.ecommerce.competitor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "price_observations")
public class PriceObservation {

    @Id
    private String id;

    @Indexed
    private String mappingId;

    @Indexed
    private String productId;

    @Indexed
    private String sourceId;

    private String sourceName;
    private BigDecimal observedPrice;
    private String currency = "USD";
    private String status; // SUCCESS, FETCH_FAILED, EXTRACTION_FAILED, CURRENCY_MISMATCH
    private String errorMessage;

    @Indexed
    private Instant observedAt = Instant.now();

    public PriceObservation() {
    }

    public PriceObservation(String id, String mappingId, String productId, String sourceId,
                            String sourceName, BigDecimal observedPrice, String currency, String status, String errorMessage) {
        this.id = id;
        this.mappingId = mappingId;
        this.productId = productId;
        this.sourceId = sourceId;
        this.sourceName = sourceName;
        this.observedPrice = observedPrice;
        this.currency = currency != null ? currency.toUpperCase() : "USD";
        this.status = status;
        this.errorMessage = errorMessage;
        this.observedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMappingId() {
        return mappingId;
    }

    public void setMappingId(String mappingId) {
        this.mappingId = mappingId;
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

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public BigDecimal getObservedPrice() {
        return observedPrice;
    }

    public void setObservedPrice(BigDecimal observedPrice) {
        this.observedPrice = observedPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(Instant observedAt) {
        this.observedAt = observedAt;
    }
}
