package com.server.config;

import java.util.Arrays;
import java.util.List;

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
import com.server.service.auction.strategy.OptimisticLockValidation;
import com.server.websocket.Broadcaster;

public class AppConfig {

    /**
     * Class để gom các thành phần cần thiết mang về ServerApp
     */
    public static class AppComponents {
        public final ApiRouter apiRouter;
        public final AuctionService auctionService;

        public AppComponents(ApiRouter apiRouter, AuctionService auctionService) {
            this.apiRouter = apiRouter;
            this.auctionService = auctionService;
        }
    }

    public static AppComponents buildDependencies() {
        // Repositories (DAOs)
        UserRepository userRepo = new UserRepository();
        ItemRepository itemRepo = new ItemRepository();
        AuctionRepository auctionRepo = new AuctionRepository();
        BidTransactionRepository bidRepo = new BidTransactionRepository();
        AutoBidRepository autoBidRepo = new AutoBidRepository();
        SellerRepository sellerRepo = new SellerRepository();
        BidderRepository bidderRepoReal = new BidderRepository();

        // Utilities & Chuỗi Validate (Chain of Responsibility)
        JwtUtil jwtUtil = JwtUtil.fromEnvironment();
        List<BidValidationStrategy> rules = Arrays.asList(
                new BasicBidValidation(),
                new MinimumIncrementValidation()
        );
        BidValidationChain validator = new BidValidationChain(rules);

        // Processors & Broadcaster
        ManualBidProcessor manualProc = new ManualBidProcessor(auctionRepo, bidderRepoReal);
        AutoBidProcessor autoProc = new AutoBidProcessor(autoBidRepo, auctionRepo, bidderRepoReal);
        Broadcaster broadcaster = new Broadcaster();

        // Services
        AuthService authService = new AuthService(userRepo);
        ItemService itemService = new ItemService(itemRepo);
        AdminService adminService = new AdminService();
        UserService userService = new UserService();
        BidderService bidderService = new BidderService();
        SellerService sellerService = new SellerService(sellerRepo);
        
        AuctionService auctionService = new AuctionService(
                auctionRepo, bidRepo, autoBidRepo, itemService, userRepo,
                validator, new DefaultAntiSnipingStrategy(), manualProc, autoProc
        );
        
        // Gắn broadcaster để gửi real-time updates qua WebSocket
        auctionService.setEventListener(broadcaster);

        // Controllers
        AuthController authController = new AuthController(authService, jwtUtil);
        AuctionController auctionController = new AuctionController(auctionService);
        AdminController adminController = new AdminController(adminService);
        UserController userController = new UserController(userService);
        BidderController bidderController = new BidderController(bidderService);
        SellerController sellerController = new SellerController(sellerService);
        ImageController imageController = new ImageController();

        // Khởi tạo Router (Controllers)
		CommandFactory commandFactory = new CommandFactory(auctionService, itemService, userService);
        ApiRouter apiRouter = new ApiRouter(authController, auctionController, adminController,
			bidderController, sellerController, itemService, auctionService, jwtUtil, 
			userController, imageController, 
			commandFactory); 

        return new AppComponents(apiRouter, auctionService);
    }
}