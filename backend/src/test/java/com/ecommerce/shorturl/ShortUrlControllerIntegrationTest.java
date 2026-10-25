package com.ecommerce.shorturl;

import com.ecommerce.shorturl.dto.CreateShortUrlRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ShortUrlControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @AfterEach
    void tearDown() {
        shortUrlRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/short-urls should create short URL and redirect should return HTTP 302")
    void testCreateAndRedirect() throws Exception {
        CreateShortUrlRequest request = new CreateShortUrlRequest("https://google.com/search?q=ecommerce", "test-alias", 7);

        // 1. Create short URL
        mockMvc.perform(post("/api/v1/short-urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shortCode").value("test-alias"))
                .andExpect(jsonPath("$.data.originalUrl").value("https://google.com/search?q=ecommerce"))
                .andExpect(jsonPath("$.data.shortUrl", containsString("/r/test-alias")));

        // 2. Perform redirect via /r/test-alias
        mockMvc.perform(get("/r/test-alias"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://google.com/search?q=ecommerce"));

        // 3. Inspect details to verify click count incremented to 1
        mockMvc.perform(get("/api/v1/short-urls/test-alias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shortCode").value("test-alias"))
                .andExpect(jsonPath("$.data.clickCount").value(1))
                .andExpect(jsonPath("$.data.expired").value(false));
    }

    @Test
    @DisplayName("GET /r/{unknown} should return HTTP 404")
    void testUnknownShortUrlReturns404() throws Exception {
        mockMvc.perform(get("/r/nonexistent-code-12345"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/short-urls/my should return HTTP 401 when unauthorized")
    void testMyUrlsRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/short-urls/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/short-urls with invalid URL should return HTTP 400")
    void testInvalidUrlReturns400() throws Exception {
        CreateShortUrlRequest request = new CreateShortUrlRequest("not-a-valid-url", null, null);

        mockMvc.perform(post("/api/v1/short-urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
