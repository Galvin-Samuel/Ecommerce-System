package com.ecommerce.service;

import com.ecommerce.dto.PagedResponse;
import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.Product;
import com.ecommerce.model.UserRole;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service managing product catalog lifecycle, queries, persistence,
 * and role/ownership authorization.
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "name", "price", "category", "stockQuantity", "createdAt", "updatedAt"
    );

    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;

    public ProductService(ProductRepository productRepository, MongoTemplate mongoTemplate) {
        this.productRepository = productRepository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Creates a new product catalog entry.
     * Requires SELLER or ADMIN role. The sellerId is derived from the authenticated principal.
     *
     * @param request   validated product creation payload
     * @param principal authenticated user principal
     * @return ProductResponse with generated ID and metadata
     */
    @CacheEvict(value = "product_searches", allEntries = true)
    public ProductResponse createProduct(ProductRequest request, UserPrincipal principal) {
        if (principal == null || (principal.role() != UserRole.SELLER && principal.role() != UserRole.ADMIN)) {
            log.warn("Unauthorized attempt to create product by principal: {}", principal);
            throw new AccessDeniedException("Only sellers and administrators can create products.");
        }

        Product product = new Product(
                request.name().trim(),
                request.description(),
                request.category().trim(),
                request.price(),
                request.stockQuantity(),
                request.imageUrl(),
                principal.id()
        );

        Product savedProduct = productRepository.save(product);
        log.info("Product created with ID '{}' by seller '{}'", savedProduct.getId(), principal.id());
        return ProductResponse.fromEntity(savedProduct);
    }

    /**
     * Retrieves an existing product by its unique database ID.
     * Publicly accessible.
     *
     * @param id product ID
     * @return ProductResponse details
     * @throws ResourceNotFoundException if no product exists with the given ID
     */
    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product lookup failed for ID '{}'", id);
                    return new ResourceNotFoundException("Product with ID '" + id + "' not found.");
                });

        return ProductResponse.fromEntity(product);
    }

    /**
     * Retrieves a paginated list of catalog products, optionally filtered by category.
     * Publicly accessible.
     *
     * @param category optional category filter
     * @param page     0-indexed page number
     * @param size     page size limit (1..100)
     * @param sort     sort specification (e.g. "price,asc" or "createdAt,desc")
     * @return PagedResponse containing ProductResponse items
     */
    @Cacheable(value = "product_searches", key = "T(java.lang.String).format('list|%s|%d|%d|%s', #category != null ? #category.trim() : '', #page, #size, #sort != null ? #sort.trim() : '')")
    public PagedResponse<ProductResponse> getProducts(String category, int page, int size, String sort) {
        int boundedPage = Math.max(0, page);
        int boundedSize = Math.min(Math.max(1, size), 100);
        Sort sortOrder = parseSort(sort);

        Pageable pageable = PageRequest.of(boundedPage, boundedSize, sortOrder);

        Page<Product> productPage;
        if (StringUtils.hasText(category)) {
            productPage = productRepository.findByCategory(category.trim(), pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }

        Page<ProductResponse> responsePage = productPage.map(ProductResponse::fromEntity);
        return PagedResponse.fromPage(responsePage);
    }

    /**
     * Searches products using MongoDB criteria matching on name/description,
     * optional category, and optional price ranges with pagination and sorting.
     * Special characters in query are quoted to ensure safe literal regex matching.
     *
     * @param q        search keyword (matched against name and description)
     * @param category category filter
     * @param minPrice minimum price filter (optional)
     * @param maxPrice maximum price filter (optional)
     * @param page     page number (0-indexed)
     * @param size     page size limit (1..100)
     * @param sort     sort specification
     * @return PagedResponse containing matched ProductResponse items
     */
    @Cacheable(value = "product_searches", key = "T(java.lang.String).format('search|%s|%s|%s|%s|%d|%d|%s', #q != null ? #q.trim() : '', #category != null ? #category.trim() : '', #minPrice != null ? #minPrice.toString() : '', #maxPrice != null ? #maxPrice.toString() : '', #page, #size, #sort != null ? #sort.trim() : '')")
    public PagedResponse<ProductResponse> searchProducts(
            String q,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sort
    ) {
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("minPrice cannot be negative.");
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("maxPrice cannot be negative.");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice cannot be greater than maxPrice.");
        }

        int boundedPage = Math.max(0, page);
        int boundedSize = Math.min(Math.max(1, size), 100);
        Sort sortOrder = parseSort(sort);
        Pageable pageable = PageRequest.of(boundedPage, boundedSize, sortOrder);

        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (StringUtils.hasText(q)) {
            String trimmed = q.trim();
            Pattern pattern = Pattern.compile(Pattern.quote(trimmed), Pattern.CASE_INSENSITIVE);
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("name").regex(pattern),
                    Criteria.where("description").regex(pattern)
            ));
        }

        if (StringUtils.hasText(category)) {
            criteriaList.add(Criteria.where("category").is(category.trim()));
        }

        if (minPrice != null || maxPrice != null) {
            Criteria priceCriteria = Criteria.where("price");
            if (minPrice != null) {
                priceCriteria = priceCriteria.gte(new org.bson.types.Decimal128(minPrice));
            }
            if (maxPrice != null) {
                priceCriteria = priceCriteria.lte(new org.bson.types.Decimal128(maxPrice));
            }
            criteriaList.add(priceCriteria);
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, Product.class);
        query.with(pageable);
        List<Product> products = mongoTemplate.find(query, Product.class);

        Page<Product> productPage = new PageImpl<>(products, pageable, total);
        Page<ProductResponse> responsePage = productPage.map(ProductResponse::fromEntity);
        return PagedResponse.fromPage(responsePage);
    }

    /**
     * Updates an existing catalog product.
     * Allowed only for the product owner (seller) or an ADMIN.
     *
     * @param id        product ID to update
     * @param request   validated update payload
     * @param principal authenticated user principal
     * @return updated ProductResponse
     */
    @Caching(evict = {
            @CacheEvict(value = "products", key = "#id"),
            @CacheEvict(value = "product_searches", allEntries = true)
    })
    public ProductResponse updateProduct(String id, ProductRequest request, UserPrincipal principal) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID '" + id + "' not found."));

        validateOwnership(product, principal, "update");

        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setCategory(request.category().trim());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setImageUrl(request.imageUrl());
        product.setUpdatedAt(Instant.now());

        Product updatedProduct = productRepository.save(product);
        log.info("Product '{}' updated successfully by principal '{}'", id, principal.id());
        return ProductResponse.fromEntity(updatedProduct);
    }

    /**
     * Deletes an existing catalog product.
     * Allowed only for the product owner (seller) or an ADMIN.
     *
     * @param id        product ID to delete
     * @param principal authenticated user principal
     */
    @Caching(evict = {
            @CacheEvict(value = "products", key = "#id"),
            @CacheEvict(value = "product_searches", allEntries = true)
    })
    public void deleteProduct(String id, UserPrincipal principal) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID '" + id + "' not found."));

        validateOwnership(product, principal, "delete");

        productRepository.delete(product);
        log.info("Product '{}' deleted by principal '{}'", id, principal.id());
    }

    private void validateOwnership(Product product, UserPrincipal principal, String action) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required to " + action + " products.");
        }

        boolean isOwner = product.getSellerId() != null && product.getSellerId().equals(principal.id());
        boolean isAdmin = principal.role() == UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            log.warn("Access denied: principal '{}' (role {}) attempted to {} product '{}' owned by seller '{}'",
                    principal.id(), principal.role(), action, product.getId(), product.getSellerId());
            throw new AccessDeniedException("You do not have permission to " + action + " this product.");
        }
    }

    private Sort parseSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sort.split(",");
        String property = parts[0].trim();
        if (!ALLOWED_SORT_PROPERTIES.contains(property)) {
            property = "createdAt";
        }

        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
            direction = Sort.Direction.ASC;
        }

        return Sort.by(direction, property);
    }
}
