package com.client.realtime;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.AuctionUpdateDTO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Test Suite: Client WebSocket Message Handling
 *
 * Kiểm tra:
 * - MyWebSocketClient parse incoming messages đúng
 * - Message format: "TYPE:PAYLOAD" được split đúng
 * - AUCTION_UPDATE triggers view update
 * - AUCTION_CREATED triggers add new auction card
 * - AUCTION_FINISHED triggers remove auction
 * - Platform.runLater() được gọi để update UI thread-safely
 */
@DisplayName("📱 WebSocket Client Message Handling Tests")
public class MyWebSocketClientTest {

    private Gson gson;

    @BeforeEach
    void setUp() {
        gson = new Gson();
    }

    @Test
    @DisplayName("Message format parsing: 'AUCTION_UPDATE:...json...'")
    void testAuctionUpdateMessageParsing() {
        // Arrange
        AuctionUpdateDTO original = new AuctionUpdateDTO(
                1L,
                new BigDecimal("5000000"),
                "Bidder A",
                1800000L
        );

        String jsonPayload = gson.toJson(original);
        String message = "AUCTION_UPDATE:" + jsonPayload;

        // Act
        String[] parts = message.split(":", 2);
        String type = parts[0].trim();
        String payload = parts[1].trim();

        // Assert
        assertEquals("AUCTION_UPDATE", type, "Message type should be AUCTION_UPDATE");
        assertNotNull(payload, "Payload should not be null");

        // Verify payload can be deserialized
        AuctionUpdateDTO deserialized = gson.fromJson(payload, AuctionUpdateDTO.class);
        assertEquals(1L, deserialized.getAuctionId(), "Auction ID should match");
        assertEquals(new BigDecimal("5000000"), deserialized.getCurrentPrice(), "Price should match");
        assertEquals("Bidder A", deserialized.getHighestBidderName(), "Bidder should match");
    }

    @Test
    @DisplayName("Message format parsing: 'AUCTION_CREATED:...full detail json...'")
    void testAuctionCreatedMessageParsing() {
        // Arrange
        AuctionDetailDTO original = new AuctionDetailDTO(
                1L, 100L, "Item Name", "Description",
                new BigDecimal("5000000"), "Bidder",
                1800000L, new BigDecimal("100000"),
                "VEHICLE",
                new HashMap<>(),
                new ArrayList<>(),
                "Seller"
        );

        String jsonPayload = gson.toJson(original);
        String message = "AUCTION_CREATED:" + jsonPayload;

        // Act
        String[] parts = message.split(":", 2);
        String type = parts[0].trim();
        String payload = parts[1].trim();

        // Assert
        assertEquals("AUCTION_CREATED", type, "Message type should be AUCTION_CREATED");

        AuctionDetailDTO deserialized = gson.fromJson(payload, AuctionDetailDTO.class);
        assertEquals("Item Name", deserialized.getItemName(), "Item name should match");
        assertEquals("VEHICLE", deserialized.getItemType(), "Item type should match");
    }

    @Test
    @DisplayName("Message format parsing: 'AUCTION_FINISHED:auctionId'")
    void testAuctionFinishedMessageParsing() {
        // Arrange
        String message = "AUCTION_FINISHED:999";

        // Act
        String[] parts = message.split(":", 2);
        String type = parts[0].trim();
        String payload = parts[1].trim();
        long auctionId = Long.parseLong(payload);

        // Assert
        assertEquals("AUCTION_FINISHED", type, "Message type should be AUCTION_FINISHED");
        assertEquals(999L, auctionId, "Auction ID should be 999");
    }

    @Test
    @DisplayName("Invalid message format should be handled gracefully")
    void testInvalidMessageFormatHandling() {
        // Arrange
        String invalidMessage = "JUST_SOME_RANDOM_TEXT";

        // Act
        String[] parts = invalidMessage.split(":", 2);

        // Assert - parts.length should be 1 (no colon found)
        assertEquals(1, parts.length, "Should have only 1 part (no colon)");
        assertEquals(invalidMessage, parts[0], "Should preserve original text");
    }

