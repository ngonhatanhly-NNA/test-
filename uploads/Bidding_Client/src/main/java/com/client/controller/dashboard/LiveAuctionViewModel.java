package com.client.controller.dashboard;

import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.AuctionUpdateDTO;
import javafx.beans.property.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class LiveAuctionViewModel {
    // Các Property để View (Controller) Bind (trói) vào
    private final StringProperty currentPrice = new SimpleStringProperty("0 VNĐ");
    private final StringProperty leader = new SimpleStringProperty("—");
    private final StringProperty itemName = new SimpleStringProperty("---");
    private final StringProperty itemType = new SimpleStringProperty("Type: GENERAL");
    private final StringProperty status = new SimpleStringProperty("Tình trạng: ĐANG MỞ");
    private final StringProperty remainingTime = new SimpleStringProperty("Còn lại: Đang tải...");
    private final StringProperty stepHint = new SimpleStringProperty("");
    private final BooleanProperty isAuctionActive = new SimpleBooleanProperty(true);

    private long endTimeMillis = 0;

    // Khi chọn 1 phiên đấu giá mới
    public void setAuctionDetail(AuctionDetailDTO a) {
        itemName.set(a.getItemName() != null ? a.getItemName() : ("Item #" + a.getItemId()));
        itemType.set("Type: " + (a.getItemType() != null ? a.getItemType() : "GENERAL"));
        currentPrice.set(formatMoney(a.getCurrentPrice()) + " VNĐ");
        leader.set("Người dẫn đầu: " + (a.getHighestBidderName() != null ? a.getHighestBidderName() : "—"));
        status.set("Tình trạng: ĐANG MỞ");
        isAuctionActive.set(true);
        endTimeMillis = System.currentTimeMillis() + a.getRemainingTime();
        
        if (a.getStepPrice() != null) {
            stepHint.set("* Bước giá: " + formatMoney(a.getStepPrice()) + " VNĐ (Nhập số tiền muốn CỘNG THÊM)");
        }
    }

    // Khi có người đặt giá thành công (Realtime)
    public void updateRealtime(AuctionUpdateDTO updateData) {
        if (updateData.getCurrentPrice() != null) {
            currentPrice.set(formatMoney(updateData.getCurrentPrice()) + " VNĐ");
        }
        if (updateData.getHighestBidderName() != null) {
            leader.set("Người dẫn đầu: " + updateData.getHighestBidderName());
        }
        endTimeMillis = System.currentTimeMillis() + updateData.getRemainingTime();
    }

    // Cập nhật đồng hồ đếm ngược
    public void updateCountdown() {
        if (!isAuctionActive.get()) return;
        long remaining = endTimeMillis - System.currentTimeMillis();
        if (remaining <= 0) {
            remainingTime.set("Còn lại: Hết giờ đấu giá");
        } else {
            long sec = remaining / 1000;
            long h = sec / 3600;
            long m = (sec % 3600) / 60;
            long s = sec % 60;
            if (h > 0) remainingTime.set("Còn lại: " + h + " giờ " + m + " phút " + s + " giây");
            else if (m > 0) remainingTime.set("Còn lại: " + m + " phút " + s + " giây");
            else remainingTime.set("Còn lại: " + s + " giây");
        }
    }

    // Khóa phiên đấu giá
    public void lockAuction() {
        isAuctionActive.set(false);
        status.set("FINISHED");
        remainingTime.set("TIME UP");
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