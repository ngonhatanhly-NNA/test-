package com.server.service.auction.core;

import com.server.DAO.UserRepository;
import com.server.model.Auction;
import com.server.model.BidTransaction;
import com.server.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionSharedState {
    private static final Logger logger = LoggerFactory.getLogger(AuctionSharedState.class);

    public final ConcurrentHashMap<Long, Auction> auctionCache = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Long, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Long, String> userDisplayNameCache = new ConcurrentHashMap<>();
    public final LinkedBlockingQueue<BidTransaction> bidQueue = new LinkedBlockingQueue<>();

    private final UserRepository userRepository;

    public AuctionSharedState(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

	/**
     * Thêm phiên đấu giá vào cache
     */
    public void cacheAuction(Auction auction) {
        auctionCache.put(auction.getId(), auction);
        if (auction.getStatus() == Auction.AuctionStatus.OPEN || auction.getStatus() == Auction.AuctionStatus.RUNNING) {
            auctionLocks.putIfAbsent(auction.getId(), new ReentrantLock());
        }
    }

	/**
     * Hiển thị tên người thắng
     */
    public String resolveUserDisplayName(long userId, String fallbackPrefix) {
        if (userId <= 0) {
            return fallbackPrefix + "_UNKNOWN";
        }

        String cached = userDisplayNameCache.get(userId);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }

        try {
            User user = userRepository.getUserById(userId);
            if (user != null) {
                String fullName = user.getFullName();
                String username = user.getUsername();
                String displayName = (fullName != null && !fullName.isBlank())
                        ? fullName
                        : ((username != null && !username.isBlank()) ? username : fallbackPrefix + "_" + userId);
                userDisplayNameCache.put(userId, displayName);
                return displayName;
            }
        } catch (Exception e) {
            logger.debug("Không lấy được user {} để hiển thị tên: {}", userId, e.getMessage());
        }

        return fallbackPrefix + "_" + userId;
    }
}