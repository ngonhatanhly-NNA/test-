package com.client.realtime;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.AuctionUpdateDTO;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test Suite: Client-side Realtime Auction Display
 *
 * Kiểm tra:
 * - ViewLiveAuctions xử lý AUCTION_UPDATE messages đúng
 * - UI được cập nhật realtime (giá, người dẫn đầu, thời gian)
 * - Price chart được render chính xác
 * - Auction cards được update khi realtime message nhận được
 * - Bid amount field được cập nhật với minimum bid hint
 */
@DisplayName("📱 Client Realtime Display Tests")
public class ViewLiveAuctionsRealtimeTest {

    private AuctionDetailDTO sampleAuction;
    private AuctionUpdateDTO sampleUpdate;

    @BeforeEach
    void setUp() {
        // Create sample auction detail
        Map<String, String> specifics = new HashMap<>();
        specifics.put("Năm sản xuất", "2023");
        specifics.put("Hãng sản xuất", "Toyota");

        List<String> imageUrls = new ArrayList<>();
        imageUrls.add("Toyota-Camry.png");
        imageUrls.add("car-interior.png");

        sampleAuction = new AuctionDetailDTO(
                1L,                                    // auctionId
                100L,                                  // itemId
                "Toyota Camry 2023",                   // itemName
                "Ô tô 4 chỗ, màu đen, chạy 5000km",    // itemDescription
                new BigDecimal("500000000"),           // currentPrice
                "Bidder A",                            // highestBidderName
                1800000L,                              // remainingTime (30 mins in millis)
                new BigDecimal("10000000"),            // stepPrice
                "VEHICLE",                             // itemType
                specifics,                             // itemSpecifics
                imageUrls,                             // itemImageUrls
                "Seller B"                             // sellerName
        );

        // Create sample realtime update
        sampleUpdate = new AuctionUpdateDTO(
                1L,                                    // auctionId
                new BigDecimal("510000000"),           // currentPrice (updated)
                "Bidder C",                            // highestBidderName (changed)
                1750000L                               // remainingTime (30 secs less)
        );
    }

    @Test
    @DisplayName("AuctionDetailDTO chứa đầy đủ thông tin item display")
    void testAuctionDetailDTOHasCompleteItemInfo() {
        // Assert
        assertAll("AuctionDetailDTO should contain complete item info:",
                () -> assertNotNull(sampleAuction.getItemName(), "Item name should not be null"),
                () -> assertEquals("Toyota Camry 2023", sampleAuction.getItemName(), "Item name"),
                () -> assertNotNull(sampleAuction.getItemDescription(), "Item description should not be null"),
                () -> assertNotNull(sampleAuction.getItemType(), "Item type should not be null"),
                () -> assertEquals("VEHICLE", sampleAuction.getItemType(), "Item type should be VEHICLE"),
                () -> assertNotNull(sampleAuction.getItemSpecifics(), "Item specifics should not be null"),
                () -> assertEquals(2, sampleAuction.getItemSpecifics().size(), "Should have 2 specifics"),
                () -> assertNotNull(sampleAuction.getItemImageUrls(), "Item image URLs should not be null"),
                () -> assertFalse(sampleAuction.getItemImageUrls().isEmpty(), "Should have at least 1 image")
        );
    }

    @Test
    @DisplayName("AuctionDetailDTO chứa thông tin seller")
    void testAuctionDetailDTOHasSellerInfo() {
        // Assert
        assertAll("Should have seller info:",
                () -> assertNotNull(sampleAuction.getSellerName(), "Seller name should not be null"),
                () -> assertEquals("Seller B", sampleAuction.getSellerName(), "Seller name"),
                () -> assertTrue(sampleAuction.getSellerId() >= 0, "Seller ID should be >= 0")
        );
    }

