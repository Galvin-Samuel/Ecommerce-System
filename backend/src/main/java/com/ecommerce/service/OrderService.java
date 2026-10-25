package com.ecommerce.service;

import com.ecommerce.dto.OrderItemResponse;
import com.ecommerce.dto.OrderRequest;
import com.ecommerce.dto.OrderResponse;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.OrderStatus;
import com.ecommerce.model.Product;
import com.ecommerce.model.UserRole;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.security.UserPrincipal;
import com.mongodb.client.result.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Service managing customer order lifecycle, inventory verification,
 * atomic stock decrements with compensating rollbacks on standalone MongoDB,
 * cart clearing, and ownership authorization.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;

    public OrderService(
            OrderRepository orderRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            MongoTemplate mongoTemplate
    ) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Creates a new order snapshot from the authenticated user's current cart.
     * Performs pre-validation, server-side price computation, conditional stock decrements
     * with compensating rollback, and cart clearing.
     *
     * @param request   optional order submission metadata
     * @param principal authenticated user principal
     * @return OrderResponse with placed order details
     */
    public OrderResponse createOrder(OrderRequest request, UserPrincipal principal) {
        validateAuthenticated(principal);

        List<CartItem> cartItems = cartItemRepository.findByUserId(principal.id());
        if (cartItems.isEmpty()) {
            log.warn("Order creation rejected for user '{}': cart is empty", principal.id());
            throw new BadRequestException("Cannot create an order with an empty shopping cart.");
        }

        // Phase 1: Pre-validation & Server-side price calculation
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cartItems) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product with ID '" + item.getProductId() + "' not found."));

            int availableStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            if (availableStock < item.getQuantity()) {
                log.warn("Order placement rejected: product '{}' has stock {} but requested {}",
                        product.getName(), availableStock, item.getQuantity());
                throw new BadRequestException(String.format(
                        "Insufficient stock for product '%s'. Available: %d, requested: %d.",
                        product.getName(), availableStock, item.getQuantity()
                ));
            }

            BigDecimal unitPrice = product.getPrice() != null ? product.getPrice() : BigDecimal.ZERO;
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            orderItems.add(new OrderItem(
                    product.getId(),
                    product.getName(),
                    item.getQuantity(),
                    unitPrice,
                    subtotal
            ));
        }

        // Phase 2: Atomic conditional stock deduction with compensating rollback for standalone MongoDB
        List<CartItem> decrementedItems = new ArrayList<>();
        for (CartItem item : cartItems) {
            Query query = new Query(Criteria.where("id").is(item.getProductId())
                    .and("stockQuantity").gte(item.getQuantity()));
            Update update = new Update().inc("stockQuantity", -item.getQuantity());

            UpdateResult result = mongoTemplate.updateFirst(query, update, Product.class);
            if (result.getModifiedCount() == 0) {
                log.error("Concurrent update or stock depletion for product '{}'. Rolling back previous decrements.",
                        item.getProductId());
                // Compensating transaction: reverse previous decrements
                for (CartItem prev : decrementedItems) {
                    Query rollQuery = new Query(Criteria.where("id").is(prev.getProductId()));
                    Update rollUpdate = new Update().inc("stockQuantity", prev.getQuantity());
                    mongoTemplate.updateFirst(rollQuery, rollUpdate, Product.class);
                }
                throw new BadRequestException("Order placement failed due to concurrent stock change for product ID '" + item.getProductId() + "'.");
            }
            decrementedItems.add(item);
        }

        // Phase 3: Persist Order snapshot
        Order order = new Order(
                principal.id(),
                orderItems,
                totalAmount,
                OrderStatus.CONFIRMED
        );

        Order savedOrder = orderRepository.save(order);
        log.info("Order '{}' successfully placed for user '{}', total amount: {}",
                savedOrder.getId(), principal.id(), totalAmount);

        // Phase 4: Clear user shopping cart
        cartItemRepository.deleteByUserId(principal.id());
        log.info("Cleared shopping cart for user '{}' following order '{}' creation", principal.id(), savedOrder.getId());

        return OrderResponse.fromEntity(savedOrder);
    }

    /**
     * Retrieves all orders placed by the authenticated user in reverse chronological order.
     *
     * @param principal authenticated user principal
     * @return List of OrderResponse items
     */
    public List<OrderResponse> getOrders(UserPrincipal principal) {
        validateAuthenticated(principal);

        List<Order> orders;
        if (principal.role() == UserRole.ADMIN) {
            orders = orderRepository.findAll();
        } else {
            orders = orderRepository.findByUserIdOrderByCreatedAtDesc(principal.id());
        }

        return orders.stream().map(OrderResponse::fromEntity).toList();
    }

    /**
     * Retrieves detailed information for a specific order.
     * Access is restricted to the owning customer or an ADMIN.
     *
     * @param orderId   order ID
     * @param principal authenticated user principal
     * @return OrderResponse details
     */
    public OrderResponse getOrderById(String orderId, UserPrincipal principal) {
        validateAuthenticated(principal);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID '" + orderId + "' not found."));

        boolean isOwner = order.getUserId() != null && order.getUserId().equals(principal.id());
        boolean isAdmin = principal.role() == UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            log.warn("Access denied: principal '{}' (role {}) attempted to access order '{}' owned by user '{}'",
                    principal.id(), principal.role(), orderId, order.getUserId());
            throw new AccessDeniedException("You do not have permission to access this order.");
        }

        return OrderResponse.fromEntity(order);
    }

    private void validateAuthenticated(UserPrincipal principal) {
        if (principal == null || principal.id() == null) {
            throw new AccessDeniedException("Authentication required to access order operations.");
        }
    }
}
