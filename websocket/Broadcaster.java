package com.server.websocket;

import com.google.gson.Gson;
import com.shared.dto.*;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;

public class Broadcaster implements AuctionEventListener {
    // CopyOnWriterSet for safety in mlti-thread
    private static final Logger logger = LoggerFactory.getLogger(Broadcaster.class);
    private static final CopyOnWriteArrayList<WebSocket> clients = new CopyOnWriteArrayList<>();
    private static final Gson gson = new Gson();

    public static void addClient (WebSocket conn) {
        clients.add(conn); // Add các liên kếts, tạo dần Thread hỗ trợ
        logger.info("Client connected: {}", conn.getRemoteSocketAddress());
    }

    // Xóa kết nối khi offline
    public static void removeClient (WebSocket conn) {
        clients.remove(conn);
        logger.info("Client disconnected: {}", conn.getRemoteSocketAddress());
    }

    // Auction Serveice gọi khi kết ối thành cong
    @Override
    public void onAuctionUpdate (AuctionUpdateDTO update) {
        broadcast("AUCTION_UPDATE", gson.toJson(update));
    }

    @Override
    public void onAuctionCreated(AuctionDetailDTO detail) {
        broadcast("AUCTION_CREATED", gson.toJson(detail));
    }

    @Override
    public void onAuctionFinished(long auctionId) {
        broadcast("AUCTION_FINISHED", String.valueOf(auctionId));
    }

    private void broadcast(String type, String payload) {
        String msg = type + ":" + payload;

        for (WebSocket client : clients) {
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


}