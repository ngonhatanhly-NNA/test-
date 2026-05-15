-- ============================================================
-- RESET DATABASE (Demo Auction)
-- Truncate all tables to prepare for fresh demo data
-- MySQL/MariaDB
-- ============================================================

USE auction_db;

SET FOREIGN_KEY_CHECKS = 0;

-- Clear all tables in correct order (children first, parents last)
TRUNCATE TABLE auto_bids;
TRUNCATE TABLE bid_transactions;
TRUNCATE TABLE auctions;
TRUNCATE TABLE items;
TRUNCATE TABLE sellers;
TRUNCATE TABLE bidders;
TRUNCATE TABLE admins;
TRUNCATE TABLE users;

-- Reset AUTO_INCREMENT counters
ALTER TABLE users AUTO_INCREMENT = 1;
ALTER TABLE bidders AUTO_INCREMENT = 1;
ALTER TABLE sellers AUTO_INCREMENT = 1;
ALTER TABLE items AUTO_INCREMENT = 1;
ALTER TABLE auctions AUTO_INCREMENT = 1;
ALTER TABLE bid_transactions AUTO_INCREMENT = 1;
ALTER TABLE auto_bids AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;


