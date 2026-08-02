CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                       updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                       name VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       pix_key VARCHAR(255),
                       role VARCHAR(50) NOT NULL
);

CREATE TABLE announcements (
                               id UUID PRIMARY KEY,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               title VARCHAR(255) NOT NULL,
                               description TEXT,
                               credentials_encrypted TEXT NOT NULL,
                               notes TEXT,
                               pix_key VARCHAR(255) NOT NULL,
                               token VARCHAR(255) NOT NULL UNIQUE,
                               status VARCHAR(50) NOT NULL,
                               seller_id UUID NOT NULL,
                               CONSTRAINT fk_announcements_seller FOREIGN KEY (seller_id) REFERENCES users(id)
);

CREATE TABLE transactions (
                              id UUID PRIMARY KEY,
                              created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                              updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                              announcement_id UUID NOT NULL UNIQUE,
                              buyer_id UUID NOT NULL,
                              seller_id UUID NOT NULL,
                              mercadopago_payment_id VARCHAR(255),
                              status VARCHAR(50) NOT NULL,
                              amount DECIMAL(10,2) NOT NULL,
                              CONSTRAINT fk_transactions_announcement FOREIGN KEY (announcement_id) REFERENCES announcements(id),
                              CONSTRAINT fk_transactions_buyer FOREIGN KEY (buyer_id) REFERENCES users(id),
                              CONSTRAINT fk_transactions_seller FOREIGN KEY (seller_id) REFERENCES users(id)
);

CREATE TABLE dispute_chats (
                               id UUID PRIMARY KEY,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               transaction_id UUID NOT NULL UNIQUE,
                               initiated_by UUID NOT NULL,
                               status VARCHAR(50) NOT NULL,
                               reason TEXT NOT NULL,
                               resolution_notes TEXT,
                               CONSTRAINT fk_dispute_chats_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id),
                               CONSTRAINT fk_dispute_chats_initiator FOREIGN KEY (initiated_by) REFERENCES users(id)
);

CREATE INDEX idx_announcements_seller ON announcements(seller_id);
CREATE INDEX idx_transactions_buyer ON transactions(buyer_id);
CREATE INDEX idx_transactions_seller ON transactions(seller_id);
CREATE INDEX idx_dispute_chats_transaction ON dispute_chats(transaction_id);
CREATE INDEX idx_dispute_chats_initiator ON dispute_chats(initiated_by);



