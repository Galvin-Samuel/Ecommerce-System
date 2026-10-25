package com.ecommerce.ratelimit;

import com.ecommerce.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private RedisRateLimiterService rateLimiterService;

    @Mock
    private ClientIdentifierResolver identifierResolver;

    @Mock
    private FilterChain filterChain;

    private RateLimitingProperties properties;
    private RateLimitingFilter filter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new RateLimitingProperties();
        filter = new RateLimitingFilter(rateLimiterService, identifierResolver, properties);
        objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    @Test
    @DisplayName("Should bypass /api/health endpoints without rate limit enforcement")
    void shouldBypassHealthEndpoints() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health/dependencies");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(rateLimiterService, never()).checkRateLimit(anyString(), anyString());
    }

    @Test
    @DisplayName("Should pass requests under quota with X-RateLimit telemetry headers")
    void shouldPassRequestUnderLimitWithHeaders() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(identifierResolver.resolveIdentifier(request)).thenReturn("ip:127.0.0.1");
        when(rateLimiterService.checkRateLimit("ip:127.0.0.1", "auth"))
                .thenReturn(RateLimitResult.allowed(10, 8));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals("10", response.getHeader("X-RateLimit-Limit"));
        assertEquals("8", response.getHeader("X-RateLimit-Remaining"));
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Should block requests exceeding quota with HTTP 429, Retry-After, and ApiErrorResponse")
    void shouldBlockRequestWhenQuotaExceeded() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(identifierResolver.resolveIdentifier(request)).thenReturn("ip:192.168.1.50");
        when(rateLimiterService.checkRateLimit("ip:192.168.1.50", "auth"))
                .thenReturn(RateLimitResult.rejected(10, 45));

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertEquals(429, response.getStatus());
        assertEquals("45", response.getHeader("Retry-After"));
        assertEquals("10", response.getHeader("X-RateLimit-Limit"));
        assertEquals("0", response.getHeader("X-RateLimit-Remaining"));

        ApiErrorResponse error = objectMapper.readValue(response.getContentAsString(), ApiErrorResponse.class);
        assertNotNull(error);
        assertEquals(429, error.getStatus());
        assertEquals("Too Many Requests", error.getError());
    }

    @Test
    @DisplayName("Should attach degraded headers when Redis is in fail-open mode")
    void shouldAttachDegradedHeaders() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/products");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(identifierResolver.resolveIdentifier(request)).thenReturn("ip:127.0.0.1");
        when(rateLimiterService.checkRateLimit("ip:127.0.0.1", "api-default"))
                .thenReturn(RateLimitResult.degradedAllow(60, "redis-unavailable"));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals("true", response.getHeader("X-RateLimit-Degraded"));
        assertEquals("redis-unavailable", response.getHeader("X-RateLimit-Fallback"));
    }
}
