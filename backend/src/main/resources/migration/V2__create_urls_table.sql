CREATE TABLE urls
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id      BIGINT       NULL,
    original_url TEXT         NOT NULL,
    short_code   VARCHAR(20)  NOT NULL,
    custom_alias VARCHAR(100) NULL,
    title        VARCHAR(255) NULL,
    is_active    BOOLEAN   DEFAULT TRUE,
    is_flagged   BOOLEAN   DEFAULT FALSE,
    expires_at   TIMESTAMP    NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_short_code UNIQUE (short_code),
    CONSTRAINT uq_custom_alias UNIQUE (custom_alias),
    CONSTRAINT fk_urls_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL,

    INDEX idx_short_code (short_code),
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at)
);