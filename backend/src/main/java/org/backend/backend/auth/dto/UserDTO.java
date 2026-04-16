package org.backend.backend.auth.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private boolean emailVerified;
    private boolean enabled;
    private String provider;
    private String profileImageUrl;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private Set<String> roles;
}