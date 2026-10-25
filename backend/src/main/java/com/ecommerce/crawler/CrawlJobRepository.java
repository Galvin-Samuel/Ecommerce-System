package com.ecommerce.crawler;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CrawlJobRepository extends MongoRepository<CrawlJob, String> {

    Optional<CrawlJob> findByJobId(String jobId);
}
