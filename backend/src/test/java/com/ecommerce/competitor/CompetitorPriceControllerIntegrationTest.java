package com.ecommerce.competitor;

import com.ecommerce.competitor.dto.CreateMappingRequest;
import com.ecommerce.competitor.dto.CreateSourceRequest;
import com.ecommerce.competitor.repository.CompetitorProductMappingRepository;
import com.ecommerce.competitor.repository.CompetitorSourceRepository;
import com.ecommerce.competitor.repository.PriceObservationRepository;
import com.ecommerce.crawler.PageContent;
import com.ecommerce.crawler.PageFetcher;
import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CompetitorPriceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompetitorSourceRepository sourceRepository;

    @Autowired
    private CompetitorProductMappingRepository mappingRepository;

    @Autowired
    private PriceObservationRepository observationRepository;

    @Autowired
    private ProductRepository productRepository;

    @MockBean
    private PageFetcher pageFetcher;

    @AfterEach
    void tearDown() {
        observationRepository.deleteAll();
        mappingRepository.deleteAll();
        sourceRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create source, map product, and retrieve comparison")
    void testCompetitorFlow() throws Exception {
        when(pageFetcher.fetch(any())).thenReturn(new PageContent("https://partner.example.com/item", 200, "text/html", "<div class='price'>$79.99</div>"));

        // 1. Create source
        CreateSourceRequest srcReq = new CreateSourceRequest("PartnerOutlet", "partner.example.com", ".price", "USD", 30);
        String srcRes = mockMvc.perform(post("/api/v1/competitors/sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(srcReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("PartnerOutlet"))
                .andReturn().getResponse().getContentAsString();

        String sourceId = objectMapper.readTree(srcRes).path("data").path("id").asText();

        // 2. Create product
        Product product = new Product("Smart Speaker", "Audio device", "Electronics", BigDecimal.valueOf(99.99), 10, "img.jpg", "seller-1");
        Product savedProduct = productRepository.save(product);

        // 3. Create mapping
        CreateMappingRequest mapReq = new CreateMappingRequest(savedProduct.getId(), sourceId, "https://partner.example.com/item");
        mockMvc.perform(post("/api/v1/competitors/mappings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mapReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.productId").value(savedProduct.getId()));

        // 4. Retrieve comparison
        mockMvc.perform(get("/api/v1/competitors/products/" + savedProduct.getId() + "/comparison"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productName").value("Smart Speaker"))
                .andExpect(jsonPath("$.data.localPrice").value(99.99))
                .andExpect(jsonPath("$.data.competitorPrices", hasSize(1)))
                .andExpect(jsonPath("$.data.competitorPrices[0].competitorPrice").value(79.99))
                .andExpect(jsonPath("$.data.competitorPrices[0].sourceName").value("PartnerOutlet"));
    }
}
