package org.backend.backend.url.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.backend.backend.url.dtos.requestDtos.CreateUrlRequest;
import org.backend.backend.url.dtos.responseDto.UrlResponse;
import org.backend.backend.url.service.UrlService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    // POST /api/v1/urls — auth optional (guest or user)
    @PostMapping
    public ResponseEntity<UrlResponse> createShortUrl(
            @Valid @RequestBody CreateUrlRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails != null ? userDetails.getUsername() : null;
        UrlResponse response = urlService.createShortUrl(request, email);
        return ResponseEntity.ok(response);
    }

    // GET /api/v1/urls — get my links (auth required)
    @GetMapping
    public ResponseEntity<Page<UrlResponse>> getMyUrls(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(urlService.getMyUrls(userDetails.getUsername(), pageable));
    }

    // GET /api/v1/urls/{id}
    @GetMapping("/{id}")
    public ResponseEntity<UrlResponse> getUrlById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(urlService.getUrlById(id, userDetails.getUsername()));
    }

    // PATCH /api/v1/urls/{id}/toggle — enable/disable
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<UrlResponse> toggleActive(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(urlService.toggleActive(id, userDetails.getUsername()));
    }

    // DELETE /api/v1/urls/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUrl(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        urlService.deleteUrl(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}