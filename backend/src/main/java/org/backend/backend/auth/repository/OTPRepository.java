package org.backend.backend.auth.repository;

import org.backend.backend.auth.model.OTP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OTPRepository extends JpaRepository<OTP, Long> {
    Optional<OTP> findByEmailAndOtpCodeAndVerifiedFalse(String email, String otpCode);
    void deleteByEmail(String email);
}