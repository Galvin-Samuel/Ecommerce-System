package com.ecommerce.crawler;

import com.ecommerce.crawler.dto.SubmitCrawlJobRequest;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.id.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrawlerServiceTest {

    @Mock
    private CrawlJobRepository jobRepository;

    @Mock
    private CrawlResultRepository resultRepository;

    @Mock
    private PageFetcher pageFetcher;

    @Mock
    private IdGenerator idGenerator;

    private CrawlerService crawlerService;

    @BeforeEach
    void setUp() {
        // Allow private network for testing local fixture domains safely
        SsrfValidator testValidator = new SsrfValidator(true);
        crawlerService = new CrawlerService(jobRepository, resultRepository, pageFetcher, idGenerator, testValidator);
    }

    @Test
    @DisplayName("Should submit crawl job and persist SUBMITTED status")
    void testSubmitCrawlJob() {
        when(idGenerator.nextId()).thenReturn(99999L);
        when(jobRepository.save(any(CrawlJob.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmitCrawlJobRequest request = new SubmitCrawlJobRequest(
                List.of("https://example.com/catalog"),
                Set.of("example.com"),
                20,
                2,
                0L
        );

        CrawlJob submitted = crawlerService.submitJob(request, "admin-1");

        assertThat(submitted.getJobId()).isEqualTo("crawl-99999");
        assertThat(submitted.getSeedUrls()).containsExactly("https://example.com/catalog");
        assertThat(submitted.getStatus()).isEqualTo(CrawlJobStatus.SUBMITTED);
        verify(jobRepository, atLeastOnce()).save(any(CrawlJob.class));
    }

    @Test
    @DisplayName("Should reject job submission with dangerous loopback seed when SSRF protection is active")
    void testRejectSsrfSeed() {
        CrawlerService strictService = new CrawlerService(jobRepository, resultRepository, pageFetcher, idGenerator, new SsrfValidator(false));
        SubmitCrawlJobRequest request = new SubmitCrawlJobRequest(
                List.of("http://127.0.0.1:8080/admin"),
                null,
                10,
                1,
                0L
        );

        assertThatThrownBy(() -> strictService.submitJob(request, "user-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Seed URL rejected by security/domain policy");
    }

    @Test
    @DisplayName("Should crawl seed page, extract outgoing links, and obey max depth and deduplication")
    void testExecuteJobTraversalAndDeduplication() throws Exception {
        String seedUrl = "https://example.com/home";
        String child1Url = "https://example.com/page1";
        String child2Url = "https://example.com/page2";

        CrawlJob job = new CrawlJob("job-test-1", List.of(seedUrl), Set.of("example.com"), 10, 1, "user-1");
        job.setRequestDelayMs(0L);

        when(jobRepository.findByJobId("job-test-1")).thenReturn(Optional.of(job));

        // Seed page contains links to child1, child2, and a duplicate link back to seed
        when(pageFetcher.fetch(seedUrl)).thenReturn(new PageContent(
                seedUrl, 200, "text/html",
                "<html><title>Home</title><body><a href='/page1'>Page 1</a><a href='/page2'>Page 2</a><a href='/home'>Self</a></body></html>"
        ));

        when(pageFetcher.fetch(child1Url)).thenReturn(new PageContent(
                child1Url, 200, "text/html",
                "<html><title>Page 1 Title</title><body>Content for page 1</body></html>"
        ));

        when(pageFetcher.fetch(child2Url)).thenReturn(new PageContent(
                child2Url, 200, "text/html",
                "<html><title>Page 2 Title</title><body>Content for page 2</body></html>"
        ));

        crawlerService.executeJob("job-test-1");

        // Verify result saves: 3 pages total (seed, child1, child2; self link is deduplicated)
        ArgumentCaptor<CrawlResult> resultCaptor = ArgumentCaptor.forClass(CrawlResult.class);
        verify(resultRepository, times(3)).save(resultCaptor.capture());

        List<CrawlResult> savedResults = resultCaptor.getAllValues();
        List<String> crawledUrls = savedResults.stream().map(CrawlResult::getUrl).toList();
        assertThat(crawledUrls).containsExactlyInAnyOrder(seedUrl, child1Url, child2Url);

        assertThat(job.getStatus()).isEqualTo(CrawlJobStatus.COMPLETED);
        assertThat(job.getPagesProcessed()).isEqualTo(3);
        assertThat(job.getPagesFailed()).isEqualTo(0);
    }
}
