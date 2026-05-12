package com.server.controller;

import com.server.service.AuctionService;
import com.shared.dto.AuctionDetailDTO;
import com.shared.network.Response;
import io.javalin.http.Context;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AuctionController: Xử lý HTTP requests liên quan đến đấu giá
 * (Các API core đã được migrate sang kiến trúc Command Pattern)
 */
public class AuctionController {
    private static final Logger logger = LoggerFactory.getLogger(AuctionController.class);

    public final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /**
     * GET /api/auctions/seller/{sellerId}/active
     * Lấy các phiên đấu giá đang hoạt động của MỘT seller cụ thể.
     * Dùng cho tab "Live Auction Monitoring" trong Seller Portal.
     */
    public void getActiveAuctionsBySeller(Context ctx) {
        try {
            long sellerId = Long.parseLong(ctx.pathParam("sellerId"));
            List<AuctionDetailDTO> auctions = auctionService.getActiveAuctionsBySeller(sellerId);
            ctx.json(new Response("SUCCESS", "Seller active auctions loaded", auctions));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new Response("ERROR", "Seller ID không hợp lệ", null));
        } catch (Exception e) {
            logger.error("Lỗi load phiên đấu giá của seller: {}", e.getMessage(), e);
            ctx.status(500).json(new Response("ERROR", "Failed to load seller auctions: " + e.getMessage(), null));
        }
    }

    /**
	 * GET /api/auctions/bidder/{bidderId}/won
     * Lấy danh sách phiên đấu giá đã kết thúc mà bidderId là người thắng.
     * Dùng cho phần "Won Auctions" ở My Inventory.
     */
    public void getWonAuctionsByBidder(Context ctx) {
        try {
            long bidderId = Long.parseLong(ctx.pathParam("bidderId"));
            List<AuctionDetailDTO> wonAuctions = auctionService.getWonAuctionsByBidder(bidderId);
            ctx.json(new Response("SUCCESS", "Won auctions loaded", wonAuctions));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new Response("ERROR", "Bidder ID không hợp lệ", null));
        } catch (Exception e) {
            logger.error("Lỗi load won auctions của bidder: {}", e.getMessage(), e);
            ctx.status(500).json(new Response("ERROR", "Failed to load won auctions: " + e.getMessage(), null));
        }
    }

    /**
     * GET /api/auctions/item/{itemId}/status
     * Trả về trạng thái đấu giá của item: ACTIVE | FINISHED | NONE
     * Client dùng để disable nút "Open Auction" khi item đang/đã đấu giá.
     */
    public void getAuctionStatusByItemId(Context ctx) {
        try {
            long itemId = Long.parseLong(ctx.pathParam("itemId"));
            String status = auctionService.getAuctionStatusByItemId(itemId);
            ctx.json(new Response("SUCCESS", "Auction status for item", status));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new Response("ERROR", "Item ID không hợp lệ", null));
        } catch (Exception e) {
            logger.error("Lỗi lấy auction status cho item: {}", e.getMessage(), e);
            ctx.status(500).json(new Response("ERROR", "Failed: " + e.getMessage(), null));
        }
    }
}