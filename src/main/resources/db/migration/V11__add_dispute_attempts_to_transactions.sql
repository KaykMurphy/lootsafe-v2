-- Adiciona a coluna dispute_attempts na tabela transactions para controle de tentativas de reabertura de disputa
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS dispute_attempts INTEGER NOT NULL DEFAULT 0;
