package com.ecommerce.competitor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "competitor_mappings")
@CompoundIndexes({
        @CompoundIndex(name = "prod_source_idx", def = "{'productId': 1, 'sourceId': 1}", unique = true)
})
public class CompetitorProductMapping {

    @Id
    private String id;

    @Indexed
    private String productId;

    @Indexed
    private String sourceId;

    private String competitorUrl;
    private boolean enabled = true;
    private Instant lastPolledAt;
    private Instant createdAt = Instant.now();

    public CompetitorProductMapping() {
    }

    public CompetitorProductMapping(String id, String productId, String sourceId, String competitorUrl) {
        this.id = id;
        this.productId = productId;
        this.sourceId = sourceId;
        this.competitorUrl = competitorUrl;
        this.enabled = true;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getLastPolledAt() {
        return lastPolledAt;
    }

    public void setLastPolledAt(Instant lastPolledAt) {
        this.lastPolledAt = lastPolledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
