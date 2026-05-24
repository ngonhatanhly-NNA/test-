package com.server.realtime;

import com.google.gson.Gson;
import com.server.service.AuctionService;
import com.server.websocket.Broadcaster;
import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.AuctionUpdateDTO;
import com.shared.dto.AuctionWinnerDTO;
import org.java_websocket.WebSocket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocket Broadcaster Tests")
public class BroadcasterTest {

    @Mock private WebSocket mockClient1;
    @Mock private WebSocket mockClient2;
    @Mock private WebSocket mockClient3;
    @Mock private AuctionService auctionService;

    private Broadcaster broadcaster;
    private final Gson gson = new Gson();

    // Dùng port 0 để OS tự chọn port trống, tránh conflict trên CI
    private static final InetSocketAddress ADDR = new InetSocketAddress("localhost", 0);

    @BeforeEach
    void setUp() throws Exception {
        broadcaster = new Broadcaster(ADDR);
        broadcaster.setAuctionService(auctionService);

        // KHÔNG gọi broadcaster.start() — tránh bind TCP thật.
        // Thay vào đó inject trực tiếp vào static fields qua reflection.
        clearStaticState();

        // Stub getRemoteSocketAddress() để tránh NPE trong logger
        InetSocketAddress dummyAddr1 = new InetSocketAddress("127.0.0.1", 10001);
        InetSocketAddress dummyAddr2 = new InetSocketAddress("127.0.0.1", 10002);
        InetSocketAddress dummyAddr3 = new InetSocketAddress("127.0.0.1", 10003);

        lenient().when(mockClient1.getRemoteSocketAddress()).thenReturn(dummyAddr1);
        lenient().when(mockClient2.getRemoteSocketAddress()).thenReturn(dummyAddr2);
        lenient().when(mockClient3.getRemoteSocketAddress()).thenReturn(dummyAddr3);

        lenient().when(mockClient1.isOpen()).thenReturn(true);
        lenient().when(mockClient2.isOpen()).thenReturn(true);
        lenient().when(mockClient3.isOpen()).thenReturn(false); // Disconnected

        // Gọi onOpen để đăng ký client — không cần server thật
        broadcaster.onOpen(mockClient1, null);
        broadcaster.onOpen(mockClient2, null);
        broadcaster.onOpen(mockClient3, null);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clear static state sau mỗi test để không ảnh hưởng test khác
        clearStaticState();
    }

    /**
     * Xóa sạch static fields globalClients và rooms trong Broadcaster
     * để tránh state rò rỉ giữa các test (do field là static final).
     */
    private void clearStaticState() throws Exception {
        Field clientsField = Broadcaster.class.getDeclaredField("globalClients");
        clientsField.setAccessible(true);
        ((CopyOnWriteArrayList<?>) clientsField.get(null)).clear();

        Field roomsField = Broadcaster.class.getDeclaredField("rooms");
        roomsField.setAccessible(true);
        ((ConcurrentHashMap<?, ?>) roomsField.get(null)).clear();
    }

    @Test
    @DisplayName("onAuctionUpdate() sends AUCTION_UPDATE to clients in the correct room")
    void testOnAuctionUpdate() {
        long auctionId = 1L;
        // mockClient1 join room
        broadcaster.onMessage(mockClient1, "JOIN_ROOM:" + auctionId);

        AuctionUpdateDTO update = new AuctionUpdateDTO(auctionId, new BigDecimal("100"), "Test", 1000L);
        broadcaster.onAuctionUpdate(update);

        String expectedPayload = "AUCTION_UPDATE:" + gson.toJson(update);
        verify(mockClient1, times(1)).send(expectedPayload);
        verify(mockClient2, never()).send(anyString());
    }

    @Test
    @DisplayName("onAuctionCreated() sends AUCTION_CREATED to all connected clients")
    void testOnAuctionCreated() {
        AuctionDetailDTO detail = new AuctionDetailDTO(
                1L, 100L, "Item", "Desc", BigDecimal.TEN, "Bidder",
                3600000L, BigDecimal.ONE, "TYPE", new HashMap<>(), new ArrayList<>(),
                1L, "Seller", LocalDateTime.now().toString(), LocalDateTime.now().plusHours(1).toString()
        );

        broadcaster.onAuctionCreated(detail);

        String expectedPayload = "AUCTION_CREATED:" + gson.toJson(detail);
        verify(mockClient1, times(1)).send(expectedPayload);
        verify(mockClient2, times(1)).send(expectedPayload);
        // mockClient3 isOpen=false nên không được gửi
        verify(mockClient3, never()).send(anyString());
    }

    @Test
    @DisplayName("onAuctionFinished() sends AUCTION_FINISHED to all connected clients")
    void testOnAuctionFinished() {
        long auctionId = 1L;
        broadcaster.onAuctionFinished(auctionId);

        String expectedPayload = "AUCTION_FINISHED:" + auctionId;
        verify(mockClient1, times(1)).send(expectedPayload);
        verify(mockClient2, times(1)).send(expectedPayload);
    }

    @Test
    @DisplayName("onAuctionWon() sends AUCTION_WON to room and AUCTION_FINISHED to all")
    void testOnAuctionWon() {
        long auctionId = 1L;
        broadcaster.onMessage(mockClient1, "JOIN_ROOM:" + auctionId);

        AuctionWinnerDTO winner = new AuctionWinnerDTO(auctionId, "Item", 2L, "Winner", BigDecimal.TEN);
        broadcaster.onAuctionWon(winner);

        String wonPayload = "AUCTION_WON:" + gson.toJson(winner);
        verify(mockClient1, times(1)).send(wonPayload);
        verify(mockClient2, never()).send(wonPayload);

        String finishedPayload = "AUCTION_FINISHED:" + auctionId;
        verify(mockClient1, times(1)).send(finishedPayload);
        verify(mockClient2, times(1)).send(finishedPayload);
    }

    @Test
    @DisplayName("Client disconnection (onClose) removes them from future broadcasts")
    void testClientDisconnection() {
        broadcaster.onClose(mockClient1, 1000, "Normal closure", true);

        broadcaster.onAuctionFinished(1L);

        verify(mockClient1, never()).send(anyString());
        verify(mockClient2, times(1)).send("AUCTION_FINISHED:1");
    }

    @Test
    @DisplayName("onError() does not throw even with mocked conn")
    void testOnError() {
        // Đảm bảo onError không ném exception
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> broadcaster.onError(mockClient1, new Exception("test error"))
        );
    }

    @Test
    @DisplayName("onMessage() với JOIN_ROOM không hợp lệ không throw exception")
    void testOnMessageInvalidRoom() {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> broadcaster.onMessage(mockClient1, "JOIN_ROOM:not_a_number")
        );
    }

    @Test
    @DisplayName("onMessage() với loại message không biết không throw exception")
    void testOnMessageUnknownType() {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> broadcaster.onMessage(mockClient1, "UNKNOWN_TYPE:payload")
        );
    }
}