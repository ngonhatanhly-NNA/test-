package com.client.controller.dashboard.strategy;

import java.util.HashMap;
import java.util.Map;

public class ProfileUIStrategyFactory {
    private static final Map<String, IProfileUIStrategy> strategies = new HashMap<>();

    static {
        strategies.put("ADMIN", new AdminProfileUIStrategy());
        strategies.put("BIDDER", new BidderProfileUIStrategy());
        strategies.put("SELLER", new SellerProfileUIStrategy());
    }

    public static IProfileUIStrategy getStrategy(String role) {
        return strategies.getOrDefault(role.toUpperCase(), new BidderProfileUIStrategy());
    }
}