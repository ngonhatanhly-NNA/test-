package com.server.service;

import com.server.DAO.*;
import com.server.exception.AuctionException;
import com.server.service.auction.antisnipe.AntiSnipingStrategy;
import com.server.service.auction.core.*;
import com.server.service.auction.processor.AutoBidProcessor;
import com.server.service.auction.processor.BidProcessor;
import com.server.service.auction.strategy.BidValidationChain;
import com.server.websocket.AuctionEventListener;
import com.shared.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

public class AuctionService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);

    private final AuctionQueryService queryService;
    private final AuctionCommandService commandService;
    private final AuctionScheduler scheduler;

    public AuctionService(
            AuctionRepository auctionRepo,
            BidTransactionRepository bidRepo,
            AutoBidRepository autoBidRepo,
            ItemService itemService,
            UserRepository userRepository,
            BidValidationChain validationChain,
            AntiSnipingStrategy antiSnipingStrategy,
            BidProcessor bidProcessor,
            AutoBidProcessor autoBidProcessor) {

        // shared state
        AuctionSharedState sharedState = new AuctionSharedState(userRepository);

        // Query Service
        this.queryService = new AuctionQueryService(sharedState, auctionRepo, itemService);
        // Scheduler Service
        this.scheduler = new AuctionScheduler(sharedState, auctionRepo, userRepository, queryService);
		this.queryService.setScheduler(this.scheduler);
        // Command Service
        this.commandService = new AuctionCommandService(
                sharedState, auctionRepo, autoBidRepo, bidRepo, itemService,
                validationChain, antiSnipingStrategy, bidProcessor, autoBidProcessor, scheduler, queryService
        );

        this.scheduler.init();
    }

    public void setEventListener(AuctionEventListener listener) {
        this.commandService.setEventListener(listener);
        this.scheduler.setEventListener(listener);
    }

    // ==========COMMAND ==========
    public AuctionUpdateDTO placeBid(BidRequestDTO request) throws AuctionException {
        return commandService.placeBid(request);
    }

    public long createAuction(CreateAuctionDTO dto) throws AuctionException {
        return commandService.createAuction(dto);
    }

    public void cancelAutoBid(long auctionId, long bidderId) throws AuctionException {
        commandService.cancelAutoBid(auctionId, bidderId);
    }

    public void updateAutoBidAmount(long auctionId, long bidderId, BigDecimal newMaxAmount, BigDecimal customStepPrice) throws AuctionException {
        commandService.updateAutoBidAmount(auctionId, bidderId, newMaxAmount, customStepPrice);
    }

    // ==========QUERY ==========
    public AuctionDetailDTO getAuctionDetail(long auctionId) throws AuctionException {
        return queryService.getAuctionDetail(auctionId);
    }

    public List<BidHistoryDTO> getBidHistory(long auctionId) {
        return queryService.getBidHistory(auctionId);
    }

    public List<AuctionDetailDTO> getActiveAuctions() {
        return queryService.getActiveAuctions();
    }

    public List<AuctionDetailDTO> getUpcomingAuctions() {
        return queryService.getUpcomingAuctions();
    }

    public List<AuctionDetailDTO> getActiveAuctionsBySeller(long sellerId) {
        return queryService.getActiveAuctionsBySeller(sellerId);
    }

    public List<AuctionDetailDTO> getWonAuctionsByBidder(long bidderId) {
        return queryService.getWonAuctionsByBidder(bidderId);
    }

    public String getAuctionStatusByItemId(long itemId) {
        return queryService.getAuctionStatusByItemId(itemId);
    }

    // Sập nguồn :)))
    public void shutdown() {
        commandService.shutdown();
        scheduler.shutdown();
        logger.info("AuctionService shutdown successfully ");
    }
}