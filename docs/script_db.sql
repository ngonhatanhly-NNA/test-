CREATE DATABASE IF NOT EXISTS auction_db;
USE auction_db;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS auto_bids, bid_transactions, wallet_transactions, auctions, items, sellers, bidders, admins, users;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    passwordHash VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    fullName VARCHAR(100),
    role VARCHAR(20) NOT NULL,
    phoneNumber VARCHAR(20),
    address VARCHAR(255),
    status VARCHAR(20),
    avatar_url VARCHAR(255) DEFAULT NULL
);

CREATE TABLE admins (
    user_id INT PRIMARY KEY,
    roleLevel VARCHAR(50),
    lastLoginIp VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE bidders (
    user_id INT PRIMARY KEY,
    walletBalance DOUBLE DEFAULT 0.0,
    creditCardInfo VARCHAR(255),
    reserved_amount DECIMAL(19, 2) DEFAULT 0.00,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE sellers (
    bidder_id INT PRIMARY KEY,
    shopName VARCHAR(100),
    rating DOUBLE DEFAULT 0.0,
    totalReviews INT DEFAULT 0,
    bankAccountNumber VARCHAR(50),
    isVerified BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (bidder_id) REFERENCES bidders(user_id) ON DELETE CASCADE
);

CREATE TABLE items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    seller_id INT NOT NULL,
    item_type VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    startingPrice DOUBLE,
    item_condition VARCHAR(50),
    imageUrls TEXT,
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

CREATE TABLE auctions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL,
    seller_id INT NOT NULL,
    start_time DATETIME,
    end_time DATETIME,
    step_price DECIMAL(15,2),
    current_highest_bid DECIMAL(15,2),
    highest_bidder_id INT,
    winner_id INT,
    status VARCHAR(20),
    created_at DATETIME,
    updated_at DATETIME,
    version INT DEFAULT 0,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (highest_bidder_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (winner_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE bid_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    auction_id INT NOT NULL,
    bidder_id INT NOT NULL,
    bid_amount DECIMAL(15,2),
    timestamp DATETIME,
    is_auto_bid BOOLEAN DEFAULT FALSE,
    event_seq BIGINT DEFAULT NULL,
    created_at DATETIME,
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_bid_transactions_order ON bid_transactions(auction_id, event_seq DESC, id DESC);

CREATE TABLE auto_bids (
    id INT AUTO_INCREMENT PRIMARY KEY,
    auction_id INT NOT NULL,
    bidder_id INT NOT NULL,
    max_bid_amount DECIMAL(15,2),
    custom_step_price DECIMAL(15,2) DEFAULT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME,
    updated_at DATETIME,
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE wallet_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    type VARCHAR(20) NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_wallet_tx_user_id (user_id),
    INDEX idx_wallet_tx_created_at (created_at),
    CONSTRAINT fk_wallet_tx_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);