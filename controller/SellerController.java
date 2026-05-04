package com.server.controller;

import com.server.service.SellerService;
import com.shared.network.Response;
import io.javalin.http.Context;
import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SellerController: Xử lý HTTP requests cho /api/sellers/*
 * Cung cấp API để Client lấy thông tin seller, items và thống kê.
 */
public class SellerController {
    private static final Logger logger = LoggerFactory.getLogger(SellerController.class);

    private final SellerService sellerService;
    private final Gson gson = new Gson();

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    /**
     * GET /api/sellers/{sellerId}
     * Lấy thông tin seller theo ID.
     */
    public void getSellerById(Context ctx) {
        try {
            long sellerId = Long.parseLong(ctx.pathParam("sellerId"));
            Response response = sellerService.getSellerDetails(sellerId);
            ctx.json(response);
        } catch (NumberFormatException e) {
            ctx.status(400).json(new Response("ERROR", "Seller ID không hợp lệ", null));
        } catch (Exception e) {
            logger.error("Lỗi lấy thông tin seller: {}", e.getMessage(), e);
            ctx.status(500).json(new Response("ERROR", "Lỗi server: " + e.getMessage(), null));
        }
    }

    /**
     * GET /api/sellers/{sellerId}/items
     * Lấy danh sách sản phẩm mà seller đã đưa vào đấu giá.
     */
    public void getSellerItems(Context ctx) {
        try {
            long sellerId = Long.parseLong(ctx.pathParam("sellerId"));
            Response response = sellerService.getSellerItems(sellerId);
            ctx.json(response);
        } catch (NumberFormatException e) {
            ctx.status(400).json(new Response("ERROR", "Seller ID không hợp lệ", null));
        } catch (Exception e) {
            logger.error("Lỗi lấy items của seller: {}", e.getMessage(), e);
            ctx.status(500).json(new Response("ERROR", "Lỗi server: " + e.getMessage(), null));
        }
    }

    /**
     * GET /api/sellers/{sellerId}/statistics
     * Lấy thống kê bán hàng của seller.
     */
    public void getSellerStatistics(Context ctx) {
        try {
            long sellerId = Long.parseLong(ctx.pathParam("sellerId"));
            Response response = sellerService.getSellerStatistics(sellerId);
            ctx.json(response);
        } catch (NumberFormatException e) {
            ctx.status(400).json(new Response("ERROR", "Seller ID không hợp lệ", null));
        } catch (Exception e) {
            logger.error("Lỗi lấy statistics của seller: {}", e.getMessage(), e);
            ctx.status(500).json(new Response("ERROR", "Lỗi server: " + e.getMessage(), null));
        }
    }
}