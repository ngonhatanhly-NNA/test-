package com.client.network;

import com.google.gson.Gson;
import com.shared.dto.AuctionUpdateDTO;
import com.shared.network.Response;
import javafx.application.Platform;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class MyWebSocketClient extends WebSocketClient {

    private static MyWebSocketClient instance;

    public MyWebSocketClient(URI serverUri) {
        super(serverUri);
    }

    // Design Pattern Singleton: Đảm bảo Client chỉ có 1 kết nối duy nhất
    public static void connectToServer() {
        try {
            instance = new MyWebSocketClient(new URI("ws://localhost:8080"));
            instance.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static MyWebSocketClient getInstance() {
        return instance;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Đã kết nối thành công tới Server!");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Server trả lời: " + message);
        Gson gson = new Gson();

        // 1. NẾU LÀ TIN NHẮN CẬP NHẬT GIÁ ĐẤU
        if (message.startsWith("AUCTION_UPDATE:")) {
            // Cắt bỏ phần tiền tố để lấy đúng cục JSON
            String jsonStr = message.substring("AUCTION_UPDATE:".length());
            AuctionUpdateDTO updateData = gson.fromJson(jsonStr, AuctionUpdateDTO.class);

            // Bắt buộc dùng Platform.runLater để không làm crash JavaFX UI
            Platform.runLater(() -> {
                // TODO:Tùy theo Controller
                // ViewLiveAuctionsController.getInstance().updatePriceUI(updateData);
                System.out.println("GIÁ MỚI REALTIME: " + updateData.getCurrentPrice());
            });
            return; // Dừng hàm tại đây
        }

        // 2. NẾU LÀ CÁC THÔNG BÁO KHÁC (ví dụ: đăng nhập, đăng ký)
        try {
            Response res = gson.fromJson(message, Response.class);
            if ("SUCCESS".equals(res.getStatus())) {
                Platform.runLater(() -> {
                    System.out.println("Giao diện: Đăng ký thành công!");
                });
            }
        } catch (Exception e) {
            System.out.println("Không thể parse JSON Response thường: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) { }

    @Override
    public void onError(Exception ex) { }
}