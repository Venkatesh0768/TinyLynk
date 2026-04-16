package org.backend.backend.url.model;


import jakarta.persistence.*;
import lombok.*;
import org.backend.backend.auth.model.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "urls")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(name = "original_url" , unique = true , columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "short_code" , unique = true , nullable = false , length = 20)
    private String shortCode;

    @Column(name = "custom_alias" , unique = true, length = 100)
    private String customAlias;

    @Column(name = "title" , length = 255)
    private  String title;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_flagged")
    @Builder.Default
    private Boolean isFlagged = false;

    @Column(name = "expires_at")
    private LocalDateTime  expiresAt;

    @Column(name = "created_at" , updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt = LocalDateTime.now();
    }

}
