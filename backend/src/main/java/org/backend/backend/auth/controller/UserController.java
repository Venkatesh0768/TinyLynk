package org.backend.backend.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.backend.backend.auth.dto.ApiResponse;
import org.backend.backend.auth.dto.ChangePasswordRequest;
import org.backend.backend.auth.dto.UpdateProfileRequest;
import org.backend.backend.auth.dto.UserDTO;
import org.backend.backend.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@Tag(name = "User", description = "Authenticated user self-service operations")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final org.backend.backend.auth.repository.UserRepository userRepository;
    private final AuthService authService;

    @Operation(summary = "Get current user's full profile")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getMe(Authentication authentication) {
        String email = authentication.getName();
        org.backend.backend.auth.model.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.backend.backend.auth.exception.UserNotFoundException("User not found"));

        UserDTO dto = UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .emailVerified(user.isEmailVerified())
                .enabled(user.isEnabled())
                .provider(user.getProvider())
                .profileImageUrl(user.getProfileImageUrl())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()))
                .build();

        return ResponseEntity.ok(new ApiResponse(true, "Profile retrieved", dto));
    }

    @Operation(summary = "Update current user's profile (name, avatar)")
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(authentication.getName(), request));
    }

    @Operation(summary = "Change password (requires current password)")
    @PostMapping("/me/change-password")
    public ResponseEntity<ApiResponse> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(authentication.getName(), request));
    }

    @Operation(summary = "Revoke all active sessions (logout all devices)")
    @DeleteMapping("/me/sessions")
    public ResponseEntity<ApiResponse> logoutAll(Authentication authentication) {
        return ResponseEntity.ok(authService.logoutAll(authentication.getName()));
    }
}