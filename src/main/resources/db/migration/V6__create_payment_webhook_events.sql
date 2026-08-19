CREATE TABLE payment_webhook_events (
                                        id                UUID PRIMARY KEY,
                                        created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
                                        updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
                                        external_event_id VARCHAR(255),
                                        type              VARCHAR(100),
                                        payload           TEXT NOT NULL,
                                        status            VARCHAR(50) NOT NULL
);

CREATE INDEX idx_payment_webhook_events_external_event ON payment_webhook_events(external_event_id);
CREATE INDEX idx_payment_webhook_events_status ON payment_webhook_events(status);