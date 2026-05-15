package com.server.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static final Logger logger = LoggerFactory.getLogger(DBConnection.class);

    // Biến static lưu trữ instance duy nhất
    private static DBConnection instance;
    private HikariDataSource dataSource;
	private Properties properties;

    // Khóa constructor không cho tạo ngoài
    private DBConnection() {
		loadProperties();
		
        HikariConfig config = new HikariConfig();

        String jdbcUrl = getConfigValue("db.url", "DB_URL", "jdbc:mysql://localhost:3306/auction_db");
        String dbUser = getConfigValue("db.user", "DB_USER", "root");
        String dbPassword = getConfigValue("db.password", "DB_PASSWORD", "");
        int maxPoolSize = Integer.parseInt(getConfigValue("db.pool.maxSize", "DB_MAX_POOL", "20"));
        int minIdle = Integer.parseInt(getConfigValue("db.pool.minIdle", "DB_MIN_IDLE", "5"));

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);

        this.dataSource = new HikariDataSource(config);
        logger.info("HikariCP pool initialized | URL: {} | User: {} | MaxPoolSize: {}", jdbcUrl, dbUser, maxPoolSize);
    }
	
	/**
     * Hàm đọc file application.properties từ thư mục resources
     */
    private void loadProperties() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
                logger.info("Loaded application.properties successfully.");
            } else {
                logger.warn("application.properties not found in classpath. Falling back to environment variables.");
            }
        } catch (Exception e) {
            logger.error("Error loading application.properties", e);
        }
    }

    /**
     * Hàm lấy giá trị cấu hình linh hoạt
     */
    private String getConfigValue(String propKey, String envKey, String fallback) {
        // Tìm trong file properties trước
        String value = properties.getProperty(propKey);
        
        //Nếu không có, tìm trong biến môi trường của hệ thống
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(envKey);
        }
        
        //Nếu vẫn không có, dùng giá trị mặc định
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

