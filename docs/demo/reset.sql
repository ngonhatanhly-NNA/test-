-- ============================================================
-- RESET DATABASE (Demo Auction)
-- Truncate toàn bộ bảng liên quan để chạy seed_demo.sql với ID ổn định.
-- MySQL/MariaDB
-- ============================================================

USE auction_db;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE auto_bids;
TRUNCATE TABLE bid_transactions;
TRUNCATE TABLE auctions;
TRUNCATE TABLE sellers;
TRUNCATE TABLE bidders;
TRUNCATE TABLE admins;
TRUNCATE TABLE users;
TRUNCATE TABLE items;
SET FOREIGN_KEY_CHECKS = 1;


