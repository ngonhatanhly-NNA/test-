package com.server.controller;

import com.server.DAO.AuctionRepository;
import com.server.DAO.BidTransactionRepository;
import com.server.service.AuctionService;
import com.shared.network.*;
import com.shared.dto.*;

import java.util.List;

public class AuctionController {
    private final AuctionService auctionService;

    public AuctionController(AuctionRepository auctionRepo,
                             BidTransactionRepository bidRepo) {
        this.auctionService = new AuctionService(auctionRepo, bidRepo);
    }

    /**
     * REST API: GET /api/auctions/active
     * Trả về danh sách auction đang chạy
     */
    public Response getActiveAuctions(Request request) {
        try {
            List<AuctionDetailDTO> auctions = auctionService.getActiveAuctions();
            return Response.success("Active auctions loaded", auctions);
        } catch (Exception e) {
            return Response.error("Failed to load auctions: " + e.getMessage());
        }
    }

    /**
     * REST API: POST /api/auctions/{auctionId}/bid
     * Client gửi BidRequestDTO khi click "Đặt giá"
     */
    public Response placeBid(Request request) {
        try {
            // Parse BidRequestDTO từ request payload
            BidRequestDTO bidRequest = (BidRequestDTO) request.getPayload();
            AuctionUpdateDTO result = auctionService.placeBid(bidRequest);
            return Response.success("Bid placed successfully", result);
        } catch (Exception e) {
            return Response.error("Bid failed: " + e.getMessage());
        }
    }

    /**
     * REST API: GET /api/auctions/{auctionId}
     * Chi tiết 1 auction (cho trang chi tiết)
     */
    public Response getAuctionDetail(Request request) {
        try {
            // Lấy auctionId từ request params
            long auctionId = Long.parseLong((String) request.getParams().get("auctionId"));
            AuctionDetailDTO detail = auctionService.getAuctionDetail(auctionId);
            if (detail == null) {
                return Response.error("Auction not found");
            }
            return Response.success("Auction details loaded", detail);
        } catch (Exception e) {
            return Response.error("Invalid auction ID");
        }
    }

    /**
     * WebSocket Handler: Client subscribe room
     * Message: {"action": "SUBSCRIBE", "auctionId": 123}
     */
    public Response subscribeAuction(Request request) {
        try {
            long auctionId = Long.parseLong((String) request.getPayload());
            // WebSocket sẽ handle riêng (Giai đoạn 3)
            return Response.success("Subscribed to auction " + auctionId);
        } catch (Exception e) {
            return Response.error("Invalid auction ID");
        }
    }
}