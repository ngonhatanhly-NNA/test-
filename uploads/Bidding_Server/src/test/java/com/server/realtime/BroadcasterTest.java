package com.server.realtime;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.java_websocket.WebSocket;

import com.google.gson.Gson;
import com.server.websocket.Broadcaster;
import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.AuctionUpdateDTO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Test Suite: WebSocket Broadcaster - Message Format & Delivery
 *
 * Kiểm tra:
 * - Broadcaster gửi đúng message format cho clients
 * - Message được JSON encode đúng
 * - Tất cả connected clients nhận được message (except disconnected ones)
 * - Multiple message types: AUCTION_UPDATE, AUCTION_CREATED, AUCTION_FINISHED
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("🔌 WebSocket Broadcaster Tests")
public class BroadcasterTest {

    @Mock
    private WebSocket mockClient1;

    @Mock
    private WebSocket mockClient2;

    @Mock
    private WebSocket mockClient3;

    private Broadcaster broadcaster;
    private Gson gson;

    @BeforeEach
    void setUp() {
        broadcaster = new Broadcaster();
        gson = new Gson();

        // Setup mock clients
        // lenient() allows some mocks to be unused in some tests
        lenient().when(mockClient1.isOpen()).thenReturn(true);
        lenient().when(mockClient2.isOpen()).thenReturn(true);
        lenient().when(mockClient3.isOpen()).thenReturn(false); // Disconnected
    }

    @Test
    @DisplayName("Broadcaster.onAuctionUpdate() gửi AUCTION_UPDATE message với JSON")
    void testBroadcasterSendsAuctionUpdateMessage() {
        // Arrange
        AuctionUpdateDTO update = new AuctionUpdateDTO(
                1L,
                new BigDecimal("1000000"),
                "Bidder A",
                1800000
        );

        // Add clients to broadcaster
        Broadcaster.addClient(mockClient1);
        Broadcaster.addClient(mockClient2);
        Broadcaster.addClient(mockClient3);

        // Act
        broadcaster.onAuctionUpdate(update);

        // Assert message format: "AUCTION_UPDATE:{JSON}"
        verify(mockClient1, times(1)).send(argThat((String msg) -> msg.contains("AUCTION_UPDATE")));
        verify(mockClient2, times(1)).send(argThat((String msg) -> msg.contains("AUCTION_UPDATE")));
        // Client 3 should be skipped since isOpen() returns false
        verify(mockClient3, never()).send(anyString());
    }

    @Test
    @DisplayName("Broadcaster.onAuctionCreated() gửi AUCTION_CREATED message với full details")
    void testBroadcasterSendsAuctionCreatedMessage() {
        // Arrange
        AuctionDetailDTO detail = new AuctionDetailDTO(
                1L,                                      // auctionId
                100L,                                    // itemId
                "Toyota Camry",                          // itemName
                "Beautiful car",                         // itemDescription
                new BigDecimal("500000000"),             // currentPrice
                "Bidder A",                              // highestBidderName
                1800000,                                 // remainingTime
                new BigDecimal("10000000"),              // stepPrice
                "VEHICLE",                               // itemType
                new HashMap<>(),                         // itemSpecifics
                new ArrayList<>(),                       // itemImageUrls
                "Seller B"                               // sellerName
        );

        Broadcaster.addClient(mockClient1);
        Broadcaster.addClient(mockClient2);

        // Act
        broadcaster.onAuctionCreated(detail);

        // Assert
        verify(mockClient1, times(1)).send(argThat((String msg) ->
                msg.startsWith("AUCTION_CREATED:") && msg.contains("Toyota Camry")));
    }

    @Test
    @DisplayName("Broadcaster.onAuctionFinished() gửi AUCTION_FINISHED message với auction ID")
    void testBroadcasterSendsAuctionFinishedMessage() {
        // Arrange
        long auctionId = 999L;

        Broadcaster.addClient(mockClient1);
        Broadcaster.addClient(mockClient2);

        // Act
        broadcaster.onAuctionFinished(auctionId);

        // Assert
        String expectedMessage = "AUCTION_FINISHED:999";

        verify(mockClient1, times(1)).send(expectedMessage);
        verify(mockClient2, times(1)).send(expectedMessage);
    }

    @Test
    @DisplayName("Message format tuân theo convention: TYPE:PAYLOAD")
    void testMessageFormatCompliance() {
        // Arrange
        AuctionUpdateDTO update = new AuctionUpdateDTO(
                1L, new BigDecimal("5000"), "Winner", 60000);

        Broadcaster.addClient(mockClient1);

        // Act
        broadcaster.onAuctionUpdate(update);

        // Assert message follows "TYPE:PAYLOAD" format
        verify(mockClient1).send(argThat((String msg) -> {
            String[] parts = msg.split(":", 2);
            return parts.length == 2 && "AUCTION_UPDATE".equals(parts[0]);
        }));
    }

    @Test
    @DisplayName("JSON trong message được escaped/encoded đúng")
    void testJSONEncodingInMessage() {
        // Arrange
        AuctionUpdateDTO update = new AuctionUpdateDTO(
                1L,
                new BigDecimal("5000"),
                "Bidder \"Quoted\" Name",  // Name with quotes
                60000
        );

        Broadcaster.addClient(mockClient1);

        // Act
        broadcaster.onAuctionUpdate(update);

        // Assert
        verify(mockClient1).send(argThat((String msg) -> {
            try {
                String[] parts = msg.split(":", 2);
                String payload = parts[1];
                // Should be able to deserialize back
                AuctionUpdateDTO deserialized = gson.fromJson(payload, AuctionUpdateDTO.class);
                return deserialized.getHighestBidderName().contains("Quoted");
            } catch (Exception e) {
                return false;
            }
        }));
    }

