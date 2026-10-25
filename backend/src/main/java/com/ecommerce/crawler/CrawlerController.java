package com.ecommerce.crawler;

import com.ecommerce.crawler.dto.SubmitCrawlJobRequest;
import com.ecommerce.dto.ApiResponse;
import com.ecommerce.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/crawler")
public class CrawlerController {

    private final CrawlerService crawlerService;

    public CrawlerController(CrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @PostMapping("/jobs")
    public ResponseEntity<ApiResponse<CrawlJob>> submitJob(
            @Valid @RequestBody SubmitCrawlJobRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String userId = principal != null ? principal.id() : null;
        CrawlJob job = crawlerService.submitJob(request, userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Crawler job submitted successfully", job));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ApiResponse<CrawlJob>> getJobStatus(@PathVariable String jobId) {
        CrawlJob job = crawlerService.getJobStatus(jobId);
        return ResponseEntity.ok(ApiResponse.success("Crawler job status retrieved", job));
    }

    @GetMapping("/jobs/{jobId}/results")
    public ResponseEntity<ApiResponse<List<CrawlResult>>> getJobResults(@PathVariable String jobId) {
        List<CrawlResult> results = crawlerService.getJobResults(jobId);
        return ResponseEntity.ok(ApiResponse.success("Crawler job results retrieved", results));
    }

    @PostMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelJob(@PathVariable String jobId) {
        crawlerService.cancelJob(jobId);
        return ResponseEntity.ok(ApiResponse.success("Crawler job cancellation requested", jobId));
    }
}
