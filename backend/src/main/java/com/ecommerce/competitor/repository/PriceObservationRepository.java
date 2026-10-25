package com.ecommerce.competitor.repository;

import com.ecommerce.competitor.PriceObservation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PriceObservationRepository extends MongoRepository<PriceObservation, String> {

    List<PriceObservation> findByMappingIdOrderByObservedAtDesc(String mappingId);

    List<PriceObservation> findByProductIdOrderByObservedAtDesc(String productId);

    Optional<PriceObservation> findFirstByMappingIdOrderByObservedAtDesc(String mappingId);

    Optional<PriceObservation> findFirstByProductIdAndSourceIdOrderByObservedAtDesc(String productId, String sourceId);
}
