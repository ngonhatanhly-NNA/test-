package com.client.network;

import com.client.util.event.AuctionFinishedEvent;
import com.client.util.event.ConnectionEvent;
import com.client.util.event.EventBus;
import com.google.gson.Gson;
import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.AuctionUpdateDTO;
import com.shared.dto.AuctionWinnerDTO;
import com.shared.network.Response;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;

public class MyWebSocketClient extends WebSocketClient {

    private static MyWebSocketClient instance;
    private static final Logger logger = LoggerFactory.getLogger(MyWebSocketClient.class);

    public MyWebSocketClient(URI serverUri) {
        super(serverUri);
    }

    public static void connectToServer() {
        try {
            if (instance != null) {
                org.java_websocket.enums.ReadyState state = instance.getReadyState();
                if (state == org.java_websocket.enums.ReadyState.OPEN
                        || state == org.java_websocket.enums.ReadyState.NOT_YET_CONNECTED) {
                    return;
                }
            }
            instance = new MyWebSocketClient(new URI("ws://localhost:8080"));
            instance.connect();
        } catch (Exception e) {
            logger.error("Lỗi khi kết nối tới Server", e);
        }
    }

    public static MyWebSocketClient getInstance() { return instance; }

    public void joinRoom(long auctionId) {
        if (this.isOpen()) {
            this.send("JOIN_ROOM:" + auctionId);
            logger.info("Đã gửi yêu cầu tham gia phòng đấu giá: {}", auctionId);
        } else {
            logger.error("WebSocket chưa kết nối, không thể tham gia phòng!");
        }
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        logger.info("Đã kết nối thành công tới Server!");
        // PHÁT SÓNG SỰ KIỆN KẾT NỐI MẠNG (Observer Pattern)
        EventBus.getInstance().publish(new ConnectionEvent(true, "🟢 Đã kết nối Server Realtime"));
		long currentId = com.client.controller.dashboard.ViewLiveAuctions.getInstance() != null ? 
                 com.client.controller.dashboard.ViewLiveAuctions.getInstance().getCurrentAuctionId() : 0;
		if (currentId > 0) {
			this.send("JOIN_ROOM:" + currentId);
		}
    }

    @Override
    public void onMessage(String message) {
        logger.info("Server trả lời: {}", message);
        Gson gson = new Gson();
        String[] parts = message != null ? message.split(":", 2) : new String[0];
        
        if (parts.length == 2) {
            String type = parts[0].trim();
            String payload = parts[1].trim();

            switch (type) {
                case "AUCTION_UPDATE":
                    AuctionUpdateDTO updateData = gson.fromJson(payload, AuctionUpdateDTO.class);
                    EventBus.getInstance().publish(updateData); // Phát sóng DTO cập nhật giá
                    break;

                case "AUCTION_CREATED":
                    AuctionDetailDTO detail = gson.fromJson(payload, AuctionDetailDTO.class);
                    EventBus.getInstance().publish(detail); // Phát sóng DTO phiên đấu giá mới
                    break;

                case "AUCTION_FINISHED":
                    try {
                        long auctionId = Long.parseLong(payload);
                        EventBus.getInstance().publish(new AuctionFinishedEvent(auctionId)); // Phát sóng sự kiện ế hàng
                    } catch (NumberFormatException e) {
                        logger.warn("Không parse được AUCTION_FINISHED payload: {}", payload);
                    }
                    break;

                case "AUCTION_WON":
                    try {
                        AuctionWinnerDTO winnerData = gson.fromJson(payload, AuctionWinnerDTO.class);
                        EventBus.getInstance().publish(winnerData); // Phát sóng người thắng
                    } catch (Exception e) {
                        logger.warn("Không parse được AUCTION_WON payload: {}", payload);
                    }
                    break;

                default:
                    logger.debug("Loại tin nhắn WebSocket không được hỗ trợ: {}", type);
                    break;
            }
            return;
        }

        try {
            Response res = gson.fromJson(message, Response.class);
            if ("SUCCESS".equals(res.getStatus())) {
                logger.info("Xử lý Response thường thành công!");
            }
        } catch (Exception e) {
            logger.error("Không thể parse JSON Response thường: {}", e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.info("Kết nối với Server đã đóng. Code: {}, Lý do: {}", code, reason);
        EventBus.getInstance().publish(new ConnectionEvent(false, "🔴 Mất kết nối. Đang thử lại..."));
        
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                logger.info("Đang kết nối lại....");
                this.reconnect();
            } catch (InterruptedException e) {
                logger.error("Lỗi khi chờ kết nối lại: {}", e.getMessage());
            }
        }).start();
    }

    @Override
    public void onError(Exception ex) {
        logger.error("Lỗi WebSocket: {}", ex.getMessage());
        EventBus.getInstance().publish(new ConnectionEvent(false, "🔴 Lỗi đường truyền!"));
    }
}