package com.server.service.auction.antisnipe;

import com.server.model.Auction;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Anti-Sniping Strategy Pattern
 * Xử lý logic chống đặt giá ở giây cuối
 */
public interface AntiSnipingStrategy {
    /**
     * Kiểm tra và xử lý anti-sniping
     * @return true nếu cần extend thời gian, false nếu không cần
     */
    boolean shouldExtendTime(Auction auction);

    /**
     * Lấy thời gian cần extend (milliseconds)
     */
    long getExtensionMillis();

    /**
     * Lấy threshold tính bằng giây (nếu còn ít hơn giá trị này thì extend)
     */
    long getThresholdSeconds();
}

