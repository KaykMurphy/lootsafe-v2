CREATE TABLE announcements
(
    id                    UUID                     NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    title                 TEXT,
    description           TEXT,
    credentials_encrypted TEXT                     NOT NULL,
    notes                 VARCHAR(255),
    pix_key               VARCHAR(255)             NOT NULL,
    token                 VARCHAR(255)             NOT NULL,
    status                VARCHAR(255)             NOT NULL,
    seller_id             UUID                     NOT NULL,
    CONSTRAINT pk_announcements PRIMARY KEY (id)
);

CREATE TABLE dispute_chats
(
    id               UUID                     NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    status           VARCHAR(255),
    reason           TEXT                     NOT NULL,
    resolution_notes TEXT,
    transaction_id   UUID                     NOT NULL,
    initiated_by     UUID                     NOT NULL,
    CONSTRAINT pk_dispute_chats PRIMARY KEY (id)
);

CREATE TABLE transactions
(
    id                      UUID                     NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    mercado_pago_payment_id VARCHAR(255),
    amount                  DECIMAL(10, 2),
    status                  VARCHAR(255),
    announcement_id         UUID                     NOT NULL,
    buyer_id                UUID                     NOT NULL,
    seller_id               UUID                     NOT NULL,
    CONSTRAINT pk_transactions PRIMARY KEY (id)
);

CREATE TABLE users
(
    id            UUID                     NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    name          VARCHAR(255)             NOT NULL,
    email         VARCHAR(255)             NOT NULL,
    password_hash VARCHAR(255)             NOT NULL,
    pix_key       VARCHAR(255),
    role          VARCHAR(255)             NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

ALTER TABLE announcements
    ADD CONSTRAINT uc_announcements_token UNIQUE (token);

ALTER TABLE dispute_chats
    ADD CONSTRAINT uc_dispute_chats_transaction UNIQUE (transaction_id);

ALTER TABLE transactions
    ADD CONSTRAINT uc_transactions_announcement UNIQUE (announcement_id);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE announcements
    ADD CONSTRAINT FK_ANNOUNCEMENTS_ON_SELLER FOREIGN KEY (seller_id) REFERENCES users (id);

ALTER TABLE dispute_chats
    ADD CONSTRAINT FK_DISPUTE_CHATS_ON_INITIATED_BY FOREIGN KEY (initiated_by) REFERENCES users (id);

ALTER TABLE dispute_chats
    ADD CONSTRAINT FK_DISPUTE_CHATS_ON_TRANSACTION FOREIGN KEY (transaction_id) REFERENCES transactions (id);

ALTER TABLE transactions
    ADD CONSTRAINT FK_TRANSACTIONS_ON_ANNOUNCEMENT FOREIGN KEY (announcement_id) REFERENCES announcements (id);

ALTER TABLE transactions
    ADD CONSTRAINT FK_TRANSACTIONS_ON_BUYER FOREIGN KEY (buyer_id) REFERENCES users (id);

ALTER TABLE transactions
    ADD CONSTRAINT FK_TRANSACTIONS_ON_SELLER FOREIGN KEY (seller_id) REFERENCES users (id);