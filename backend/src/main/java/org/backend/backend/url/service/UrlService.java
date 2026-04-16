package org.backend.backend.url.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.backend.auth.exception.UserNotFoundException;
import org.backend.backend.auth.model.User;
import org.backend.backend.auth.repository.UserRepository;
import org.backend.backend.url.config.Base62Encoder;
import org.backend.backend.url.dtos.requestDtos.CreateUrlRequest;
import org.backend.backend.url.dtos.responseDto.UrlResponse;
import org.backend.backend.url.model.Url;
import org.backend.backend.url.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlService {

    private static final String REDIS_PREFIX = "url:";
    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final Base62Encoder base62Encoder;
    private final RedisTemplate<String, String> redisTemplate;
    @Value("${app.base-url}")
    private String baseUrl;
    @Value("${app.short-url-ttl}")
    private long shortUrlTtl;

    // ─── Create Short URL ────────────────────────────────────────────────────

    @Transactional
    public UrlResponse createShortUrl(CreateUrlRequest request, String userEmail) {

        // validate custom alias not taken
        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            if (urlRepository.existsByCustomAlias(request.getCustomAlias())) {
                throw new IllegalArgumentException("Custom alias already taken");
            }
        }

        // resolve user: null means guest, but authenticated identity must resolve to a real user
        User user = null;
        if (userEmail != null) {
            user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));
        }

        // build and save entity first to get auto-generated ID
        Url url = Url.builder()
                .user(user)
                .originalUrl(request.getOriginalUrl())
                .shortCode("temp") // placeholder
                .customAlias(request.getCustomAlias())
                .title(request.getTitle())
                .expiresAt(parseExpiry(request.getExpiresAt()))
                .isActive(true)
                .isFlagged(false)
                .build();

        Url saved = urlRepository.save(url);

        // generate short code from ID using Base62
        String shortCode = base62Encoder.encode(saved.getId());
        saved.setShortCode(shortCode);
        saved = urlRepository.save(saved);

        // cache in Redis
        cacheUrl(shortCode, saved.getOriginalUrl(), saved.getExpiresAt());

        log.info("Created short URL: {} -> {}", shortCode, saved.getOriginalUrl());

        return toResponse(saved);
    }

    // ─── Resolve Short URL (used by redirect endpoint) ───────────────────────

    public String resolveUrl(String shortCode) {

        // 1. check Redis cache first
        String cached = redisTemplate.opsForValue().get(REDIS_PREFIX + shortCode);
        if (cached != null) {
            log.debug("Cache HIT for shortCode: {}", shortCode);
            return cached;
        }

        // 2. cache miss → hit DB
        log.debug("Cache MISS for shortCode: {}", shortCode);
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new IllegalArgumentException("Short URL not found"));

        // check if active
        if (!url.getIsActive()) {
            throw new IllegalStateException("This link has been disabled");
        }

        // check if expired
        if (url.getExpiresAt() != null && LocalDateTime.now().isAfter(url.getExpiresAt())) {
            throw new IllegalStateException("This link has expired");
        }

        // re-cache
        cacheUrl(shortCode, url.getOriginalUrl(), url.getExpiresAt());

        return url.getOriginalUrl();
    }

    // ─── Get My URLs (paginated) ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<UrlResponse> getMyUrls(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return urlRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::toResponse);
    }

    // ─── Get Single URL ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UrlResponse getUrlById(Long id, String userEmail) {
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("URL not found"));
        validateOwnership(url, userEmail);
        return toResponse(url);
    }

    // ─── Toggle Active ───────────────────────────────────────────────────────

    @Transactional
    public UrlResponse toggleActive(Long id, String userEmail) {
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("URL not found"));
        validateOwnership(url, userEmail);

        url.setIsActive(!url.getIsActive());
        urlRepository.save(url);

        // invalidate cache
        redisTemplate.delete(REDIS_PREFIX + url.getShortCode());
        log.info("Toggled active status for shortCode: {} → {}", url.getShortCode(), url.getIsActive());

        return toResponse(url);
    }

    // ─── Delete URL ──────────────────────────────────────────────────────────

    @Transactional
    public void deleteUrl(Long id, String userEmail) {
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("URL not found"));
        validateOwnership(url, userEmail);

        redisTemplate.delete(REDIS_PREFIX + url.getShortCode());
        urlRepository.delete(url);
        log.info("Deleted URL id: {}", id);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void cacheUrl(String shortCode, String originalUrl, LocalDateTime expiresAt) {
        String key = REDIS_PREFIX + shortCode;
        if (expiresAt != null) {
            long secondsUntilExpiry = java.time.Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
            if (secondsUntilExpiry > 0) {
                redisTemplate.opsForValue().set(key, originalUrl, secondsUntilExpiry, TimeUnit.SECONDS);
            }
        } else {
            redisTemplate.opsForValue().set(key, originalUrl, shortUrlTtl, TimeUnit.SECONDS);
        }
    }

    private void validateOwnership(Url url, String userEmail) {
        if (url.getUser() == null) return; // guest link, skip
        if (!url.getUser().getEmail().equals(userEmail)) {
            throw new SecurityException("You do not have permission to access this URL");
        }
    }

    private LocalDateTime parseExpiry(String expiresAt) {
        if (expiresAt == null || expiresAt.isBlank()) return null;
        return LocalDateTime.parse(expiresAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private UrlResponse toResponse(Url url) {
        String effectiveCode = url.getCustomAlias() != null
                ? url.getCustomAlias()
                : url.getShortCode();
        return UrlResponse.builder()
                .id(url.getId())
                .originalUrl(url.getOriginalUrl())
                .shortCode(url.getShortCode())
                .customAlias(url.getCustomAlias())
                .title(url.getTitle())
                .shortUrl(baseUrl + "/" + effectiveCode)
                .isActive(url.getIsActive())
                .isFlagged(url.getIsFlagged())
                .expiresAt(url.getExpiresAt())
                .createdAt(url.getCreatedAt())
                .build();
    }
}
