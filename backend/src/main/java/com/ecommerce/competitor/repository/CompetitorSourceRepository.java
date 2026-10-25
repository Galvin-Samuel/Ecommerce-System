package com.ecommerce.competitor.repository;

import com.ecommerce.competitor.CompetitorSource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompetitorSourceRepository extends MongoRepository<CompetitorSource, String> {

    Optional<CompetitorSource> findByName(String name);

    boolean existsByName(String name);
}
