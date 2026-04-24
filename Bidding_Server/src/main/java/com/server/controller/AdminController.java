package com.server.controller;

import com.server.service.AdminService;
import com.shared.network.Response;
import io.javalin.http.Context;
import com.google.gson.Gson;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AdminController - Xử lý các requests liên quan đến admin
 */
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;
    private final Gson gson = new Gson();

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    public void getDashboard(Context ctx) {
        try {
            String result = adminService.getDashboard();
            ctx.result(result);
        } catch (Exception e) {
            logger.error("Lỗi lấy dashboard: {}", e.getMessage(), e);
            ctx.status(500).json(new Response("ERROR", "Lỗi lấy dashboard: " + e.getMessage(), null));
        }
    }

    public void getAllUsers(Context ctx) {
        try {
            ctx.json(new Response("SUCCESS", "Danh sách ngườii dùng", null));
        } catch (Exception e) {
            logger.error("Lỗi lấy danh sách user: {}", e.getMessage(), e);
            ctx.status(500).json(new Response("ERROR", "Lỗi lấy danh sách user", null));
        }
    }

    public void searchUser(Context ctx) {
        try {
            String username = ctx.pathParam("username");
            String result = adminService.timKiemNguoiDung(username);
            ctx.result(result);
        } catch (Exception e) {
            logger.error("Lỗi tìm kiếm user '{}': {}", ctx.pathParam("username"), e.getMessage(), e);
            ctx.status(400).json(new Response("ERROR", "Lỗi tìm kiếm user", null));
        }
    }

    public void banUser(Context ctx) {
        try {
            Map<String, String> request = gson.fromJson(ctx.body(), Map.class);
            String username = (String) request.get("username");
            String reason = (String) request.getOrDefault("reason", "Vi phạm điều khoản");

            String result = adminService.camTaiKhoan(username);
            logger.info("[ADMIN] Ban user '{}' với lý do: {}", username, reason);
            ctx.result(result);
        } catch (Exception e) {
            logger.error("Lỗi ban user: {}", e.getMessage(), e);
            ctx.status(400).json(new Response("ERROR", "Lỗi ban user: " + e.getMessage(), null));
        }
    }

    public void unbanUser(Context ctx) {
        try {
            Map<String, String> request = gson.fromJson(ctx.body(), Map.class);
            String username = (String) request.get("username");

            String result = adminService.boCamTaiKhoan(username);
            logger.info("[ADMIN] Unban user '{}'", username);
            ctx.result(result);
        } catch (Exception e) {
            logger.error("Lỗi unban user: {}", e.getMessage(), e);
            ctx.status(400).json(new Response("ERROR", "Lỗi unban user", null));
        }
    }

    public void approveSeller(Context ctx) {
        try {
            Map<String, String> request = gson.fromJson(ctx.body(), Map.class);
            String username = (String) request.get("username");

            String result = adminService.pheDuyetNguoiBan(username);
            logger.info("[ADMIN] Phê duyệt seller '{}'", username);
            ctx.result(result);
        } catch (Exception e) {
            logger.error("Lỗi phê duyệt seller: {}", e.getMessage(), e);
            ctx.status(400).json(new Response("ERROR", "Lỗi phê duyệt seller", null));
        }
    }

    public void getProductAnalytics(Context ctx) {
        try {
            String result = adminService.phanTichSanPham();
            ctx.result(result);
        } catch (Exception e) {
            logger.error("Lỗi lấy analytics: {}", e.getMessage(), e);
            ctx.status(500).json(new Response("ERROR", "Lỗi lấy analytics", null));
        }
    }

    public void getRevenueEstimate(Context ctx) {
        try {
            String result = adminService.uocTinhDoanhThu();
            ctx.result(result);
        } catch (Exception e) {
            logger.error("Lỗi lấy doanh thu: {}", e.getMessage(), e);
            ctx.status(500).json(new Response("ERROR", "Lỗi lấy doanh thu", null));
        }
    }

    public void getUserActivityLog(Context ctx) {
        try {
            String username = ctx.pathParam("username");
            ctx.json(new Response("SUCCESS", "Lịch hoạt động của " + username, null));
        } catch (Exception e) {
            logger.error("Lỗi lấy lịch hoạt động user '{}': {}", ctx.pathParam("username"), e.getMessage(), e);
            ctx.status(400).json(new Response("ERROR", "Lỗi lấy lịch hoạt động", null));
        }
    }

    public void cancelAuction(Context ctx) {
        try {
            Map<String, Object> request = gson.fromJson(ctx.body(), Map.class);
            long auctionId = ((Number) request.get("auctionId")).longValue();
            String reason = (String) request.getOrDefault("reason", "Admin cancelled");

            logger.info("[ADMIN] Hủy phiên đấu giá {} với lý do: {}", auctionId, reason);
            ctx.json(new Response("SUCCESS", "Phiên đấu giá đã hủy", null));
        } catch (Exception e) {
            logger.error("Lỗi hủy phiên đấu giá: {}", e.getMessage(), e);
            ctx.status(400).json(new Response("ERROR", "Lỗi hủy phiên đấu giá", null));
        }
    }
}

