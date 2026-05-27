package com.server;

import java.sql.Connection;
import java.sql.SQLException;

import com.server.config.AppConfig;
import com.server.config.AppConfig.AppComponents;
import com.server.config.DBConnection;
import com.server.util.ResponseUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

// [ĐÃ SỬA] KHÔNG KẾ THỪA WebSocketServer NỮA!
public class ServerApp {
    private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);

    public static void main(String[] args) {
        // 1. Kiểm tra kết nối DB
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            if (conn != null) {
                logger.info("Kết nối DB qua HikariCP thành công!");
            }
        } catch (SQLException e) {
            logger.error("Lỗi kết nối DB: {}", e.getMessage());
            return;
        }

        // 2. Lấy bộ Dependencies đã được lắp ráp từ AppConfig
        AppComponents appComponents = AppConfig.buildDependencies();

        // 3. Khởi động WebSocket (Đã được config chạy ngầm sẵn ở cổng 8080 bên AppConfig)
        appComponents.broadcaster.start();

        // 4. Khởi động REST API (Server )
        logger.info("=== TRẠM REST API CHẠY CỔNG 7070 ===");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
                    config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
                    config.staticFiles.add("uploads", Location.EXTERNAL);
                    config.jsonMapper(new JavalinJackson(objectMapper, true));
                })
                .exception(Exception.class, (e, ctx) -> {
                    logger.error("Javalin bắt được lỗi ngầm: {}", e.getMessage());
                    ctx.status(400).json(ResponseUtils.fail("ERROR", e.getMessage()));
                })
                .start(7070);

        // 5. Cài đặt Routes cho API HTTP
        appComponents.apiRouter.setupRoutes(app);
    }
}