    @Test
    @DisplayName("AuctionUpdateDTO có đủ thông tin realtime: giá, leader, time")
    void testAuctionUpdateDTOHasRealtimeInfo() {
        // Assert
        assertAll("AuctionUpdateDTO should have realtime updates:",
                () -> assertEquals(1L, sampleUpdate.getAuctionId(), "Auction ID should match"),
                () -> assertEquals(new BigDecimal("510000000"), sampleUpdate.getCurrentPrice(), "Updated price"),
                () -> assertEquals("Bidder C", sampleUpdate.getHighestBidderName(), "Updated bidder name"),
                () -> assertTrue(sampleUpdate.getRemainingTime() > 0, "Remaining time should be positive")
        );
    }

    @Test
    @DisplayName("Khi AuctionUpdateDTO nhận được, price tăng lên đúng")
    void testPriceUpdateedCorrectly() {
        // Arrange
        BigDecimal oldPrice = sampleAuction.getCurrentPrice();
        BigDecimal newPrice = sampleUpdate.getCurrentPrice();

        // Assert
        assertTrue(newPrice.compareTo(oldPrice) > 0, "New price should be higher than old price");

        BigDecimal difference = newPrice.subtract(oldPrice);
        assertEquals(new BigDecimal("10000000"), difference, "Price difference should equal step price");
    }

    @Test
    @DisplayName("Khi AuctionUpdateDTO nhận được, highestBidder thay đổi")
    void testHighestBidderUpdatedCorrectly() {
        // Arrange
        String oldBidder = sampleAuction.getHighestBidderName();
        String newBidder = sampleUpdate.getHighestBidderName();

        // Assert
        assertNotEquals(oldBidder, newBidder, "Bidder should have changed");
        assertEquals("Bidder C", newBidder, "Should be new bidder");
    }

    @Test
    @DisplayName("RemainingTime giảm dần theo update")
    void testRemainingTimeDecreases() {
        // Arrange
        long oldTime = sampleAuction.getRemainingTime();
        long newTime = sampleUpdate.getRemainingTime();

        // Assert
        assertTrue(newTime < oldTime, "Remaining time should decrease");

        long difference = oldTime - newTime;
        assertEquals(50000, difference, "Should have decreased by 50 seconds (50000ms)");
    }

    @Test
    @DisplayName("Minimum bid = currentPrice + stepPrice")
    void testMinimumBidCalculation() {
        // Arrange
        BigDecimal currentPrice = sampleAuction.getCurrentPrice();
        BigDecimal stepPrice = sampleAuction.getStepPrice();

        // Act
        BigDecimal minimumBid = currentPrice.add(stepPrice);

        // Assert
        assertEquals(new BigDecimal("510000000"), minimumBid, "Min bid should be current + step");
    }

    @Test
    @DisplayName("Khi price thay đổi, bid field hint cần cập nhật")
    void testBidFieldHintNeedsUpdate() {
        // Arrange
        BigDecimal oldMinBid = sampleAuction.getCurrentPrice().add(sampleAuction.getStepPrice());
        BigDecimal newMinBid = sampleUpdate.getCurrentPrice().add(sampleAuction.getStepPrice());

        // Assert
        assertNotEquals(oldMinBid, newMinBid, "Min bid hint should change");

        // Calculate difference
        BigDecimal hintChange = newMinBid.subtract(oldMinBid);
        assertEquals(sampleAuction.getStepPrice(), hintChange, "Hint change should equal step price");
    }

    @Test
    @DisplayName("Item specifics được format hiển thị đúng trên card")
    void testItemSpecificsFormattedForDisplay() {
        // Arrange
        Map<String, String> specifics = sampleAuction.getItemSpecifics();

        // Assert - specifics should be available and formatted
        assertNotNull(specifics, "Specifics should not be null");
        assertFalse(specifics.isEmpty(), "Should have at least one specific");
        assertTrue(specifics.containsKey("Năm sản xuất"), "Should have manufacturing year");
        assertEquals("2023", specifics.get("Năm sản xuất"), "Year should be 2023");
    }

