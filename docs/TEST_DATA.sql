USE auction_db;

SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE users AUTO_INCREMENT = 1;
ALTER TABLE bidders AUTO_INCREMENT = 1;
ALTER TABLE sellers AUTO_INCREMENT = 1;
ALTER TABLE items AUTO_INCREMENT = 1;
ALTER TABLE auctions AUTO_INCREMENT = 1;
ALTER TABLE bid_transactions AUTO_INCREMENT = 1;
ALTER TABLE auto_bids AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO users (id, username, passwordHash, email, fullName, role, phoneNumber, address, status, avatar_url) VALUES
(1, 'admin1',  '$2a$10$OwLmfCBgCEH38VA2bbZpDOmWkmc8XdDtDB.WnTzmjBST1tQntJqt2', 'admin@pokemon.com',  'Giáo Sư Oak', 'ADMIN',  '0909000000', 'Pallet Town',  'ACTIVE', 'shopkeeper.png'),
(2, 'seller1', '$2a$10$Z/fbEk9IH3QMSfyuBw8puuE6JYsnaARxMQ0WTN5se4f.LetF1D36W', 'seller1@pokemon.com', 'Thương Gia Poke', 'SELLER', '0909111111', 'Celadon City', 'ACTIVE', 'seller1_avatar.png'),
(3, 'bidder1', '$2a$10$ObpJ/v12fpMEcoGTJQ1K/eT2rua0h4RpZ6Ha.XHg0il.eXT.d4QtS', 'bidder1@pokemon.com', 'Satoshi', 'BIDDER', '0909222222', 'Kanto',  'ACTIVE', NULL),
(4, 'bidder2', '$2a$10$ObpJ/v12fpMEcoGTJQ1K/eT2rua0h4RpZ6Ha.XHg0il.eXT.d4QtS', 'bidder2@pokemon.com', 'Kasumi', 'BIDDER', '0909333333', 'Cerulean City', 'ACTIVE', NULL),
(5, 'bidder3', '$2a$10$ObpJ/v12fpMEcoGTJQ1K/eT2rua0h4RpZ6Ha.XHg0il.eXT.d4QtS', 'bidder3@pokemon.com', 'Takeshi', 'BIDDER', '0909444444', 'Pewter City',  'ACTIVE', NULL),
-- Team Members
(6, 'anh', '$2a$10$OwLmfCBgCEH38VA2bbZpDOmWkmc8XdDtDB.WnTzmjBST1tQntJqt2', 'anh@team.com', 'Ánh', 'ADMIN', '0912000001', 'Hanoi', 'ACTIVE', 'shopkeeper.png'),
(7, 'phong', '$2a$10$OwLmfCBgCEH38VA2bbZpDOmWkmc8XdDtDB.WnTzmjBST1tQntJqt2', 'phong@team.com', 'Phong', 'SELLER', '0912000002', 'Danang', 'ACTIVE', 'shopkeeper.png'),
(8, 'tuong', '$2a$10$OwLmfCBgCEH38VA2bbZpDOmWkmc8XdDtDB.WnTzmjBST1tQntJqt2', 'tuong@team.com', 'Tưởng', 'BIDDER', '0912000003', 'Hanoi', 'ACTIVE', 'shopkeeper.png'),
(9, 'vu', '$2a$10$OwLmfCBgCEH38VA2bbZpDOmWkmc8XdDtDB.WnTzmjBST1tQntJqt2', 'vu@team.com', 'Vũ', 'SELLER', '0912000004', 'HCMC', 'ACTIVE', 'shopkeeper.png'),
-- Random Users
(10, 'random_seller', '$2a$10$OwLmfCBgCEH38VA2bbZpDOmWkmc8XdDtDB.WnTzmjBST1tQntJqt2', 'randseller@test.com', 'Random Seller', 'SELLER', '0988000001', 'Hai Phong', 'ACTIVE', 'shopkeeper.png'),
(11, 'random_bidder', '$2a$10$OwLmfCBgCEH38VA2bbZpDOmWkmc8XdDtDB.WnTzmjBST1tQntJqt2', 'randbidder@test.com', 'Random Bidder', 'BIDDER', '0988000002', 'Hue', 'ACTIVE', 'shopkeeper.png');

INSERT INTO admins (user_id, roleLevel, lastLoginIp) VALUES
(1, 'SUPER', '127.0.0.1'),
(6, 'SUPER', '127.0.0.1'); 

