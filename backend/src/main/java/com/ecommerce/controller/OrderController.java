package com.ecommerce.controller;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.OrderRequest;
import com.ecommerce.dto.OrderResponse;
import com.ecommerce.security.UserPrincipal;
import com.ecommerce.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for creating and retrieving customer orders.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Creates an order snapshot from the authenticated user's current shopping cart.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestBody(required = false) OrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OrderResponse order = orderService.createOrder(request, principal);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully.", order));
    }

    /**
     * Lists all orders placed by the authenticated user in reverse chronological order.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<OrderResponse> orders = orderService.getOrders(principal);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    /**
     * Retrieves details for a specific order.
     * Accessible only by the order owner or an administrator.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable String orderId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OrderResponse order = orderService.getOrderById(orderId, principal);
        return ResponseEntity.ok(ApiResponse.success(order));
    }
}
