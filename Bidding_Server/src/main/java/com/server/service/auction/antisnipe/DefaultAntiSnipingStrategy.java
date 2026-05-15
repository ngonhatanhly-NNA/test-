package com.server.service.auction.antisnipe;

import com.server.model.Auction;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Implementation: Anti-Sniping mặc định
 * Nếu còn dưới 30 giây, extend thêm 60 giây
 */
public class DefaultAntiSnipingStrategy implements AntiSnipingStrategy {
    private static final long THRESHOLD_SECONDS = 30;
    private static final long EXTENSION_MILLIS = 60_000; // 60 giây

    @Override
    public boolean shouldExtendTime(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        long secondsRemaining = Duration.between(now, auction.getEndTime()).getSeconds();
        return secondsRemaining > 0 && secondsRemaining < THRESHOLD_SECONDS;
    }

    @Override
    public long getExtensionMillis() {
        return EXTENSION_MILLIS;
    }

    @Override
    public long getThresholdSeconds() {
        return THRESHOLD_SECONDS;
    }
}