INSERT INTO bidders (user_id, walletBalance, creditCardInfo, reserved_amount) VALUES
(2, 10000000, 'VISA-SELLER-0001', 0),
(3, 50000000, 'VISA-BIDDER-0001', 950000),
(4, 30000000, 'VISA-BIDDER-0002', 0),
(5, 20000000, 'VISA-BIDDER-0003', 0),
(7, 100000000, 'VISA-TEAM-PHONG', 0),    -- Phong (Seller)
(8, 200000000, 'VISA-TEAM-TUONG', 500000), -- Tưởng (Bidder)
(9, 150000000, 'VISA-TEAM-VU', 0),       -- Vũ (Seller)
(10, 50000000, 'VISA-RAND-SELLER', 0),   -- Random Seller
(11, 80000000, 'VISA-RAND-BIDDER', 0);   -- Random Bidder

INSERT INTO sellers (bidder_id, shopName, rating, totalReviews, bankAccountNumber, isVerified) VALUES
(2, 'Celadon Department Store', 4.9, 350, 'POKEBANK-000111222', TRUE),
(7, 'Phong Luxury Store', 5.0, 120, 'VCOMBANK-123456', TRUE),
(9, 'Vu Tech & Arts', 4.8, 90, 'TCBANK-654321', TRUE),
(10, 'Random Auto & Goods', 4.5, 45, 'MBBANK-000999', TRUE);

INSERT INTO items (seller_id, item_type, name, description, startingPrice, item_condition, imageUrls, brand, model, artistName, hasCertificateOfAuthenticity) VALUES
(2, 'ART', 'Chems Illustrator Art', 'Popular meme', 5000000, 'NEW', '/images/art_popular.png', 'Nintendo', 'Card', 'Atsuko Nishida', TRUE),
(7, 'ART', 'De sterrennatch', 'Đêm đầy sao của Van Gogh...', 1500000, 'LIKE_NEW', '/images/starsky.png', 'Vincent van Gogh', 'Polular art', NULL, TRUE),
(9, 'ART', 'Absol Action Figure', 'Mô hình Absol phiên bản giới hạn', 800000, 'NEW', '/images/ABSOL.png', 'Pokemon Center', 'Figure', NULL, FALSE),
(10, 'ELECTRONICS', 'ROBOT the only one', 'Mẫu robot mới nhất được phát triển', 3000000, 'NEW', '/images/robot.png', 'Silph Co', 'Roboto', NULL, TRUE),
(2, 'ELECTRONICS', 'GUITAR', 'OnlyOne Guitar', 2500000, 'NEW', '/images/guitar.png', 'Pokemon Ranger', 'Egg', NULL, TRUE),
(7, 'ART', 'Mona Lisa', 'Bức tranh Nàng Mona Lisa', 400000, 'NEW', '/images/Vangoh.png', 'Leonardo da Vinci', 'Popular Art ', NULL, FALSE),
(9, 'ELECTRONICS', 'Gaming Lap', 'Laptop Gaming', 100000, 'NEW', '/images/gaminglap.png', 'Poke Mart', 'devices', NULL, FALSE),
(10, 'ART', 'Pixel Art Starter Kit', 'Bộ công cụ sáng tạo Pixel Art không giới hạn', 150000, 'NEW', '/images/pixel_art_kit.png', 'PokeStudio', 'Kit', NULL, FALSE),
-- Thêm Vehicles
(7, 'VEHICLE', 'Porsche 911 GT3', 'Siêu xe thể thao cực hiếm', 25000000, 'NEW', '/images/car.png', 'Porsche', '911 GT3', NULL, TRUE),
(9, 'VEHICLE', 'Mercedes-AMG G63', 'SUV hạng sang, offroad đỉnh cao', 35000000, 'NEW', '/images/car1.png', 'Mercedes-Benz', 'G63', NULL, TRUE);

