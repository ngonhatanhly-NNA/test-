package com.server.controller;

import com.server.service.AdminService;
import com.shared.network.Response;
import io.javalin.http.Context;
import com.google.gson.Gson;
import java.util.Map;

/**
 * AdminController - Xử lý các requests liên quan đến admin
 */
public class AdminController {
    private final AdminService adminService;
    private final Gson gson = new Gson();

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * GET /api/admin/dashboard - Lấy dữ liệu dashboard
     */
    public void getDashboard(Context ctx) {
        try {
            String result = adminService.getDashboard();
            ctx.result(result);
        } catch (Exception e) {
            ctx.status(500).json(new Response("ERROR", "Lỗi lấy dashboard: " + e.getMessage(), null));
        }
    }

    /**
     * GET /api/admin/users - Lấy danh sách tất cả user
     */
    public void getAllUsers(Context ctx) {
        try {
            // TODO: Implement getAllUsers in AdminService
            ctx.json(new Response("SUCCESS", "Danh sách người dùng", null));
        } catch (Exception e) {
            ctx.status(500).json(new Response("ERROR", "Lỗi lấy danh sách user", null));
        }
    }

    /**
     * GET /api/admin/users/search/:username - Tìm kiếm user theo username
     */
    public void searchUser(Context ctx) {
        try {
            String username = ctx.pathParam("username");
            String result = adminService.timKiemNguoiDung(username);
            ctx.result(result);
        } catch (Exception e) {
            ctx.status(400).json(new Response("ERROR", "Lỗi tìm kiếm user", null));
        }
    }

    /**
     * POST /api/admin/users/ban - Ban user
     */
    public void banUser(Context ctx) {
        try {
            Map<String, String> request = gson.fromJson(ctx.body(), Map.class);
            String username = (String) request.get("username");
            String reason = (String) request.getOrDefault("reason", "Vi phạm điều khoản");

            String result = adminService.camTaiKhoan(username);
            ctx.result(result);
        } catch (Exception e) {
            ctx.status(400).json(new Response("ERROR", "Lỗi ban user: " + e.getMessage(), null));
        }
    }

    /**
     * POST /api/admin/users/unban - Unban user
     */
    public void unbanUser(Context ctx) {
        try {
            Map<String, String> request = gson.fromJson(ctx.body(), Map.class);
            String username = (String) request.get("username");

            String result = adminService.boCamTaiKhoan(username);
            ctx.result(result);
        } catch (Exception e) {
            ctx.status(400).json(new Response("ERROR", "Lỗi unban user", null));
        }
    }

    /**
     * POST /api/admin/sellers/approve - Phê duyệt seller
     */
    public void approveSeller(Context ctx) {
        try {
            Map<String, String> request = gson.fromJson(ctx.body(), Map.class);
            String username = (String) request.get("username");
            String shopName = (String) request.getOrDefault("shopName", username + "_Shop");
            String bankAccount = (String) request.getOrDefault("bankAccountNumber", "BANK_" + username);

            String result = adminService.pheDuyetNguoiBan(username);
            ctx.result(result);
        } catch (Exception e) {
            ctx.status(400).json(new Response("ERROR", "Lỗi phê duyệt seller", null));
        }
    }

    /**
     * GET /api/admin/items/analytics - Lấy analytics sản phẩm
     */
    public void getProductAnalytics(Context ctx) {
        try {
            String result = adminService.phanTichSanPham();
            ctx.result(result);
        } catch (Exception e) {
            ctx.status(500).json(new Response("ERROR", "Lỗi lấy analytics", null));
        }
    }

    /**
     * GET /api/admin/finance/revenue-estimate - Lấy ước tính doanh thu
     */
    public void getRevenueEstimate(Context ctx) {
        try {
            String result = adminService.uocTinhDoanhThu();
            ctx.result(result);
        } catch (Exception e) {
            ctx.status(500).json(new Response("ERROR", "Lỗi lấy doanh thu", null));
        }
    }

    /**
     * GET /api/admin/users/:username/activity - Lấy lịch hoạt động user
     */
    public void getUserActivityLog(Context ctx) {
        try {
            String username = ctx.pathParam("username");
            // TODO: Implement activity log
            ctx.json(new Response("SUCCESS", "Lịch hoạt động của " + username, null));
        } catch (Exception e) {
            ctx.status(400).json(new Response("ERROR", "Lỗi lấy lịch hoạt động", null));
        }
    }

    /**
     * POST /api/admin/auctions/cancel - Hủy phiên đấu giá
     */
    public void cancelAuction(Context ctx) {
        try {
            Map<String, Object> request = gson.fromJson(ctx.body(), Map.class);
            long auctionId = ((Number) request.get("auctionId")).longValue();
            String reason = (String) request.getOrDefault("reason", "Admin cancelled");

            // TODO: Implement cancel auction in AuctionService
            ctx.json(new Response("SUCCESS", "Phiên đấu giá đã hủy", null));
        } catch (Exception e) {
            ctx.status(400).json(new Response("ERROR", "Lỗi hủy phiên đấu giá", null));
        }
    }
}

