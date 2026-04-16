package org.backend.backend.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.backend.backend.auth.dto.*;
import org.backend.backend.auth.exception.InvalidTokenException;
import org.backend.backend.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Authentication", description = "User registration, login, OTP, and token management")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user account")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> signup(@Valid @RequestBody SignupRequest request) {
        return new ResponseEntity<>(authService.signup(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Verify email address using OTP sent during signup")
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse> verifyOTP(@Valid @RequestBody OTPVerificationRequest request) {
        return ResponseEntity.ok(authService.verifyOTP(request));
    }

    @Operation(summary = "Resend verification OTP to email")
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse> resendOTP(@RequestParam String email) {
        return ResponseEntity.ok(authService.resendOTP(email));
    }

    @Operation(summary = "Request a password reset OTP")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(value = "email", required = false) String emailParam) {

        String email = (body != null) ? body.get("email") : null;
        if (email == null || email.isBlank()) email = emailParam;
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Email is required", null));
        }

        authService.requestPasswordReset(email.trim());
        return ResponseEntity.ok(new ApiResponse(true,
                "If an account with that email exists, an OTP has been sent.", null));
    }

    @Operation(summary = "Reset password using the OTP received by email")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @Operation(summary = "Refresh access token using a valid refresh token")
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String token = (body != null) ? body.get("refreshToken") : null;
        if ((token == null || token.isBlank()) && authHeader != null) {
            token = authHeader.startsWith("Bearer ") ? authHeader.substring(7).trim() : authHeader;
        }
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Refresh token is required");
        }

        return ResponseEntity.ok(authService.refreshToken(token));
    }

    @Operation(summary = "Logout — invalidates the provided refresh token")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@Valid @RequestBody LogoutRequest request) {
        return ResponseEntity.ok(authService.logout(request.getRefreshToken()));
    }
}
