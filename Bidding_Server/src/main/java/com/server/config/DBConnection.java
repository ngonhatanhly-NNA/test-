package com.server.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DBConnection {
    // Biến static lưu trữ instance duy nhất
    private static DBConnection instance;
    private HikariDataSource dataSource;

    // Khóa constructor không cho tạo ngoài
    private DBConnection() {
        HikariConfig config = new HikariConfig();
        // BẠN ĐỔI LẠI THÔNG TIN BÊN DƯỚI CHO ĐÚNG NHÉ
        config.setJdbcUrl("jdbc:mysql://localhost:3306/auction_db");
        config.setUsername("root");
        config.setPassword("");

        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);

        this.dataSource = new HikariDataSource(config);
    }

    // Cổng lấy Singleton Thread-safe
    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    // Lấy kết nối từ hồ bơi
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}