package com.ecommerce.shorturl;

import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ConflictException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.id.IdGenerator;
import com.ecommerce.shorturl.dto.CreateShortUrlRequest;
import com.ecommerce.shorturl.dto.ShortUrlResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ShortUrlService {

    private static final Logger log = LoggerFactory.getLogger(ShortUrlService.class);
    private static final int MAX_COLLISION_RETRIES = 5;

    private final ShortUrlRepository shortUrlRepository;
    private final IdGenerator idGenerator;
    private final MongoTemplate mongoTemplate;

    public ShortUrlService(ShortUrlRepository shortUrlRepository, IdGenerator idGenerator, MongoTemplate mongoTemplate) {
        this.shortUrlRepository = shortUrlRepository;
        this.idGenerator = idGenerator;
        this.mongoTemplate = mongoTemplate;
    }

    public ShortUrlResponse createShortUrl(CreateShortUrlRequest request, String userId, String baseUrl) {
        validateDestinationUrl(request.getOriginalUrl());

        String shortCode;
        if (request.getCustomAlias() != null && !request.getCustomAlias().trim().isEmpty()) {
            shortCode = request.getCustomAlias().trim();
            if (shortUrlRepository.existsByShortCode(shortCode)) {
                throw new ConflictException("Custom alias '" + shortCode + "' is already in use");
            }
        } else {
            shortCode = generateUniqueShortCode();
        }

        Instant now = Instant.now();
        Instant expiresAt = null;
        if (request.getExpiresInDays() != null) {
            expiresAt = now.plus(request.getExpiresInDays(), ChronoUnit.DAYS);
        }

        ShortUrl shortUrl = new ShortUrl(shortCode, request.getOriginalUrl(), now, expiresAt, userId);
        ShortUrl saved = shortUrlRepository.save(shortUrl);
        log.info("Created short URL '{}' for destination '{}' (expiresAt: {})", shortCode, request.getOriginalUrl(), expiresAt);

        return toResponse(saved, baseUrl);
    }

    public String resolveAndRecordClick(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found for code: " + shortCode));

        if (shortUrl.isExpired()) {
            log.warn("Attempted to access expired short URL '{}'", shortCode);
            throw new ShortUrlExpiredException("Short URL '" + shortCode + "' has expired");
        }

        // Atomically increment click count
        Query query = new Query(Criteria.where("shortCode").is(shortCode));
        Update update = new Update().inc("clickCount", 1).set("lastAccessedAt", Instant.now());
        mongoTemplate.findAndModify(query, update, ShortUrl.class);

        return shortUrl.getOriginalUrl();
    }

    public ShortUrlResponse getShortUrlDetails(String shortCode, String baseUrl) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found for code: " + shortCode));
        return toResponse(shortUrl, baseUrl);
    }

    public List<ShortUrlResponse> getUserShortUrls(String userId, String baseUrl) {
        return shortUrlRepository.findByCreatedByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(url -> toResponse(url, baseUrl))
                .toList();
    }

    private String generateUniqueShortCode() {
        for (int i = 0; i < MAX_COLLISION_RETRIES; i++) {
            long uniqueId = idGenerator.nextId();
            String code = Base62.encode(uniqueId);
            if (!shortUrlRepository.existsByShortCode(code)) {
                return code;
            }
            log.warn("Short code collision on '{}', retrying ({}/{})...", code, i + 1, MAX_COLLISION_RETRIES);
        }
        throw new IllegalStateException("Failed to generate unique short code after " + MAX_COLLISION_RETRIES + " attempts");
    }

    private void validateDestinationUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new BadRequestException("Original URL cannot be empty");
        }
        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new BadRequestException("Invalid URL scheme. Only HTTP and HTTPS are permitted: " + scheme);
            }
            if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
                throw new BadRequestException("Invalid URL: host is required");
            }
        } catch (URISyntaxException e) {
            throw new BadRequestException("Malformed destination URL: " + e.getMessage());
        }
    }

    private ShortUrlResponse toResponse(ShortUrl shortUrl, String baseUrl) {
        String fullShortUrl = (baseUrl != null ? baseUrl.replaceAll("/+$", "") : "") + "/r/" + shortUrl.getShortCode();
        return new ShortUrlResponse(
                shortUrl.getShortCode(),
                fullShortUrl,
                shortUrl.getOriginalUrl(),
                shortUrl.getCreatedAt(),
                shortUrl.getExpiresAt(),
                shortUrl.getClickCount(),
                shortUrl.isExpired(),
                shortUrl.getLastAccessedAt()
        );
    }
}
