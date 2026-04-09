package com.server.service.auction.logging;

/**
 * Facade Pattern: Centralized logging cho Auction Service
 * Dễ bảo trì và mở rộng sau này
 */
public class AuctionLogger {
    private static final String PREFIX = "[AUCTION]";

    public static void logBidPlaced(long auctionId, long bidderId, String bidAmount, boolean isAuto) {
        String type = isAuto ? "AUTO" : "MANUAL";
        System.out.println(String.format("%s %s Bid - Auction: %d, User: %d, Amount: %s",
            PREFIX, type, auctionId, bidderId, bidAmount));
    }

    public static void logAutoBidEnabled(long auctionId, long bidderId, String maxAmount) {
        System.out.println(String.format("%s Auto-bid enabled - Auction: %d, User: %d, Max: %s",
            PREFIX, auctionId, bidderId, maxAmount));
    }

    public static void logAutoBidDisabled(long auctionId, long bidderId, String reason) {
        System.out.println(String.format("%s Auto-bid disabled - Auction: %d, User: %d, Reason: %s",
            PREFIX, auctionId, bidderId, reason));
    }

    public static void logAuctionFinished(long auctionId, long winnerId, String winAmount) {
        System.out.println(String.format("%s Auction finished - ID: %d, Winner: %d, Amount: %s",
            PREFIX, auctionId, winnerId, winAmount));
    }

    public static void logTimeExtended(long auctionId, long extensionSeconds) {
        System.out.println(String.format("%s Time extended (Anti-sniping) - Auction: %d, +%d seconds",
            PREFIX, auctionId, extensionSeconds));
    }

    public static void logError(String operation, long auctionId, String message) {
        System.err.println(String.format("%s ERROR [%s] - Auction: %d, Message: %s",
            PREFIX, operation, auctionId, message));
    }

    public static void logWarning(String message) {
        System.out.println(String.format("%s WARNING: %s", PREFIX, message));
    }

    public static void logInfo(String message) {
        System.out.println(String.format("%s INFO: %s", PREFIX, message));
    }
}

