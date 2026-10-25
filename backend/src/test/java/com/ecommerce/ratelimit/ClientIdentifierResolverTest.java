package com.ecommerce.ratelimit;

import com.ecommerce.model.UserRole;
import com.ecommerce.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIdentifierResolverTest {

    private RateLimitingProperties properties;
    private ClientIdentifierResolver resolver;

    @BeforeEach
    void setUp() {
        properties = new RateLimitingProperties();
        resolver = new ClientIdentifierResolver(properties);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should resolve identifier from UserPrincipal for authenticated user")
    void shouldResolveAuthenticatedUserPrincipal() {
        UserPrincipal principal = new UserPrincipal("usr-12345", "user@example.com", UserRole.CUSTOMER);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null,
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + principal.role().name())));
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");

        String identifier = resolver.resolveIdentifier(request);

        assertEquals("user:usr-12345", identifier);
    }

    @Test
    @DisplayName("Should resolve identifier from remote IP for anonymous client when trustProxy is false")
    void shouldResolveRemoteIpWhenTrustProxyIsFalse() {
        properties.setTrustProxy(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.195");
        request.addHeader("X-Forwarded-For", "198.51.100.1, 10.0.0.1");

        String identifier = resolver.resolveIdentifier(request);

        // Header must be ignored to prevent spoofing
        assertEquals("ip:203.0.113.195", identifier);
    }

    @Test
    @DisplayName("Should resolve identifier from X-Forwarded-For header when trustProxy is true")
    void shouldResolveForwardedIpWhenTrustProxyIsTrue() {
        properties.setTrustProxy(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.2");
        request.addHeader("X-Forwarded-For", "198.51.100.42, 10.0.0.1");

        String identifier = resolver.resolveIdentifier(request);

        assertEquals("ip:198.51.100.42", identifier);
    }

    @Test
    @DisplayName("Should fallback to remote IP when X-Forwarded-For is missing even if trustProxy is true")
    void shouldFallbackToRemoteIpWhenHeaderMissing() {
        properties.setTrustProxy(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.50");

        String identifier = resolver.resolveIdentifier(request);

        assertEquals("ip:203.0.113.50", identifier);
    }
}
