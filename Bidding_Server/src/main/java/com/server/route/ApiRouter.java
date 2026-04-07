package com.server.route;

import com.server.controller.*;
import com.server.DAO.*;
import com.server.config.DBConnection;
import io.javalin.Javalin;

public class ApiRouter {

    public static void setupRoutes(Javalin app) {
        // 1. Khởi tạo các thành phần (Chỉ khởi tạo 1 lần)
        DBConnection dbConnection = DBConnection.getDBConnection();
        
        AuthController authController = new AuthController();
        ItemController itemController = new ItemController(); // Ví dụ
        AuctionController auctionController = new AuctionController(
                new AuctionRepository(dbConnection), 
                new BidTransactionRepository(dbConnection)
        );

        // ==========================================
        // 2. ĐỊNH TUYẾN CÁC NHÓM API
        // ==========================================

        // --- Nhóm API Xác thực (Auth) ---
        app.post("/api/login", authController::login);
        app.post("/api/register", authController::register);

        // --- Nhóm API Sản phẩm (Items) ---
        app.get("/api/items", itemController::getAllItems);
        app.post("/api/items", itemController::createItem);

        // --- Nhóm API Đấu giá (Auctions) ---
        app.get("/api/auctions/active", auctionController::getActiveAuctions);
        app.post("/api/auctions/bid", auctionController::placeBid);
        app.get("/api/auctions/{auctionId}", auctionController::getAuctionDetail);
    }
}