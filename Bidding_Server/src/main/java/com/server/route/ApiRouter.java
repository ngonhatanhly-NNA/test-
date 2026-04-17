package com.server.route;

import com.server.DAO.ItemRepository;
import com.server.controller.AuthController;
import com.server.DAO.AuctionRepository;
import com.server.DAO.BidTransactionRepository;
import com.server.controller.command.CancelAutoBidCommand;
import com.server.controller.command.CreateItemCommand;
import com.server.controller.command.GetActiveAuctionsCommand;
import com.server.controller.command.GetAllItemsCommand;
import com.server.controller.command.GetAuctionDetailCommand;
import com.server.controller.command.PlaceBidCommand;
import com.server.controller.command.UpdateAutoBidAmountCommand;
import com.server.service.AuctionService;
import com.server.service.ItemService;
import com.shared.dto.ItemResponseDTO;
import io.javalin.Javalin;

import java.util.List;

public class ApiRouter {

    public static void setupRoutes(Javalin app) {
        // 1. Khởi tạo các Controller và Service
        AuthController authController = new AuthController();
        ItemService itemService = new ItemService(new ItemRepository()); // Sửa ItemService thành method mới nhé
        AuctionService auctionService = new AuctionService(
                new AuctionRepository(),
                new BidTransactionRepository()
        );

        // --- Nhóm API Xác thực (Auth) ---
        RegisterRoute.RESTregister(app, authController);
        LoginRoute.RESTLogin(app, authController);

        // --- Nhóm API Sản phẩm (Items) ---
        // Thay vì viết ctx -> { ... }, ta nhét nguyên cái class Command vào đây
        app.get("/api/items", new GetAllItemsCommand(itemService));
        app.post("/api/items", new CreateItemCommand(itemService));
        //Lưu ý là dùng POST dùng để gửi dữ liệu lên, GET dùng để lấy dữ liệu về.

        // --- Nhóm API Đấu giá (Auctions) ---
        app.get("/api/auctions/active", new GetActiveAuctionsCommand(auctionService));
        app.post("/api/auctions/bid", new PlaceBidCommand(auctionService));
        app.get("/api/auctions/{auctionId}", new GetAuctionDetailCommand(auctionService));

        // Auto-bidd
        app.post("/api/auctions/{auctionId}/auto-bid/cancel", new CancelAutoBidCommand(auctionService));
        app.put("/api/auctions/{auctionId}/auto-bid/update", new UpdateAutoBidAmountCommand(auctionService));
    }
}