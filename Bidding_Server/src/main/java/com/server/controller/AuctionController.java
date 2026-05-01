package com.server.controller;

import com.server.service.AuctionService;
import com.server.exception.AuctionException;
import com.shared.dto.*;
import com.shared.network.Response;
import io.javalin.http.Context;
import com.google.gson.Gson;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AuctionController: Xử lý HTTP requests liên quan đến đấu giá
 */
public class AuctionController {
    private static final Logger logger = LoggerFactory.getLogger(AuctionController.class);

    public final AuctionService auctionService;
    private final Gson gson = new Gson();

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public void createAuction(Context ctx) {
        try {
            CreateAuctionDTO request = gson.fromJson(ctx.body(), CreateAuctionDTO.class);
            if (request == null) {
                ctx.status(400).json(new Response("ERROR", "Request không hợp lệ", null));
                return;
            }

            long auctionId = auctionService.createAuction(request);
            AuctionDetailDTO detail = auctionService.getAuctionDetail(auctionId);
            logger.info("Tạo phiên đấu giá thành công: id={}, itemId={}, sellerId={}", auctionId, request.getItemId(), request.getSellerId());
            ctx.json(new Response("SUCCESS", "Phiên đấu giá đã tạo thành công", detail));

        } catch (AuctionException e) {
            logger.warn("Lỗi tạo phiên đấu giá: {}", e.getMessage());
            handleAuctionException(ctx, e);
        } catch (Exception e) {
            logger.error("Lỗi không xác định khi tạo phiên đấu giá: {}", e.getMessage(), e);
            ctx.status(400).json(new Response("ERROR", "Lỗi tạo phiên đấu giá: " + e.getMessage(), null));
        }
    }

    /**
     * GET /api/auctions/active
     * Lấy tất cả phiên đấu giá đang hoạt động (dùng cho trang Live Auctions chung).
     */
    public void getActiveAuctions(Context ctx) {
        try {
            List<AuctionDetailDTO> auctions = auctionService.getActiveAuctions();
            ctx.json(new Response("SUCCESS", "Active auctions loaded", auctions));
        } catch (Exception e) {
            logger.error("Lỗi load danh sách phiên đấu giá: {}", e.getMessage(), e);
            ctx.status(500).json(new Response("ERROR", "Failed to load auctions: " + e.getMessage(), null));
        }
    }

