package org.backend.backend.url.repository;

import org.backend.backend.auth.model.User;
import org.backend.backend.url.model.Url;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    Optional<Url> findByCustomAlias(String customAlias);

    boolean existsByShortCode(String shortCode);

    boolean existsByCustomAlias(String customAlias);

    Page<Url> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    long countByUser(User user);

    List<Url> findByExpiresAtBeforeAndIsActiveTrue(LocalDateTime now);
}