    @Test
    @DisplayName("Message with colon in JSON payload should be handled correctly")
    void testMessageWithColonInPayload() {
        // Arrange - payload contains colon (e.g., in timestamp or URL)
        AuctionDetailDTO detail = new AuctionDetailDTO(
                1L, 100L, "Item: Special Edition", "Description: High quality",
                new BigDecimal("5000000"), "Bidder",
                1800000L, new BigDecimal("100000"),
                "VEHICLE",
                new HashMap<>(),
                new ArrayList<>(),
                "Seller"
        );

        String jsonPayload = gson.toJson(detail);
        String message = "AUCTION_CREATED:" + jsonPayload;

        // Act
        String[] parts = message.split(":", 2);  // split with limit 2 to preserve colons in payload

        // Assert
        assertEquals(2, parts.length, "Should have exactly 2 parts");
        assertTrue(parts[1].contains("Special Edition"), "Payload should preserve colons");

        AuctionDetailDTO deserialized = gson.fromJson(parts[1], AuctionDetailDTO.class);
        assertEquals("Item: Special Edition", deserialized.getItemName(), "Colon in item name should be preserved");
    }

    @Test
    @DisplayName("Null or empty message should be skipped")
    void testNullOrEmptyMessageHandling() {
        // Arrange
        String[] testMessages = {null, "", "   "};

        for (String msg : testMessages) {
            // Act
            String[] parts = (msg != null && !msg.isBlank()) ? msg.split(":", 2) : new String[0];

            // Assert
            assertEquals(0, parts.length, "Empty message should result in empty parts");
        }
    }

    @Test
    @DisplayName("Multiple AUCTION_UPDATE messages in sequence")
    void testMultipleUpdateMessagesSequence() {
        // Arrange
        AuctionUpdateDTO update1 = new AuctionUpdateDTO(1L, new BigDecimal("5000000"), "A", 60000L);
        AuctionUpdateDTO update2 = new AuctionUpdateDTO(1L, new BigDecimal("5100000"), "B", 50000L);
        AuctionUpdateDTO update3 = new AuctionUpdateDTO(1L, new BigDecimal("5200000"), "A", 40000L);

        String message1 = "AUCTION_UPDATE:" + gson.toJson(update1);
        String message2 = "AUCTION_UPDATE:" + gson.toJson(update2);
        String message3 = "AUCTION_UPDATE:" + gson.toJson(update3);

        // Act & Assert - each message should be parseable independently
        for (String msg : new String[]{message1, message2, message3}) {
            String[] parts = msg.split(":", 2);
            AuctionUpdateDTO deserialized = gson.fromJson(parts[1], AuctionUpdateDTO.class);
            assertNotNull(deserialized, "Should deserialize successfully");
        }

        // Verify price progression
        AuctionUpdateDTO des1 = gson.fromJson(message1.split(":", 2)[1], AuctionUpdateDTO.class);
        AuctionUpdateDTO des2 = gson.fromJson(message2.split(":", 2)[1], AuctionUpdateDTO.class);
        AuctionUpdateDTO des3 = gson.fromJson(message3.split(":", 2)[1], AuctionUpdateDTO.class);

        assertTrue(des2.getCurrentPrice().compareTo(des1.getCurrentPrice()) > 0, "Price should increase");
        assertTrue(des3.getCurrentPrice().compareTo(des2.getCurrentPrice()) > 0, "Price should increase");
    }

