package com.server.controller;

import com.google.gson.Gson;
import com.server.service.AuctionService;
import com.shared.dto.BidHistoryDTO;
import com.shared.network.Response;
import io.javalin.http.Context;

import java.util.List;

public class TransactionController {

    private final AuctionService auctionService;
    private final Gson gson = new Gson();

    /**
     * Constructor để "tiêm" AuctionService từ bên ngoài.
     * Điều này rất quan trọng để có thể viết test.
     * @param auctionService Service sẽ được sử dụng.
     */
    public TransactionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /**
     * Lấy lịch sử đặt giá của một phiên đấu giá.
     * API Endpoint (ví dụ): GET /api/transactions/auction/{auctionId}
     * @param ctx Context của Javalin.
     */
    public void getBidHistoryByAuction(Context ctx) {
        try {
            long auctionId = Long.parseLong(ctx.pathParam("auctionId"));
            // Sử dụng phương thức getBidHistory đã tồn tại trong AuctionService
            List<BidHistoryDTO> history = auctionService.getBidHistory(auctionId);
            ctx.json(new Response("SUCCESS", "Lịch sử giao dịch đã được tải", history));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new Response("ERROR", "Auction ID không hợp lệ", null));
        } catch (Exception e) {
            ctx.status(500).json(new Response("ERROR", "Lỗi server: " + e.getMessage(), null));
        }
    }
}
