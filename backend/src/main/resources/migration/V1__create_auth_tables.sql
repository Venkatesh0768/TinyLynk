CREATE TABLE otps
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    email       VARCHAR(255)          NOT NULL,
    otp_code    VARCHAR(255)          NOT NULL,
    expiry_time datetime              NOT NULL,
    verified    BIT(1)                NOT NULL,
    created_at  datetime              NOT NULL,
    CONSTRAINT pk_otps PRIMARY KEY (id)
);

CREATE TABLE refresh_tokens
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    user_id     BIGINT                NULL,
    token       VARCHAR(255)          NOT NULL,
    expiry_date datetime              NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id)
);

CREATE TABLE roles
(
    id   BIGINT AUTO_INCREMENT NOT NULL,
    name VARCHAR(255)          NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id)
);

CREATE TABLE user_roles
(
    role_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (role_id, user_id)
);

CREATE TABLE users
(
    id                    BIGINT AUTO_INCREMENT NOT NULL,
    email                 VARCHAR(255)          NOT NULL,
    password              VARCHAR(255)          NULL,
    first_name            VARCHAR(255)          NOT NULL,
    last_name             VARCHAR(255)          NOT NULL,
    email_verified        BIT(1)                NOT NULL,
    enabled               BIT(1)                NOT NULL,
    provider              VARCHAR(255)          NOT NULL,
    provider_id           VARCHAR(255)          NULL,
    profile_image_url     VARCHAR(255)          NULL,
    failed_login_attempts INT                   NOT NULL,
    account_locked_until  datetime              NULL,
    last_login_at         datetime              NULL,
    created_at            datetime              NOT NULL,
    updated_at            datetime              NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT uc_refresh_tokens_token UNIQUE (token);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT uc_refresh_tokens_user UNIQUE (user_id);

ALTER TABLE roles
    ADD CONSTRAINT uc_roles_name UNIQUE (name);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT FK_REFRESH_TOKENS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE user_roles
    ADD CONSTRAINT fk_userol_on_role FOREIGN KEY (role_id) REFERENCES roles (id);

ALTER TABLE user_roles
    ADD CONSTRAINT fk_userol_on_user FOREIGN KEY (user_id) REFERENCES users (id);