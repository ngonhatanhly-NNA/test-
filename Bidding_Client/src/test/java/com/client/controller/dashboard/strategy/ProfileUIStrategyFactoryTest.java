package com.client.controller.dashboard.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProfileUIStrategyFactory Tests")
public class ProfileUIStrategyFactoryTest {

    @Test
    void testAdminRole() {
        assertInstanceOf(AdminProfileUIStrategy.class,
                ProfileUIStrategyFactory.getStrategy("ADMIN"));
    }

    @Test
    void testBidderRole() {
        assertInstanceOf(BidderProfileUIStrategy.class,
                ProfileUIStrategyFactory.getStrategy("BIDDER"));
    }

    @Test
    void testSellerRole() {
        assertInstanceOf(SellerProfileUIStrategy.class,
                ProfileUIStrategyFactory.getStrategy("SELLER"));
    }

    @Test
    void testCaseInsensitiveAdmin() {
        assertInstanceOf(AdminProfileUIStrategy.class,
                ProfileUIStrategyFactory.getStrategy("admin"));
    }

    @Test
    void testCaseInsensitiveBidder() {
        assertInstanceOf(BidderProfileUIStrategy.class,
                ProfileUIStrategyFactory.getStrategy("bidder"));
    }

    @Test
    void testCaseInsensitiveSeller() {
        assertInstanceOf(SellerProfileUIStrategy.class,
                ProfileUIStrategyFactory.getStrategy("seller"));
    }

    @Test
    void testUnknownRoleDefaultsToBidder() {
        assertInstanceOf(BidderProfileUIStrategy.class,
                ProfileUIStrategyFactory.getStrategy("UNKNOWN"));
    }

    @Test
    void testNeverReturnsNull() {
        assertNotNull(ProfileUIStrategyFactory.getStrategy("ADMIN"));
        assertNotNull(ProfileUIStrategyFactory.getStrategy("BIDDER"));
        assertNotNull(ProfileUIStrategyFactory.getStrategy("SELLER"));
        assertNotNull(ProfileUIStrategyFactory.getStrategy("XYZ"));
    }

    @Test
    void testAdminStrategyInstantiable() {
        assertNotNull(new AdminProfileUIStrategy());
    }

    @Test
    void testBidderStrategyInstantiable() {
        assertNotNull(new BidderProfileUIStrategy());
    }

    @Test
    void testSellerStrategyInstantiable() {
        assertNotNull(new SellerProfileUIStrategy());
    }
}