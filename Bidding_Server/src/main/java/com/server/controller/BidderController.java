package com.server.controller;

import com.google.gson.Gson;
import com.server.model.Bidder;
import com.server.service.BidderService;
import com.shared.dto.DepositRequestDTO;
import com.shared.network.Response;
import io.javalin.http.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BidderController: Xử lý HTTP requests cho /api/bidders/*
 * Cung cấp API để Bidder (và Seller vì kế thừa) quản lý ví tiền, nạp tiền, v.v.
 */
public class BidderController {
    private static final Logger logger = LoggerFactory.getLogger(BidderController.class);

    private final BidderService bidderService;
    private final Gson gson = new Gson();

    public BidderController(BidderService bidderService) {
        this.bidderService = bidderService;
    }

    /**
     * PUT /api/bidders/{bidderId}/deposit
     * Nạp tiền vào ví của Bidder (cũng dùng cho Seller vì Seller extends Bidder)
     * Request body: {"depositAmount": 100000, "paymentMethod": "CREDIT_CARD"}
     */
    public void depositMoney(Context ctx) {
        try {
            long bidderId = Long.parseLong(ctx.pathParam("bidderId"));
            DepositRequestDTO depositRequest = gson.fromJson(ctx.body(), DepositRequestDTO.class);

            // Validate input
            if (depositRequest == null || depositRequest.getDepositAmount() == null) {
                ctx.status(400).json(new Response("FAIL", "Số tiền nạp không hợp lệ", null));
                return;
            }

            // Gọi service để xử lý nạp tiền
            String result = bidderService.depositMoney(bidderId, depositRequest.getDepositAmount().doubleValue());

            // Parse response từ service
            Response response = gson.fromJson(result, Response.class);
            ctx.json(response);
            logger.info("Nạp tiền thành công cho bidder ID {}: {} VNĐ", bidderId, depositRequest.getDepositAmount());

        } catch (NumberFormatException e) {
            logger.warn("Bidder ID không hợp lệ: {}", e.getMessage());
            ctx.status(400).json(new Response("FAIL", "Bidder ID không hợp lệ", null));
        } catch (Exception e) {
            logger.error("Lỗi nạp tiền cho bidder: {}", e.getMessage(), e);
            ctx.status(500).json(new Response("ERROR", "Lỗi server: " + e.getMessage(), null));
        }
    }

    /**
     * GET /api/bidders/{bidderId}/wallet
     * Lấy thông tin ví (số dư hiện tại) của Bidder
     * Response: { "walletBalance": 500000, "creditCardInfo": "****1234" }
     */
    public void getWalletBalance(Context ctx) {
        try {
            long bidderId = Long.parseLong(ctx.pathParam("bidderId"));
            Bidder bidder = bidderService.getBidderDetails(bidderId);

            if (bidder == null) {
                ctx.status(404).json(new Response("FAIL", "Bidder không tồn tại", null));
                return;
            }

            WalletInfo walletInfo = new WalletInfo(
                    bidder.getWalletBalance(),
                    bidder.getCreditCardInfo()
            );

            ctx.json(new Response("SUCCESS", "Lấy thông tin ví thành công", walletInfo));
            logger.info("Lấy thông tin ví cho bidder ID {}", bidderId);

        } catch (NumberFormatException e) {
            logger.warn("Bidder ID không hợp lệ");
            ctx.status(400).json(new Response("FAIL", "Bidder ID không hợp lệ", null));
        } catch (Exception e) {
            logger.error("Lỗi lấy thông tin ví: {}", e.getMessage(), e);
            ctx.status(500).json(new Response("ERROR", "Lỗi server: " + e.getMessage(), null));
        }
    }

    /**
     * GET /api/bidders/{bidderId}/profile
     * Lấy thông tin chi tiết Bidder
     */
    public void getBidderProfile(Context ctx) {
        try {
            long bidderId = Long.parseLong(ctx.pathParam("bidderId"));
            Bidder bidder = bidderService.getBidderDetails(bidderId);

            if (bidder == null) {
                ctx.status(404).json(new Response("FAIL", "Bidder không tồn tại", null));
                return;
            }

            ctx.json(new Response("SUCCESS", "Lấy thông tin bidder thành công", bidder));

        } catch (NumberFormatException e) {
            ctx.status(400).json(new Response("FAIL", "Bidder ID không hợp lệ", null));
        } catch (Exception e) {
            logger.error("Lỗi lấy thông tin bidder: {}", e.getMessage(), e);
            ctx.status(500).json(new Response("ERROR", "Lỗi server: " + e.getMessage(), null));
        }
    }

    /**
     * Helper class để trả về thông tin ví
     */
    public static class WalletInfo {
        private Object walletBalance; // Có thể là BigDecimal từ DB
        private String creditCardInfo;

        public WalletInfo(Object walletBalance, String creditCardInfo) {
            this.walletBalance = walletBalance;
            this.creditCardInfo = creditCardInfo;
        }

        public Object getWalletBalance() { return walletBalance; }
        public String getCreditCardInfo() { return creditCardInfo; }
    }
}

