package com.server;

import com.server.DAO.*;
import com.server.config.DBConnection;
import com.server.controller.*;
import com.server.route.ApiRouter;
import com.server.security.JwtUtil;
import com.server.service.*;
import com.server.service.auction.antisnipe.DefaultAntiSnipingStrategy;
import com.server.service.auction.processor.AutoBidProcessor;
import com.server.service.auction.processor.ManualBidProcessor;
import com.server.service.auction.strategy.BidValidationChain;
import com.server.websocket.Broadcaster;
import io.javalin.Javalin;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ThreadPoolExecutor;

public class ServerApp extends WebSocketServer {

    public ServerApp(InetSocketAddress address) { super(address); }

    // PHẦN WEBSOCKET (CHỈ DÙNG CHO ĐẤU GIÁ SAU NÀY)
    private AuctionService auctionService;
    private com.google.gson.Gson gson = new com.google.gson.Gson();

    public void setAuctionService(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Client kết nối mới: " + conn.getRemoteSocketAddress());
        Broadcaster.addClient(conn);
    }
    @Override public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Cleint ngắt kết nối: " + conn.getRemoteSocketAddress());
        Broadcaster.removeClient(conn);
    }
    @Override public void onMessage(WebSocket conn, String message) {
        System.out.println("nhận lệnh đấu giá: " + message);
        try {
            handleAuctionMessage(message);
        } catch (Exception e) {
            System.err.println("Lỗi xử lý message: " + e.getMessage());
        }
    }
    @Override public void onError(WebSocket conn, Exception ex) { ex.printStackTrace(); }
    @Override public void onStart() { System.out.println("=== TRẠM ĐẤU GIÁ CHẠY CỔNG " + getPort() + " ==="); }

    /**
     * Xử lý WebSocket message từ client
     */
    private void handleAuctionMessage(String message) {
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
                case "PLACE_BID":
                    handlePlaceBid(payload);
                    break;
                case "CANCEL_AUTO_BID":
                    handleCancelAutoBid(payload);
                    break;
                case "UPDATE_AUTO_BID":
                    handleUpdateAutoBid(payload);
                    break;
                default:
                    System.out.println("Loại message không hợp lệ: " + type);
            }
        } catch (Exception e) {
            System.err.println("Lỗi xử lý " + type + ": " + e.getMessage());
        }
    }

    /**
     * Xử lý đặt giá từ WebSocket
     */
    private void handlePlaceBid(String payload) throws Exception {
        com.shared.dto.BidRequestDTO bidRequest = gson.fromJson(payload, com.shared.dto.BidRequestDTO.class);
        if (bidRequest != null) {
            auctionService.placeBid(bidRequest);
        }
    }

    /**
     * Xử lý hủy auto-bid từ WebSocket
     */
    private void handleCancelAutoBid(String payload) throws Exception {
        com.shared.dto.AutoBidCancelDTO cancelRequest = gson.fromJson(payload, com.shared.dto.AutoBidCancelDTO.class);
        if (cancelRequest != null) {
            auctionService.cancelAutoBid(cancelRequest.getAuctionId(), cancelRequest.getBidderId());
        }
    }

    /**
     * Xử lý cập nhật auto-bid từ WebSocket
     */
    private void handleUpdateAutoBid(String payload) throws Exception {
        com.shared.dto.AutoBidUpdateDTO updateRequest = gson.fromJson(payload, com.shared.dto.AutoBidUpdateDTO.class);
        if (updateRequest != null) {
            auctionService.updateAutoBidAmount(updateRequest.getAuctionId(), updateRequest.getBidderId(), updateRequest.getMaxBidAmount());
        }
    }


    // ====== HÀM MAIN CHẠY CẢ 2 HỆ THỐNG ======
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            if (conn != null) {
                System.out.println("Kết nối DB qua HikariCP thành công!");
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kết nối DB: " + e.getMessage());
            return; // Dừng server luôn nếu không kết nối được Database
        }

        // Khởi động WebSocket (Cổng 8080)
        ServerApp wsServer = new ServerApp(new InetSocketAddress("localhost", 8080));
        wsServer.start();

        // 3. Khởi động REST API (CỔNG 7070)

        // Khoi tao cac DI, update fix cho nay sau :)
        UserRepository userRepo = new UserRepository();
        ItemRepository itemRepo = new ItemRepository();
        AuctionRepository auctionRepo = new AuctionRepository();
        BidTransactionRepository bidRepo = new BidTransactionRepository();
        AutoBidRepository autoBidRepo = new AutoBidRepository();

        BidValidationChain validator = new BidValidationChain();
        ManualBidProcessor manualProc = new ManualBidProcessor();
        AutoBidProcessor autoProc = new AutoBidProcessor(autoBidRepo);
        Broadcaster broadcaster = new Broadcaster();

        AuctionService auctionService = new AuctionService(
                auctionRepo, bidRepo, autoBidRepo,
                validator, new DefaultAntiSnipingStrategy(), manualProc, autoProc);

        // Gắn broadcaster để gửi real-time updates qua WebSocket
        auctionService.setEventListener(broadcaster);
        
        // Gắn auctionService vào WebSocket server để xử lý messages
        wsServer.setAuctionService(auctionService);

        AuthService authService = new AuthService(userRepo);
        ItemService itemService = new ItemService(itemRepo);
        AdminService adminService = new AdminService();
        JwtUtil jwtUtil = JwtUtil.fromEnvironment();

        AuthController authController = new AuthController(authService, jwtUtil);
        AuctionController auctionController = new AuctionController(auctionService);
        AdminController adminController = new AdminController(adminService);
        // ===========================================//
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
        }).start(7070);
        System.out.println("=== TRẠM REST API CHẠY CỔNG 7070 ===");

        ApiRouter apiRouter = new ApiRouter(authController, auctionController, adminController, itemService, auctionService, jwtUtil);
        apiRouter.setupRoutes(app);
    }
}