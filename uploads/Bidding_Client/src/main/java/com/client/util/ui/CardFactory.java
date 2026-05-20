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
                .addInfoText("Giá hiện tại: " + formatMoney(a.getCurrentPrice()) + " đ", "#D4AF37", true)
                .addInfoText("📅 Từ: " + a.getStartTime(), "#A0A0A0", false)
                .addInfoText("🏁 Đến: " + a.getEndTime(), "#E74C3C", true)
                .setOnClick(onClick)
                .build();
    }

    // 2. Thẻ cho ViewDashboard (Khám phá Item)
    public static VBox createExploreItemCard(ItemResponseDTO item, Class<?> ctx, EventHandler<ActionEvent> onGoLive) {
        return new UICardBuilder(ctx)
                .setCustomStyle("-fx-background-color: #181818; -fx-border-color: rgba(212,175,55,0.5); -fx-border-width: 1.5;")
                .setImage(item.getThumbnailUrl())
                .setTitle(item.getName() != null ? item.getName() : "Item #" + item.getId(), "#F5F5F5", 16)
                .addInfoText("Giá khởi điểm: " + formatMoney(item.getStartingPrice()) + " VNĐ", "#D4AF37", true)
                .addInfoText("Loại: " + item.getType(), "#A0A0A0", false)
                .setActionButton("Tới phòng đấu giá", "-fx-background-color: linear-gradient(to right, #D4AF37, #FFD700);", onGoLive)
                .setUserData(item.getId())
                .build();
    }

    // 3. Thẻ cho ViewLiveAuctions (Sidebar)
    public static VBox createSidebarAuctionCard(AuctionDetailDTO a, Class<?> ctx, EventHandler<ActionEvent> onSelect) {
        return new UICardBuilder(ctx)
                .setCustomStyle("-fx-border-color: rgba(212,175,55,0.3);")
                .setTitle(a.getItemName(), "#F5F5F5", 16)
                .addInfoText("Loại: " + a.getItemType(), "#A0A0A0", false)
                .addInfoText("Giá: " + formatMoney(a.getCurrentPrice()) + " đ", "#D4AF37", true)
                .addInfoText("Dẫn đầu: " + (a.getHighestBidderName() != null ? a.getHighestBidderName() : "—"), "#E0E0E0", false)
                .addInfoText("Còn lại: Đang tải...", "#A0A0A0", false) // Label này sẽ được Timeline update
                .setActionButton("Vào đấu giá", "-fx-background-color: linear-gradient(to right, #D4AF37, #FFD700);", onSelect)
                .setUserData(a.getAuctionId())
                .build();
    }

    // 4. Thẻ cho MyInventory (Đang bán)
    public static VBox createInventorySellingCard(ItemResponseDTO item, Class<?> ctx, EventHandler<ActionEvent> onOpen) {
        return new UICardBuilder(ctx)
                .setCustomStyle("-fx-background-color: #181818; -fx-border-color: rgba(212,175,55,0.3); -fx-border-width: 1.5;")
                .setTitle(item.getName(), "#F5F5F5", 16)
                .addInfoText("Category: " + item.getType(), "#A0A0A0", false)
                .addInfoText("Start Price: " + formatMoney(item.getStartingPrice()) + " VND", "#D4AF37", true)
                .setActionButton("Open Auction", "-fx-background-color: linear-gradient(to right, #D4AF37, #FFD700);", onOpen)
                .setUserData(item.getId())
                .build();
    }

    // 5. Thẻ cho MyInventory (Đã thắng)
    public static VBox createInventoryWonCard(AuctionDetailDTO auction, Class<?> ctx) {
        return new UICardBuilder(ctx)
                .setCustomStyle("-fx-border-color: rgba(76,175,80,0.5); -fx-border-width: 1.5;")
                .setTitle("🏆 " + auction.getItemName(), "#F5F5F5", 15)
                .addInfoText("Giá thắng: " + formatMoney(auction.getCurrentPrice()) + " VND", "#D4AF37", true)
                .addInfoText("Người thắng: " + (auction.getHighestBidderName() != null ? auction.getHighestBidderName() : "Bạn"), "#A0A0A0", false)
                .addInfoText("Loại: " + auction.getItemType(), "#A0A0A0", false)
                .addBadge("✅ ĐÃ THẮNG", "rgba(76,175,80,0.1)", "#4CAF50", "rgba(76,175,80,0.4)")
                .build();
    }
}