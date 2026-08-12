CREATE TABLE refresh_tokens
(
    id          UUID                        NOT NULL,
    token       VARCHAR(255)                NOT NULL,
    user_id     UUID                        NOT NULL,
    expiry_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    revoked     BOOLEAN                     NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id)
);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT uc_refresh_tokens_token UNIQUE (token);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT uc_refresh_tokens_user UNIQUE (user_id);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_on_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);