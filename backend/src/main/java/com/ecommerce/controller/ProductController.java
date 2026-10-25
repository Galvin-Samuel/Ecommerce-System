package com.ecommerce.controller;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.PagedResponse;
import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.security.UserPrincipal;
import com.ecommerce.service.ProductService;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.autocomplete.AutocompleteService;
import com.ecommerce.autocomplete.ProductSuggestionResponse;

import java.util.List;

/**
 * REST controller exposing endpoints for product catalog management,
 * retrieval, pagination, updates, and deletion.
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;
    private final AutocompleteService autocompleteService;

    public ProductController(ProductService productService, AutocompleteService autocompleteService) {
        this.productService = productService;
        this.autocompleteService = autocompleteService;
    }

    /**
     * Autocomplete and search suggestions based on prefix query.
     * Publicly accessible.
     */
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<ProductSuggestionResponse>>> getSuggestions(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<ProductSuggestionResponse> suggestions = autocompleteService != null
                ? autocompleteService.getSuggestions(q, limit)
                : List.of();
        return ResponseEntity.ok(ApiResponse.success("Search suggestions retrieved successfully", suggestions));
    }

    /**
     * Lists products with pagination and optional category filtering.
     * Publicly accessible.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        PagedResponse<ProductResponse> response = productService.getProducts(category, page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Searches products with keyword matching (name/description), category filtering,
     * price range filtering, pagination, and sorting.
     * Publicly accessible.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> searchProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        PagedResponse<ProductResponse> response = productService.searchProducts(
                q, category, minPrice, maxPrice, page, size, sort
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Retrieves an individual product by its ID.
     * Publicly accessible.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable String id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Creates a new catalog product.
     * Protected: requires authenticated SELLER or ADMIN role.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProductResponse response = productService.createProduct(request, principal);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully.", response));
    }

    /**
     * Updates an existing catalog product.
     * Protected: requires product owner or ADMIN.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProductResponse response = productService.updateProduct(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully.", response));
    }

    /**
     * Deletes an existing catalog product.
     * Protected: requires product owner or ADMIN.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        productService.deleteProduct(id, principal);
        return ResponseEntity.noContent().build();
    }
}
