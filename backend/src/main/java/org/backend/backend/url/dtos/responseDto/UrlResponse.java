package org.backend.backend.url.dtos.responseDto;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UrlResponse {

    private Long id;
    private String originalUrl;
    private String shortCode;
    private String customAlias;
    private String title;
    private String shortUrl;       // full short URL e.g. http://localhost:5000/abc123
    private Boolean isActive;
    private Boolean isFlagged;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}