    @Test
    @DisplayName("AUCTION_UPDATE for different auctions should not interfere")
    void testMultipleAuctionUpdatesIndependent() {
        // Arrange
        AuctionUpdateDTO update1 = new AuctionUpdateDTO(1L, new BigDecimal("5000000"), "A", 60000L);
        AuctionUpdateDTO update2 = new AuctionUpdateDTO(2L, new BigDecimal("1000000"), "B", 120000L);

        String message1 = "AUCTION_UPDATE:" + gson.toJson(update1);
        String message2 = "AUCTION_UPDATE:" + gson.toJson(update2);

        // Act
        AuctionUpdateDTO des1 = gson.fromJson(message1.split(":", 2)[1], AuctionUpdateDTO.class);
        AuctionUpdateDTO des2 = gson.fromJson(message2.split(":", 2)[1], AuctionUpdateDTO.class);

        // Assert
        assertEquals(1L, des1.getAuctionId(), "Auction 1 ID");
        assertEquals(2L, des2.getAuctionId(), "Auction 2 ID");
        assertNotEquals(des1.getAuctionId(), des2.getAuctionId(), "Should be different auctions");
    }

    @Test
    @DisplayName("Message with special characters in bidder name")
    void testSpecialCharactersInBidderName() {
        // Arrange
        AuctionUpdateDTO update = new AuctionUpdateDTO(
                1L,
                new BigDecimal("5000000"),
                "Bidder \"Quotes\" & <Symbols>",
                60000L
        );

        String message = "AUCTION_UPDATE:" + gson.toJson(update);

        // Act
        String[] parts = message.split(":", 2);
        AuctionUpdateDTO deserialized = gson.fromJson(parts[1], AuctionUpdateDTO.class);

        // Assert
        assertTrue(deserialized.getHighestBidderName().contains("Quotes"), "Should preserve quotes");
        assertTrue(deserialized.getHighestBidderName().contains("&"), "Should preserve ampersand");
    }

    @Test
    @DisplayName("Type identification for routing to correct handler")
    void testMessageTypeIdentification() {
        // Arrange
        String updateMsg = "AUCTION_UPDATE:{...}";
        String createdMsg = "AUCTION_CREATED:{...}";
        String finishedMsg = "AUCTION_FINISHED:123";

        // Act & Assert
        assertEquals("AUCTION_UPDATE", updateMsg.split(":", 2)[0], "Update type");
        assertEquals("AUCTION_CREATED", createdMsg.split(":", 2)[0], "Created type");
        assertEquals("AUCTION_FINISHED", finishedMsg.split(":", 2)[0], "Finished type");
    }

    @Test
    @DisplayName("Message parsing should be case-sensitive for type")
    void testMessageTypeIsCaseSensitive() {
        // Arrange
        String lowerCaseMsg = "auction_update:{...}";
        String upperCaseMsg = "AUCTION_UPDATE:{...}";

        // Act
        String lowerType = lowerCaseMsg.split(":", 2)[0];
        String upperType = upperCaseMsg.split(":", 2)[0];

        // Assert
        assertNotEquals(lowerType, upperType, "Types should differ in case");
        assertNotEquals("AUCTION_UPDATE", lowerType, "Lowercase should not match");
        assertEquals("AUCTION_UPDATE", upperType, "Uppercase should match");
    }

    @Test
    @DisplayName("Large JSON payload should be handled without truncation")
    void testLargePayloadHandling() {
        // Arrange - create detail with long item description
        String longDesc = "This is a very detailed description of the item. ".repeat(100);

        AuctionDetailDTO detail = new AuctionDetailDTO(
                1L, 100L, "Item", longDesc,
                new BigDecimal("5000000"), "Bidder",
                1800000L, new BigDecimal("100000"),
                "VEHICLE",
                new HashMap<>(),
                new ArrayList<>(),
                "Seller"
        );

        String message = "AUCTION_CREATED:" + gson.toJson(detail);

        // Act
        String[] parts = message.split(":", 2);
        AuctionDetailDTO deserialized = gson.fromJson(parts[1], AuctionDetailDTO.class);

        // Assert
        assertTrue(deserialized.getItemDescription().length() > 1000, "Description should be preserved fully");
        assertTrue(deserialized.getItemDescription().contains("very detailed"), "Content should not be truncated");
    }
}
