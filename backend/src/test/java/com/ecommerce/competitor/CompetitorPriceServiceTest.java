package com.ecommerce.competitor;

import com.ecommerce.competitor.dto.CreateMappingRequest;
import com.ecommerce.competitor.dto.CreateSourceRequest;
import com.ecommerce.competitor.dto.PriceComparisonResponse;
import com.ecommerce.competitor.repository.CompetitorProductMappingRepository;
import com.ecommerce.competitor.repository.CompetitorSourceRepository;
import com.ecommerce.competitor.repository.PriceObservationRepository;
import com.ecommerce.crawler.PageContent;
import com.ecommerce.crawler.PageFetcher;
import com.ecommerce.crawler.SsrfValidator;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ConflictException;
import com.ecommerce.id.IdGenerator;
import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompetitorPriceServiceTest {

    @Mock
    private CompetitorSourceRepository sourceRepository;

    @Mock
    private CompetitorProductMappingRepository mappingRepository;

    @Mock
    private PriceObservationRepository observationRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PageFetcher pageFetcher;

    @Mock
    private IdGenerator idGenerator;

    private CompetitorPriceService service;

    @BeforeEach
    void setUp() {
        service = new CompetitorPriceService(
                sourceRepository, mappingRepository, observationRepository,
                productRepository, pageFetcher, idGenerator, new SsrfValidator(true)
        );
    }

    @Test
    @DisplayName("Should create competitor source and reject duplicate names")
    void testCreateSource() {
        when(sourceRepository.existsByName("BestBuyRetail")).thenReturn(false);
        when(idGenerator.nextId()).thenReturn(101L);
        when(sourceRepository.save(any(CompetitorSource.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateSourceRequest request = new CreateSourceRequest("BestBuyRetail", "bestbuy.example.com", ".pricing-current", "USD", 60);
        CompetitorSource source = service.createSource(request);

        assertThat(source.getId()).isEqualTo("src-101");
        assertThat(source.getName()).isEqualTo("BestBuyRetail");
        assertThat(source.getAllowedDomain()).isEqualTo("bestbuy.example.com");

        // Reject duplicate
        when(sourceRepository.existsByName("BestBuyRetail")).thenReturn(true);
        assertThatThrownBy(() -> service.createSource(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Should reject mapping if competitor URL does not belong to authorized source domain")
    void testRejectDisallowedDomainMapping() {
        CompetitorPriceService strictService = new CompetitorPriceService(
                sourceRepository, mappingRepository, observationRepository,
                productRepository, pageFetcher, idGenerator, new SsrfValidator(false)
        );

        Product product = new Product();
        product.setId("prod-1");
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        CompetitorSource source = new CompetitorSource("src-1", "MegaStore", "megastore.example.com", ".price", "USD", 60);
        when(sourceRepository.findById("src-1")).thenReturn(Optional.of(source));

        CreateMappingRequest badDomainReq = new CreateMappingRequest("prod-1", "src-1", "https://unauthorized-competitor.com/item");
        assertThatThrownBy(() -> strictService.createMapping(badDomainReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must belong to domain 'megastore.example.com'");
    }

    @Test
    @DisplayName("Should refresh mapping, parse HTML price, record observation, and never modify local product price")
    void testRefreshMappingAndVerifyLocalPriceIntegrity() throws Exception {
        CompetitorProductMapping mapping = new CompetitorProductMapping("map-1", "prod-42", "src-1", "https://megastore.example.com/gadget");
        CompetitorSource source = new CompetitorSource("src-1", "MegaStore", "megastore.example.com", ".price-tag", "USD", 60);

        when(mappingRepository.findById("map-1")).thenReturn(Optional.of(mapping));
        when(sourceRepository.findById("src-1")).thenReturn(Optional.of(source));
        when(idGenerator.nextId()).thenReturn(505L);
        when(observationRepository.save(any(PriceObservation.class))).thenAnswer(inv -> inv.getArgument(0));

        String html = "<html><body><span class='price-tag'>$119.99</span></body></html>";
        when(pageFetcher.fetch("https://megastore.example.com/gadget")).thenReturn(new PageContent("https://megastore.example.com/gadget", 200, "text/html", html));

        PriceObservation obs = service.refreshMapping("map-1");

        assertThat(obs.getId()).isEqualTo("obs-505");
        assertThat(obs.getStatus()).isEqualTo("SUCCESS");
        assertThat(obs.getObservedPrice()).isEqualTo(new BigDecimal("119.99"));
        assertThat(obs.getSourceName()).isEqualTo("MegaStore");

        // Verify that productRepository.save was NEVER called (confirming local price was not modified)
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should generate accurate price comparison against catalogue product")
    void testGetComparisonForProduct() {
        Product localProduct = new Product();
        localProduct.setId("prod-100");
        localProduct.setName("Flagship Smartphone");
        localProduct.setPrice(new BigDecimal("100.00"));
        when(productRepository.findById("prod-100")).thenReturn(Optional.of(localProduct));

        CompetitorProductMapping mapping = new CompetitorProductMapping("map-1", "prod-100", "src-1", "https://comp.com/item");
        CompetitorSource source = new CompetitorSource("src-1", "CompetitorDirect", "comp.com", ".price", "USD", 60);

        when(mappingRepository.findByProductId("prod-100")).thenReturn(List.of(mapping));
        when(sourceRepository.findById("src-1")).thenReturn(Optional.of(source));

        PriceObservation obs = new PriceObservation(
                "obs-1", "map-1", "prod-100", "src-1", "CompetitorDirect",
                new BigDecimal("125.00"), "USD", "SUCCESS", null
        );
        when(observationRepository.findFirstByMappingIdOrderByObservedAtDesc("map-1")).thenReturn(Optional.of(obs));

        PriceComparisonResponse comparison = service.getComparisonForProduct("prod-100");

        assertThat(comparison.productName()).isEqualTo("Flagship Smartphone");
        assertThat(comparison.localPrice()).isEqualTo(new BigDecimal("100.00"));
        assertThat(comparison.competitorPrices()).hasSize(1);
        assertThat(comparison.competitorPrices().get(0).competitorPrice()).isEqualTo(new BigDecimal("125.00"));
        assertThat(comparison.competitorPrices().get(0).priceDifference()).isEqualTo(new BigDecimal("25.00"));
        assertThat(comparison.competitorPrices().get(0).percentageDifference()).isEqualTo(25.0);
        assertThat(comparison.summary()).contains("lower than monitored competitors");
    }
}
