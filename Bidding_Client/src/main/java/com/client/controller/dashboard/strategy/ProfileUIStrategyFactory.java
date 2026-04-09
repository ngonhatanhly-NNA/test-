package com.client.controller.dashboard.strategy;

import java.util.HashMap;
import java.util.Map;

public class ProfileUIStrategyFactory {
    private static final Map<String, IProfileUIStrategy> strategies = new HashMap<>();

    static {
        strategies.put("ADMIN", new AdminUIStrategy()); // Bạn tạo tương tự AdminUIStrategy nhé
        strategies.put("BIDDER", new BidderUIStrategy());
        strategies.put("SELLER", new SellerUIStrategy());
    }

    public static IProfileUIStrategy getStrategy(String role) {
        return strategies.getOrDefault(role.toUpperCase(), new BidderUIStrategy());
    }
}