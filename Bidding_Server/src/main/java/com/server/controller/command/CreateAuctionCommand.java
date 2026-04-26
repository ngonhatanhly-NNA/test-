package com.server.controller.command;

import com.server.exception.AuctionAppException;
import com.server.exception.AuctionException;
import com.server.exception.AuthValidationException;
import com.server.service.AuctionService;
import com.server.util.ResponseUtils;
import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.CreateAuctionDTO;
import io.javalin.http.Context;

public class CreateAuctionCommand extends BaseApiCommand {
    private final AuctionService auctionService;

    public CreateAuctionCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    protected void execute(Context ctx) throws Exception {
        try {
            CreateAuctionDTO request = gson.fromJson(ctx.body(), CreateAuctionDTO.class);

            if (request == null || request.getItemId() <= 0 || request.getSellerId() <= 0) {
                throw new AuthValidationException("ItemId và SellerId phải lớn hơn 0");
            }

            long auctionId = auctionService.createAuction(request);
            AuctionDetailDTO detail = auctionService.getAuctionDetail(auctionId);

            String json =
                    gson.toJson(ResponseUtils.success("Phiên đấu giá đã tạo thành công", detail));
            ctx.status(201).result(json).contentType("application/json");

        } catch (AuctionException e) {
            throw AuctionAppException.from(e);
        } catch (AuthValidationException e) {
            throw e;
        }
    }
}

