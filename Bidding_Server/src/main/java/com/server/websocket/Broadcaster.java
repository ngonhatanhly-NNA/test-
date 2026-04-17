package com.server.websocket;

import com.google.gson.Gson;
import com.shared.dto.*;
import org.java_websocket.WebSocket;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class Broadcaster implements AuctionEventListener {
    // CopyOnWriterSet for safety in mlti-thread
    private static final CopyOnWriteArrayList<WebSocket> clients = new CopyOnWriteArrayList<>();
    private static final Gson gson = new Gson();

    public static void addClient (WebSocket conn) {
        clients.add(conn); // Add các liên kếts, tạo dần Thread hỗ trợ
    }

    // Xóa kết nối khi offline
    public static void removeClient (WebSocket conn) {
        clients.remove(conn);
    }

    // Auction Serveice gọi khi kết ối thành cong
    @Override
    public void onAuctionUpdate (AuctionUpdateDTO update) {
        String jsonUpdate = gson.toJson(update);

        // Báo cho CLient biết gói tin cập nhật phiên đấu giá 1 sp
        String msg = "AUCTION_UPDATE: " + jsonUpdate;

        for (WebSocket client : clients) {
            if (client.isOpen()) {
                client.send(msg);
            }
        }
    }


}