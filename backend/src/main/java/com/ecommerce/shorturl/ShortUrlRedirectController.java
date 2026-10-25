package com.ecommerce.shorturl;

import com.ecommerce.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.URI;
import java.util.Set;

@Controller
public class ShortUrlRedirectController {

    private static final Logger log = LoggerFactory.getLogger(ShortUrlRedirectController.class);

    private static final Set<String> RESERVED_KEYWORDS = Set.of(
            "api", "error", "actuator", "swagger-ui", "v3", "health", "favicon.ico"
    );

    private final ShortUrlService shortUrlService;

    public ShortUrlRedirectController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    @GetMapping({"/r/{shortCode}", "/{shortCode:[a-zA-Z0-9_-]{3,32}}"})
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        if (RESERVED_KEYWORDS.contains(shortCode.toLowerCase())) {
            throw new ResourceNotFoundException("Reserved path: " + shortCode);
        }

        String targetUrl = shortUrlService.resolveAndRecordClick(shortCode);
        log.debug("Redirecting short code '{}' -> '{}'", shortCode, targetUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(targetUrl));
        // HTTP 302 Found ensures clients contact the server on every click, allowing accurate analytics & expiration enforcement
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