INSERT INTO auctions (item_id, seller_id, start_time, end_time, step_price, current_highest_bid, highest_bidder_id, winner_id, status, created_at, updated_at) VALUES
-- 1. Phiên đang diễn ra (RUNNING/OPEN) - Có người bid (seller 2)
(1, 2, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 2 HOUR), 100000, 5200000, 3, NULL, 'OPEN', NOW(), NOW()),
-- 2. Phiên đang diễn ra (RUNNING/OPEN) - Chưa có người bid (seller 7)
(2, 7, DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 3 HOUR), 50000, 0, NULL, NULL, 'OPEN', NOW(), NOW()),
-- 3. Phiên sắp diễn ra (UPCOMING) (seller 9)
(3, 9, DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 4 HOUR), 20000, 0, NULL, NULL, 'OPEN', NOW(), NOW()),
-- 4. Phiên sắp kết thúc (Sắp hêt giờ, test Anti-sniping) (seller 10)
(4, 10, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 5 MINUTE), 100000, 3800000, 8, NULL, 'OPEN', NOW(), NOW()),
-- 5. Phiên đã kết thúc (FINISHED) - Đã có người thắng (seller 2)
(5, 2, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 50000, 2800000, 5, 5, 'CLOSED', NOW(), NOW()),
-- 6. Phiên mới tạo (seller 7)
(6, 7, NOW(), DATE_ADD(NOW(), INTERVAL 210 MINUTE), 10000, 0, NULL, NULL, 'OPEN', NOW(), NOW()),
-- 7. Phiên đang diễn ra (seller 9)
(7, 9, NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY), 5000, 0, NULL, NULL, 'OPEN', NOW(), NOW()),
-- 8. Phiên dài hạn (seller 10)
(8, 10, NOW(), DATE_ADD(NOW(), INTERVAL 100 YEAR), 20000, 0, NULL, NULL, 'OPEN', NOW(), NOW()),
-- 9. Phiên đấu giá siêu xe Porsche (seller 7)
(9, 7, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 1 DAY), 500000, 28000000, 11, NULL, 'OPEN', NOW(), NOW()),
-- 10. Phiên đấu giá SUV Mercedes (seller 9)
(10, 9, NOW(), DATE_ADD(NOW(), INTERVAL 5 DAY), 1000000, 0, NULL, NULL, 'OPEN', NOW(), NOW());

INSERT INTO bid_transactions (auction_id, bidder_id, bid_amount, timestamp, is_auto_bid, created_at) VALUES
-- Lịch sử cho Auction 1
(1, 4, 5100000, DATE_SUB(NOW(), INTERVAL 45 MINUTE), FALSE, DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(1, 3, 5200000, DATE_SUB(NOW(), INTERVAL 15 MINUTE), TRUE, DATE_SUB(NOW(), INTERVAL 15 MINUTE)),

-- Lịch sử cho Auction 4 (Test nhiều autobid tranh nhau)
(4, 3, 3100000, DATE_SUB(NOW(), INTERVAL 1 HOUR), FALSE, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(4, 11, 3300000, DATE_SUB(NOW(), INTERVAL 45 MINUTE), TRUE, DATE_SUB(NOW(), INTERVAL 45 MINUTE)), -- random_bidder autobid
(4, 5, 3500000, DATE_SUB(NOW(), INTERVAL 30 MINUTE), FALSE, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(4, 8, 3800000, DATE_SUB(NOW(), INTERVAL 10 MINUTE), TRUE, DATE_SUB(NOW(), INTERVAL 10 MINUTE)), -- Tưởng autobid lên 3.8m

-- Lịch sử cho Auction 5 (Đã kết thúc)
(5, 4, 2600000, DATE_SUB(NOW(), INTERVAL 36 HOUR), FALSE, DATE_SUB(NOW(), INTERVAL 36 HOUR)),
(5, 5, 2800000, DATE_SUB(NOW(), INTERVAL 25 HOUR), TRUE, DATE_SUB(NOW(), INTERVAL 25 HOUR)),

-- Lịch sử cho Auction 9 (Siêu xe Porsche)
(9, 8, 26000000, DATE_SUB(NOW(), INTERVAL 1 HOUR), FALSE, DATE_SUB(NOW(), INTERVAL 1 HOUR)), -- Tưởng bid tay
(9, 11, 28000000, DATE_SUB(NOW(), INTERVAL 30 MINUTE), TRUE, DATE_SUB(NOW(), INTERVAL 30 MINUTE)); -- random_bidder autobid


INSERT INTO auto_bids (auction_id, bidder_id, max_bid_amount, custom_step_price, is_active, created_at, updated_at) VALUES
(1, 3, 8000000, 200000, TRUE, NOW(), NOW()),
(4, 11, 4000000, NULL, TRUE, DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW()), -- random_bidder autobid
(4, 8, 5500000, 150000, TRUE, DATE_SUB(NOW(), INTERVAL 30 MINUTE), NOW()), -- Tưởng autobid
(9, 11, 40000000, 1000000, TRUE, DATE_SUB(NOW(), INTERVAL 2 HOUR), NOW()), -- random_bidder bid xe
(10, 8, 60000000, 2000000, TRUE, NOW(), NOW()); -- Tưởng setup autobid xe Mercedes sớm