package com.server.websocket;

import com.google.gson.Gson;
import com.shared.dto.*;
import com.server.service.AuctionService;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// [SỬA MỚI] Cho Broadcaster kế thừa trực tiếp WebSocketServer
public class Broadcaster extends WebSocketServer implements AuctionEventListener {
    private static final Logger logger = LoggerFactory.getLogger(Broadcaster.class);

    private static final CopyOnWriteArrayList<WebSocket> globalClients = new CopyOnWriteArrayList<>();
    private static final ConcurrentHashMap<Long, CopyOnWriteArrayList<WebSocket>> rooms = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();

    // [THÊM MỚI] Nhận AuctionService để xử lý lệnh PlaceBid từ client
    private AuctionService auctionService;

    public Broadcaster(InetSocketAddress address) {
        super(address);
    }

    public void setAuctionService(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    // ==========================================
    // PHẦN 1: VÒNG ĐỜI WEBSOCKET
    // ==========================================
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        logger.info("Client connected: {}", conn.getRemoteSocketAddress());
        globalClients.add(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        logger.info("Client disconnected: {}", conn.getRemoteSocketAddress());
        globalClients.remove(conn);
        for (CopyOnWriteArrayList<WebSocket> roomClients : rooms.values()) {
            roomClients.remove(conn);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        logger.info("Received auction message from {}: {}", conn.getRemoteSocketAddress(), message);
        handleAuctionMessage(conn, message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        logger.error("Lỗi xảy ra với client {}: {}", conn.getRemoteSocketAddress(), ex.getMessage());
    }

    @Override
    public void onStart() {
        logger.info("WebSocket Broadcaster started on port {}", getPort());
    }

    // ==========================================
    // PHẦN 2: XỬ LÝ LỆNH TỪ CLIENT GỬI LÊN (WS)
    // ==========================================
    private void handleAuctionMessage(WebSocket conn, String message) {
        if (message == null || message.isEmpty() || auctionService == null) return;

        String[] parts = message.split(":", 2);
        if (parts.length < 2) return;

        String type = parts[0];
        String payload = parts[1];

        try {
            switch (type) {
                case "PLACE_BID":
                    BidRequestDTO bidRequest = gson.fromJson(payload, BidRequestDTO.class);
                    if (bidRequest != null) auctionService.placeBid(bidRequest);
                    break;
                case "CANCEL_AUTO_BID":
                    AutoBidCancelDTO cancelRequest = gson.fromJson(payload, AutoBidCancelDTO.class);
                    if (cancelRequest != null) auctionService.cancelAutoBid(cancelRequest.getAuctionId(), cancelRequest.getBidderId());
                    break;
                case "UPDATE_AUTO_BID":
                    AutoBidUpdateDTO updateRequest = gson.fromJson(payload, AutoBidUpdateDTO.class);
                    if (updateRequest != null) auctionService.updateAutoBidAmount(updateRequest.getAuctionId(), updateRequest.getBidderId(), updateRequest.getMaxBidAmount(), updateRequest.getCustomStepPrice());
                    break;
                case "JOIN_ROOM":
                    try {
                        long roomId = Long.parseLong(payload.trim());
                        rooms.putIfAbsent(roomId, new CopyOnWriteArrayList<>());
                        rooms.get(roomId).add(conn);
                        logger.info("Client joined room {}: {}", roomId, conn.getRemoteSocketAddress());
                    } catch (NumberFormatException e) {
                        logger.error("Lỗi ID phòng không hợp lệ: {}", payload);
                    }
                    break;
                default: logger.warn("Loại message không hợp lệ: {}", type);
            }
        } catch (Exception e) {
            logger.error("Lỗi xử lý message {}: {}", type, e.getMessage());
        }
    }

    // ==========================================
    // PHẦN 3: LẮNG NGHE SỰ KIỆN TỪ SERVER ĐỂ BROADCAST
    // ==========================================
    @Override
    public void onAuctionUpdate (AuctionUpdateDTO update) {
        broadcastToRoom(update.getAuctionId(), "AUCTION_UPDATE", gson.toJson(update));
    }

    @Override
    public void onAuctionCreated(AuctionDetailDTO detail) {
        broadcastToAll("AUCTION_CREATED", gson.toJson(detail));
    }

    @Override
    public void onAuctionFinished(long auctionId) {
        broadcastToAll("AUCTION_FINISHED", String.valueOf(auctionId));
    }

    @Override
    public void onAuctionWon(AuctionWinnerDTO winnerData) {
        broadcastToRoom(winnerData.getAuctionId(), "AUCTION_WON", gson.toJson(winnerData));
        broadcastToAll("AUCTION_FINISHED", String.valueOf(winnerData.getAuctionId()));
    }

    private void broadcastToAll(String type, String payload) {
        String msg = type + ":" + payload;
        for (WebSocket client : globalClients) {
            try {
                if (client.isOpen()) client.send(msg);
            } catch (Exception e) {
                logger.error("Lỗi gửi tới client: {}", e.getMessage());
            }
        }
    }

    private void broadcastToRoom(long auctionId, String type, String payload) {
        CopyOnWriteArrayList<WebSocket> roomClients = rooms.get(auctionId);
        if (roomClients != null) {
            String msg = type + ":" + payload;
            for (WebSocket client : roomClients) {
                if (client.isOpen()) client.send(msg);
            }
        }
    }
}