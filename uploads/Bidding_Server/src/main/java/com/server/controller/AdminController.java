package com.server.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.server.service.AdminService;
import com.shared.network.Response;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Map;

public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final AdminService adminService;
    private final Gson gson = new Gson();

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    public void getAllUsers(Context ctx) {
        logger.info("AdminController: Received request to get all users");
        Response response = adminService.getAllUsers();
        ctx.json(response);
    }

    public void getDashboard(Context ctx) {
        Response response = adminService.getDashboard();
        ctx.json(response);
    }

    public void searchUser(Context ctx) {
        String username = ctx.pathParam("username");
        Response response = adminService.timKiemNguoiDung(username);
        ctx.json(response);
    }

    public void banUser(Context ctx) {
        try {
            String jsonBody = ctx.body();
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> bodyMap = gson.fromJson(jsonBody, type);
            String username = bodyMap.get("username");

            if (username == null || username.trim().isEmpty()) {
                ctx.json(new Response("FAIL", "Username is required.", null));
                return;
            }
            logger.info("AdminController: Received request to BAN user '{}'", username);
            Response response = adminService.camTaiKhoan(username);
            ctx.json(response);
        } catch (Exception e) {
            logger.error("Error banning user: {}", e.getMessage());
            ctx.status(400).json(new Response("ERROR", "Invalid request body.", null));
        }
    }

    public void unbanUser(Context ctx) {
        try {
            String jsonBody = ctx.body();
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> bodyMap = gson.fromJson(jsonBody, type);
            String username = bodyMap.get("username");

            if (username == null || username.trim().isEmpty()) {
                ctx.json(new Response("FAIL", "Username is required.", null));
                return;
            }
            logger.info("AdminController: Received request to UNBAN user '{}'", username);
            Response response = adminService.boCamTaiKhoan(username);
            ctx.json(response);
        } catch (Exception e) {
            logger.error("Error unbanning user: {}", e.getMessage());
            ctx.status(400).json(new Response("ERROR", "Invalid request body.", null));
        }
    }

    public void approveSeller(Context ctx) {
        // Implementation for approveSeller
        ctx.json(new Response("SUCCESS", "Approve seller logic to be implemented", null));
    }

    public void getProductAnalytics(Context ctx) {
        Response response = adminService.phanTichSanPham();
        ctx.json(response);
    }

    public void deleteItem(Context ctx) {
        long itemId = Long.parseLong(ctx.pathParam("itemId"));
        Response response = adminService.deleteItem(itemId);
        ctx.json(response);
    }

    public void getRevenueEstimate(Context ctx) {
        Response response = adminService.uocTinhDoanhThu();
        ctx.json(response);
    }

    public void getUserActivityLog(Context ctx) {
        // Implementation for getUserActivityLog
        ctx.json(new Response("SUCCESS", "Get user activity log logic to be implemented", null));
    }

    public void cancelAuction(Context ctx) {
        // Implementation for cancelAuction
        ctx.json(new Response("SUCCESS", "Cancel auction logic to be implemented", null));
    }
}
