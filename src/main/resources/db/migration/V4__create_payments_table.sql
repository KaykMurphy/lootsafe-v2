ALTER TABLE transactions DROP COLUMN IF EXISTS mercadopago_payment_id;

CREATE TABLE payments (
                          id                 UUID PRIMARY KEY,
                          created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
                          updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
                          transaction_id     UUID                     NOT NULL,
                          provider           VARCHAR(50)              NOT NULL,
                          external_id        VARCHAR(255),
                          external_reference VARCHAR(255),
                          idempotency_key    VARCHAR(255)             NOT NULL UNIQUE,
                          status             VARCHAR(50)              NOT NULL,
                          amount             DECIMAL(10, 2)           NOT NULL,
                          payment_method     VARCHAR(50),
                          pix_code           TEXT,
                          qr_code_base64     TEXT,
                          ticket_url         VARCHAR(500),
                          status_detail      VARCHAR(255),
                          expires_at         TIMESTAMP WITH TIME ZONE,
                          paid_at            TIMESTAMP WITH TIME ZONE,

                          CONSTRAINT fk_payments_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
);

CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);
CREATE INDEX idx_payments_external_id ON payments(external_id);
CREATE INDEX idx_payments_external_reference ON payments(external_reference);
CREATE INDEX idx_payments_idempotency_key ON payments(idempotency_key);