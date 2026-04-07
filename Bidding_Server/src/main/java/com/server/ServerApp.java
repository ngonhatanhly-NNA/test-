package com.server;

import com.google.gson.Gson;
import com.server.DAO.AuctionRepository;
import com.server.DAO.BidTransactionRepository;
import com.server.service.AuctionService;
import com.shared.dto.*;
import com.server.route.*;
import java.util.*;

import com.server.config.DBConnection;
import com.server.controller.AuthController;
import com.server.service.ItemService;
import io.javalin.Javalin;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.sql.Connection;


import com.shared.network.*;
public class ServerApp extends WebSocketServer {

    public ServerApp(InetSocketAddress address) { super(address); }

    // PHẦN WEBSOCKET (CHỈ DÙNG CHO ĐẤU GIÁ SAU NÀY)  // REST API xử lí đăng nập và đăng kí vì tính chất bảo mật và nhẹ hơn
    @Override public void onOpen(WebSocket conn, ClientHandshake handshake) {}
    @Override public void onClose(WebSocket conn, int code, String reason, boolean remote) {}
    @Override public void onMessage(WebSocket conn, String message) {
        System.out.println("nhận lệnh đấu giá: " + message);
        // Code đấu giá
    }
    @Override public void onError(WebSocket conn, Exception ex) { ex.printStackTrace(); }
    @Override public void onStart() { System.out.println("=== TRẠM ĐẤU GIÁ CHẠY CỔNG " + getPort() + " ==="); }


    // ====== HÀM MAIN CHẠY CẢ 2 HỆ THỐNG ======
    public static void main(String[] args) {
        // 1. Kết nối Database
        Connection conn = DBConnection.getDBConnection().getConnection();
        if (conn != null) { System.out.println("Kết nối DB thành công!"); }

        // WebSocket (Cổng 8080)
        ServerApp wsServer = new ServerApp(new InetSocketAddress("localhost", 8080));
        wsServer.start();

        // REST API (CỔNG 7070) CHO ĐĂNG KÝ / ĐĂNG NHẬP
        Javalin app = Javalin.create(config -> {
            config.plugins.enableCors(cors -> cors.add(it -> it.anyHost())); // Chống lỗi CORS bảo mật
        }).start(7070);
        System.out.println("=== TRẠM REST API (AUTH) CHẠY CỔNG 7070 ===");

        AuthController authController = new AuthController();
        ItemService itemService = new ItemService();
        AuctionRepository auctionRepo = new AuctionRepository(new DBConnection()); // Cần refactor để lấy singleton DB
        BidTransactionRepository bidRepo = new BidTransactionRepository(new DBConnection());
        AuctionService auctionService = new AuctionService(auctionRepo, bidRepo);
        Gson gson = new Gson();

        RegisterRoute.RESTregister(app, authController);
        LoginRoute.RESTLogin(app, authController);

        //TODO: Dinh tuyen API cho Item
        // Định tuyến API cho Item (Ví dụ: Lấy danh sách sản phẩm hiển thị ra Dashboard)
        app.get("/api/items", ctx -> {
            // 1. Nhờ ItemService đi lấy danh sách từ Database
            String jsonResponse = itemService.getAllItems();

            // 2. Trả danh sách đó về cho màn hình Dashboard
            ctx.contentType("application/json");
            ctx.result(jsonResponse);
        });

        app.get("/api/auctions/active", ctx -> {
            // Trả về danh sách đang đấu giá
            List<AuctionDetailDTO> auctions = auctionService.getActiveAuctions();
            ctx.json(new Response("SUCCESS", "Lấy danh sách thành công", auctions));
        });

        app.post("/api/auctions/bid", ctx -> {
            try {
                BidRequestDTO bidReq = gson.fromJson(ctx.body(), BidRequestDTO.class);
                AuctionUpdateDTO result = auctionService.placeBid(bidReq);
                ctx.json(new Response("SUCCESS", "Đặt giá thành công!", result));
            } catch (Exception e) {
                ctx.json(new Response("FAIL", e.getMessage(), null));
            }
        });;
    }
}