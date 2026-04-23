package com.server.route;

import com.server.DAO.ItemRepository;
import com.server.controller.AuctionController;
import com.server.controller.AuthController;
import com.server.controller.AdminController;
import com.server.DAO.AuctionRepository;
import com.server.DAO.BidTransactionRepository;
import com.server.controller.command.CancelAutoBidCommand;
import com.server.controller.command.CreateAuctionCommand;
import com.server.controller.command.CreateItemCommand;
import com.server.controller.command.GetActiveAuctionsCommand;
import com.server.controller.command.GetAllItemsCommand;
import com.server.controller.command.GetAuctionDetailCommand;
import com.server.controller.command.GetBidHistoryCommand;
import com.server.controller.command.PlaceBidCommand;
import com.server.controller.command.UpdateAutoBidAmountCommand;
import com.server.controller.command.UpdateProfileCommand;
import com.server.service.AuctionService;
import com.server.service.ItemService;
import com.server.service.UserService;
import com.server.model.Role;
import com.server.security.AuthGuard;
import com.server.security.JwtUtil;
import io.javalin.Javalin;
import io.javalin.http.HandlerType;

import java.util.List;

public class ApiRouter {
    private final AuthController authController;
    private final AuctionController auctionController;
    private final AdminController adminController;
    private final ItemService itemService;
    private final AuctionService auctionService;
    private final JwtUtil jwtUtil;

    // Router nhận các Controller đã được lắp ráp đầy đủ phụ thuộc
    public ApiRouter(AuthController authController, AuctionController auctionController, AdminController adminController, ItemService itemService, AuctionService auctionService, JwtUtil jwtUtil) {
        this.authController = authController;
        this.auctionController = auctionController;
        this.adminController = adminController;
        this.itemService = itemService;
        this.auctionService = auctionService;
        this.jwtUtil = jwtUtil;
    }

    public void setupRoutes(Javalin app) { // Fix: public instance
        UserService userService = new UserService();
        AuthGuard authGuard = new AuthGuard(userService, jwtUtil);

        app.before("/api/admin/*", ctx -> authGuard.requireRole(ctx, Role.ADMIN));
        app.before("/api/users/profile", ctx -> authGuard.requireLogin(ctx));
        app.before("/api/items", ctx -> {
            if (ctx.method() == HandlerType.POST) {
                authGuard.requireRole(ctx, Role.SELLER);
            }
        });
        app.before("/api/auctions", ctx -> {
            if (ctx.method() == HandlerType.POST) {
                authGuard.requireRole(ctx, Role.SELLER);
            }
        });
        app.before("/api/auctions/bid", ctx -> authGuard.requireRole(ctx, Role.BIDDER, Role.SELLER));
        app.before("/api/auctions/*/auto-bid/*", ctx -> authGuard.requireRole(ctx, Role.BIDDER, Role.SELLER));

        // --- Nhóm API Xác thực (Auth) ---
        // Khớp với AuthNetwork.java (dùng /api/login, /api/register)
        app.post("/api/login", ctx -> authController.processLoginRest(ctx));
        app.post("/api/register", ctx -> authController.processRegisterRest(ctx));
        app.get("/api/users/profile", ctx -> authController.getUserProfile(ctx));
        // Update profile cần session username + update full profile theo role
        app.put("/api/users/update", new UpdateProfileCommand(userService));

        // --- Nhóm API Sản phẩm (Items) ---
        app.get("/api/items", new GetAllItemsCommand(itemService));
        app.post("/api/items", new CreateItemCommand(itemService));
        //Lưu ý là dùng POST dùng để gửi dữ liệu lên, GET dùng để lấy dữ liệu về.

        // --- Nhóm API Đấu giá (Auctions) ---
        app.post("/api/auctions", new CreateAuctionCommand(auctionService));
        app.get("/api/auctions/active", new GetActiveAuctionsCommand(auctionService));
        app.post("/api/auctions/bid", new PlaceBidCommand(auctionService));
        app.get("/api/auctions/{auctionId}", new GetAuctionDetailCommand(auctionService));
        app.get("/api/auctions/{auctionId}/bids", new GetBidHistoryCommand(auctionService));

        // Auto-bid
        app.post("/api/auctions/{auctionId}/auto-bid/cancel", new CancelAutoBidCommand(auctionService));
        app.put("/api/auctions/{auctionId}/auto-bid/update", new UpdateAutoBidAmountCommand(auctionService));

        // --- Nhóm API Admin ---
        app.get("/api/admin/dashboard", ctx -> adminController.getDashboard(ctx));
        app.get("/api/admin/users", ctx -> adminController.getAllUsers(ctx));
        app.get("/api/admin/users/search/{username}", ctx -> adminController.searchUser(ctx));
        app.post("/api/admin/users/ban", ctx -> adminController.banUser(ctx));
        app.post("/api/admin/users/unban", ctx -> adminController.unbanUser(ctx));
        app.post("/api/admin/sellers/approve", ctx -> adminController.approveSeller(ctx));
        app.get("/api/admin/items/analytics", ctx -> adminController.getProductAnalytics(ctx));
        app.get("/api/admin/finance/revenue-estimate", ctx -> adminController.getRevenueEstimate(ctx));
        app.get("/api/admin/users/{username}/activity", ctx -> adminController.getUserActivityLog(ctx));
        app.post("/api/admin/auctions/cancel", ctx -> adminController.cancelAuction(ctx));
    }
}