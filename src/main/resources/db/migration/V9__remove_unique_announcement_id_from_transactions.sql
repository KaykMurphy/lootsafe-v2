ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_announcement_id_key;
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS uc_transactions_announcement_id;
CREATE INDEX IF NOT EXISTS idx_transactions_announcement_id ON transactions(announcement_id);
