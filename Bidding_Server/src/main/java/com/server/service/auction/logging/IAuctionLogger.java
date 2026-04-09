package com.server.service.auction.logging;

/**
 * Logger Interface - Dependency Inversion
 * Can be easily mocked, replaced with SLF4J, etc.
 */
public interface IAuctionLogger {
    void logBidPlaced(long auctionId, long bidderId, String bidAmount, boolean isAuto);
    void logAutoBidEnabled(long auctionId, long bidderId, String maxAmount);
    void logAutoBidDisabled(long auctionId, long bidderId, String reason);
    void logAuctionFinished(long auctionId, long winnerId, String winAmount);
    void logTimeExtended(long auctionId, long extensionSeconds);
    void logError(String operation, long auctionId, String message);
    void logWarning(String message);
    void logInfo(String message);
}

