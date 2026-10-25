package com.ecommerce.competitor;

import com.ecommerce.competitor.dto.CreateMappingRequest;
import com.ecommerce.competitor.dto.CreateSourceRequest;
import com.ecommerce.competitor.dto.PriceComparisonResponse;
import com.ecommerce.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/competitors")
public class CompetitorPriceController {

    private final CompetitorPriceService competitorPriceService;

    public CompetitorPriceController(CompetitorPriceService competitorPriceService) {
        this.competitorPriceService = competitorPriceService;
    }

    @PostMapping("/sources")
    public ResponseEntity<ApiResponse<CompetitorSource>> createSource(@Valid @RequestBody CreateSourceRequest request) {
        CompetitorSource source = competitorPriceService.createSource(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Competitor source registered successfully", source));
    }

    @GetMapping("/sources")
    public ResponseEntity<ApiResponse<List<CompetitorSource>>> getAllSources() {
        List<CompetitorSource> sources = competitorPriceService.getAllSources();
        return ResponseEntity.ok(ApiResponse.success("Competitor sources retrieved successfully", sources));
    }

    @PostMapping("/mappings")
    public ResponseEntity<ApiResponse<CompetitorProductMapping>> createMapping(@Valid @RequestBody CreateMappingRequest request) {
        CompetitorProductMapping mapping = competitorPriceService.createMapping(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Competitor product mapping created successfully", mapping));
    }

    @PostMapping("/mappings/{mappingId}/refresh")
    public ResponseEntity<ApiResponse<PriceObservation>> refreshMapping(@PathVariable String mappingId) {
        PriceObservation observation = competitorPriceService.refreshMapping(mappingId);
        return ResponseEntity.ok(ApiResponse.success("Price observation refreshed successfully", observation));
    }

    @GetMapping("/mappings/{mappingId}/history")
    public ResponseEntity<ApiResponse<List<PriceObservation>>> getMappingHistory(@PathVariable String mappingId) {
        List<PriceObservation> history = competitorPriceService.getMappingHistory(mappingId);
        return ResponseEntity.ok(ApiResponse.success("Price observation history retrieved successfully", history));
    }

    @GetMapping("/products/{productId}/comparison")
    public ResponseEntity<ApiResponse<PriceComparisonResponse>> getComparison(@PathVariable String productId) {
        PriceComparisonResponse response = competitorPriceService.getComparisonForProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("Price comparison retrieved successfully", response));
    }
}
