package com.ecommerce.shorturl;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.security.UserPrincipal;
import com.ecommerce.shorturl.dto.CreateShortUrlRequest;
import com.ecommerce.shorturl.dto.ShortUrlResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/short-urls")
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    public ShortUrlController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShortUrlResponse>> createShortUrl(
            @Valid @RequestBody CreateShortUrlRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest) {
        String userId = principal != null ? principal.id() : null;
        String baseUrl = resolveBaseUrl(servletRequest);
        ShortUrlResponse response = shortUrlService.createShortUrl(request, userId, baseUrl);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Short URL created successfully", response));
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<ApiResponse<ShortUrlResponse>> getDetails(
            @PathVariable String shortCode,
            HttpServletRequest servletRequest) {
        String baseUrl = resolveBaseUrl(servletRequest);
        ShortUrlResponse response = shortUrlService.getShortUrlDetails(shortCode, baseUrl);
        return ResponseEntity.ok(ApiResponse.success("Short URL details retrieved successfully", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ShortUrlResponse>>> getMyShortUrls(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest) {
        String baseUrl = resolveBaseUrl(servletRequest);
        List<ShortUrlResponse> list = shortUrlService.getUserShortUrls(principal.id(), baseUrl);
        return ResponseEntity.ok(ApiResponse.success("User short URLs retrieved successfully", list));
    }

    private String resolveBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        boolean isDefaultPort = ("http".equalsIgnoreCase(scheme) && serverPort == 80)
                || ("https".equalsIgnoreCase(scheme) && serverPort == 443);
        return isDefaultPort ? scheme + "://" + serverName : scheme + "://" + serverName + ":" + serverPort;
    }
}
