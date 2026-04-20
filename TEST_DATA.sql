-- ============================================================
-- TEST DATA cho hệ thống đấu giá
-- ============================================================

USE auction_db;

-- 1. Thêm test users
INSERT INTO users (username, passwordHash, email, fullName, role, phoneNumber, address, status) VALUES
('seller1', 'password123', 'seller1@test.com', 'Bán Hàng Một', 'SELLER', '0909111111', 'TP HCM', 'ACTIVE'),
('bidder1', 'password456', 'bidder1@test.com', 'Người Mua Một', 'BIDDER', '0909222222', 'HN', 'ACTIVE'),
('bidder2', 'password789', 'bidder2@test.com', 'Người Mua Hai', 'BIDDER', '0909333333', 'HCM', 'ACTIVE'),
('bidder3', 'password999', 'bidder3@test.com', 'Người Mua Ba', 'BIDDER', '0909444444', 'DN', 'ACTIVE'),
('admin1', 'admin123', 'admin@test.com', 'Quản Trị Viên', 'ADMIN', '0909000000', 'HCM', 'ACTIVE');

-- 2. Thêm seller vào bảng sellers (seller_id = 1)
INSERT INTO bidders (user_id, walletBalance) VALUES (1, 1000000);
INSERT INTO sellers (bidder_id, shopName, rating, totalReviews, isVerified)
VALUES (1, 'Pokemon Shop 1', 4.8, 120, TRUE);

-- 3. Thêm bidders
INSERT INTO bidders (user_id, walletBalance) VALUES
(2, 5000000),
(3, 3000000),
(4, 2000000);

-- 4. Thêm items để đấu giá
INSERT INTO items (item_type, name, description, startingPrice, item_condition, imageUrls, brand, model, warrantyMonths) VALUES
('ELECTRONICS', 'iPhone 15 Pro Max', 'Điện thoại Apple mới nhất', 800000, 'NEW', 'iphone15.jpg', 'Apple', 'iPhone 15 Pro Max', 12),
('ELECTRONICS', 'MacBook Pro 16', 'Laptop cao cấp từ Apple', 3500000, 'NEW', 'macbook.jpg', 'Apple', 'MacBook Pro 16', 24),
('ELECTRONICS', 'Samsung Galaxy S24', 'Điện thoại flagship Samsung', 600000, 'NEW', 'galaxy.jpg', 'Samsung', 'Galaxy S24', 12);

-- 5. Thêm phiên đấu giá (seller_id = 1, là người bán)
-- LƯỚI ƯU: Sử dụng TIME_ADD để tính toán thời gian
-- Phiên 1: Bắt đầu ngay bây giờ, kết thúc sau 2 giờ
INSERT INTO auctions (item_id, seller_id, start_time, end_time, step_price, current_highest_bid, winner_id, status, created_at, updated_at) VALUES
(1, 1, NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR), 50000, 0, NULL, 'OPEN', NOW(), NOW()),
(2, 1, NOW(), DATE_ADD(NOW(), INTERVAL 3 HOUR), 100000, 0, NULL, 'OPEN', NOW(), NOW()),
(3, 1, DATE_ADD(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 4 HOUR), 30000, 0, NULL, 'OPEN', NOW(), NOW());

-- 5b. Thêm thêm items & phiên đấu giá để test (tạo nhiều phòng live hơn)
INSERT INTO items (item_type, name, description, startingPrice, item_condition, imageUrls, brand, model, warrantyMonths) VALUES
('ELECTRONICS', 'Nintendo Switch OLED', 'Máy chơi game cầm tay', 500000, 'LIKE_NEW', 'switch.jpg', 'Nintendo', 'Switch OLED', 6),
('ELECTRONICS', 'Sony WH-1000XM5', 'Tai nghe chống ồn', 250000, 'NEW', 'sony-xm5.jpg', 'Sony', 'WH-1000XM5', 12),
('ELECTRONICS', 'Dell XPS 13', 'Laptop mỏng nhẹ', 1200000, 'USED', 'xps13.jpg', 'Dell', 'XPS 13', 3);

-- Các item mới sẽ có id tiếp theo (4,5,6 nếu DB trống và chạy đúng thứ tự file)
INSERT INTO auctions (item_id, seller_id, start_time, end_time, step_price, current_highest_bid, winner_id, status, created_at, updated_at) VALUES
(4, 1, NOW(), DATE_ADD(NOW(), INTERVAL 90 MINUTE), 20000, 0, NULL, 'OPEN', NOW(), NOW()),
(5, 1, NOW(), DATE_ADD(NOW(), INTERVAL 150 MINUTE), 10000, 0, NULL, 'OPEN', NOW(), NOW()),
(6, 1, DATE_ADD(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 210 MINUTE), 50000, 0, NULL, 'OPEN', NOW(), NOW());

-- 6. Thêm bid transactions (lịch sử đặt giá) cho phiên 1
-- Giả sử bidder1 (user_id=2) đặt giá 850000 vào 11:30
-- Và bidder2 (user_id=3) đặt giá 900000 vào 11:45
INSERT INTO bid_transactions (auction_id, bidder_id, bid_amount, timestamp, is_auto_bid, created_at) VALUES
(1, 2, 850000, DATE_SUB(NOW(), INTERVAL 15 MINUTE), FALSE, DATE_SUB(NOW(), INTERVAL 15 MINUTE)),
(1, 3, 900000, DATE_SUB(NOW(), INTERVAL 10 MINUTE), FALSE, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(1, 2, 950000, DATE_SUB(NOW(), INTERVAL 5 MINUTE), TRUE, DATE_SUB(NOW(), INTERVAL 5 MINUTE));

-- 7. Thêm auto-bid
INSERT INTO auto_bids (auction_id, bidder_id, max_bid_amount, is_active, created_at, updated_at) VALUES
(1, 2, 2000000, TRUE, NOW(), NOW()),
(1, 3, 1500000, TRUE, NOW(), NOW());

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
SELECT ab.id, u.fullName as bidder_name, ab.max_bid_amount, ab.is_active
FROM auto_bids ab
JOIN users u ON ab.bidder_id = u.id
WHERE ab.auction_id = 1 AND ab.is_active = TRUE;

