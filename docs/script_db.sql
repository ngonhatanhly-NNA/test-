-- ============================================================
-- SCRIPT KHỞI TẠO DATABASE VÀ TEST DATA (TEAM 13)
-- ============================================================

CREATE DATABASE IF NOT EXISTS auction_db;
USE auction_db;

-- ============================================================
-- 1. XÓA BẢNG CŨ (DROP TABLES) - Xóa bảng con trước, bảng cha sau
-- ============================================================
SET FOREIGN_KEY_CHECKS = 0; -- Tạm tắt kiểm tra khóa ngoại để xóa thoải mái

DROP TABLE IF EXISTS auto_bids;
DROP TABLE IF EXISTS bid_transactions;
DROP TABLE IF EXISTS auctions;
DROP TABLE IF EXISTS items;
DROP TABLE IF EXISTS sellers;
DROP TABLE IF EXISTS bidders;
DROP TABLE IF EXISTS admins;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1; -- Bật lại kiểm tra khóa ngoại

-- ============================================================
-- 2. TẠO BẢNG MỚI (CREATE TABLES) - Tạo bảng cha trước, bảng con sau
-- ============================================================

-- BẢNG CHA: Users
CREATE TABLE users (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       passwordHash VARCHAR(255) NOT NULL,
                       email VARCHAR(100),
                       fullName VARCHAR(100),
                       role VARCHAR(20) NOT NULL,
                       phoneNumber VARCHAR(20),
                       address VARCHAR(255),
                       status VARCHAR(20)
);

-- BẢNG CON: Admins
CREATE TABLE admins (
                        user_id INT PRIMARY KEY,
                        roleLevel VARCHAR(50),
                        lastLoginIp VARCHAR(50),
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- BẢNG CON: Bidders
CREATE TABLE bidders (
                         user_id INT PRIMARY KEY,
                         walletBalance DOUBLE DEFAULT 0.0,
                         creditCardInfo VARCHAR(255),
                         FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- BẢNG CHÁU: Sellers (extends Bidders)
CREATE TABLE sellers (
                         bidder_id INT PRIMARY KEY,
                         shopName VARCHAR(100),
                         rating DOUBLE DEFAULT 0.0,
                         totalReviews INT DEFAULT 0,
                         bankAccountNumber VARCHAR(50),
                         isVerified BOOLEAN DEFAULT FALSE,
                         FOREIGN KEY (bidder_id) REFERENCES bidders(user_id) ON DELETE CASCADE
);

-- BẢNG CHA: Items
CREATE TABLE items (
                       id INT AUTO_INCREMENT PRIMARY KEY,
                       seller_id INT NOT NULL,
                       item_type VARCHAR(50) NOT NULL,
                       name VARCHAR(100) NOT NULL,
                       description TEXT,
                       startingPrice DOUBLE,
                       item_condition VARCHAR(50),
                       imageUrls TEXT,
    -- Thuộc tính riêng gộp chung
                       brand VARCHAR(100),
                       model VARCHAR(100),
                       warrantyMonths INT,
                       artistName VARCHAR(100),
                       material VARCHAR(100),
                       hasCertificateOfAuthenticity BOOLEAN,
                       manufactureYear INT,
                       vinNumber VARCHAR(50),
                       mileage INT,
                       FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
);

-- BẢNG: Auctions
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

-- BẢNG: Bid Transactions
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

-- BẢNG: Auto Bids
CREATE TABLE auto_bids (
                           id INT AUTO_INCREMENT PRIMARY KEY,
                           auction_id INT NOT NULL,
                           bidder_id INT NOT NULL,
                           max_bid_amount DECIMAL(15,2),
                           is_active BOOLEAN,
                           created_at DATETIME,
                           updated_at DATETIME,
                           FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
                           FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
);
