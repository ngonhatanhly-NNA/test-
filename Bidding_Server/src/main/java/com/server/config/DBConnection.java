package com.server.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DBConnection {
    private static final Logger logger = LoggerFactory.getLogger(DBConnection.class);

    // Biến static lưu trữ instance duy nhất
    private static DBConnection instance;
    private HikariDataSource dataSource;

    // Khóa constructor không cho tạo ngoài
    private DBConnection() {
        HikariConfig config = new HikariConfig();

        String jdbcUrl = getEnvOrDefault("DB_URL", "jdbc:mysql://localhost:3306/auction_db");
        String dbUser = getEnvOrDefault("DB_USER", "root");
        String dbPassword = getEnvOrDefault("DB_PASSWORD", "phong2007");
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);

        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);

        this.dataSource = new HikariDataSource(config);
        logger.info("HikariCP pool initialized | URL: {} | User: {} | MaxPoolSize: 20", jdbcUrl, dbUser);
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
        try {
            Connection conn = dataSource.getConnection();
            logger.debug("Connection obtained from HikariCP pool");
            return conn;
        } catch (SQLException e) {
            logger.error("Failed to obtain connection from HikariCP pool: {}", e.getMessage(), e);
            throw e;
        }
    }
}

