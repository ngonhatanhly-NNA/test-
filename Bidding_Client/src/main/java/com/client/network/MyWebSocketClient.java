package com.client.network;

import com.client.controller.dashboard.ViewLiveAuctions;
import com.google.gson.Gson;
import com.shared.dto.AuctionUpdateDTO;
import com.shared.network.Response;
import javafx.application.Platform;
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

    // Design Pattern Singleton: Đảm bảo Client chỉ có 1 kết nối duy nhất
    public static void connectToServer() {
        try {
            instance = new MyWebSocketClient(new URI("ws://localhost:8080"));
            instance.connect();
        } catch (Exception e) {
            logger.error("Lỗi khi kết nối tới Server", e);
        }
    }

    public static MyWebSocketClient getInstance() {
        return instance;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        logger.info("Đã kết nối thành công tới Server!");
        if (ViewLiveAuctions.getInstance() != null) {
            ViewLiveAuctions.getInstance().updateConnectionStatus(true, "🟢 Đã kết nối Server Realtime");
        }
    }

    @Override
    public void onMessage(String message) {
        logger.info("Server trả lời: {}", message);
        Gson gson = new Gson();

        // NẾU LÀ TIN NHẮN CẬP NHẬT GIÁ ĐẤU
        if (message.startsWith("AUCTION_UPDATE:")) {
            String jsonStr = message.substring("AUCTION_UPDATE:".length());
            AuctionUpdateDTO updateData = gson.fromJson(jsonStr, AuctionUpdateDTO.class);

            Platform.runLater(() -> {
                if (ViewLiveAuctions.getInstance() != null) {
                    ViewLiveAuctions.getInstance().updatePriceRealtime(updateData);
                }
                logger.info("GIÁ MỚI REALTIME: {}", updateData.getCurrentPrice());
            });
            return; 
        }

        //NẾU LÀ CÁC THÔNG BÁO KHÁC (ví dụ: đăng nhập, đăng ký)
        try {
            Response res = gson.fromJson(message, Response.class);
            if ("SUCCESS".equals(res.getStatus())) {
                Platform.runLater(() -> {
                    logger.info("Giao diện: Đăng ký thành công!");
                });
            }
        } catch (Exception e) {
            logger.error("Không thể parse JSON Response thường: {}", e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.info("Kết nối với Server đã đóng. Code: {}, Lý do: {}", code, reason);
        if (ViewLiveAuctions.getInstance() != null) {
            ViewLiveAuctions.getInstance().updateConnectionStatus(false, "🔴 Mất kết nối. Đang thử lại...");
        }
        new Thread( () -> {
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
        if (ViewLiveAuctions.getInstance() != null) {
            ViewLiveAuctions.getInstance().updateConnectionStatus(false, "🔴 Lỗi đường truyền!");
        }
     }
}