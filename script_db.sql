-- Tạo sẵn luôn bảng items để tí nữa em vào việc cho tiện
USE auction_db;

-- Xóa bảng cũ bị sai đi
-- DROP TABLE IF EXISTS items;	(Xóa ký tự cmt để kích hoạt nhé)

-- Tạo bảng mới chuẩn xác theo đúng các file Java của em
CREATE TABLE items (
    -- Các thuộc tính CHUNG của thằng Cha (Item)
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       item_type VARCHAR(50) NOT NULL, -- RẤT QUAN TRỌNG: Để phân biệt nó là Art, Electronics hay Vehicle
                       name VARCHAR(100) NOT NULL,
                       description TEXT,
                       startingPrice DOUBLE,
                       item_condition VARCHAR(50), -- Trong DB không nên dùng chữ 'condition' vì dễ trùng từ khóa SQL
                       imageUrls TEXT, -- Tạm lưu danh sách ảnh dưới dạng chuỗi cách nhau bằng dấu phẩy

    -- Các thuộc tính RIÊNG của Electronics
                       brand VARCHAR(100),
                       model VARCHAR(100),
                       warrantyMonths INT,

    -- Các thuộc tính RIÊNG của Art
                       artistName VARCHAR(100),
                       material VARCHAR(100),
                       hasCertificateOfAuthenticity BOOLEAN,

    -- Các thuộc tính RIÊNG của Vehicle
                       manufactureYear INT,
                       vinNumber VARCHAR(50),
                       mileage INT
);

USE auction_db;
SELECT * FROM users;
--
--
USE auction_db;

-- Dọn dẹp nhà cửa trước khi xây (Xóa bảng con trước, bảng cha sau để không đứt xích)
-- DROP TABLE IF EXISTS sellers;	(Xóa ký tự cmt để kích hoạt nhé)
-- DROP TABLE IF EXISTS bidders;
-- DROP TABLE IF EXISTS admins;
-- DROP TABLE IF EXISTS users;

-- 1. BẢNG GỐC (Chứa các thuộc tính chung của User.java)
CREATE TABLE users (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       passwordHash VARCHAR(255) NOT NULL,
                       email VARCHAR(100),
                       fullName VARCHAR(100),
                       role VARCHAR(20) NOT NULL, -- "Bảng tên" phân biệt Admin/Bidder/Seller
                       phoneNumber VARCHAR(20),
                       address VARCHAR(255),
                       status VARCHAR(20)
);

-- 2. BẢNG ADMINS (Kế thừa Users)
CREATE TABLE admins (
                        user_id INT PRIMARY KEY, -- Vừa làm Khóa chính, vừa làm Khóa ngoại
                        roleLevel VARCHAR(50),
                        lastLoginIp VARCHAR(50),
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. BẢNG BIDDERS (Kế thừa Users)
CREATE TABLE bidders (
                         user_id INT PRIMARY KEY,
                         walletBalance DOUBLE DEFAULT 0.0,
                         creditCardInfo VARCHAR(255),
                         FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 4. BẢNG SELLERS (Kế thừa Bidders)
CREATE TABLE sellers (
                         bidder_id INT PRIMARY KEY, -- Nối thẳng vào bảng bidders vì Seller extends Bidder
                         shopName VARCHAR(100),
                         rating DOUBLE DEFAULT 0.0,
                         totalReviews INT DEFAULT 0,
                         bankAccountNumber VARCHAR(50),
                         isVerified BOOLEAN DEFAULT FALSE,
                         FOREIGN KEY (bidder_id) REFERENCES bidders(user_id) ON DELETE CASCADE
);

-- Tạo bảng quản lý phiên đấu giá
CREATE TABLE auctions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL,
    seller_id INT NOT NULL,
    start_time DATETIME,
    end_time DATETIME,
    step_price DECIMAL(15,2),
    current_highest_bid DECIMAL(15,2),
    winner_id INT,
    status VARCHAR(20),
    created_at DATETIME,
    updated_at DATETIME,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Tạo bảng lưu lịch sử đặt giá
CREATE TABLE bid_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    auction_id INT NOT NULL,
    bidder_id INT NOT NULL,
    bid_amount DECIMAL(15,2),
    timestamp DATETIME,
    is_auto_bid BOOLEAN,
    created_at DATETIME,
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Tạo bảng quản lý tự động đặt giá (Auto-bid)
CREATE TABLE auto_bids (
    id INT AUTO_INCREMENT PRIMARY KEY,
    auction_id INT NOT NULL,
    bidder_id INT NOT NULL,
    max_bid_amount DECIMAL(15,2) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_auction_bidder (auction_id, bidder_id)
);