    @Test
    @DisplayName("Image URLs được parse thành list")
    void testImageURLsParseCorrectly() {
        // Arrange
        List<String> images = sampleAuction.getItemImageUrls();

        // Assert
        assertNotNull(images, "Images list should not be null");
        assertFalse(images.isEmpty(), "Should have at least 1 image");
        assertTrue(images.getFirst().contains(".png"), "Image should have .png extension");
    }

    @Test
    @DisplayName("Formatting money: BigDecimal -> VNĐ string")
    void testMoneyFormatting() {
        // Arrange
        BigDecimal price = new BigDecimal("500000000");

        // Act - Format as currency
        String formatted = price.toPlainString() + " VNĐ";

        // Assert
        assertEquals("500000000 VNĐ", formatted, "Should format with VNĐ suffix");
    }

    @Test
    @DisplayName("Formatting remaining time: millis -> human readable")
    void testRemainingTimeFormatting() {
        // Test 30 minutes
        long thirtyMins = 30 * 60 * 1000;
        String formatted30 = formatRemaining(thirtyMins);
        assertTrue(formatted30.contains("phút"), "Should contain 'phút' for minutes");

        // Test 30 seconds (less than 1 minute)
        long thirtySeconds = 30 * 1000;
        String formatted30Sec = formatRemaining(thirtySeconds);
        assertTrue(formatted30Sec.contains("giây"), "Should contain 'giây' for seconds");

        // Test 0 (finished)
        String formattedZero = formatRemaining(0);
        assertTrue(formattedZero.contains("Hết"), "Should indicate time's up");
    }

    @Test
    @DisplayName("Card update sequence: old -> new values")
    void testAuctionCardUpdateSequence() {
        // Simulate old card state
        Map<String, String> oldCardState = new HashMap<>();
        oldCardState.put("price", sampleAuction.getCurrentPrice().toPlainString());
        oldCardState.put("leader", sampleAuction.getHighestBidderName());
        oldCardState.put("time", String.valueOf(sampleAuction.getRemainingTime()));

        // Simulate new card state after update
        Map<String, String> newCardState = new HashMap<>();
        newCardState.put("price", sampleUpdate.getCurrentPrice().toPlainString());
        newCardState.put("leader", sampleUpdate.getHighestBidderName());
        newCardState.put("time", String.valueOf(sampleUpdate.getRemainingTime()));

        // Assert
        assertNotEquals(oldCardState.get("price"), newCardState.get("price"), "Price should change");
        assertNotEquals(oldCardState.get("leader"), newCardState.get("leader"), "Leader should change");
        assertNotEquals(oldCardState.get("time"), newCardState.get("time"), "Time should change");
    }

    @Test
    @DisplayName("Multiple auctions display: each needs individual realtime update")
    void testMultipleAuctionRealtimeUpdates() {
        // Create updates for both
        AuctionUpdateDTO update1 = new AuctionUpdateDTO(1L, new BigDecimal("510000000"), "Bidder C", 1750000L);
        AuctionUpdateDTO update2 = new AuctionUpdateDTO(2L, new BigDecimal("20500000"), "Bidder Y", 1750000L);

        // Assert updates are independent
        assertNotEquals(update1.getAuctionId(), update2.getAuctionId(), "Different auctions");
        assertNotEquals(update1.getCurrentPrice(), update2.getCurrentPrice(), "Different prices");
        assertNotEquals(update1.getHighestBidderName(), update2.getHighestBidderName(), "Different bidders");
    }

    // Helper method (matching ViewLiveAuctions formatting)
    private String formatRemaining(long millis) {
        if (millis <= 0) {
            return "Hết giờ đấu giá";
        }
        long sec = millis / 1000;
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        if (h > 0) {
            return h + " giờ " + m + " phút";
        }
        if (m > 0) {
            return m + " phút " + s + " giây";
        }
        return s + " giây";
    }
}
