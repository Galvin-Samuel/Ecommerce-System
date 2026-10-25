package com.ecommerce.crawler;

import com.ecommerce.crawler.dto.SubmitCrawlJobRequest;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Service
public class CrawlerService {

    private static final Logger log = LoggerFactory.getLogger(CrawlerService.class);

    private final CrawlJobRepository jobRepository;
    private final CrawlResultRepository resultRepository;
    private final PageFetcher pageFetcher;
    private final IdGenerator idGenerator;
    private final HtmlPageParser pageParser;
    private final SsrfValidator ssrfValidator;
    private final ExecutorService executorService;
    private final Set<String> cancelledJobs = ConcurrentHashMap.newKeySet();

    @org.springframework.beans.factory.annotation.Autowired
    public CrawlerService(CrawlJobRepository jobRepository,
                          CrawlResultRepository resultRepository,
                          PageFetcher pageFetcher,
                          IdGenerator idGenerator) {
        this(jobRepository, resultRepository, pageFetcher, idGenerator, new SsrfValidator());
    }

    public CrawlerService(CrawlJobRepository jobRepository,
                          CrawlResultRepository resultRepository,
                          PageFetcher pageFetcher,
                          IdGenerator idGenerator,
                          SsrfValidator ssrfValidator) {
        this.jobRepository = jobRepository;
        this.resultRepository = resultRepository;
        this.pageFetcher = pageFetcher;
        this.idGenerator = idGenerator;
        this.pageParser = new HtmlPageParser();
        this.ssrfValidator = ssrfValidator != null ? ssrfValidator : new SsrfValidator();
        this.executorService = Executors.newFixedThreadPool(4);
    }

    public CrawlJob submitJob(SubmitCrawlJobRequest request, String userId) {
        if (request.getSeedUrls() == null || request.getSeedUrls().isEmpty()) {
            throw new BadRequestException("At least one seed URL must be provided");
        }

        List<String> validSeeds = new ArrayList<>();
        for (String rawSeed : request.getSeedUrls()) {
            String normalized = UrlNormalizer.normalize(rawSeed);
            if (normalized == null) {
                throw new BadRequestException("Invalid seed URL format: " + rawSeed);
            }
            URI uri;
            try {
                uri = URI.create(normalized);
            } catch (Exception e) {
                throw new BadRequestException("Invalid seed URL: " + rawSeed);
            }
            if (!ssrfValidator.isSafe(uri, request.getAllowedDomains())) {
                throw new BadRequestException("Seed URL rejected by security/domain policy: " + rawSeed);
            }
            validSeeds.add(normalized);
        }

        String jobId = "crawl-" + idGenerator.nextId();
        CrawlJob job = new CrawlJob(
                jobId,
                validSeeds,
                request.getAllowedDomains(),
                request.getMaxPages(),
                request.getMaxDepth(),
                userId
        );
        job.setRequestDelayMs(request.getRequestDelayMs());
        CrawlJob saved = jobRepository.save(job);

        // Execute asynchronously
        executorService.submit(() -> executeJob(jobId));

        return saved;
    }

    public void executeJob(String jobId) {
        CrawlJob job = jobRepository.findByJobId(jobId).orElse(null);
        if (job == null) {
            return;
        }

        job.setStatus(CrawlJobStatus.RUNNING);
        job.setStartTime(Instant.now());
        jobRepository.save(job);

        log.info("Starting crawler job '{}' with {} seeds", jobId, job.getSeedUrls().size());

        Queue<CrawlTask> queue = new ConcurrentLinkedQueue<>();
        Set<String> visited = ConcurrentHashMap.newKeySet();

        for (String seed : job.getSeedUrls()) {
            queue.add(new CrawlTask(seed, 0));
        }

        int processedCount = 0;
        int failedCount = 0;

        try {
            while (!queue.isEmpty() && processedCount < job.getMaxPages()) {
                if (cancelledJobs.contains(jobId)) {
                    log.info("Crawler job '{}' was cancelled by user", jobId);
                    job.setStatus(CrawlJobStatus.CANCELLED);
                    break;
                }

                CrawlTask task = queue.poll();
                if (task == null) {
                    break;
                }

                String url = task.url();
                if (!visited.add(url)) {
                    continue; // Already processed
                }

                // Politeness delay
                if (job.getRequestDelayMs() > 0) {
                    try {
                        Thread.sleep(job.getRequestDelayMs());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                try {
                    PageContent page = pageFetcher.fetch(url);
                    if (page.statusCode() >= 200 && page.statusCode() < 300) {
                        ParsedPage parsed = pageParser.parse(page.htmlBody(), url);

                        CrawlResult result = new CrawlResult(
                                jobId,
                                url,
                                task.depth(),
                                parsed.title(),
                                parsed.canonicalUrl(),
                                parsed.summary(),
                                parsed.outgoingLinks(),
                                page.statusCode(),
                                page.contentType(),
                                Instant.now()
                        );
                        resultRepository.save(result);
                        processedCount++;

                        // Discover new links within depth bounds
                        if (task.depth() < job.getMaxDepth()) {
                            for (String outgoing : parsed.outgoingLinks()) {
                                if (!visited.contains(outgoing)) {
                                    try {
                                        URI uri = URI.create(outgoing);
                                        if (ssrfValidator.isSafe(uri, job.getAllowedDomains())) {
                                            queue.add(new CrawlTask(outgoing, task.depth() + 1));
                                        }
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        }
                    } else {
                        log.warn("Non-2xx response ({}) for '{}'", page.statusCode(), url);
                        failedCount++;
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch '{}': {}", url, e.getMessage());
                    failedCount++;
                }
            }

            if (!cancelledJobs.contains(jobId)) {
                job.setStatus(CrawlJobStatus.COMPLETED);
            }
        } catch (Exception e) {
            log.error("Fatal error executing crawler job '{}'", jobId, e);
            job.setStatus(CrawlJobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
        } finally {
            job.setPagesProcessed(processedCount);
            job.setPagesFailed(failedCount);
            job.setCompletionTime(Instant.now());
            jobRepository.save(job);
            cancelledJobs.remove(jobId);
            log.info("Crawler job '{}' finished with status {}, pages processed: {}, failed: {}",
                    jobId, job.getStatus(), processedCount, failedCount);
        }
    }

    public CrawlJob getJobStatus(String jobId) {
        return jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Crawler job not found: " + jobId));
    }

    public List<CrawlResult> getJobResults(String jobId) {
        if (!jobRepository.findByJobId(jobId).isPresent()) {
            throw new ResourceNotFoundException("Crawler job not found: " + jobId);
        }
        return resultRepository.findByJobId(jobId);
    }

    public void cancelJob(String jobId) {
        CrawlJob job = getJobStatus(jobId);
        if (job.getStatus() == CrawlJobStatus.RUNNING || job.getStatus() == CrawlJobStatus.SUBMITTED) {
            cancelledJobs.add(jobId);
            job.setStatus(CrawlJobStatus.CANCELLED);
            job.setCompletionTime(Instant.now());
            jobRepository.save(job);
        }
    }
}
