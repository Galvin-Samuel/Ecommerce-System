package com.ecommerce.ratelimit;

import com.ecommerce.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that intercepts API requests and applies Redis distributed rate limits.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisRateLimiterService rateLimiterService;
    private final ClientIdentifierResolver identifierResolver;
    private final RateLimitingProperties properties;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(
            RedisRateLimiterService rateLimiterService,
            ClientIdentifierResolver identifierResolver,
            RateLimitingProperties properties
    ) {
        this.rateLimiterService = rateLimiterService;
        this.identifierResolver = identifierResolver;
        this.properties = properties;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip health check endpoints from rate limiting
        return path.startsWith("/api/health");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String category = resolveCategory(request.getRequestURI());
        String clientId = identifierResolver.resolveIdentifier(request);

        RateLimitResult result = rateLimiterService.checkRateLimit(clientId, category);

        // Always write standard rate limit telemetry headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.retryAfterSeconds() > 0 ? result.retryAfterSeconds() : 60));

        if (result.degraded()) {
            response.setHeader("X-RateLimit-Degraded", "true");
            if (result.fallbackReason() != null) {
                response.setHeader("X-RateLimit-Fallback", result.fallbackReason());
            }
        }

        if (!result.allowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ApiErrorResponse errorResponse = new ApiErrorResponse(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Too Many Requests",
                    "Rate limit exceeded. Quota resets in " + result.retryAfterSeconds() + " seconds.",
                    request.getRequestURI()
            );

            objectMapper.writeValue(response.getOutputStream(), errorResponse);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveCategory(String path) {
        if (path.startsWith("/api/v1/auth")) {
            return "auth";
        } else if (path.startsWith("/api/v1/orders")) {
            return "orders";
        }
        return "api-default";
    }
}
