CREATE TABLE dispute_messages (
                                  id              UUID PRIMARY KEY,
                                  created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
                                  updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
                                  dispute_chat_id UUID NOT NULL,
                                  sender_id       UUID NOT NULL,
                                  content         TEXT NOT NULL,
                                  CONSTRAINT fk_dispute_messages_dispute FOREIGN KEY (dispute_chat_id) REFERENCES dispute_chats(id),
                                  CONSTRAINT fk_dispute_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id)
);

CREATE INDEX idx_dispute_messages_dispute ON dispute_messages(dispute_chat_id);
CREATE INDEX idx_dispute_messages_sender ON dispute_messages(sender_id);