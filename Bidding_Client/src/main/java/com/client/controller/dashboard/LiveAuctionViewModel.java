package com.client.controller.dashboard;

import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.AuctionUpdateDTO;
import javafx.beans.property.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class LiveAuctionViewModel {
    // Các Property để View (Controller) Bind (trói) vào
    private final StringProperty currentPrice = new SimpleStringProperty("0 VND"); // [ĐÃ SỬA]
    private final StringProperty leader = new SimpleStringProperty("—");
    private final StringProperty itemName = new SimpleStringProperty("---");
    private final StringProperty itemType = new SimpleStringProperty("Type: GENERAL");
    private final StringProperty status = new SimpleStringProperty("Status: OPEN"); // [ĐÃ SỬA]
    private final StringProperty remainingTime = new SimpleStringProperty("Remaining: Loading..."); // [ĐÃ SỬA]
    private final StringProperty stepHint = new SimpleStringProperty("");
    private final BooleanProperty isAuctionActive = new SimpleBooleanProperty(true);

    private long endTimeMillis = 0;

    // Khi chọn 1 phiên đấu giá mới
    public void setAuctionDetail(AuctionDetailDTO a) {
        itemName.set(a.getItemName() != null ? a.getItemName() : ("Item #" + a.getItemId()));
        itemType.set("Type: " + (a.getItemType() != null ? a.getItemType() : "GENERAL"));
        currentPrice.set(formatMoney(a.getCurrentPrice()) + " VND"); // [ĐÃ SỬA]
        leader.set("Leader: " + (a.getHighestBidderName() != null ? a.getHighestBidderName() : "—")); // [ĐÃ SỬA]
        status.set("Status: OPEN"); // [ĐÃ SỬA]
        isAuctionActive.set(true);
        endTimeMillis = System.currentTimeMillis() + a.getRemainingTime();

        if (a.getStepPrice() != null) {
            // [ĐÃ SỬA ĐÚNG LOGIC]: Nhập tổng số tiền, chứ không phải số tiền cộng thêm
            stepHint.set("* Step Price: " + formatMoney(a.getStepPrice()) + " VND (Enter total amount to bid)");
        }
    }

    // Khi có người đặt giá thành công (Realtime)
    public void updateRealtime(AuctionUpdateDTO updateData) {
        if (updateData.getCurrentPrice() != null) {
            currentPrice.set(formatMoney(updateData.getCurrentPrice()) + " VND"); // [ĐÃ SỬA]
        }
        if (updateData.getHighestBidderName() != null) {
            leader.set("Leader: " + updateData.getHighestBidderName()); // [ĐÃ SỬA]
        }
        endTimeMillis = System.currentTimeMillis() + updateData.getRemainingTime();
    }

    // Cập nhật đồng hồ đếm ngược
    public void updateCountdown() {
        if (!isAuctionActive.get()) return;
        long remaining = endTimeMillis - System.currentTimeMillis();
        if (remaining <= 0) {
            remainingTime.set("Remaining: Auction Ended"); // [ĐÃ SỬA]
        } else {
            long sec = remaining / 1000;
            long h = sec / 3600;
            long m = (sec % 3600) / 60;
            long s = sec % 60;
            if (h > 0) remainingTime.set("Remaining: " + h + "h " + m + "m " + s + "s"); // [ĐÃ SỬA]
            else if (m > 0) remainingTime.set("Remaining: " + m + "m " + s + "s"); // [ĐÃ SỬA]
            else remainingTime.set("Remaining: " + s + "s"); // [ĐÃ SỬA]
        }
    }

    // Khóa phiên đấu giá
    public void lockAuction() {
        isAuctionActive.set(false);
        status.set("Status: FINISHED"); // [ĐÃ SỬA]
        remainingTime.set("Remaining: TIME UP"); // [ĐÃ SỬA]
    }

    // Getters cho việc Binding
    public StringProperty currentPriceProperty() { return currentPrice; }
    public StringProperty leaderProperty() { return leader; }
    public StringProperty itemNameProperty() { return itemName; }
    public StringProperty itemTypeProperty() { return itemType; }
    public StringProperty statusProperty() { return status; }
    public StringProperty remainingTimeProperty() { return remainingTime; }
    public StringProperty stepHintProperty() { return stepHint; }
    public BooleanProperty isAuctionActiveProperty() { return isAuctionActive; }
    public long getRemainingTimeMillis() { return endTimeMillis - System.currentTimeMillis(); }

    private String formatMoney(BigDecimal v) {
        return v == null ? "0" : NumberFormat.getNumberInstance(Locale.of("vi", "VN")).format(v);
    }
}