package com.ecommerce.shorturl;

import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ConflictException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.id.IdGenerator;
import com.ecommerce.shorturl.dto.CreateShortUrlRequest;
import com.ecommerce.shorturl.dto.ShortUrlResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

    @Mock
    private ShortUrlRepository repository;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private MongoTemplate mongoTemplate;

    private ShortUrlService service;

    @BeforeEach
    void setUp() {
        service = new ShortUrlService(repository, idGenerator, mongoTemplate);
    }

    @Test
    @DisplayName("Should successfully shorten a valid URL with generated Snowflake Base62 code")
    void testShortenValidUrl() {
        when(idGenerator.nextId()).thenReturn(123456789L);
        when(repository.existsByShortCode(any())).thenReturn(false);
        when(repository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateShortUrlRequest request = new CreateShortUrlRequest("https://ecommerce.example.com/products/42", null, 30);
        ShortUrlResponse response = service.createShortUrl(request, "user-1", "http://localhost:8080");

        assertThat(response).isNotNull();
        assertThat(response.getShortCode()).isNotBlank();
        assertThat(response.getOriginalUrl()).isEqualTo("https://ecommerce.example.com/products/42");
        assertThat(response.getShortUrl()).contains("/r/" + response.getShortCode());
        assertThat(response.getExpiresAt()).isNotNull();
        verify(repository, times(1)).save(any(ShortUrl.class));
    }

    @Test
    @DisplayName("Should use custom alias if provided and available")
    void testCustomAlias() {
        when(repository.existsByShortCode("summer-deal")).thenReturn(false);
        when(repository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateShortUrlRequest request = new CreateShortUrlRequest("https://example.com/deal", "summer-deal", null);
        ShortUrlResponse response = service.createShortUrl(request, "user-1", "http://localhost:8080");

        assertThat(response.getShortCode()).isEqualTo("summer-deal");
        assertThat(response.getExpiresAt()).isNull();
    }

    @Test
    @DisplayName("Should reject custom alias if already in use")
    void testDuplicateCustomAliasRejected() {
        when(repository.existsByShortCode("already-taken")).thenReturn(true);

        CreateShortUrlRequest request = new CreateShortUrlRequest("https://example.com", "already-taken", null);
        assertThatThrownBy(() -> service.createShortUrl(request, "user-1", "http://localhost:8080"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already in use");
    }

    @Test
    @DisplayName("Should reject invalid or unsupported URL schemes")
    void testInvalidUrlSchemeRejected() {
        CreateShortUrlRequest ftpReq = new CreateShortUrlRequest("ftp://files.example.com/pub", null, null);
        assertThatThrownBy(() -> service.createShortUrl(ftpReq, null, "http://localhost:8080"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only HTTP and HTTPS are permitted");

        CreateShortUrlRequest jsReq = new CreateShortUrlRequest("javascript:alert(1)", null, null);
        assertThatThrownBy(() -> service.createShortUrl(jsReq, null, "http://localhost:8080"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Should resolve short code and increment click count")
    void testResolveAndRecordClick() {
        ShortUrl entity = new ShortUrl("code123", "https://example.com/item", Instant.now(), null, "user-1");
        when(repository.findByShortCode("code123")).thenReturn(Optional.of(entity));

        String resolved = service.resolveAndRecordClick("code123");

        assertThat(resolved).isEqualTo("https://example.com/item");
        verify(mongoTemplate, times(1)).findAndModify(any(Query.class), any(Update.class), eq(ShortUrl.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for unknown short code")
    void testUnknownShortCodeThrowsNotFound() {
        when(repository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveAndRecordClick("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Short URL not found");
    }

    @Test
    @DisplayName("Should throw ShortUrlExpiredException when accessing expired short code")
    void testExpiredShortCodeThrowsException() {
        Instant past = Instant.now().minus(2, ChronoUnit.DAYS);
        ShortUrl expired = new ShortUrl("expired-code", "https://example.com", past.minus(10, ChronoUnit.DAYS), past, "user-1");
        when(repository.findByShortCode("expired-code")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resolveAndRecordClick("expired-code"))
                .isInstanceOf(ShortUrlExpiredException.class)
                .hasMessageContaining("has expired");
    }
}
