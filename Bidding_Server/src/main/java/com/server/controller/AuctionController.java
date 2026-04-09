package com.server.controller;

import com.server.DAO.AuctionRepository;
import com.server.DAO.BidTransactionRepository;
import com.server.service.AuctionService;
import com.server.exception.AuctionException;
import com.shared.dto.*;
import com.shared.network.Response;
import io.javalin.http.Context;
import com.google.gson.Gson;

import java.math.BigDecimal;
import java.util.List;

/**
 * AuctionController: Xử lý HTTP requests liên quan đến đấu giá
 *
 * Responsibilities:
 * - Convert JSON requests to DTOs
 * - Delegate business logic to AuctionService
 * - Handle exceptions và trả về Response hợp lệ
 */
public class AuctionController {
    public final AuctionService auctionService;
    private final Gson gson = new Gson();

    public AuctionController(AuctionRepository auctionRepo, BidTransactionRepository bidRepo) {
        this.auctionService = new AuctionService(auctionRepo, bidRepo);
    }

    /**
     * GET /api/auctions/active
     * Lấy danh sách các phiên đấu giá đang hoạt động
     */
    public void getActiveAuctions(Context ctx) {
        try {
            List<AuctionDetailDTO> auctions = auctionService.getActiveAuctions();
            ctx.json(new Response("SUCCESS", "Active auctions loaded", auctions));
        } catch (Exception e) {
            ctx.status(500).json(new Response("ERROR", "Failed to load auctions: " + e.getMessage(), null));
        }
    }

    /**
     * POST /api/auctions/bid
     * Đặt giá cho phiên đấu giá
     */
    public void placeBid(Context ctx) {
        try {
            BidRequestDTO bidRequest = gson.fromJson(ctx.body(), BidRequestDTO.class);
            if (bidRequest == null) {
                ctx.status(400).json(new Response("ERROR", "Bid request không hợp lệ", null));
                return;
            }

            AuctionUpdateDTO result = auctionService.placeBid(bidRequest);
            ctx.json(new Response("SUCCESS", "Bid placed successfully", result));

        } catch (AuctionException e) {
            handleAuctionException(ctx, e);
        } catch (Exception e) {
            ctx.status(400).json(new Response("ERROR", "Lỗi khi đặt giá: " + e.getMessage(), null));
        }
    }

    /**
     * GET /api/auctions/{auctionId}
     * Lấy chi tiết một phiên đấu giá
     */
    public void getAuctionDetail(Context ctx) {
        try {
            long auctionId = Long.parseLong(ctx.pathParam("auctionId"));
            // TODO: Implement getAuctionById in AuctionService
            ctx.json(new Response("INFO", "Lấy chi tiết auction #" + auctionId + " (Chưa implement)", null));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new Response("ERROR", "ID phiên đấu giá không hợp lệ", null));
        } catch (Exception e) {
            ctx.status(500).json(new Response("ERROR", "Lỗi server: " + e.getMessage(), null));
        }
    }

    /**
     * POST /api/auctions/{auctionId}/auto-bid/cancel
     * Hủy auto-bid
     */
    public void cancelAutoBid(Context ctx) {
        try {
            long auctionId = Long.parseLong(ctx.pathParam("auctionId"));
            AutoBidCancelDTO cancelRequest = gson.fromJson(ctx.body(), AutoBidCancelDTO.class);

            if (cancelRequest == null || cancelRequest.getBidderId() <= 0) {
                ctx.status(400).json(new Response("ERROR", "Bidder ID không hợp lệ", null));
                return;
            }

            auctionService.cancelAutoBid(auctionId, cancelRequest.getBidderId());
            ctx.json(new Response("SUCCESS", "Auto-bid cancelled successfully", null));

        } catch (AuctionException e) {
            handleAuctionException(ctx, e);
        } catch (NumberFormatException e) {
            ctx.status(400).json(new Response("ERROR", "ID không hợp lệ", null));
        } catch (Exception e) {
            ctx.status(400).json(new Response("ERROR", "Failed to cancel auto-bid: " + e.getMessage(), null));
        }
    }

    /**
     * PUT /api/auctions/{auctionId}/auto-bid/update
     * Cập nhật giá tối đa auto-bid
     */
    public void updateAutoBidAmount(Context ctx) {
        try {
            long auctionId = Long.parseLong(ctx.pathParam("auctionId"));
            AutoBidUpdateDTO updateRequest = gson.fromJson(ctx.body(), AutoBidUpdateDTO.class);

            if (updateRequest == null || updateRequest.getBidderId() <= 0 || updateRequest.getMaxBidAmount() == null) {
                ctx.status(400).json(new Response("ERROR", "Request không hợp lệ", null));
                return;
            }

            auctionService.updateAutoBidAmount(auctionId, updateRequest.getBidderId(), updateRequest.getMaxBidAmount());
            ctx.json(new Response("SUCCESS", "Auto-bid updated successfully", null));

        } catch (AuctionException e) {
            handleAuctionException(ctx, e);
        } catch (NumberFormatException e) {
            ctx.status(400).json(new Response("ERROR", "ID không hợp lệ", null));
        } catch (Exception e) {
            ctx.status(400).json(new Response("ERROR", "Failed to update auto-bid: " + e.getMessage(), null));
        }
    }

    /**
     * Helper: Xử lý AuctionException
     */
    private void handleAuctionException(Context ctx, AuctionException e) {
        AuctionException.ErrorCode code = e.getErrorCode();
        int statusCode = switch (code) {
            case AUCTION_NOT_FOUND, AUCTION_NOT_ACTIVE -> 404;
            case INVALID_BID_AMOUNT, BID_AMOUNT_TOO_LOW, INVALID_AUTO_BID_CONFIG -> 400;
            case AUCTION_ALREADY_FINISHED -> 410;
            default -> 500;
        };

        ctx.status(statusCode).json(new Response("ERROR", e.getMessage(), null));
    }
}