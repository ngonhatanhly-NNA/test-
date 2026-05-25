package com.server.config;

import java.util.Arrays;
import java.util.List;
import java.net.InetSocketAddress;

import com.server.DAO.*;
import com.server.controller.*;
import com.server.controller.command.CommandFactory;
import com.server.route.ApiRouter;
import com.server.security.JwtUtil;
import com.server.service.*;
import com.server.service.auction.antisnipe.DefaultAntiSnipingStrategy;
import com.server.service.auction.processor.AutoBidProcessor;
import com.server.service.auction.processor.ManualBidProcessor;
import com.server.service.auction.strategy.BasicBidValidation;
import com.server.service.auction.strategy.BidValidationChain;
import com.server.service.auction.strategy.BidValidationStrategy;
import com.server.service.auction.strategy.MinimumIncrementValidation;
import com.server.websocket.Broadcaster;

public class AppConfig {

    public static class AppComponents {
        public final ApiRouter apiRouter;
        public final AuctionService auctionService;
        public final Broadcaster broadcaster; // [THÊM MỚI] Gói thêm Broadcaster

        public AppComponents(ApiRouter apiRouter, AuctionService auctionService, Broadcaster broadcaster) {
            this.apiRouter = apiRouter;
            this.auctionService = auctionService;
            this.broadcaster = broadcaster;
        }
    }

    public static AppComponents buildDependencies() {
     
        UserRepository userRepo = new UserRepository();
        ItemRepository itemRepo = new ItemRepository();
        AuctionRepository auctionRepo = new AuctionRepository();
        BidTransactionRepository bidRepo = new BidTransactionRepository();
        AutoBidRepository autoBidRepo = new AutoBidRepository();
        SellerRepository sellerRepo = new SellerRepository();
        BidderRepository bidderRepoReal = new BidderRepository();

        JwtUtil jwtUtil = JwtUtil.fromEnvironment();
        List<BidValidationStrategy> rules = Arrays.asList(new BasicBidValidation(), new MinimumIncrementValidation());
        BidValidationChain validator = new BidValidationChain(rules);

        ManualBidProcessor manualProc = new ManualBidProcessor(auctionRepo, bidderRepoReal);
        AutoBidProcessor autoProc = new AutoBidProcessor(autoBidRepo, auctionRepo, bidderRepoReal);
        
        //  Khởi tạo WebSocket ở Cổng 8080
        Broadcaster broadcaster = new Broadcaster(new InetSocketAddress("0.0.0.0", 8080));

        AuthService authService = new AuthService(userRepo);
        ItemService itemService = new ItemService(itemRepo);
        AdminService adminService = new AdminService();
        UserService userService = new UserService();
        BidderService bidderService = new BidderService();
        SellerService sellerService = new SellerService(sellerRepo);
        
        //Nhớ truyền bidderRepo
        AuctionService auctionService = new AuctionService(
                auctionRepo, bidRepo, autoBidRepo, itemService, userRepo, bidderRepoReal,
                validator, new DefaultAntiSnipingStrategy(), manualProc, autoProc
        );
        
        // Circular wiring)
        auctionService.setEventListener(broadcaster); // 1. Auction gửi event cho Webscoket
        broadcaster.setAuctionService(auctionService); // 2. Websocket gửi lệnh ngược lại cho Auction

        AuthController authController = new AuthController(authService, jwtUtil);
        AuctionController auctionController = new AuctionController(auctionService);
        AdminController adminController = new AdminController(adminService);
        UserController userController = new UserController(userService);
        BidderController bidderController = new BidderController(bidderService);
        SellerController sellerController = new SellerController(sellerService);
        ImageController imageController = new ImageController();

        CommandFactory commandFactory = new CommandFactory(auctionService, itemService, userService);
        ApiRouter apiRouter = new ApiRouter(authController, auctionController, adminController,
            bidderController, sellerController, itemService, auctionService, jwtUtil, 
            userController, imageController, commandFactory); 

        return new AppComponents(apiRouter, auctionService, broadcaster);
    }
}