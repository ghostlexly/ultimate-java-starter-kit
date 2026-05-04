CREATE TABLE accounts
(
    id                  UUID         NOT NULL,
    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    email               VARCHAR(255) NOT NULL,
    password            VARCHAR(255),
    role                VARCHAR(255) NOT NULL,
    provider_id         VARCHAR(255),
    provider_account_id VARCHAR(255),
    email_verified      BOOLEAN      NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (id)
);

CREATE TABLE admins
(
    id         UUID NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    account_id UUID NOT NULL,
    CONSTRAINT pk_admins PRIMARY KEY (id)
);

CREATE TABLE app_configs
(
    id         UUID         NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    key        VARCHAR(255) NOT NULL,
    value      VARCHAR(255),
    CONSTRAINT pk_app_configs PRIMARY KEY (id)
);

CREATE TABLE customers
(
    id         UUID NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    account_id UUID NOT NULL,
    CONSTRAINT pk_customers PRIMARY KEY (id)
);

CREATE TABLE media
(
    id         UUID         NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    file_name  VARCHAR(255) NOT NULL,
    key        VARCHAR(500) NOT NULL,
    mime_type  VARCHAR(255) NOT NULL,
    size       BIGINT       NOT NULL,
    CONSTRAINT pk_media PRIMARY KEY (id)
);

CREATE TABLE sessions
(
    id         UUID NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    account_id UUID NOT NULL,
    ip_address VARCHAR(255),
    user_agent VARCHAR(255),
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_sessions PRIMARY KEY (id)
);

CREATE TABLE verification_tokens
(
    id         UUID         NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    token      VARCHAR(255) NOT NULL,
    type       VARCHAR(255) NOT NULL,
    value      VARCHAR(255),
    attempts   INTEGER      NOT NULL,
    account_id UUID         NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_verification_tokens PRIMARY KEY (id)
);

ALTER TABLE accounts
    ADD CONSTRAINT uc_6db797e7dab5534cb41591a18 UNIQUE (provider_id, provider_account_id, role);

ALTER TABLE accounts
    ADD CONSTRAINT uc_accounts_email UNIQUE (email);

ALTER TABLE admins
    ADD CONSTRAINT uc_admins_account UNIQUE (account_id);

ALTER TABLE app_configs
    ADD CONSTRAINT uc_app_configs_key UNIQUE (key);

ALTER TABLE customers
    ADD CONSTRAINT uc_customers_account UNIQUE (account_id);

ALTER TABLE verification_tokens
    ADD CONSTRAINT uc_verification_tokens_token UNIQUE (token);

ALTER TABLE admins
    ADD CONSTRAINT FK_ADMINS_ON_ACCOUNT FOREIGN KEY (account_id) REFERENCES accounts (id);

ALTER TABLE customers
    ADD CONSTRAINT FK_CUSTOMERS_ON_ACCOUNT FOREIGN KEY (account_id) REFERENCES accounts (id);

ALTER TABLE sessions
    ADD CONSTRAINT FK_SESSIONS_ON_ACCOUNT FOREIGN KEY (account_id) REFERENCES accounts (id);

ALTER TABLE verification_tokens
    ADD CONSTRAINT FK_VERIFICATION_TOKENS_ON_ACCOUNT FOREIGN KEY (account_id) REFERENCES accounts (id);