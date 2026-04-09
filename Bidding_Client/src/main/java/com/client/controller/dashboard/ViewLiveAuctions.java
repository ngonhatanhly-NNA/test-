package com.client.controller.dashboard;


import com.client.network.AuctionNetwork;
import com.shared.dto.AuctionUpdateDTO;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.CheckBox;
import java.math.BigDecimal;

public class ViewLiveAuctions {
    // Khai báo các biến tương ứng với ID trong file FXML
    @FXML private Label currentPriceLabel;
    @FXML private TextField bidAmountField;
    @FXML private CheckBox enableAutoBidCheckBox;
    @FXML private TextField maxAutoBidField;
    @FXML private Label autoBidStatusLabel;

    // TODO: Cap nhat cai nay tu db sa
    private long currentUserId = 5;
    private long currentAuctionId = 1;

    // Hàm Singleton temp
    private static ViewLiveAuctions instance;
    public ViewLiveAuctions() { instance = this; }
    public static ViewLiveAuctions getInstance() { return instance; }

    @FXML
    public void initialize() {
        //  AuctionNetwork.getActiveAuctions()
        // Ẩn auto-bid UI mặc định
        if (maxAutoBidField != null) {
            maxAutoBidField.setVisible(false);
        }
        if (autoBidStatusLabel != null) {
            autoBidStatusLabel.setVisible(false);
        }

        // Lắng nghe sự kiện checkbox
        if (enableAutoBidCheckBox != null) {
            enableAutoBidCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (maxAutoBidField != null) {
                    maxAutoBidField.setVisible(newVal);
                }
            });
        }
    }

    //  OnAction placeBid
    @FXML
    public void handlePlaceBid() {
        try {
            BigDecimal amount = new BigDecimal(bidAmountField.getText());

            // Kiểm tra auto-bid
            if (enableAutoBidCheckBox != null && enableAutoBidCheckBox.isSelected()) {
                BigDecimal maxAutoBid = new BigDecimal(maxAutoBidField.getText());

                if (maxAutoBid.compareTo(amount) < 0) {
                    showError("Giá tối đa auto-bid phải lớn hơn giá đặt hiện tại!");
                    return;
                }

                // Gọi API với auto-bid
                String response = AuctionNetwork.placeBidWithAutoBid(currentAuctionId, currentUserId, amount, maxAutoBid);
                System.out.println("Kết quả đặt giá (với auto-bid): " + response);

                if (autoBidStatusLabel != null) {
                    autoBidStatusLabel.setVisible(true);
                    autoBidStatusLabel.setText("✓ Auto-bid hoạt động (Tối đa: " + maxAutoBid + ")");
                    autoBidStatusLabel.setStyle("-fx-text-fill: green;");
                }
            } else {
                // Gọi API thông thường
                String response = AuctionNetwork.placeBid(currentAuctionId, currentUserId, amount);
                System.out.println("Kết quả đặt giá: " + response);
            }

        } catch (NumberFormatException e) {
            showError("Vui lòng nhập giá hợp lệ!");
            e.printStackTrace();
        } catch (Exception e) {
            showError("Lỗi khi đặt giá: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Hủy auto-bid
    @FXML
    public void handleCancelAutoBid() {
        try {
            String response = AuctionNetwork.cancelAutoBid(currentAuctionId, currentUserId);
            System.out.println("Hủy auto-bid: " + response);

            if (autoBidStatusLabel != null) {
                autoBidStatusLabel.setVisible(true);
                autoBidStatusLabel.setText("✗ Auto-bid đã bị hủy");
                autoBidStatusLabel.setStyle("-fx-text-fill: red;");
            }

            if (enableAutoBidCheckBox != null) {
                enableAutoBidCheckBox.setSelected(false);
            }
        } catch (Exception e) {
            showError("Lỗi khi hủy auto-bid: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Cập nhật giá auto-bid
    @FXML
    public void handleUpdateAutoBid() {
        try {
            BigDecimal newMaxBid = new BigDecimal(maxAutoBidField.getText());
            String response = AuctionNetwork.updateAutoBid(currentAuctionId, currentUserId, newMaxBid);
            System.out.println("Cập nhật auto-bid: " + response);

            if (autoBidStatusLabel != null) {
                autoBidStatusLabel.setText("✓ Auto-bid cập nhật (Tối đa: " + newMaxBid + ")");
                autoBidStatusLabel.setStyle("-fx-text-fill: green;");
            }
        } catch (Exception e) {
            showError("Lỗi khi cập nhật auto-bid: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //MyWebSocketClient gọi mỗi khi có người đặt giá thành công
    public void updatePriceRealtime(AuctionUpdateDTO updateData) {
        // Chỉ cập nhật nếu tin nhắn thuộc về phòng đấu giá đang xem, tranhs phongf nao cung nhan dc tin
        if (updateData.getAuctionId() == currentAuctionId) {
            Platform.runLater(() -> {
                currentPriceLabel.setText(updateData.getCurrentPrice().toString() + " VNĐ");
            });
        }
    }

    private void showError(String message) {
        System.err.println("LỖI: " + message);
        // TODO: Hiển thị popup hoặc thông báo lỗi trên UI
    }
}

