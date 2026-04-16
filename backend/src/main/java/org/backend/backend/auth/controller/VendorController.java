package org.backend.backend.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.backend.backend.auth.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Vendor endpoints — accessible to ROLE_VENDOR and ROLE_ADMIN.
 *
 * Protection is enforced at two levels:
 *  1. SecurityConfig — /api/vendor/** requires hasAnyRole("VENDOR", "ADMIN")
 *  2. @PreAuthorize — method-level guard as a second layer
 */
@Tag(name = "Vendor", description = "Vendor operations — requires ROLE_VENDOR or ROLE_ADMIN")
@RestController
@RequestMapping("/api/vendor")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class VendorController {

    @Operation(summary = "Vendor dashboard")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse> getDashboard(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse(true, "Vendor dashboard",
                Map.of("email", authentication.getName(), "role", "VENDOR")));
    }
}
