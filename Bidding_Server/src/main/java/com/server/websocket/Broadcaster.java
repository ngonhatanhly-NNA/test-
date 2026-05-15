package com.server.websocket;

import com.google.gson.Gson;
import com.shared.dto.*;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Broadcaster implements AuctionEventListener {
    // CopyOnWriterSet for safety in mlti-thread
    private static final Logger logger = LoggerFactory.getLogger(Broadcaster.class);

	// 1. Danh sách CHUNG cho tất cả người dùng (Để cập nhật Dashboard)
    private static final CopyOnWriteArrayList<WebSocket> globalClients = new CopyOnWriteArrayList<>();
    
    // 2. Danh sách PHÒNG (Để cập nhật giá liên tục cho những ai đang xem món đó)
    private static final ConcurrentHashMap<Long, CopyOnWriteArrayList<WebSocket>> rooms = new ConcurrentHashMap<>();
    
	private static final Gson gson = new Gson();

    public static void addClient (WebSocket conn) {
        globalClients.add(conn);
		logger.info("Client connected {}: ", conn.getRemoteSocketAddress());
    }
	
	public static void joinRoom(long auctionId, WebSocket conn) {
		rooms.putIfAbsent(auctionId, new CopyOnWriteArrayList<>());
		rooms.get(auctionId).add(conn);
		logger.info("Client joined room {} : {}", auctionId, conn.getRemoteSocketAddress());
	}

    // Xóa kết nối khi offline
    public static void removeClient (WebSocket conn) {
        globalClients.remove(conn); // Xóa khỏi sảnh chung
        
        for (CopyOnWriteArrayList<WebSocket> roomClients : rooms.values()) {
            roomClients.remove(conn); // Xóa khỏi các phòng đang xem
        }
        logger.info("Client disconnected: {}", conn.getRemoteSocketAddress());
    }

    // Auction Serveice gọi khi kết nối thành cong
    @Override
    public void onAuctionUpdate (AuctionUpdateDTO update) {
        broadcastToRoom(update.getAuctionId(), "AUCTION_UPDATE", gson.toJson(update));
    }

    @Override
    public void onAuctionCreated(AuctionDetailDTO detail) {
        broadcast("AUCTION_CREATED", gson.toJson(detail));
    }

    @Override
    public void onAuctionFinished(long auctionId) {
        broadcast("AUCTION_FINISHED", String.valueOf(auctionId));
    }

    @Override
    public void onAuctionWon(AuctionWinnerDTO winnerData) {
        broadcastToRoom(winnerData.getAuctionId(), "AUCTION_WON", gson.toJson(winnerData));
        broadcast("AUCTION_FINISHED", String.valueOf(winnerData.getAuctionId()));
    }

    private void broadcast(String type, String payload) {
        String msg = type + ":" + payload;

        for (WebSocket client : globalClients) {
            try {
                if (client.isOpen()) {
                    client.send(msg);
                }
            } catch (Exception e) {
                // Nếu 1 client lỗi, ghi log lại và vòng lặp vẫn chạy tiếp cho các client khác
                logger.error("Lỗi khi gửi thông tin đấu giá cho client {}: {}",
                             client.getRemoteSocketAddress(), e.getMessage());
            }
        }
    }

	private void broadcastToRoom(long auctionId, String type, String payload) {
        CopyOnWriteArrayList<WebSocket> roomClients = rooms.get(auctionId);
        if (roomClients != null) {
            String msg = type + ":" + payload;
            for (WebSocket client : roomClients) {
                if (client.isOpen()) {
                    client.send(msg);
                }
            }
        }
    }

}