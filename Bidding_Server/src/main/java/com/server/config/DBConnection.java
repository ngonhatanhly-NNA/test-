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

        String jdbcUrl = getEnvOrDefault("DB_URL", "jdbc:mysql://localhost:3306/auction_db");
        String dbUser = getEnvOrDefault("DB_USER", "root");
        String dbPassword = getEnvOrDefault("DB_PASSWORD", "");
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);


        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);

        this.dataSource = new HikariDataSource(config);
    }

    private String getEnvOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return (value == null || value.trim().isEmpty()) ? fallback : value;
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