-- Alterações na tabela announcements
ALTER TABLE announcements ADD COLUMN IF NOT EXISTS inspection_time_hours INTEGER NOT NULL DEFAULT 2;

-- Alterações na tabela transactions
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS inspection_time_hours INTEGER;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS inspection_expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS platform_fee NUMERIC(10, 2);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS net_amount NUMERIC(10, 2);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS payout_status VARCHAR(30) NOT NULL DEFAULT 'NOT_APPLICABLE';
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS payout_external_id VARCHAR(255);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS payout_paid_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS payout_failure_reason TEXT;

-- Criação da tabela inspection_proposals para negociação de prazos
CREATE TABLE IF NOT EXISTS inspection_proposals (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    announcement_id UUID NOT NULL,
    buyer_id UUID NOT NULL,
    proposed_hours INTEGER NOT NULL,
    proposal_message TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    authorization_token VARCHAR(255),
    CONSTRAINT fk_inspection_proposals_announcement FOREIGN KEY (announcement_id) REFERENCES announcements(id),
    CONSTRAINT fk_inspection_proposals_buyer FOREIGN KEY (buyer_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_inspection_proposals_announcement ON inspection_proposals(announcement_id);
CREATE INDEX IF NOT EXISTS idx_inspection_proposals_buyer ON inspection_proposals(buyer_id);
CREATE INDEX IF NOT EXISTS idx_inspection_proposals_auth_token ON inspection_proposals(authorization_token);