    @Test
    @DisplayName("Closed clients không nhận được messages")
    void testClosedClientsSkipped() {
        // Arrange
        when(mockClient1.isOpen()).thenReturn(true);
        when(mockClient2.isOpen()).thenReturn(false);

        Broadcaster.addClient(mockClient1);
        Broadcaster.addClient(mockClient2);

        AuctionUpdateDTO update = new AuctionUpdateDTO(1L, new BigDecimal("1000"), "A", 60000);

        // Act
        broadcaster.onAuctionUpdate(update);

        // Assert
        verify(mockClient1, times(1)).send(anyString());
        verify(mockClient2, never()).send(anyString());
    }

    @Test
    @DisplayName("Exception on one client không block broadcast ke clients khác")
    void testExceptionOnOneClientDoesntBlockOthers() {
        // Arrange
        doThrow(new RuntimeException("Send failed")).when(mockClient1).send(anyString());
        when(mockClient1.isOpen()).thenReturn(true);
        when(mockClient2.isOpen()).thenReturn(true);

        Broadcaster.addClient(mockClient1);
        Broadcaster.addClient(mockClient2);

        AuctionUpdateDTO update = new AuctionUpdateDTO(1L, new BigDecimal("1000"), "A", 60000);

        // Act
        broadcaster.onAuctionUpdate(update);

        // Assert - client2 should still get the message despite client1 failing
        verify(mockClient2, times(1)).send(anyString());
    }

    @Test
    @DisplayName("Multiple auctions broadcast independently")
    void testMultipleAuctionUpdatesIndependent() {
        // Arrange
        AuctionUpdateDTO update1 = new AuctionUpdateDTO(1L, new BigDecimal("1000"), "A", 60000);
        AuctionUpdateDTO update2 = new AuctionUpdateDTO(2L, new BigDecimal("2000"), "B", 60000);

        Broadcaster.addClient(mockClient1);

        // Act
        broadcaster.onAuctionUpdate(update1);
        broadcaster.onAuctionUpdate(update2);

        // Assert
        verify(mockClient1, times(2)).send(anyString());
    }

    @Test
    @DisplayName("Broadcaster handles rapid messages without dropping")
    void testRapidMessageSequence() {
        // Arrange
        Broadcaster.addClient(mockClient1);
        Broadcaster.addClient(mockClient2);

        // Act - Send 10 rapid updates
        for (int i = 1; i <= 10; i++) {
            AuctionUpdateDTO update = new AuctionUpdateDTO(
                    (long) i,
                    new BigDecimal(String.valueOf(i * 1000)),
                    "Bidder " + i,
                    60000
            );
            broadcaster.onAuctionUpdate(update);
        }

        // Assert - both clients should receive all 10 messages
        verify(mockClient1, times(10)).send(anyString());
        verify(mockClient2, times(10)).send(anyString());
    }

    @Test
    @DisplayName("AuctionDetailDTO contains complete item info when broadcast")
    void testDetailDTOContainsCompleteInfo() {
        // Arrange
        List<String> imageUrls = new ArrayList<>();
        imageUrls.add("image1.png");
        imageUrls.add("image2.png");

        AuctionDetailDTO detail = new AuctionDetailDTO(
                1L, 100L, "Item Name", "Description",
                new BigDecimal("1000"), "Leader",
                60000L, new BigDecimal("100"),
                "VEHICLE",
                new HashMap<>(),
                imageUrls,
                "Seller"
        );

        Broadcaster.addClient(mockClient1);

        // Act
        broadcaster.onAuctionCreated(detail);

        // Assert
        verify(mockClient1).send(argThat((String msg) -> {
            try {
                String[] parts = msg.split(":", 2);
                AuctionDetailDTO deserialized = gson.fromJson(parts[1], AuctionDetailDTO.class);
                return deserialized.getItemName().equals("Item Name") &&
                       deserialized.getItemDescription().equals("Description") &&
                       deserialized.getItemImageUrls().size() == 2;
            } catch (Exception e) {
                return false;
            }
        }));
    }

    @Test
    @DisplayName("Client can be added/removed dynamically during broadcast")
    void testDynamicClientManagement() {
        // Arrange
        Broadcaster.addClient(mockClient1);

        AuctionUpdateDTO update = new AuctionUpdateDTO(1L, new BigDecimal("1000"), "A", 60000);

        // Act & Assert - first broadcast
        broadcaster.onAuctionUpdate(update);
        verify(mockClient1, times(1)).send(anyString());

        // Add second client
        Broadcaster.addClient(mockClient2);

        // Act & Assert - second broadcast
        broadcaster.onAuctionUpdate(update);
        verify(mockClient1, times(2)).send(anyString());
        verify(mockClient2, times(1)).send(anyString());

        // Remove first client
        Broadcaster.removeClient(mockClient1);

        // Act & Assert - third broadcast
        broadcaster.onAuctionUpdate(update);
        verify(mockClient1, times(2)).send(anyString()); // Still 2, didn't increase
        verify(mockClient2, times(2)).send(anyString());
    }
}
