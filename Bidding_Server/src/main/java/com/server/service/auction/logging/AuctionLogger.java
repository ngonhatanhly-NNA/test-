package com.server.service.auction.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade Pattern: Centralized logging cho Auction Service
 * Dễ bảo trì và mở rộng sau này
 * Log lịch sử đấu giá
 */
public class AuctionLogger {
    private static final Logger logger = LoggerFactory.getLogger(AuctionLogger.class);
    private static final String PREFIX = "[AUCTION]";

    public static void logBidPlaced(long auctionId, long bidderId, String bidAmount, boolean isAuto) {
        String type = isAuto ? "AUTO" : "MANUAL";
        logger.info("{} {} Bid - Auction: {}, User: {}, Amount: {}", PREFIX, type, auctionId, bidderId, bidAmount);
    }

    public static void logAutoBidEnabled(long auctionId, long bidderId, String maxAmount) {
        logger.info("{} Auto-bid enabled - Auction: {}, User: {}, Max: {}", PREFIX, auctionId, bidderId, maxAmount);
    }

    public static void logAutoBidDisabled(long auctionId, long bidderId, String reason) {
        logger.info("{} Auto-bid disabled - Auction: {}, User: {}, Reason: {}", PREFIX, auctionId, bidderId, reason);
    }

    public static void logAuctionFinished(long auctionId, long winnerId, String winAmount) {
        logger.info("{} Auction finished - ID: {}, Winner: {}, Amount: {}", PREFIX, auctionId, winnerId, winAmount);
    }

    public static void logTimeExtended(long auctionId, long extensionSeconds) {
        logger.info("{} Time extended (Anti-sniping) - Auction: {}, +{} seconds", PREFIX, auctionId, extensionSeconds);
    }

    public static void logError(String operation, long auctionId, String message) {
        logger.error("{} ERROR [{}] - Auction: {}, Message: {}", PREFIX, operation, auctionId, message);
    }

    public static void logWarning(String message) {
        logger.warn("{} WARNING: {}", PREFIX, message);
    }

    public static void logInfo(String message) {
        logger.info("{} INFO: {}", PREFIX, message);
    }
}

