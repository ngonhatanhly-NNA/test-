USE auction_db;

SET FOREIGN_KEY_CHECKS = 0;

-- Xóa dữ liệu cũ
DELETE FROM auto_bids;
DELETE FROM bid_transactions;
DELETE FROM auctions;
DELETE FROM items;
DELETE FROM sellers;
DELETE FROM bidders;
DELETE FROM admins;
DELETE FROM users;

-- Reset AUTO_INCREMENT counters
ALTER TABLE users AUTO_INCREMENT = 1;
ALTER TABLE bidders AUTO_INCREMENT = 1;
ALTER TABLE sellers AUTO_INCREMENT = 1;
ALTER TABLE items AUTO_INCREMENT = 1;
ALTER TABLE auctions AUTO_INCREMENT = 1;
ALTER TABLE bid_transactions AUTO_INCREMENT = 1;
ALTER TABLE auto_bids AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- Demo Users (Test data for demo scenarios)
-- Credentials: username/password (passwords are bcrypt hashed)
-- admin@demo/admin123
-- seller@demo/seller123
-- buyer1@demo/buyer123
-- buyer2@demo/buyer123
-- ============================================================
INSERT INTO users (id, username, passwordHash, email, fullName, role, phoneNumber, address, status) VALUES
(1, 'admin@demo',  '$2a$10$OwLmfCBgCEH38VA2bbZpDOmWkmc8XdDtDB.WnTzmjBST1tQntJqt2', 'admin@demo.com',  'Demo Admin', 'ADMIN',  '0900000001', 'Hanoi', 'ACTIVE'),
(2, 'seller@demo', '$2a$10$Z/fbEk9IH3QMSfyuBw8puuE6JYsnaARxMQ0WTN5se4f.LetF1D36W', 'seller@demo.com', 'Demo Seller', 'SELLER', '0900000002', 'Ho Chi Minh', 'ACTIVE'),
(3, 'buyer1@demo', '$2a$10$ObpJ/v12fpMEcoGTJQ1K/eT2rua0h4RpZ6Ha.XHg0il.eXT.d4QtS', 'buyer1@demo.com', 'Demo Buyer 1', 'BIDDER', '0900000003', 'Hanoi', 'ACTIVE'),
(4, 'buyer2@demo', '$2a$10$ObpJ/v12fpMEcoGTJQ1K/eT2rua0h4RpZ6Ha.XHg0il.eXT.d4QtS', 'buyer2@demo.com', 'Demo Buyer 2', 'BIDDER', '0900000004', 'Da Nang', 'ACTIVE');

-- ============================================================
-- Admin Profile
-- ============================================================
INSERT INTO admins (user_id, roleLevel, lastLoginIp) VALUES
(1, 'SUPER', '127.0.0.1');

-- ============================================================
-- Bidders (sellers must also be bidders)
-- ============================================================
INSERT INTO bidders (user_id, walletBalance, creditCardInfo) VALUES
(2, 10000000, 'VISA-DEMO-SELLER'),
(3, 50000000, 'VISA-DEMO-BUYER1'),
(4, 30000000, 'VISA-DEMO-BUYER2');

-- ============================================================
-- Sellers
-- ============================================================
INSERT INTO sellers (bidder_id, shopName, rating, totalReviews, bankAccountNumber, isVerified) VALUES
(2, 'Demo Shop', 4.9, 500, 'VCB-999999999', TRUE);

-- ============================================================
-- Demo Items (5 different product types)
-- ============================================================
INSERT INTO items (seller_id, item_type, name, description, startingPrice, item_condition, imageUrls, brand, model, warrantyMonths) VALUES
(2, 'ELECTRONICS', 'Demo iPhone 16', 'Latest Apple flagship smartphone with advanced camera system', 999000, 'NEW', 'iphone16.jpg', 'Apple', 'iPhone 16 Pro', 12),
(2, 'ELECTRONICS', 'Demo MacBook Air', 'Lightweight powerful laptop for professionals', 3999000, 'NEW', 'macbook-air.jpg', 'Apple', 'MacBook Air M3', 24),
(2, 'ELECTRONICS', 'Demo PS5 Console', 'Latest gaming console with 4K support', 1999000, 'LIKE_NEW', 'ps5.jpg', 'Sony', 'PS5 Pro', 12),
(2, 'ELECTRONICS', 'Demo Headphones', 'Premium wireless noise-cancelling headphones', 299000, 'NEW', 'headphones.jpg', 'Bose', 'QC95 Ultra', 12),
(2, 'ELECTRONICS', 'Demo Smartwatch', 'Advanced fitness and health tracking smartwatch', 599000, 'NEW', 'smartwatch.jpg', 'Apple', 'Watch Ultra 2', 12);

