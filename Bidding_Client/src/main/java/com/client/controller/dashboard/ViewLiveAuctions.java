package com.client.controller.dashboard;


import com.client.network.AuctionNetwork;
import com.shared.dto.AuctionUpdateDTO;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.math.BigDecimal;

public class ViewLiveAuctions {
    // Khai báo các biến tương ứng với ID trong file FXML
    @FXML private Label currentPriceLabel;
    @FXML private TextField bidAmountField;

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
    }

    //  OnAction placeBid
    @FXML
    public void handlePlaceBid() {
        try {
            BigDecimal amount = new BigDecimal(bidAmountField.getText());
            // Gọi API lên Server
            String response = AuctionNetwork.placeBid(currentAuctionId, currentUserId, amount);
            System.out.println("Kết quả đặt giá: " + response);
            // (Tuỳ chọn: Hiện Popup thông báo thành công/thất bại)
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //MyWebSocketClient gọi mỗi khi có người đặt giá thành công
    public void updatePriceRealtime(AuctionUpdateDTO updateData) {
        // Chỉ cập nhật nếu tin nhắn thuộc về phòng đấu giá đang xem, tranhs phongf nao cung nhan dc tin
        if (updateData.getAuctionId() == currentAuctionId) {
            currentPriceLabel.setText(updateData.getCurrentPrice().toString() + " VNĐ");
        }
    }
}

