package com.server.route;

import com.server.controller.AuctionController;
import com.server.controller.AuthController;
import com.server.DAO.AuctionRepository;
import com.server.DAO.BidTransactionRepository;
import com.server.service.ItemService;
import io.javalin.Javalin;

public class ApiRouter {

    public static void setupRoutes(Javalin app) {
        // 1. Khởi tạo các Controller và Service
        AuthController authController = new AuthController();
        ItemService itemService = new ItemService(); // Dùng tạm ItemService

        AuctionController auctionController = new AuctionController(
                new AuctionRepository(),
                new BidTransactionRepository()
        );

        // --- Nhóm API Xác thực (Auth) ---
        RegisterRoute.RESTregister(app, authController);
        LoginRoute.RESTLogin(app, authController);

        // --- Nhóm API Sản phẩm (Items) ---
        app.get("/api/items", ctx -> {
            // Lấy data và trả thẳng về dạng JSON 
            String jsonResponse = itemService.getAllItems();
            ctx.json(jsonResponse);
        });

        // --- Nhóm API Đấu giá (Auctions) ---
        app.get("/api/auctions/active", auctionController::getActiveAuctions);
        app.post("/api/auctions/bid", auctionController::placeBid);
        app.get("/api/auctions/{auctionId}", auctionController::getAuctionDetail);

        // Auto-bidd
        app.post("/api/auctions/{auctionId}/auto-bid/cancel", auctionController::cancelAutoBid);
        app.put("/api/auctions/{auctionId}/auto-bid/update", auctionController::updateAutoBidAmount);
    }
}