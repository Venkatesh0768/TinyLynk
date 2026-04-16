package org.backend.backend.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Nullable — OAuth2 users (Google/GitHub) do not have a local password.
     * Local users always have a BCrypt-hashed password.
     */
    @Column(nullable = true)
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(nullable = false)
    private boolean enabled = false;

    /**
     * Authentication provider: "local" | "google" | "github"
     */
    @Column(nullable = false)
    @Builder.Default
    private String provider = "local";

    /**
     * Subject / user-id returned by the OAuth2 provider (null for local users).
     */
    @Column
    private String providerId;

    /**
     * Profile picture URL — populated from OAuth2 provider data.
     */
    @Column
    private String profileImageUrl;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column
    private LocalDateTime accountLockedUntil;

    @Column
    private LocalDateTime lastLoginAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public boolean isAccountLocked() {
        return accountLockedUntil != null && LocalDateTime.now().isBefore(accountLockedUntil);
    }
}