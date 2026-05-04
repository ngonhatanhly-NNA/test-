package com.server.route;

import com.server.controller.AuctionController;
import com.server.controller.AuthController;
import com.server.controller.AdminController;
import com.server.controller.SellerController;
import com.server.controller.UserController;
import com.server.controller.command.CancelAutoBidCommand;
import com.server.controller.command.CreateAuctionCommand;
import com.server.controller.command.CreateItemCommand;
import com.server.controller.command.GetActiveAuctionsCommand;
import com.server.controller.command.GetAllItemsCommand;
import com.server.controller.command.GetItemsBySellerIdCommand;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.server.controller.ImageController; // [MỚI] Thêm import này để xử lý ảnh

public class ApiRouter {
    private static final Logger logger = LoggerFactory.getLogger(ApiRouter.class);

    private final AuthController authController;
    private final AuctionController auctionController;
    private final AdminController adminController;
    private final SellerController sellerController;
    private final ItemService itemService;
    private final AuctionService auctionService;
    private final JwtUtil jwtUtil;
    private final UserController userController;
    private final ImageController imageController; // [MỚI] Thêm controller xử lý ảnh

    public ApiRouter(AuthController authController, AuctionController auctionController,
                     AdminController adminController, SellerController sellerController,
                     ItemService itemService, AuctionService auctionService,
                     JwtUtil jwtUtil, UserController userController,
                     ImageController imageController) { // [MỚI] Thêm ImageController vào constructor
        this.authController = authController;
        this.auctionController = auctionController;
        this.adminController = adminController;
        this.sellerController = sellerController;
        this.itemService = itemService;
        this.auctionService = auctionService;
        this.jwtUtil = jwtUtil;
        this.userController = userController;
        this.imageController = imageController; // [MỚI]
    }

    public void setupRoutes(Javalin app) {
        UserService userService = new UserService();
        AuthGuard authGuard = new AuthGuard(userService, jwtUtil);

        // --- Bảo vệ các routes ---
        app.before("/api/admin/*", ctx -> authGuard.requireRole(ctx, Role.ADMIN));
        app.before("/api/users/profile", ctx -> authGuard.requireLogin(ctx));
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

        // [MỚI] ĐĂNG KÝ NHÓM API QUẢN LÝ ẢNH
        // Upload ảnh (Cần role SELLER)
        app.post("/api/images/upload", ctx -> imageController.uploadImage(ctx));
        // Xem ảnh (Mở public cho tất cả mọi người cùng xem)
        app.get("/api/images/{filename}", ctx -> imageController.serveImage(ctx));

        // --- Nhóm API Xác thực (Auth) ---
        app.post("/api/login", ctx -> authController.processLoginRest(ctx));
        app.post("/api/register", ctx -> authController.processRegisterRest(ctx));
        app.get("/api/users/profile", ctx -> authController.getUserProfile(ctx));
        app.put("/api/users/update", new UpdateProfileCommand(userService));

        app.post("/api/users/upgrade-to-seller", ctx -> {
            String username = authGuard.getUsernameFromToken(ctx);
            String resultJson = userController.handleUpgradeToSeller(username);
            ctx.json(resultJson);
        });

        // --- Nhóm API Sản phẩm (Items) ---
        app.get("/api/items", new GetAllItemsCommand(itemService));
        app.post("/api/items", new CreateItemCommand(itemService));
        app.get("/api/items/seller/{sellerId}", new GetItemsBySellerIdCommand(itemService));

        // --- Nhóm API Đấu giá (Auctions) ---
        app.post("/api/auctions", new CreateAuctionCommand(auctionService));
        app.get("/api/auctions/active", new GetActiveAuctionsCommand(auctionService));
        app.post("/api/auctions/bid", new PlaceBidCommand(auctionService));
        app.get("/api/auctions/{auctionId}", new GetAuctionDetailCommand(auctionService));
        app.get("/api/auctions/{auctionId}/bids", new GetBidHistoryCommand(auctionService));

        // [MỚI] Lấy phiên đấu giá đang hoạt động theo seller - dùng cho Live Auction Monitoring
        app.get("/api/auctions/seller/{sellerId}/active",
                ctx -> auctionController.getActiveAuctionsBySeller(ctx));

        // [MỚI] Lấy phiên đấu giá đã thắng theo bidder - dùng cho Won Auctions ở My Inventory
        app.get("/api/auctions/bidder/{bidderId}/won",
                ctx -> auctionController.getWonAuctionsByBidder(ctx));

        // Auto-bid
        app.post("/api/auctions/{auctionId}/auto-bid/cancel", new CancelAutoBidCommand(auctionService));
        app.put("/api/auctions/{auctionId}/auto-bid/update", new UpdateAutoBidAmountCommand(auctionService));

        // --- Nhóm API Seller ---
        app.get("/api/sellers/{sellerId}", ctx -> sellerController.getSellerById(ctx));
        app.get("/api/sellers/{sellerId}/items", ctx -> sellerController.getSellerItems(ctx));
        app.get("/api/sellers/{sellerId}/statistics", ctx -> sellerController.getSellerStatistics(ctx));

        // --- Nhóm API Admin ---
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

        logger.info("API routes đã được thiết lập thành công");
    }
}
