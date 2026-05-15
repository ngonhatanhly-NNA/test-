package com.server.route;

import com.server.controller.AuctionController;
import com.server.controller.AuthController;
import com.server.controller.AdminController;
import com.server.controller.BidderController;
import com.server.controller.SellerController;
import com.server.controller.UserController;
import com.server.controller.ImageController;
import com.server.controller.command.CommandFactory;
import com.server.service.AuctionService;
import com.server.service.ItemService;
import com.server.service.UserService;
import com.server.model.Role;
import com.server.security.AuthGuard;
import com.server.security.JwtUtil;

import io.javalin.Javalin;
import io.javalin.http.HandlerType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiRouter {
    private static final Logger logger = LoggerFactory.getLogger(ApiRouter.class);

    private final AuthController authController;
    private final AuctionController auctionController;
    private final AdminController adminController;
    private final BidderController bidderController;
    private final SellerController sellerController;
    private final ItemService itemService;
    private final AuctionService auctionService;
    private final JwtUtil jwtUtil;
    private final UserController userController;
    private final ImageController imageController;
    private final CommandFactory commandFactory;

    public ApiRouter(AuthController authController, AuctionController auctionController,
                     AdminController adminController, BidderController bidderController,
                     SellerController sellerController, ItemService itemService,
                     AuctionService auctionService, JwtUtil jwtUtil, UserController userController,
                     ImageController imageController, CommandFactory commandFactory) {
        this.authController = authController;
        this.auctionController = auctionController;
        this.adminController = adminController;
        this.bidderController = bidderController;
        this.sellerController = sellerController;
        this.itemService = itemService;
        this.auctionService = auctionService;
        this.jwtUtil = jwtUtil;
        this.userController = userController;
        this.imageController = imageController;
        this.commandFactory = commandFactory;
    }

    /**
     * HÀM CHÍNH: Lắp ráp các module Router
     */
    public void setupRoutes(Javalin app) {
        UserService userService = new UserService();
        AuthGuard authGuard = new AuthGuard(userService, jwtUtil);

        // Chia để trị
        setupSecurityGuards(app, authGuard);
        setupImageRoutes(app);
        setupAuthAndUserRoutes(app, authGuard);
        setupBidderAndSellerRoutes(app);
        setupItemRoutes(app);
        setupAuctionRoutes(app);
        setupAdminRoutes(app);

        logger.info("API routes đã được thiết lập thành công (Kiến trúc Modular Router)");
    }

    // ==============================================================
    // 1. MODULE BẢO VỆ (SECURITY GUARDS)
    // ==============================================================
    private void setupSecurityGuards(Javalin app, AuthGuard authGuard) {
        app.before("/api/admin/*", ctx -> authGuard.requireRole(ctx, Role.ADMIN));
        app.before("/api/users/profile", ctx -> authGuard.requireLogin(ctx));
        app.before("/api/users/update", ctx -> authGuard.requireLogin(ctx));
        app.before("/api/users/upgrade-to-seller", ctx -> authGuard.requireRole(ctx, Role.BIDDER));
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
    }

    // ==============================================================
    // 2. MODULE HÌNH ẢNH
    // ==============================================================
    private void setupImageRoutes(Javalin app) {
        app.post("/api/images/upload", ctx -> imageController.uploadImage(ctx));
        app.get("/api/images/{filename}", ctx -> imageController.serveImage(ctx));
    }

    // ==============================================================
    // 3. MODULE NGƯỜI DÙNG & XÁC THỰC
    // ==============================================================
    private void setupAuthAndUserRoutes(Javalin app, AuthGuard authGuard) {
        app.post("/api/login", ctx -> authController.processLoginRest(ctx));
        app.post("/api/register", ctx -> authController.processRegisterRest(ctx));
        app.get("/api/users/profile", ctx -> authController.getUserProfile(ctx));

        // Đoạn này có dùng chung Command Pattern
        app.put("/api/users/update", commandFactory.updateProfile());

        app.post("/api/users/avatar", ctx -> {
            String jsonBody = ctx.body();
            String resultJson = userController.handleUpdateAvatar(jsonBody);
            ctx.json(resultJson);
        });

        app.post("/api/users/upgrade-to-seller", ctx -> {
            String username = authGuard.getUsernameFromToken(ctx);
            String resultJson = userController.handleUpgradeToSeller(username);
            ctx.json(resultJson);
        });

        app.post("/api/users/change-password", ctx -> {
            String username = authGuard.getUsernameFromToken(ctx);
            if (username == null) {
                ctx.status(401).json("{\"status\":\"FAIL\",\"message\":\"Chưa đăng nhập\"}");
                return;
            }
            String resultJson = userController.handleChangePassword(username, ctx.body());
            ctx.json(resultJson);
        });
    }

    // ==============================================================
    // 4. MODULE TÀI CHÍNH & VÍ (BIDDER / SELLER)
    // ==============================================================
    private void setupBidderAndSellerRoutes(Javalin app) {
        // Nhóm Bidder
        app.put("/api/bidders/{bidderId}/deposit", ctx -> bidderController.depositMoney(ctx));
        app.get("/api/bidders/{bidderId}/wallet", ctx -> bidderController.getWalletBalance(ctx));
        app.get("/api/bidders/{bidderId}/profile", ctx -> bidderController.getBidderProfile(ctx));

        // Nhóm Seller
        app.put("/api/sellers/{sellerId}/deposit", ctx -> bidderController.depositMoney(ctx));
        app.get("/api/sellers/{sellerId}/wallet", ctx -> bidderController.getWalletBalance(ctx));
        app.get("/api/sellers/{sellerId}", ctx -> sellerController.getSellerById(ctx));
        app.get("/api/sellers/{sellerId}/items", ctx -> sellerController.getSellerItems(ctx));
        app.get("/api/sellers/{sellerId}/statistics", ctx -> sellerController.getSellerStatistics(ctx));
        app.get("/api/sellers/{sellerId}/transfer-history", ctx -> sellerController.getTransferHistory(ctx));
    }

    // ==============================================================
    // 5. MODULE SẢN PHẨM (ITEMS) - Dùng Command Pattern
    // ==============================================================
    private void setupItemRoutes(Javalin app) {
        app.get("/api/items", commandFactory.getAllItems());
        app.post("/api/items", commandFactory.createItem());
        app.get("/api/items/seller/{sellerId}", commandFactory.getItemsBySeller(0));
    }

    // ==============================================================
    // 6. MODULE ĐẤU GIÁ (AUCTIONS) - Nơi bạn "múa" code
    // ==============================================================
    private void setupAuctionRoutes(Javalin app) {
        // --- Nhóm dùng Command Pattern ---
        app.post("/api/auctions", commandFactory.createAuction());
        app.post("/api/auctions/bid", commandFactory.placeBid());
        app.get("/api/auctions/active", commandFactory.getActiveAuctions());
        app.get("/api/auctions/upcoming", commandFactory.getUpcomingAuctions());
        app.get("/api/auctions/{auctionId}/bids", commandFactory.getBidHistory());
        app.post("/api/auctions/{auctionId}/auto-bid/cancel", commandFactory.cancelAutoBid());
        app.put("/api/auctions/{auctionId}/auto-bid/update", commandFactory.updateAutoBid());
        app.get("/api/auctions/{auctionId}", commandFactory.getAuctionDetail());

        // --- Nhóm dùng Controller truyền thống ---
        app.get("/api/auctions/seller/{sellerId}/active", ctx -> auctionController.getActiveAuctionsBySeller(ctx));
        app.get("/api/auctions/bidder/{bidderId}/won", ctx -> auctionController.getWonAuctionsByBidder(ctx));
        app.get("/api/auctions/item/{itemId}/status", ctx -> auctionController.getAuctionStatusByItemId(ctx));
    }

    // ==============================================================
    // 7. MODULE QUẢN TRỊ VIÊN (ADMIN)
    // ==============================================================
    private void setupAdminRoutes(Javalin app) {
        app.get("/api/admin/dashboard", ctx -> adminController.getDashboard(ctx));
        app.get("/api/admin/users", ctx -> adminController.getAllUsers(ctx));
        app.get("/api/admin/users/search/{username}", ctx -> adminController.searchUser(ctx));
        app.post("/api/admin/users/ban", ctx -> adminController.banUser(ctx));
        app.post("/api/admin/users/unban", ctx -> adminController.unbanUser(ctx));
        app.post("/api/admin/sellers/approve", ctx -> adminController.approveSeller(ctx));
        app.get("/api/admin/items/analytics", ctx -> adminController.getProductAnalytics(ctx));
        app.delete("/api/admin/items/{itemId}", ctx -> adminController.deleteItem(ctx));
        app.get("/api/admin/finance/revenue-estimate", ctx -> adminController.getRevenueEstimate(ctx));
        app.get("/api/admin/users/{username}/activity", ctx -> adminController.getUserActivityLog(ctx));
        app.post("/api/admin/auctions/cancel", ctx -> adminController.cancelAuction(ctx));
    }
}