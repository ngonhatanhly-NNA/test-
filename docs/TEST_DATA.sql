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
-- 1. Thêm test users (mật khẩu đã bcrypt)
-- admin1/admin123, seller1/seller123, bidder1/bidder123, bidder2/bidder123, bidder3/bidder123
-- ============================================================
INSERT INTO users (id, username, passwordHash, email, fullName, role, phoneNumber, address, status) VALUES
(1, 'admin1',  '$2a$10$OwLmfCBgCEH38VA2bbZpDOmWkmc8XdDtDB.WnTzmjBST1tQntJqt2', 'admin@test.com',  'Quản Trị Viên', 'ADMIN',  '0909000000', 'HCM',  'ACTIVE'),
(2, 'seller1', '$2a$10$Z/fbEk9IH3QMSfyuBw8puuE6JYsnaARxMQ0WTN5se4f.LetF1D36W', 'seller1@test.com', 'Bán Hàng Một', 'SELLER', '0909111111', 'TP HCM', 'ACTIVE'),
(3, 'bidder1', '$2a$10$ObpJ/v12fpMEcoGTJQ1K/eT2rua0h4RpZ6Ha.XHg0il.eXT.d4QtS', 'bidder1@test.com', 'Người Mua Một', 'BIDDER', '0909222222', 'HN',  'ACTIVE'),
(4, 'bidder2', '$2a$10$ObpJ/v12fpMEcoGTJQ1K/eT2rua0h4RpZ6Ha.XHg0il.eXT.d4QtS', 'bidder2@test.com', 'Người Mua Hai', 'BIDDER', '0909333333', 'HCM', 'ACTIVE'),
(5, 'bidder3', '$2a$10$ObpJ/v12fpMEcoGTJQ1K/eT2rua0h4RpZ6Ha.XHg0il.eXT.d4QtS', 'bidder3@test.com', 'Người Mua Ba', 'BIDDER', '0909444444', 'DN',  'ACTIVE');

-- ============================================================
-- 2. Thêm admin profile
-- ============================================================
INSERT INTO admins (user_id, roleLevel, lastLoginIp) VALUES
(1, 'SUPER', '127.0.0.1');

-- ============================================================
-- 3. Thêm bidders (seller cũng phải là bidder trước)
-- ============================================================
INSERT INTO bidders (user_id, walletBalance, creditCardInfo) VALUES
(2, 1000000, 'VISA-SELLER-0001'),
(3, 5000000, 'VISA-BIDDER-0001'),
(4, 3000000, 'VISA-BIDDER-0002'),
(5, 2000000, 'VISA-BIDDER-0003');

-- ============================================================
-- 4. Thêm seller (bidder_id = 2)
-- ============================================================
INSERT INTO sellers (bidder_id, shopName, rating, totalReviews, bankAccountNumber, isVerified) VALUES
(2, 'Pokemon Shop 1', 4.8, 120, 'VCB-000111222', TRUE);

-- ============================================================
-- 5. Thêm items để đấu giá
-- ============================================================
INSERT INTO items (seller_id, item_type, name, description, startingPrice, item_condition, imageUrls, brand, model, warrantyMonths) VALUES
(2, 'ELECTRONICS', 'iPhone 15 Pro Max', 'Điện thoại Apple mới nhất', 800000, 'NEW', 'iphone15.jpg', 'Apple', 'iPhone 15 Pro Max', 12),
(2, 'ELECTRONICS', 'MacBook Pro 16', 'Laptop cao cấp từ Apple', 3500000, 'NEW', 'macbook.jpg', 'Apple', 'MacBook Pro 16', 24),
(2, 'ELECTRONICS', 'Samsung Galaxy S24', 'Điện thoại flagship Samsung', 600000, 'NEW', 'galaxy.jpg', 'Samsung', 'Galaxy S24', 12),
(2, 'ELECTRONICS', 'Nintendo Switch OLED', 'Máy chơi game cầm tay', 500000, 'LIKE_NEW', 'switch.jpg', 'Nintendo', 'Switch OLED', 6),
(2, 'ELECTRONICS', 'Sony WH-1000XM5', 'Tai nghe chống ồn', 250000, 'NEW', 'sony-xm5.jpg', 'Sony', 'WH-1000XM5', 12),
(2, 'ELECTRONICS', 'Dell XPS 13', 'Laptop mỏng nhẹ', 1200000, 'USED', 'xps13.jpg', 'Dell', 'XPS 13', 3);

