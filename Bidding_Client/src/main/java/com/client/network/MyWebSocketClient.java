package com.client.network;

import com.client.controller.dashboard.ViewLiveAuctions;
import com.google.gson.Gson;
import com.shared.dto.AuctionDetailDTO;
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

    public static MyWebSocketClient getInstance() {
        return instance;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        logger.info("Đã kết nối thành công tới Server!");
        ViewLiveAuctions view = ViewLiveAuctions.getExistingInstance();
        if (view != null) {
            view.updateConnectionStatus(true, "🟢 Đã kết nối Server Realtime");
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

            if ("AUCTION_UPDATE".equals(type)) {
                AuctionUpdateDTO updateData = gson.fromJson(payload, AuctionUpdateDTO.class);
                Platform.runLater(() -> {
                    ViewLiveAuctions view = ViewLiveAuctions.getExistingInstance();
                    if (view != null) {
                        view.updatePriceRealtime(updateData);
                    }
                });
                return;
            }

            if ("AUCTION_CREATED".equals(type)) {
                AuctionDetailDTO detail = gson.fromJson(payload, AuctionDetailDTO.class);
                Platform.runLater(() -> {
                    ViewLiveAuctions view = ViewLiveAuctions.getExistingInstance();
                    if (view != null) {
                        view.addOrUpdateAuctionRealtime(detail);
                    }
                    com.client.controller.dashboard.SellerDashboardController sellerView =
                            com.client.controller.dashboard.SellerDashboardController.getExistingInstance();
                    if (sellerView != null) {
                        sellerView.addOrUpdateAuctionRealtime(detail);
                    }
                });
                return;
            }

            if ("AUCTION_FINISHED".equals(type)) {
                try {
                    long auctionId = Long.parseLong(payload);
                    Platform.runLater(() -> {
                        ViewLiveAuctions view = ViewLiveAuctions.getExistingInstance();
                        if (view != null) {
                            view.removeAuctionRealtime(auctionId);
                        }
                        com.client.controller.dashboard.SellerDashboardController sellerView =
                                com.client.controller.dashboard.SellerDashboardController.getExistingInstance();
                        if (sellerView != null) {
                            sellerView.removeAuctionRealtime(auctionId);
                        }
                    });
                } catch (NumberFormatException e) {
                    logger.warn("Không parse được AUCTION_FINISHED payload: {}", payload);
                }
                return;
            }
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
        ViewLiveAuctions view = ViewLiveAuctions.getExistingInstance();
        if (view != null) {
            view.updateConnectionStatus(false, "🔴 Mất kết nối. Đang thử lại...");
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
        ViewLiveAuctions view = ViewLiveAuctions.getExistingInstance();
        if (view != null) {
            view.updateConnectionStatus(false, "🔴 Lỗi đường truyền!");
        }
     }
}