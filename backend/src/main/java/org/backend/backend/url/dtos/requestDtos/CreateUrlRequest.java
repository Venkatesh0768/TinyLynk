package org.backend.backend.url.dtos.requestDtos;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUrlRequest {

    @NotBlank(message = "URL is required")
    @Pattern(
            regexp = "^(https?://).+",
            message = "URL must start with http:// or https://"
    )
    private String originalUrl;

    @Size(min = 3, max = 50, message = "Alias must be between 3 and 50 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9_-]*$",
            message = "Alias can only contain letters, numbers, hyphens and underscores"
    )
    private String customAlias; // optional

    private String title; // optional

    private String expiresAt; // optional, ISO date string e.g. "2025-12-31T23:59:59"
}