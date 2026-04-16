package org.backend.backend.auth.service;

import lombok.RequiredArgsConstructor;
import org.backend.backend.auth.exception.InvalidTokenException;
import org.backend.backend.auth.model.OTP;
import org.backend.backend.auth.model.RefreshToken;
import org.backend.backend.auth.model.User;
import org.backend.backend.auth.repository.OTPRepository;
import org.backend.backend.auth.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OTPService {

    private final OTPRepository otpRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;

    @Value("${otp.expiration}")
    private long otpExpiration;

    @Value("${otp.length}")
    private int otpLength;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    @Transactional
    public void generateAndSendOTP(String email) {
        // Delete existing OTPs
        otpRepository.deleteByEmail(email);

        // Generate new OTP
        String otpCode = generateOTPCode();
        LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(otpExpiration / 1000);

        OTP otp = OTP.builder()
                .email(email)
                .otpCode(otpCode)
                .expiryTime(expiryTime)
                .build();

        otpRepository.save(otp);

        // Send email
        emailService.sendOTPEmail(email, otpCode);
    }

    @Transactional
    public void generateAndSendPasswordResetOTP(String email) {
        // Delete existing OTPs (keep only one active code)
        otpRepository.deleteByEmail(email);

        String otpCode = generateOTPCode();
        LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(otpExpiration / 1000);

        OTP otp = OTP.builder()
                .email(email)
                .otpCode(otpCode)
                .expiryTime(expiryTime)
                .build();

        otpRepository.save(otp);

        // Send reset-specific email
        emailService.sendPasswordResetOTPEmail(email, otpCode);
    }

    public boolean validateOTP(String email, String otpCode) {
        Optional<OTP> otpOptional = otpRepository
                .findByEmailAndOtpCodeAndVerifiedFalse(email, otpCode);

        if (otpOptional.isEmpty()) {
            return false;
        }

        OTP otp = otpOptional.get();

        if (otp.getExpiryTime().isBefore(LocalDateTime.now())) {
            return false;
        }

        otp.setVerified(true);
        otpRepository.save(otp);

        return true;
    }

    @Transactional
    public RefreshToken createRefreshToken(User user) {

        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser(user);

        RefreshToken refreshToken;
        if (existingToken.isPresent()) {
            refreshToken = existingToken.get();
            refreshToken.setToken(UUID.randomUUID().toString());
            refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000));
        } else {
            refreshToken = RefreshToken.builder()
                    .user(user)
                    .token(UUID.randomUUID().toString())
                    .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                    .build();
        }

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new InvalidTokenException("Refresh token expired");
        }
        return token;
    }

    private String generateOTPCode() {
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }
}