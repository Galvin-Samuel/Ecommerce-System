package com.ecommerce.crawler;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrawlResultRepository extends MongoRepository<CrawlResult, String> {

    List<CrawlResult> findByJobId(String jobId);

    Page<CrawlResult> findByJobId(String jobId, Pageable pageable);

    long countByJobId(String jobId);
}
