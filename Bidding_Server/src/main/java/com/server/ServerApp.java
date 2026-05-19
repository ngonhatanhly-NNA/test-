package com.server;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;

import com.server.config.AppConfig;
import com.server.config.AppConfig.AppComponents;
import com.server.config.DBConnection;
import com.server.service.AuctionService;
import com.server.util.ResponseUtils;
import com.server.websocket.Broadcaster;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class ServerApp extends WebSocketServer {

    public ServerApp(InetSocketAddress address) { super(address); }
    private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);

    // PHẦN WEBSOCKET
    private AuctionService auctionService;
    private com.google.gson.Gson gson = new com.google.gson.Gson();

    public void setAuctionService(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override public void onOpen(WebSocket conn, ClientHandshake handshake) {
        logger.info("Client connected: {}", conn.getRemoteSocketAddress());
        Broadcaster.addClient(conn);
    }

    @Override public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        logger.info("Client disconnected: {}", conn.getRemoteSocketAddress());
        Broadcaster.removeClient(conn);
    }

    @Override public void onMessage(WebSocket conn, String message) {
        logger.info("Received auction message from {}: {}", conn.getRemoteSocketAddress(), message);
        try {
            handleAuctionMessage(conn, message);
        } catch (Exception e) {
            logger.error("Lỗi xử lý message từ client {}: {}", conn.getRemoteSocketAddress(), e.getMessage());
        }
    }

    @Override public void onError(WebSocket conn, Exception ex) {
        logger.error("Lỗi xảy ra với client {}: {}", conn.getRemoteSocketAddress(), ex.getMessage());
        ex.printStackTrace();
    }

    @Override public void onStart() {
        logger.info("WebSocket server started on port {}", getPort());
    }

    /**
     * Xử lý WebSocket message từ client
     */
    private void handleAuctionMessage(WebSocket conn, String message) {
        if (message == null || message.isEmpty() || auctionService == null) {
            return;
        }

        // Parse message format: "TYPE:JSON_PAYLOAD"
        String[] parts = message.split(":", 2);
        if (parts.length < 2) return;

        String type = parts[0];
        String payload = parts[1];

        try {
            switch (type) {
                case "PLACE_BID": handlePlaceBid(payload); break;
                case "CANCEL_AUTO_BID": handleCancelAutoBid(payload); break;
                case "UPDATE_AUTO_BID": handleUpdateAutoBid(payload); break;
                case "JOIN_ROOM":
                    try {
                        long roomId = Long.parseLong(payload.trim());
                        Broadcaster.joinRoom(roomId, conn);
                    } catch (NumberFormatException e) {
                        logger.error("Lỗi ID phòng không hợp lệ: {}", payload);
                    }
                    break;
                default: logger.warn("Loại message không hợp lệ: {}", type);
            }
        } catch (Exception e) {
            logger.error("Lỗi xử lý message {}: {}", type, e.getMessage());
        }
    }

    private void handlePlaceBid(String payload) throws Exception {
        com.shared.dto.BidRequestDTO bidRequest = gson.fromJson(payload, com.shared.dto.BidRequestDTO.class);
        if (bidRequest != null) {
            auctionService.placeBid(bidRequest);
        }
    }

    private void handleCancelAutoBid(String payload) throws Exception {
        com.shared.dto.AutoBidCancelDTO cancelRequest = gson.fromJson(payload, com.shared.dto.AutoBidCancelDTO.class);
        if (cancelRequest != null) {
            auctionService.cancelAutoBid(cancelRequest.getAuctionId(), cancelRequest.getBidderId());
        }
    }

    private void handleUpdateAutoBid(String payload) throws Exception {
        com.shared.dto.AutoBidUpdateDTO updateRequest = gson.fromJson(payload, com.shared.dto.AutoBidUpdateDTO.class);
        if (updateRequest != null) {
            auctionService.updateAutoBidAmount(updateRequest.getAuctionId(), updateRequest.getBidderId(), updateRequest.getMaxBidAmount(), updateRequest.getCustomStepPrice());
        }
    }

    // ====== HÀM MAIN CHẠY CẢ 2 HỆ THỐNG ======
    public static void main(String[] args) {
        // 1. Kiểm tra kết nối DB
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            if (conn != null) {
                logger.info("Kết nối DB qua HikariCP thành công!");
            }
        } catch (SQLException e) {
            logger.error("Lỗi kết nối DB: {}", e.getMessage());
            return; // Dừng server luôn nếu không kết nối được Database
        }

        // 2. Lấy bộ Dependencies đã được lắp ráp từ AppConfig
        AppComponents appComponents = AppConfig.buildDependencies();

        // 3. Khởi động WebSocket (Cổng 8080)
        ServerApp wsServer = new ServerApp(new InetSocketAddress("localhost", 8080));
        wsServer.setAuctionService(appComponents.auctionService); // Truyền Service vào Server
        wsServer.start();

        // 4. Khởi động REST API (CỔNG 7070)
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

        // 5. Cài đặt Routes cho API
        appComponents.apiRouter.setupRoutes(app);
    }
}