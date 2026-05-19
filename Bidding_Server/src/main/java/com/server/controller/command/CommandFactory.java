package com.server.controller.command;

import com.server.service.AuctionService;
import com.server.service.ItemService;
import com.server.service.UserService;
import io.javalin.http.Handler;

/**
 * Factory Pattern: Tập trung toàn bộ việc khởi tạo (new) Command vào một chỗ.
 * Giúp ApiRouter không còn bị phụ thuộc vào các class cụ thể nữa.
 */
public class CommandFactory {
    private final AuctionService auctionService;
    private final ItemService itemService;
    private final UserService userService;

    public CommandFactory(AuctionService auctionService, ItemService itemService, UserService userService) {
        this.auctionService = auctionService;
        this.itemService = itemService;
        this.userService = userService;
    }

    // --- User Commands ---
    public Handler updateProfile() { return new UpdateProfileCommand(userService); }

    // --- Item Commands ---
    public Handler getAllItems() { return new GetAllItemsCommand(itemService); }
    public Handler createItem() { return new CreateItemCommand(itemService); }
    public Handler getItemsBySeller(long sellerId) { return new GetItemsBySellerIdCommand(itemService); } // Có thể bỏ tham số nếu Command tự lấy từ Path

    // --- Auction Commands ---
    public Handler createAuction() { return new CreateAuctionCommand(auctionService); }
    public Handler placeBid() { return new PlaceBidCommand(auctionService); }
    public Handler getActiveAuctions() { return new GetActiveAuctionsCommand(auctionService); }
    public Handler getUpcomingAuctions() { return new GetUpcomingAuctionsCommand(auctionService); }
    public Handler getBidHistory() { return new GetBidHistoryCommand(auctionService); }
    public Handler cancelAutoBid() { return new CancelAutoBidCommand(auctionService); }
    public Handler updateAutoBid() { return new UpdateAutoBidAmountCommand(auctionService); }
    public Handler getAuctionDetail() { return new GetAuctionDetailCommand(auctionService); }
}