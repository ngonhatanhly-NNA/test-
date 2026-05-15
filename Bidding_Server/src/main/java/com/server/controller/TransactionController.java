package com.server.controller;

import com.google.gson.Gson;
import com.server.service.AuctionService;
import com.shared.dto.BidHistoryDTO;
import com.shared.network.Response;
import io.javalin.http.Context;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionController {
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    private final AuctionService auctionService;
    private final Gson gson = new Gson();

    public TransactionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public void getBidHistoryByAuction(Context ctx) {
        try {
            long auctionId = Long.parseLong(ctx.pathParam("auctionId"));
            List<BidHistoryDTO> history = auctionService.getBidHistory(auctionId);
            logger.info("Lấy lịch sử giao dịch cho auction {}: {} records", auctionId, history.size());
            ctx.json(new Response("SUCCESS", "Lịch sử giao dịch đã được tải", history));
        } catch (NumberFormatException e) {
            ctx.status(400).json(new Response("ERROR", "Auction ID không hợp lệ", null));
        } catch (Exception e) {
            logger.error("Lỗi lấy lịch sử giao dịch: {}", e.getMessage(), e);
            ctx.status(500).json(new Response("ERROR", "Lỗi server: " + e.getMessage(), null));
        }
    }
}

