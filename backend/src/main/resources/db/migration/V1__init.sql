CREATE TABLE account
(
    id                  UUID         NOT NULL,
    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    email               VARCHAR(255) NOT NULL,
    password            VARCHAR(255),
    role                VARCHAR(255) NOT NULL,
    provider_id         VARCHAR(255),
    provider_account_id VARCHAR(255),
    email_verified   BOOLEAN      NOT NULL,
    CONSTRAINT pk_account PRIMARY KEY (id)
);

CREATE TABLE admin
(
    id         UUID NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    account_id UUID NOT NULL,
    CONSTRAINT pk_admin PRIMARY KEY (id)
);

CREATE TABLE app_config
(
    id         UUID         NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    key        VARCHAR(255) NOT NULL,
    value      VARCHAR(255),
    CONSTRAINT pk_app_config PRIMARY KEY (id)
);

CREATE TABLE customer
(
    id           UUID NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    account_id   UUID NOT NULL,
    CONSTRAINT pk_customer PRIMARY KEY (id)
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

CREATE TABLE session
(
    id         UUID NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    account_id UUID NOT NULL,
    ip_address VARCHAR(255),
    user_agent VARCHAR(255),
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_session PRIMARY KEY (id)
);

CREATE TABLE verification_token
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
    CONSTRAINT pk_verification_token PRIMARY KEY (id)
);

ALTER TABLE account
    ADD CONSTRAINT uc_8c4cf4ce83e03f7d46b0ee47c UNIQUE (provider_id, provider_account_id, role);

ALTER TABLE account
    ADD CONSTRAINT uc_account_email UNIQUE (email);

ALTER TABLE admin
    ADD CONSTRAINT uc_admin_account UNIQUE (account_id);

ALTER TABLE app_config
    ADD CONSTRAINT uc_app_config_key UNIQUE (key);

ALTER TABLE customer
    ADD CONSTRAINT uc_customer_account UNIQUE (account_id);

ALTER TABLE verification_token
    ADD CONSTRAINT uc_verification_token_token UNIQUE (token);

ALTER TABLE admin
    ADD CONSTRAINT FK_ADMIN_ON_ACCOUNT FOREIGN KEY (account_id) REFERENCES account (id);

ALTER TABLE customer
    ADD CONSTRAINT FK_CUSTOMER_ON_ACCOUNT FOREIGN KEY (account_id) REFERENCES account (id);

ALTER TABLE session
    ADD CONSTRAINT FK_SESSION_ON_ACCOUNT FOREIGN KEY (account_id) REFERENCES account (id);

ALTER TABLE verification_token
    ADD CONSTRAINT FK_VERIFICATION_TOKEN_ON_ACCOUNT FOREIGN KEY (account_id) REFERENCES account (id);