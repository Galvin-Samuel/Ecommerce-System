package com.ecommerce.competitor.repository;

import com.ecommerce.competitor.CompetitorProductMapping;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitorProductMappingRepository extends MongoRepository<CompetitorProductMapping, String> {

    List<CompetitorProductMapping> findByProductId(String productId);

    Optional<CompetitorProductMapping> findByProductIdAndSourceId(String productId, String sourceId);

    List<CompetitorProductMapping> findByEnabledTrue();
}
