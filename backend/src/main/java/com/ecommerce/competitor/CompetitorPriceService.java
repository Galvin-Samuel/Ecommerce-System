package com.ecommerce.competitor;

import com.ecommerce.competitor.dto.CompetitorObservationDetail;
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
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.id.IdGenerator;
import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CompetitorPriceService {

    private static final Logger log = LoggerFactory.getLogger(CompetitorPriceService.class);
    private static final Duration FRESHNESS_THRESHOLD = Duration.ofHours(24);

    private final CompetitorSourceRepository sourceRepository;
    private final CompetitorProductMappingRepository mappingRepository;
    private final PriceObservationRepository observationRepository;
    private final ProductRepository productRepository;
    private final PageFetcher pageFetcher;
    private final IdGenerator idGenerator;
    private final SsrfValidator ssrfValidator;

    @org.springframework.beans.factory.annotation.Autowired
    public CompetitorPriceService(CompetitorSourceRepository sourceRepository,
                                  CompetitorProductMappingRepository mappingRepository,
                                  PriceObservationRepository observationRepository,
                                  ProductRepository productRepository,
                                  PageFetcher pageFetcher,
                                  IdGenerator idGenerator) {
        this(sourceRepository, mappingRepository, observationRepository, productRepository, pageFetcher, idGenerator, new SsrfValidator());
    }

    public CompetitorPriceService(CompetitorSourceRepository sourceRepository,
                                  CompetitorProductMappingRepository mappingRepository,
                                  PriceObservationRepository observationRepository,
                                  ProductRepository productRepository,
                                  PageFetcher pageFetcher,
                                  IdGenerator idGenerator,
                                  SsrfValidator ssrfValidator) {
        this.sourceRepository = sourceRepository;
        this.mappingRepository = mappingRepository;
        this.observationRepository = observationRepository;
        this.productRepository = productRepository;
        this.pageFetcher = pageFetcher;
        this.idGenerator = idGenerator;
        this.ssrfValidator = ssrfValidator != null ? ssrfValidator : new SsrfValidator();
    }

    public CompetitorSource createSource(CreateSourceRequest request) {
        if (sourceRepository.existsByName(request.getName().trim())) {
            throw new ConflictException("Competitor source '" + request.getName() + "' already exists");
        }

        String sourceId = "src-" + idGenerator.nextId();
        CompetitorSource source = new CompetitorSource(
                sourceId,
                request.getName().trim(),
                request.getAllowedDomain().trim().toLowerCase(),
                request.getPriceCssSelector().trim(),
                request.getCurrency(),
                request.getPollingIntervalMinutes()
        );

        CompetitorSource saved = sourceRepository.save(source);
        log.info("Registered competitor source '{}' (domain: {})", saved.getName(), saved.getAllowedDomain());
        return saved;
    }

    public List<CompetitorSource> getAllSources() {
        return sourceRepository.findAll();
    }

    public CompetitorProductMapping createMapping(CreateMappingRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));

        CompetitorSource source = sourceRepository.findById(request.getSourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Competitor source not found: " + request.getSourceId()));

        if (mappingRepository.findByProductIdAndSourceId(product.getId(), source.getId()).isPresent()) {
            throw new ConflictException("Product '" + product.getId() + "' is already mapped to source '" + source.getName() + "'");
        }

        // Validate competitor URL against source domain and SSRF
        try {
            URI uri = URI.create(request.getCompetitorUrl().trim());
            if (!ssrfValidator.isSafe(uri, Set.of(source.getAllowedDomain()))) {
                throw new BadRequestException("Competitor URL rejected: must belong to domain '" + source.getAllowedDomain() + "' and pass security checks");
            }
        } catch (Exception e) {
            throw new BadRequestException("Invalid competitor URL format: " + e.getMessage());
        }

        String mappingId = "map-" + idGenerator.nextId();
        CompetitorProductMapping mapping = new CompetitorProductMapping(
                mappingId,
                product.getId(),
                source.getId(),
                request.getCompetitorUrl().trim()
        );

        CompetitorProductMapping saved = mappingRepository.save(mapping);
        log.info("Created mapping '{}' between product '{}' and competitor source '{}'", mappingId, product.getName(), source.getName());

        // Trigger immediate initial observation
        try {
            refreshMapping(saved.getId());
        } catch (Exception e) {
            log.warn("Initial price observation failed for mapping '{}': {}", mappingId, e.getMessage());
        }

        return saved;
    }

    public PriceObservation refreshMapping(String mappingId) {
        CompetitorProductMapping mapping = mappingRepository.findById(mappingId)
                .orElseThrow(() -> new ResourceNotFoundException("Mapping not found: " + mappingId));

        CompetitorSource source = sourceRepository.findById(mapping.getSourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Competitor source not found: " + mapping.getSourceId()));

        String observationId = "obs-" + idGenerator.nextId();
        BigDecimal observedPrice = null;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            PageContent content = pageFetcher.fetch(mapping.getCompetitorUrl());
            if (content.statusCode() >= 200 && content.statusCode() < 300) {
                Document doc = Jsoup.parse(content.htmlBody(), mapping.getCompetitorUrl());
                Element priceElement = doc.selectFirst(source.getPriceCssSelector());
                if (priceElement != null) {
                    Optional<BigDecimal> extracted = PriceParser.extractPrice(priceElement.text());
                    if (extracted.isPresent()) {
                        observedPrice = extracted.get();
                    } else {
                        status = "EXTRACTION_FAILED";
                        errorMessage = "Could not parse valid price from element text: " + priceElement.text();
                    }
                } else {
                    status = "EXTRACTION_FAILED";
                    errorMessage = "Price element not found matching selector: " + source.getPriceCssSelector();
                }
            } else {
                status = "FETCH_FAILED";
                errorMessage = "HTTP status code " + content.statusCode();
            }
        } catch (Exception e) {
            status = "FETCH_FAILED";
            errorMessage = e.getMessage();
        }

        PriceObservation observation = new PriceObservation(
                observationId,
                mapping.getId(),
                mapping.getProductId(),
                source.getId(),
                source.getName(),
                observedPrice,
                source.getCurrency(),
                status,
                errorMessage
        );

        PriceObservation saved = observationRepository.save(observation);
        mapping.setLastPolledAt(Instant.now());
        mappingRepository.save(mapping);

        log.info("Recorded price observation for mapping '{}': status={}, price={}", mappingId, status, observedPrice);
        return saved;
    }

    public PriceComparisonResponse getComparisonForProduct(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        List<CompetitorProductMapping> mappings = mappingRepository.findByProductId(productId);
        List<CompetitorObservationDetail> observationDetails = new ArrayList<>();

        for (CompetitorProductMapping mapping : mappings) {
            CompetitorSource source = sourceRepository.findById(mapping.getSourceId()).orElse(null);
            String sourceName = source != null ? source.getName() : "Unknown";

            Optional<PriceObservation> latestObs = observationRepository.findFirstByMappingIdOrderByObservedAtDesc(mapping.getId());
            if (latestObs.isPresent()) {
                PriceObservation obs = latestObs.get();
                boolean isFresh = obs.getObservedAt().isAfter(Instant.now().minus(FRESHNESS_THRESHOLD));
                BigDecimal priceDiff = null;
                Double pctDiff = null;

                if (obs.getObservedPrice() != null && product.getPrice() != null && product.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                    priceDiff = obs.getObservedPrice().subtract(product.getPrice());
                    pctDiff = priceDiff.divide(product.getPrice(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(1, RoundingMode.HALF_UP)
                            .doubleValue();
                }

                observationDetails.add(new CompetitorObservationDetail(
                        mapping.getSourceId(),
                        sourceName,
                        obs.getObservedPrice(),
                        obs.getCurrency(),
                        priceDiff,
                        pctDiff,
                        obs.getStatus(),
                        isFresh,
                        obs.getObservedAt(),
                        mapping.getCompetitorUrl()
                ));
            }
        }

        String summary = generateComparisonSummary(product.getName(), product.getPrice(), observationDetails);

        return new PriceComparisonResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                "USD",
                observationDetails,
                summary
        );
    }

    public List<PriceObservation> getMappingHistory(String mappingId) {
        if (!mappingRepository.existsById(mappingId)) {
            throw new ResourceNotFoundException("Mapping not found: " + mappingId);
        }
        return observationRepository.findByMappingIdOrderByObservedAtDesc(mappingId);
    }

    private String generateComparisonSummary(String productName, BigDecimal localPrice, List<CompetitorObservationDetail> details) {
        if (details.isEmpty()) {
            return "No competitor price observations available for " + productName;
        }

        long cheaperCount = details.stream().filter(d -> d.priceDifference() != null && d.priceDifference().compareTo(BigDecimal.ZERO) < 0).count();
        long moreExpensiveCount = details.stream().filter(d -> d.priceDifference() != null && d.priceDifference().compareTo(BigDecimal.ZERO) > 0).count();

        if (cheaperCount > 0 && moreExpensiveCount == 0) {
            return "Local price is higher than all monitored competitors.";
        } else if (moreExpensiveCount > 0 && cheaperCount == 0) {
            return "Local price is competitive (lower than monitored competitors).";
        } else {
            return "Local price is within current market range across monitored competitors.";
        }
    }
}
