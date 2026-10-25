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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ProductService productService;

    private UserPrincipal sellerPrincipal;
    private UserPrincipal anotherSellerPrincipal;
    private UserPrincipal customerPrincipal;
    private UserPrincipal adminPrincipal;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sellerPrincipal = new UserPrincipal("seller-1", "seller1@example.com", UserRole.SELLER);
        anotherSellerPrincipal = new UserPrincipal("seller-2", "seller2@example.com", UserRole.SELLER);
        customerPrincipal = new UserPrincipal("cust-1", "cust1@example.com", UserRole.CUSTOMER);
        adminPrincipal = new UserPrincipal("admin-1", "admin@example.com", UserRole.ADMIN);

        sampleProduct = new Product(
                "prod-1",
                "Ergonomic Keyboard",
                "Split mechanical keyboard",
                "Electronics",
                new BigDecimal("129.99"),
                25,
                "https://example.com/keyboard.jpg",
                "seller-1",
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("createProduct succeeds when principal is SELLER")
    void shouldCreateProductSuccessfullyForSeller() {
        ProductRequest request = new ProductRequest(
                "  Ergonomic Keyboard  ",
                "Split mechanical keyboard",
                "  Electronics  ",
                new BigDecimal("129.99"),
                25,
                "https://example.com/keyboard.jpg"
        );

        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductResponse response = productService.createProduct(request, sellerPrincipal);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("prod-1");
        assertThat(response.name()).isEqualTo("Ergonomic Keyboard");
        assertThat(response.sellerId()).isEqualTo("seller-1");

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Ergonomic Keyboard");
        assertThat(saved.getCategory()).isEqualTo("Electronics");
        assertThat(saved.getSellerId()).isEqualTo("seller-1");
    }

    @Test
    @DisplayName("createProduct throws AccessDeniedException when caller is CUSTOMER")
    void shouldRejectProductCreationForCustomer() {
        ProductRequest request = new ProductRequest(
                "Phone", "Desc", "Electronics", new BigDecimal("499.00"), 10, null
        );

        assertThatThrownBy(() -> productService.createProduct(request, customerPrincipal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only sellers and administrators");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("getProductById returns ProductResponse when product exists")
    void shouldReturnProductByIdWhenFound() {
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(sampleProduct));

        ProductResponse response = productService.getProductById("prod-1");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("prod-1");
        assertThat(response.name()).isEqualTo("Ergonomic Keyboard");
    }

    @Test
    @DisplayName("getProductById throws ResourceNotFoundException when product does not exist")
    void shouldThrowNotFoundWhenProductDoesNotExist() {
        when(productRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product with ID 'nonexistent' not found.");
    }

    @Test
    @DisplayName("getProducts without category returns paginated results from findAll")
    void shouldReturnPaginatedProductsWithoutCategory() {
        Page<Product> page = new PageImpl<>(List.of(sampleProduct));
        when(productRepository.findAll(any(Pageable.class))).thenReturn(page);

        PagedResponse<ProductResponse> result = productService.getProducts(null, 0, 10, "price,asc");

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).name()).isEqualTo("Ergonomic Keyboard");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getProducts with category returns paginated results from findByCategory")
    void shouldReturnPaginatedProductsWithCategory() {
        Page<Product> page = new PageImpl<>(List.of(sampleProduct));
        when(productRepository.findByCategory(eq("Electronics"), any(Pageable.class))).thenReturn(page);

        PagedResponse<ProductResponse> result = productService.getProducts("Electronics", 0, 10, "createdAt,desc");

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).category()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("updateProduct succeeds when caller is product owner")
    void shouldUpdateProductWhenCallerIsOwner() {
        ProductRequest updateRequest = new ProductRequest(
                "Updated Keyboard",
                "New description",
                "Accessories",
                new BigDecimal("149.99"),
                30,
                "https://example.com/new.jpg"
        );

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse updated = productService.updateProduct("prod-1", updateRequest, sellerPrincipal);

        assertThat(updated.name()).isEqualTo("Updated Keyboard");
        assertThat(updated.price()).isEqualTo(new BigDecimal("149.99"));
        assertThat(updated.category()).isEqualTo("Accessories");
        assertThat(updated.stockQuantity()).isEqualTo(30);
    }

    @Test
    @DisplayName("updateProduct succeeds when caller is ADMIN even if not owner")
    void shouldUpdateProductWhenCallerIsAdmin() {
        ProductRequest updateRequest = new ProductRequest(
                "Admin Modified", "Desc", "Electronics", new BigDecimal("99.00"), 5, null
        );

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse updated = productService.updateProduct("prod-1", updateRequest, adminPrincipal);

        assertThat(updated.name()).isEqualTo("Admin Modified");
    }

    @Test
    @DisplayName("updateProduct throws AccessDeniedException when caller is not the owner or admin")
    void shouldRejectUpdateWhenCallerIsNotOwner() {
        ProductRequest updateRequest = new ProductRequest(
                "Hacked", "Desc", "Category", new BigDecimal("1.00"), 1, null
        );

        when(productRepository.findById("prod-1")).thenReturn(Optional.of(sampleProduct));

        assertThatThrownBy(() -> productService.updateProduct("prod-1", updateRequest, anotherSellerPrincipal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not have permission to update this product.");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteProduct succeeds when caller is owner")
    void shouldDeleteProductWhenCallerIsOwner() {
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(sampleProduct));

        productService.deleteProduct("prod-1", sellerPrincipal);

        verify(productRepository).delete(sampleProduct);
    }

    @Test
    @DisplayName("deleteProduct throws AccessDeniedException when caller is not owner")
    void shouldRejectDeleteWhenCallerIsNotOwner() {
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(sampleProduct));

        assertThatThrownBy(() -> productService.deleteProduct("prod-1", customerPrincipal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not have permission to delete this product.");

        verify(productRepository, never()).delete(any());
    }

    @Test
    @DisplayName("searchProducts succeeds and queries MongoTemplate with bounded pagination")
    void shouldSearchProductsSuccessfully() {
        when(mongoTemplate.count(any(Query.class), eq(Product.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(Product.class))).thenReturn(List.of(sampleProduct));

        PagedResponse<ProductResponse> result = productService.searchProducts(
                "keyboard", "Electronics", new BigDecimal("50.00"), new BigDecimal("200.00"), -1, 150, "price,asc"
        );

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).name()).isEqualTo("Ergonomic Keyboard");
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(100); // bounded to 100 max
    }

    @Test
    @DisplayName("searchProducts throws BadRequestException when minPrice is negative")
    void shouldRejectNegativeMinPrice() {
        assertThatThrownBy(() -> productService.searchProducts(
                "test", null, new BigDecimal("-1.00"), new BigDecimal("10.00"), 0, 20, null
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("minPrice cannot be negative.");
    }

    @Test
    @DisplayName("searchProducts throws BadRequestException when maxPrice is negative")
    void shouldRejectNegativeMaxPrice() {
        assertThatThrownBy(() -> productService.searchProducts(
                "test", null, null, new BigDecimal("-5.00"), 0, 20, null
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("maxPrice cannot be negative.");
    }

    @Test
    @DisplayName("searchProducts throws BadRequestException when minPrice > maxPrice")
    void shouldRejectMinPriceGreaterThanMaxPrice() {
        assertThatThrownBy(() -> productService.searchProducts(
                "test", null, new BigDecimal("100.00"), new BigDecimal("50.00"), 0, 20, null
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("minPrice cannot be greater than maxPrice.");
    }
}