    /**
     * [MỚI] GET /api/auctions/seller/{sellerId}/active
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
     * [MỚI] GET /api/auctions/bidder/{bidderId}/won
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

    public void placeBid(Context ctx) {
        try {
            BidRequestDTO bidRequest = gson.fromJson(ctx.body(), BidRequestDTO.class);
            if (bidRequest == null) {
                ctx.status(400).json(new Response("ERROR", "Bid request không hợp lệ", null));
                return;
            }

            AuctionUpdateDTO result = auctionService.placeBid(bidRequest);
            logger.info("Đặt giá thành công: auction={}, bidder={}, amount={}",
                    bidRequest.getAuctionId(), bidRequest.getBidderId(), bidRequest.getBidAmount());
            ctx.json(new Response("SUCCESS", "Bid placed successfully", result));

        } catch (AuctionException e) {
            logger.warn("Lỗi đặt giá auction {}: {}",
                    ctx.body().contains("auctionId") ? "?" : "unknown", e.getMessage());
            handleAuctionException(ctx, e);
        } catch (Exception e) {
            logger.error("Lỗi không xác định khi đặt giá: {}", e.getMessage(), e);
            ctx.status(400).json(new Response("ERROR", "Lỗi khi đặt giá: " + e.getMessage(), null));
        }
    }

    public void getAuctionDetail(Context ctx) {
        try {
            long auctionId = Long.parseLong(ctx.pathParam("auctionId"));
            AuctionDetailDTO detail = auctionService.getAuctionDetail(auctionId);
            ctx.json(new Response("SUCCESS", "Đã tải chi tiết phiên đấu giá", detail));
        } catch (AuctionException e) {
            handleAuctionException(ctx, e);
        } catch (NumberFormatException e) {
            ctx.status(400).json(new Response("ERROR", "ID phiên đấu giá không hợp lệ", null));
        } catch (Exception e) {
            logger.error("Lỗi lấy chi tiết phiên đấu giá: {}", e.getMessage(), e);
            ctx.status(500).json(new Response("ERROR", "Lỗi server: " + e.getMessage(), null));
        }
    }

    public void getBidHistory(Context ctx) {
        try {
            long auctionId = Long.parseLong(ctx.pathParam("auctionId"));
            List<BidHistoryDTO> bidHistory = auctionService.getBidHistory(auctionId);
            ctx.json(new Response("SUCCESS", "Loaded bid history", bidHistory));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new Response("ERROR", "ID phiên đấu giá không hợp lệ", null));
        } catch (Exception e) {
            logger.error("Lỗi lấy lịch sử bid cho auction {}: {}", ctx.pathParam("auctionId"), e.getMessage(), e);
            ctx.status(500).json(new Response("ERROR", "Lỗi lấy lịch sử: " + e.getMessage(), null));
        }
    }

    public void cancelAutoBid(Context ctx) {
        try {
            long auctionId = Long.parseLong(ctx.pathParam("auctionId"));
            AutoBidCancelDTO cancelRequest = gson.fromJson(ctx.body(), AutoBidCancelDTO.class);

            if (cancelRequest == null || cancelRequest.getBidderId() <= 0) {
                ctx.status(400).json(new Response("ERROR", "Bidder ID không hợp lệ", null));
                return;
            }

            auctionService.cancelAutoBid(auctionId, cancelRequest.getBidderId());
            logger.info("Hủy auto-bid thành công: auction={}, bidder={}", auctionId, cancelRequest.getBidderId());
            ctx.json(new Response("SUCCESS", "Auto-bid cancelled successfully", null));

        } catch (AuctionException e) {
            handleAuctionException(ctx, e);
        } catch (NumberFormatException e) {
            ctx.status(400).json(new Response("ERROR", "ID không hợp lệ", null));
        } catch (Exception e) {
            logger.error("Lỗi hủy auto-bid: {}", e.getMessage(), e);
            ctx.status(400).json(new Response("ERROR", "Failed to cancel auto-bid: " + e.getMessage(), null));
        }
    }

    public void updateAutoBidAmount(Context ctx) {
        try {
            long auctionId = Long.parseLong(ctx.pathParam("auctionId"));
            AutoBidUpdateDTO updateRequest = gson.fromJson(ctx.body(), AutoBidUpdateDTO.class);

            if (updateRequest == null || updateRequest.getBidderId() <= 0 || updateRequest.getMaxBidAmount() == null) {
                ctx.status(400).json(new Response("ERROR", "Request không hợp lệ", null));
                return;
            }

            auctionService.updateAutoBidAmount(auctionId, updateRequest.getBidderId(), updateRequest.getMaxBidAmount());
            logger.info("Cập nhật auto-bid thành công: auction={}, bidder={}, maxAmount={}",
                    auctionId, updateRequest.getBidderId(), updateRequest.getMaxBidAmount());
            ctx.json(new Response("SUCCESS", "Auto-bid updated successfully", null));

        } catch (AuctionException e) {
            handleAuctionException(ctx, e);
        } catch (NumberFormatException e) {
            ctx.status(400).json(new Response("ERROR", "ID không hợp lệ", null));
        } catch (Exception e) {
            logger.error("Lỗi cập nhật auto-bid: {}", e.getMessage(), e);
            ctx.status(400).json(new Response("ERROR", "Failed to update auto-bid: " + e.getMessage(), null));
        }
    }

    private void handleAuctionException(Context ctx, AuctionException e) {
        AuctionException.ErrorCode code = e.getErrorCode();
        int statusCode = switch (code) {
            case AUCTION_NOT_FOUND, AUCTION_NOT_ACTIVE -> 404;
            case INVALID_REQUEST -> 400;
            case INVALID_BID_AMOUNT, BID_AMOUNT_TOO_LOW, INVALID_AUTO_BID_CONFIG -> 400;
            case AUCTION_ALREADY_FINISHED -> 410;
            default -> 500;
        };

        ctx.status(statusCode).json(new Response("ERROR", e.getMessage(), null));
    }
}
