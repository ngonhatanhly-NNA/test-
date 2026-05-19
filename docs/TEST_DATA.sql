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
-- 1. Thêm test users (mật khẩu đã bcrypt: 123456 hoặc theo mặc định cũ)
-- admin1/admin123, seller1/seller123, bidder1/bidder123, bidder2/bidder123, bidder3/bidder123
-- Cập nhật thêm avatar_url từ thư mục resources
-- ============================================================
INSERT INTO users (id, username, passwordHash, email, fullName, role, phoneNumber, address, status, avatar_url) VALUES
(1, 'admin1',  '$2a$10$OwLmfCBgCEH38VA2bbZpDOmWkmc8XdDtDB.WnTzmjBST1tQntJqt2', 'admin@pokemon.com',  'Giáo Sư Oak', 'ADMIN',  '0909000000', 'Pallet Town',  'ACTIVE', 'shopkeeper.png'),
(2, 'seller1', '$2a$10$Z/fbEk9IH3QMSfyuBw8puuE6JYsnaARxMQ0WTN5se4f.LetF1D36W', 'seller1@pokemon.com', 'Thương Gia Poke', 'SELLER', '0909111111', 'Celadon City', 'ACTIVE', 'seller1_avatar.png'),
(3, 'bidder1', '$2a$10$ObpJ/v12fpMEcoGTJQ1K/eT2rua0h4RpZ6Ha.XHg0il.eXT.d4QtS', 'bidder1@pokemon.com', 'Satoshi', 'BIDDER', '0909222222', 'Kanto',  'ACTIVE', NULL),
(4, 'bidder2', '$2a$10$ObpJ/v12fpMEcoGTJQ1K/eT2rua0h4RpZ6Ha.XHg0il.eXT.d4QtS', 'bidder2@pokemon.com', 'Kasumi', 'BIDDER', '0909333333', 'Cerulean City', 'ACTIVE', NULL),
(5, 'bidder3', '$2a$10$ObpJ/v12fpMEcoGTJQ1K/eT2rua0h4RpZ6Ha.XHg0il.eXT.d4QtS', 'bidder3@pokemon.com', 'Takeshi', 'BIDDER', '0909444444', 'Pewter City',  'ACTIVE', NULL);

-- ============================================================
-- 2. Thêm admin profile
-- ============================================================
INSERT INTO admins (user_id, roleLevel, lastLoginIp) VALUES
(1, 'SUPER', '127.0.0.1');

-- ============================================================
-- 3. Thêm bidders (seller cũng phải là bidder trước)
-- ============================================================
INSERT INTO bidders (user_id, walletBalance, creditCardInfo, reserved_amount) VALUES
(2, 10000000, 'VISA-SELLER-0001', 0),
(3, 50000000, 'VISA-BIDDER-0001', 950000), -- Giữ lại tiền do đang bid cao nhất
(4, 30000000, 'VISA-BIDDER-0002', 0),
(5, 20000000, 'VISA-BIDDER-0003', 0);

-- ============================================================
-- 4. Thêm seller (bidder_id = 2)
-- ============================================================
INSERT INTO sellers (bidder_id, shopName, rating, totalReviews, bankAccountNumber, isVerified) VALUES
(2, 'Celadon Department Store', 4.9, 350, 'POKEBANK-000111222', TRUE);

-- ============================================================
-- 5. Thêm items để đấu giá (Đồng bộ với tên file trong resources/images)
-- ============================================================
INSERT INTO items (seller_id, item_type, name, description, startingPrice, item_condition, imageUrls, brand, model, artistName, hasCertificateOfAuthenticity) VALUES
(2, 'ART', 'Chems Illustrator Art', 'Popular meme', 5000000, 'NEW', '/images/art_popular.png', 'Nintendo', 'Card', 'Atsuko Nishida', TRUE),
(2, 'ART', 'Evening night', 'Peaceful Evening Night Gardevoir', 1500000, 'LIKE_NEW', '/images/background.png', 'Silph Co', 'Mega Stone', NULL, TRUE),
(2, 'ART', 'Absol Action Figure', 'Mô hình Absol phiên bản giới hạn', 800000, 'NEW', '/images/ABSOL.png', 'Pokemon Center', 'Figure', NULL, FALSE),
(2, 'ELECTRONICS', 'Master Ball Special Edition', 'Bóng bắt Pokemon tỷ lệ 100%, bản kỷ niệm', 3000000, 'NEW', '/images/summaryball00.png', 'Silph Co', 'Master Ball', NULL, TRUE),
(2, 'ELECTRONICS', 'GUITAR', 'OnlyOne Guitar', 2500000, 'NEW', '/images/guitar.png', 'Pokemon Ranger', 'Egg', NULL, TRUE),
(2, 'ART', 'Morpeko Happy Plushie', 'Gấu bông Morpeko trạng thái no bụng', 400000, 'NEW', '/images/MorpekoHappy.png', 'Pokemon Center', 'Plushie', NULL, FALSE),
(2, 'ELECTRONICS', 'Mystic Potion', 'Bình hồi phục siêu cấp', 100000, 'NEW', '/images/item066.png', 'Poke Mart', 'Potion', NULL, FALSE);
INSERT INTO items (seller_id, item_type, name, description, startingPrice, item_condition, imageUrls, brand, model, artistName, hasCertificateOfAuthenticity) VALUES
(2, 'ART', 'Pixel Art Starter Kit', 'Bộ công cụ sáng tạo Pixel Art không giới hạn', 150000, 'NEW', '/images/pixel_art_kit.png', 'PokeStudio', 'Kit', NULL, FALSE);
-- ============================================================
-- 6. Thêm phiên đấu giá (Đa dạng trạng thái để test UI)
-- ============================================================
INSERT INTO auctions (item_id, seller_id, start_time, end_time, step_price, current_highest_bid, highest_bidder_id, winner_id, status, created_at, updated_at) VALUES
-- 1. Phiên đang diễn ra (RUNNING/OPEN) - Có người bid
(1, 2, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 2 HOUR), 100000, 5200000, 3, NULL, 'OPEN', NOW(), NOW()),