-- ============================================================
-- Demo Auctions (All starting NOW, staggered end times)
-- ============================================================
INSERT INTO auctions (item_id, seller_id, start_time, end_time, step_price, current_highest_bid, highest_bidder_id, winner_id, status, created_at, updated_at) VALUES
(1, 2, NOW(), DATE_ADD(NOW(), INTERVAL 24 HOUR), 50000, 1000000, 3, NULL, 'OPEN', NOW(), NOW()),
(2, 2, NOW(), DATE_ADD(NOW(), INTERVAL 48 HOUR), 100000, 4000000, 4, NULL, 'OPEN', NOW(), NOW()),
(3, 2, NOW(), DATE_ADD(NOW(), INTERVAL 12 HOUR), 50000, 2000000, 3, NULL, 'OPEN', NOW(), NOW()),
(4, 2, NOW(), DATE_ADD(NOW(), INTERVAL 6 HOUR), 20000, 300000, 4, NULL, 'OPEN', NOW(), NOW()),
(5, 2, DATE_ADD(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 26 HOUR), 30000, 0, NULL, NULL, 'SCHEDULED', NOW(), NOW());

-- ============================================================
-- Demo Bid Transactions (simulate bidding history)
-- ============================================================
INSERT INTO bid_transactions (auction_id, bidder_id, bid_amount, timestamp, is_auto_bid, created_at) VALUES
(1, 3, 1000000, DATE_SUB(NOW(), INTERVAL 30 MINUTE), FALSE, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(1, 4, 1050000, DATE_SUB(NOW(), INTERVAL 25 MINUTE), FALSE, DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
(1, 3, 1100000, DATE_SUB(NOW(), INTERVAL 20 MINUTE), TRUE, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(2, 4, 4000000, DATE_SUB(NOW(), INTERVAL 15 MINUTE), FALSE, DATE_SUB(NOW(), INTERVAL 15 MINUTE)),
(3, 3, 2000000, DATE_SUB(NOW(), INTERVAL 10 MINUTE), FALSE, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(4, 4, 300000, DATE_SUB(NOW(), INTERVAL 5 MINUTE), FALSE, DATE_SUB(NOW(), INTERVAL 5 MINUTE));

-- ============================================================
-- Demo Auto-bids
-- ============================================================
INSERT INTO auto_bids (auction_id, bidder_id, max_bid_amount, custom_step_price, is_active, created_at, updated_at) VALUES
(1, 3, 2000000, NULL, TRUE, NOW(), NOW()),
(1, 4, 1500000, NULL, TRUE, NOW(), NOW()),
(2, 4, 5000000, 50000, TRUE, NOW(), NOW()),
(3, 3, 3000000, NULL, TRUE, NOW(), NOW());

-- ============================================================
-- Demo Queries
-- ============================================================

-- View all open auctions
SELECT 'Open Auctions' as query_type;
SELECT a.id, i.name, a.current_highest_bid, a.step_price, a.end_time, u.fullName as seller_name
FROM auctions a
JOIN items i ON a.item_id = i.id
JOIN users u ON a.seller_id = u.id
WHERE a.status IN ('OPEN', 'RUNNING')
ORDER BY a.end_time ASC;

-- View active auto-bids
SELECT 'Active Auto-bids' as query_type;
SELECT ab.id, i.name, u.fullName as bidder_name, ab.max_bid_amount, ab.custom_step_price
FROM auto_bids ab
JOIN auctions a ON ab.auction_id = a.id
JOIN items i ON a.item_id = i.id
JOIN users u ON ab.bidder_id = u.id
WHERE ab.is_active = TRUE
ORDER BY a.id;

-- View bidding history for auction 1
SELECT 'Bidding History (Auction 1)' as query_type;
SELECT bt.id, u.fullName as bidder_name, bt.bid_amount, bt.timestamp, IF(bt.is_auto_bid, 'Auto', 'Manual') as bid_type
FROM bid_transactions bt
JOIN users u ON bt.bidder_id = u.id
WHERE bt.auction_id = 1
ORDER BY bt.timestamp DESC;
