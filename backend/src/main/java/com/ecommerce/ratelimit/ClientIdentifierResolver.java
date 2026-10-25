package com.ecommerce.ratelimit;

import com.ecommerce.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolves client identifier for rate-limiting purposes.
 * Authenticated users are keyed by their unique User ID.
 * Anonymous clients are keyed by remote IP, honoring X-Forwarded-For
 * only when proxy trust is explicitly configured.
 */
@Component
public class ClientIdentifierResolver {

    private final RateLimitingProperties properties;

    public ClientIdentifierResolver(RateLimitingProperties properties) {
        this.properties = properties;
    }

    public String resolveIdentifier(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            if (auth.getPrincipal() instanceof UserPrincipal principal && StringUtils.hasText(principal.id())) {
                return "user:" + principal.id();
            }
            if (auth.getPrincipal() instanceof String principalStr
                    && StringUtils.hasText(principalStr)
                    && !"anonymousUser".equalsIgnoreCase(principalStr)) {
                return "user:" + principalStr;
            }
        }

        return "ip:" + extractClientIp(request);
    }

    public String extractClientIp(HttpServletRequest request) {
        if (properties.isTrustProxy()) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(forwardedFor)) {
                String firstIp = forwardedFor.split(",")[0].trim();
                if (StringUtils.hasText(firstIp)) {
                    return firstIp;
                }
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr : "unknown";
    }
}
