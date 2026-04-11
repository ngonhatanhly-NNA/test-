package com.server.route;

import com.server.DAO.ItemRepository;
import com.server.controller.AuctionController;
import com.server.controller.AuthController;
import com.server.DAO.AuctionRepository;
import com.server.DAO.BidTransactionRepository;
import com.server.controller.GetAllItemsCommand;
import com.server.service.ItemService;
import com.shared.dto.ItemResponseDTO;
import io.javalin.Javalin;

import java.util.List;

public class ApiRouter {

    public static void setupRoutes(Javalin app) {
        // 1. Khởi tạo các Controller và Service
        AuthController authController = new AuthController();
        ItemService itemService = new ItemService(new ItemRepository()); // Sửa ItemService thành method mới nhé

        AuctionController auctionController = new AuctionController(
                new AuctionRepository(),
                new BidTransactionRepository()
        );

        // --- Nhóm API Xác thực (Auth) ---
        RegisterRoute.RESTregister(app, authController);
        LoginRoute.RESTLogin(app, authController);

        // --- Nhóm API Sản phẩm (Items) ---
        // Thay vì viết ctx -> { ... }, ta nhét nguyên cái class Command vào đây
        app.get("/api/items", new GetAllItemsCommand(itemService));

        // --- Nhóm API Đấu giá (Auctions) ---
        app.get("/api/auctions/active", auctionController::getActiveAuctions);
        app.post("/api/auctions/bid", auctionController::placeBid);
        app.get("/api/auctions/{auctionId}", auctionController::getAuctionDetail);

        // Auto-bidd
        app.post("/api/auctions/{auctionId}/auto-bid/cancel", auctionController::cancelAutoBid);
        app.put("/api/auctions/{auctionId}/auto-bid/update", auctionController::updateAutoBidAmount);
    }
}