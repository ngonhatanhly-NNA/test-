package com.client.network;

import com.google.gson.Gson;
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
        Response res = gson.fromJson(message, Response.class);

        // NẾU NHẬN ĐƯỢC CHỮ SUCCESS, BÁO LÊN MÀN HÌNH
        if ("SUCCESS".equals(res.getStatus())) {
            // LƯU Ý KHI DÙNG JAVAFX: Mọi thay đổi UI phải bọc trong Platform.runLater
            Platform.runLater(() -> {
                System.out.println("Giao diện: Đăng ký thành công!");
                // Chút nữa sẽ thêm lệnh chuyển màn hình ở đây
            });
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) { }

    @Override
    public void onError(Exception ex) { }
}