-- ============================================================
-- 6. Thêm phiên đấu giá (seller_id = 2)
-- ============================================================
INSERT INTO auctions (item_id, seller_id, start_time, end_time, step_price, current_highest_bid, highest_bidder_id, winner_id, status, created_at, updated_at) VALUES
(1, 2, NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR), 50000, 0, NULL, NULL, 'OPEN', NOW(), NOW()),
(2, 2, NOW(), DATE_ADD(NOW(), INTERVAL 3 HOUR), 100000, 0, NULL, NULL, 'OPEN', NOW(), NOW()),
(3, 2, DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 4 HOUR), 30000, 0, NULL, NULL, 'OPEN', NOW(), NOW()),
(4, 2, NOW(), DATE_ADD(NOW(), INTERVAL 90 MINUTE), 20000, 0, NULL, NULL, 'OPEN', NOW(), NOW()),
(5, 2, NOW(), DATE_ADD(NOW(), INTERVAL 150 MINUTE), 10000, 0, NULL, NULL, 'OPEN', NOW(), NOW()),
(6, 2, DATE_ADD(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 210 MINUTE), 50000, 0, NULL, NULL, 'OPEN', NOW(), NOW());

-- ============================================================
-- 7. Thêm bid transactions (lịch sử đặt giá)
-- ============================================================
INSERT INTO bid_transactions (auction_id, bidder_id, bid_amount, timestamp, is_auto_bid, created_at) VALUES
(1, 3, 850000, DATE_SUB(NOW(), INTERVAL 15 MINUTE), FALSE, DATE_SUB(NOW(), INTERVAL 15 MINUTE)),
(1, 4, 900000, DATE_SUB(NOW(), INTERVAL 10 MINUTE), FALSE, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(1, 3, 950000, DATE_SUB(NOW(), INTERVAL 5 MINUTE), TRUE, DATE_SUB(NOW(), INTERVAL 5 MINUTE));

-- ============================================================
-- 8. Thêm auto-bid
-- ============================================================
INSERT INTO auto_bids (auction_id, bidder_id, max_bid_amount, custom_step_price, is_active, created_at, updated_at) VALUES
(1, 3, 2000000, NULL, TRUE, NOW(), NOW()),
(1, 4, 1500000, NULL, TRUE, NOW(), NOW());

-- ============================================================
-- Các query kiểm tra dữ liệu
-- ============================================================

-- Xem tất cả phiên đấu giá đang mở
SELECT a.id, a.status, i.name, a.start_time, a.end_time, a.current_highest_bid,
       u.fullName as seller_name
FROM auctions a
JOIN items i ON a.item_id = i.id
JOIN users u ON a.seller_id = u.id
WHERE a.status IN ('OPEN', 'RUNNING');

-- Xem lịch sử bid của phiên 1
SELECT bt.id, u.fullName as bidder_name, bt.bid_amount, bt.timestamp, bt.is_auto_bid
FROM bid_transactions bt
JOIN users u ON bt.bidder_id = u.id
WHERE bt.auction_id = 1
ORDER BY bt.timestamp DESC;

-- Xem auto-bid đang kích hoạt cho phiên 1
SELECT ab.id, u.fullName as bidder_name, ab.max_bid_amount, ab.custom_step_price, ab.is_active
FROM auto_bids ab
JOIN users u ON ab.bidder_id = u.id
WHERE ab.auction_id = 1 AND ab.is_active = TRUE;

