package com.ecommerce.crawler;

import com.ecommerce.crawler.dto.SubmitCrawlJobRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CrawlerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CrawlJobRepository jobRepository;

    @Autowired
    private CrawlResultRepository resultRepository;

    @MockBean
    private PageFetcher pageFetcher;

    @AfterEach
    void tearDown() {
        resultRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/crawler/jobs should accept valid crawl job and return HTTP 202")
    void testSubmitJob() throws Exception {
        when(pageFetcher.fetch(any())).thenReturn(new PageContent("https://example.com/test", 200, "text/html", "<html><title>Demo</title><body>Hello</body></html>"));

        SubmitCrawlJobRequest request = new SubmitCrawlJobRequest(
                List.of("https://example.com/test"),
                Set.of("example.com"),
                5,
                1,
                10L
        );

        mockMvc.perform(post("/api/v1/crawler/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId", notNullValue()))
                .andExpect(jsonPath("$.data.status", isOneOf("SUBMITTED", "RUNNING", "COMPLETED")));
    }

    @Test
    @DisplayName("GET /api/v1/crawler/jobs/{jobId} should return job status and results")
    void testGetJobStatusAndResults() throws Exception {
        CrawlJob job = new CrawlJob("crawl-status-test", List.of("https://example.com"), Set.of("example.com"), 10, 1, "user-1");
        job.setStatus(CrawlJobStatus.COMPLETED);
        job.setPagesProcessed(1);
        jobRepository.save(job);

        CrawlResult result = new CrawlResult(
                "crawl-status-test", "https://example.com", 0, "Example Title",
                "https://example.com", "Page summary", List.of(), 200, "text/html", null
        );
        resultRepository.save(result);

        // 1. Status endpoint
        mockMvc.perform(get("/api/v1/crawler/jobs/crawl-status-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobId").value("crawl-status-test"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.pagesProcessed").value(1));

        // 2. Results endpoint
        mockMvc.perform(get("/api/v1/crawler/jobs/crawl-status-test/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Example Title"))
                .andExpect(jsonPath("$.data[0].url").value("https://example.com"));
    }
}
