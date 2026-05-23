package com.client.util.ui;

import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.ItemResponseDTO;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class CardFactory {
    private static final NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.of("vi", "VN"));

    private static String formatMoney(BigDecimal v) {
        return v == null ? "0" : currencyFormat.format(v);
    }

    // 1. Thẻ cho ViewDashboard (Live/Upcoming)
    public static VBox createDashboardAuctionCard(AuctionDetailDTO a, Class<?> ctx, Runnable onClick) {
        String imgUrl = (a.getItemImageUrls() != null && !a.getItemImageUrls().isEmpty()) ? a.getItemImageUrls().get(0) : null;
        return new UICardBuilder(ctx)
                .setCustomStyle("-fx-border-color: rgba(212,175,55,0.3);")
                .setImage(imgUrl)
                .setTitle(a.getItemName() != null ? a.getItemName() : "Item", "#F5F5F5", 14)
                .addInfoText("Current Price: " + formatMoney(a.getCurrentPrice()) + " VND", "#D4AF37", true) // [ĐÃ SỬA]
                .addInfoText("📅 From: " + a.getStartTime(), "#A0A0A0", false) // [ĐÃ SỬA]
                .addInfoText("🏁 To: " + a.getEndTime(), "#E74C3C", true) // [ĐÃ SỬA]
                .setOnClick(onClick)
                .build();
    }

    // 2. Thẻ cho ViewDashboard (Khám phá Item)
    public static VBox createExploreItemCard(ItemResponseDTO item, Class<?> ctx, EventHandler<ActionEvent> onGoLive) {
        return new UICardBuilder(ctx)
                .setCustomStyle("-fx-background-color: #181818; -fx-border-color: rgba(212,175,55,0.5); -fx-border-width: 1.5;")
                .setImage(item.getThumbnailUrl())
                .setTitle(item.getName() != null ? item.getName() : "Item #" + item.getId(), "#F5F5F5", 16)
                .addInfoText("Start Price: " + formatMoney(item.getStartingPrice()) + " VND", "#D4AF37", true) // [ĐÃ SỬA]
                .addInfoText("Category: " + item.getType(), "#A0A0A0", false) // [ĐÃ SỬA]
                .setActionButton("Go to Auction", "-fx-background-color: linear-gradient(to right, #D4AF37, #FFD700);", onGoLive) // [ĐÃ SỬA]
                .setUserData(item.getId())
                .build();
    }

    // 3. Thẻ cho ViewLiveAuctions (Sidebar)
    public static VBox createSidebarAuctionCard(AuctionDetailDTO a, Class<?> ctx, EventHandler<ActionEvent> onSelect) {
        return new UICardBuilder(ctx)
                .setCustomStyle("-fx-border-color: rgba(212,175,55,0.3);")
                .setTitle(a.getItemName(), "#F5F5F5", 16)
                .addInfoText("Category: " + a.getItemType(), "#A0A0A0", false) // [ĐÃ SỬA]
                .addInfoText("Price: " + formatMoney(a.getCurrentPrice()) + " VND", "#D4AF37", true) // [ĐÃ SỬA]
                .addInfoText("Leader: " + (a.getHighestBidderName() != null ? a.getHighestBidderName() : "—"), "#E0E0E0", false) // [ĐÃ SỬA]
                .addInfoText("Remaining: Loading...", "#A0A0A0", false) // [ĐÃ SỬA]
                .setActionButton("Enter Auction", "-fx-background-color: linear-gradient(to right, #D4AF37, #FFD700);", onSelect) // [ĐÃ SỬA]
                .setUserData(a.getAuctionId())
                .build();
    }

    // 4. Thẻ cho MyInventory (Đang bán)
    public static VBox createInventorySellingCard(ItemResponseDTO item, Class<?> ctx, EventHandler<ActionEvent> onOpen, EventHandler<ActionEvent> onDelete) {
        return new UICardBuilder(ctx)
                .setCustomStyle("-fx-background-color: #181818; -fx-border-color: rgba(212,175,55,0.3); -fx-border-width: 1.5;")
                .setTitle(item.getName(), "#F5F5F5", 16)
                .addInfoText("Category: " + item.getType(), "#A0A0A0", false)
                .addInfoText("Start Price: " + formatMoney(item.getStartingPrice()) + " VND", "#D4AF37", true)
                .setActionButton("Open Auction", "-fx-background-color: linear-gradient(to right, #D4AF37, #FFD700);", onOpen)
                .setActionButton("Delete", "-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;", onDelete)
                .setUserData(item.getId())
                .build();
    }

    // 5. Thẻ cho MyInventory (Đã thắng)
    public static VBox createInventoryWonCard(AuctionDetailDTO auction, Class<?> ctx) {
        return new UICardBuilder(ctx)
                .setCustomStyle("-fx-border-color: rgba(76,175,80,0.5); -fx-border-width: 1.5;")
                .setTitle("🏆 " + auction.getItemName(), "#F5F5F5", 15)
                .addInfoText("Winning Price: " + formatMoney(auction.getCurrentPrice()) + " VND", "#D4AF37", true) // [ĐÃ SỬA]
                .addInfoText("Winner: " + (auction.getHighestBidderName() != null ? auction.getHighestBidderName() : "You"), "#A0A0A0", false) // [ĐÃ SỬA]
                .addInfoText("Category: " + auction.getItemType(), "#A0A0A0", false) // [ĐÃ SỬA]
                .addBadge("✅ WON", "rgba(76,175,80,0.1)", "#4CAF50", "rgba(76,175,80,0.4)") // [ĐÃ SỬA]
                .build();
    }
}