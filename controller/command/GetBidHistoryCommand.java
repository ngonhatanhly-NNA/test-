package com.server.controller.command;

import com.server.exception.AuthValidationException;
import com.server.service.AuctionService;
import com.server.util.ResponseUtils;
import com.shared.dto.BidHistoryDTO;
import io.javalin.http.Context;
import java.util.List;

public class GetBidHistoryCommand extends BaseApiCommand {
    private final AuctionService auctionService;

    public GetBidHistoryCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    protected void execute(Context ctx) throws Exception {
        long auctionId;
        try {
            auctionId = Long.parseLong(ctx.pathParam("auctionId"));
        } catch (NumberFormatException e) {
            throw new AuthValidationException("ID phiên đấu giá không hợp lệ");
        }

        try {
            List<BidHistoryDTO> bidHistory = auctionService.getBidHistory(auctionId);
            String json = gson.toJson(ResponseUtils.success("Loaded bid history", bidHistory));
            ctx.status(200).result(json).contentType("application/json");
        } catch (Exception e) {
            throw new AuthValidationException("Lỗi lấy lịch sử: " + e.getMessage());
        }
    }
}

