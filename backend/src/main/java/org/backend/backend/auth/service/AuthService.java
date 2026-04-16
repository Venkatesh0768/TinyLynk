package org.backend.backend.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.backend.auth.dto.*;
import org.backend.backend.auth.exception.*;
import org.backend.backend.auth.model.RefreshToken;
import org.backend.backend.auth.model.Role;
import org.backend.backend.auth.model.RoleType;
import org.backend.backend.auth.model.User;
import org.backend.backend.auth.repository.OTPRepository;
import org.backend.backend.auth.repository.RefreshTokenRepository;
import org.backend.backend.auth.repository.RoleRepository;
import org.backend.backend.auth.repository.UserRepository;
import org.backend.backend.auth.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OTPRepository otpRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;
    private final OTPService otpService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // ─── Signup ──────────────────────────────────────────────────────────────

    @Transactional
    public ApiResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .emailVerified(false)
                .enabled(false)
                .provider("local")
                .build();

        Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Default role ROLE_USER not found"));
        user.getRoles().add(userRole);

        userRepository.save(user);
        otpService.generateAndSendOTP(user.getEmail());

        return new ApiResponse(true,
                "Registration successful. Please verify your email with the OTP sent to " +
                        user.getEmail(), null);
    }

    // ─── Login with brute-force protection ───────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Invalid email or password"));

        // Check account lock BEFORE attempting authentication
        if (user.isAccountLocked()) {
            long minutesLeft = java.time.Duration.between(
                    LocalDateTime.now(), user.getAccountLockedUntil()).toMinutes() + 1;
            throw new AccountLockedException(
                    "Account is temporarily locked due to too many failed attempts. " +
                    "Try again in " + minutesLeft + " minute(s).");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // Successful login — reset counter
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            if (!user.isEmailVerified()) {
                throw new EmailNotVerifiedException("Please verify your email before logging in");
            }

            String accessToken = tokenProvider.generateToken(authentication);
            RefreshToken refreshToken = otpService.createRefreshToken(user);

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .expiresIn(jwtExpiration / 1000)
                    .user(convertToUserDTO(user))
                    .build();

        } catch (BadCredentialsException | DisabledException ex) {
            // Increment failed attempts
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
                userRepository.save(user);
                log.warn("Account locked for user={} after {} failed attempts", user.getEmail(), attempts);
                throw new AccountLockedException(
                        "Account locked for " + LOCK_DURATION_MINUTES + " minutes due to too many failed login attempts.");
            }

            userRepository.save(user);
            int remaining = MAX_FAILED_ATTEMPTS - attempts;
            throw new BadCredentialsException(
                    "Invalid email or password. " + remaining + " attempt(s) remaining before lockout.");
        }
    }

    // ─── OTP Verification ────────────────────────────────────────────────────

    @Transactional
    public ApiResponse verifyOTP(OTPVerificationRequest request) {
        if (!otpService.validateOTP(request.getEmail(), request.getOtp())) {
            throw new InvalidOTPException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setEmailVerified(true);
        user.setEnabled(true);
        userRepository.save(user);

        return new ApiResponse(true, "Email verified successfully. You can now log in", null);
    }

    public ApiResponse resendOTP(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.isEmailVerified()) {
            throw new EmailAlreadyVerifiedException("Email is already verified");
        }

        otpService.generateAndSendOTP(email);
        return new ApiResponse(true, "OTP sent to " + email, null);
    }

    // ─── Password Reset ───────────────────────────────────────────────────────

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email)
                .ifPresent(user -> otpService.generateAndSendPasswordResetOTP(email));
    }

    @Transactional
    public ApiResponse resetPassword(ResetPasswordRequest request) {
        if (!otpService.validateOTP(request.getEmail(), request.getOtp())) {
            throw new InvalidOTPException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // Unlock account on password reset
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        return new ApiResponse(true, "Password reset successful. Please log in with your new password.", null);
    }

    // ─── Token Refresh ────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse refreshToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .map(otpService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    List<String> roles = user.getRoles().stream()
                            .map(r -> r.getName().name())
                            .toList();
                    String newAccessToken = tokenProvider.generateTokenFromUsername(user.getEmail(), roles);
                    return AuthResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(token)
                            .expiresIn(jwtExpiration / 1000)
                            .user(convertToUserDTO(user))
                            .build();
                })
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    @Transactional
    public ApiResponse logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
        return new ApiResponse(true, "Logged out successfully", null);
    }

    @Transactional
    public ApiResponse logoutAll(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        refreshTokenRepository.deleteByUser(user);
        return new ApiResponse(true, "All sessions revoked successfully", null);
    }

    // ─── Change Password ─────────────────────────────────────────────────────

    @Transactional
    public ApiResponse changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return new ApiResponse(true, "Password changed successfully", null);
    }

    // ─── Update Profile ───────────────────────────────────────────────────────

    @Transactional
    public ApiResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName());
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }
        userRepository.save(user);
        return new ApiResponse(true, "Profile updated successfully", convertToUserDTO(user));
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    public UserDTO convertToUserDTO(User user) {
        return UserDTO.builder()
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
    }
}