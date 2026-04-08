package com.server;

import com.server.config.DBConnection;
import com.server.route.ApiRouter;
import com.server.websocket.Broadcaster;
import io.javalin.Javalin;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;

public class ServerApp extends WebSocketServer {

    public ServerApp(InetSocketAddress address) { super(address); }

    // PHẦN WEBSOCKET (CHỈ DÙNG CHO ĐẤU GIÁ SAU NÀY)
    @Override public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Client kết nối mới: " + conn.getRemoteSocketAddress());
        Broadcaster.addClient(conn);
    }
    @Override public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Cleint ngắt kết nối: " + conn.getRemoteSocketAddress());
        Broadcaster.removeClient(conn);
    }
    @Override public void onMessage(WebSocket conn, String message) {
        System.out.println("nhận lệnh đấu giá: " + message);
        /// TODO: Code đấu giá, xử lí các lệnh Websocker khác
    }
    @Override public void onError(WebSocket conn, Exception ex) { ex.printStackTrace(); }
    @Override public void onStart() { System.out.println("=== TRẠM ĐẤU GIÁ CHẠY CỔNG " + getPort() + " ==="); }


    // ====== HÀM MAIN CHẠY CẢ 2 HỆ THỐNG ======
    public static void main(String[] args) {
        // 1. Kiểm tra Kết nối Database qua HikariCP
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            if (conn != null) {
                System.out.println("Kết nối DB qua HikariCP thành công!");
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kết nối DB: " + e.getMessage());
            return; // Dừng server luôn nếu không kết nối được Database
        }

        // 2. Khởi động WebSocket (Cổng 8080)
        ServerApp wsServer = new ServerApp(new InetSocketAddress("localhost", 8080));
        wsServer.start();

        // 3. Khởi động REST API (CỔNG 7070)
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost())); // Chống lỗi CORS bảo mật
        }).start(7070);
        System.out.println("=== TRẠM REST API CHẠY CỔNG 7070 ===");

        // 4. GỌI BỘ ĐỊNH TUYẾN (Chỉ tốn đúng 1 dòng code!)
        ApiRouter.setupRoutes(app);
    }
}