-- 2. Phiên đang diễn ra (RUNNING/OPEN) - Chưa có người bid
(2, 2, DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 3 HOUR), 50000, 0, NULL, NULL, 'OPEN', NOW(), NOW()),

-- 3. Phiên sắp diễn ra (UPCOMING)
(3, 2, DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 4 HOUR), 20000, 0, NULL, NULL, 'OPEN', NOW(), NOW()),

-- 4. Phiên sắp kết thúc (Sắp hêt giờ, test Anti-sniping)
(4, 2, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 5 MINUTE), 100000, 3500000, 4, NULL, 'OPEN', NOW(), NOW()),

-- 5. Phiên đã kết thúc (FINISHED) - Đã có người thắng
(5, 2, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 50000, 2800000, 5, 5, 'CLOSED', NOW(), NOW()),

-- 6. Phiên mới tạo
(6, 2, NOW(), DATE_ADD(NOW(), INTERVAL 210 MINUTE), 10000, 0, NULL, NULL, 'OPEN', NOW(), NOW()),

-- 7. Phiên đang diễn ra
(7, 2, NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY), 5000, 0, NULL, NULL, 'OPEN', NOW(), NOW());

-- ============================================================
-- 7. Thêm bid transactions (lịch sử đặt giá)
-- Phải khớp với current_highest_bid của auctions
-- ============================================================
INSERT INTO bid_transactions (auction_id, bidder_id, bid_amount, timestamp, is_auto_bid, created_at) VALUES
-- Lịch sử cho Auction 1 (Pikachu)
(1, 4, 5100000, DATE_SUB(NOW(), INTERVAL 45 MINUTE), FALSE, DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(1, 3, 5200000, DATE_SUB(NOW(), INTERVAL 15 MINUTE), TRUE, DATE_SUB(NOW(), INTERVAL 15 MINUTE)),

-- Lịch sử cho Auction 4 (Master Ball)
(4, 3, 3100000, DATE_SUB(NOW(), INTERVAL 1 HOUR), FALSE, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(4, 5, 3300000, DATE_SUB(NOW(), INTERVAL 30 MINUTE), FALSE, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(4, 4, 3500000, DATE_SUB(NOW(), INTERVAL 10 MINUTE), FALSE, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),

-- Lịch sử cho Auction 5 (Manaphy - Đã kết thúc)
(5, 4, 2600000, DATE_SUB(NOW(), INTERVAL 36 HOUR), FALSE, DATE_SUB(NOW(), INTERVAL 36 HOUR)),
(5, 5, 2800000, DATE_SUB(NOW(), INTERVAL 25 HOUR), TRUE, DATE_SUB(NOW(), INTERVAL 25 HOUR));
INSERT INTO auctions (item_id, seller_id, start_time, end_time, step_price, current_highest_bid, highest_bidder_id, winner_id, status, created_at, updated_at) VALUES
(8, 2, NOW(), DATE_ADD(NOW(), INTERVAL 100 YEAR), 20000, 0, NULL, NULL, 'OPEN', NOW(), NOW());

-- ============================================================
-- 8. Thêm auto-bid
-- ============================================================
INSERT INTO auto_bids (auction_id, bidder_id, max_bid_amount, custom_step_price, is_active, created_at, updated_at) VALUES
(1, 3, 8000000, 200000, TRUE, NOW(), NOW()),
(4, 4, 5000000, NULL, TRUE, NOW(), NOW());

-- ============================================================
-- Các query kiểm tra dữ liệu
-- ============================================================

-- Xem tất cả các vật phẩm đấu giá
SELECT i.name, a.status, a.current_highest_bid, u.username AS highest_bidder, i.imageUrls
FROM auctions a
JOIN items i ON a.item_id = i.id
LEFT JOIN users u ON a.highest_bidder_id = u.id;