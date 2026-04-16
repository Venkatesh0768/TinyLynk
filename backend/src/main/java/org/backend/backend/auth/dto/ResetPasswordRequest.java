package org.backend.backend.auth.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResetPasswordRequest {
    private String email;
    private String otp;
    private String newPassword;
}