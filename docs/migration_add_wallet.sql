-- ============================================================
-- MIGRATION: Add tempBalance column to bidders table
-- ============================================================
-- Purpose: Support Wallet Value Object with main and temp balance
-- Date: May 6, 2026
-- ============================================================

USE auction_db;

-- Check if column exists before adding (MySQL 8.0+)
ALTER TABLE bidders 
ADD COLUMN IF NOT EXISTS tempBalance DECIMAL(19,2) DEFAULT 0.00 
COMMENT 'Ví tạm thời (dự phòng cho các tính năng mở rộng)';

-- Verify migration
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, IS_NULLABLE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'bidders' 
AND TABLE_SCHEMA = 'auction_db'
ORDER BY ORDINAL_POSITION;

-- ============================================================
-- Rollback script (nếu cần revert)
-- ALTER TABLE bidders DROP COLUMN IF EXISTS tempBalance;
-- ============================================================
