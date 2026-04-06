package com.client.controller.dashboard;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
// Import UserSession hoặc DTO chứa thông tin user đăng nhập của team em

public class MyInventoryController {

    @FXML private Button btnCreateItem;

    @FXML
    public void initialize() {
        // Giả sử em lấy được role của user đang đăng nhập từ Session
        String userRole = "BIDDER"; // Fake data để test

        if (!"SELLER".equals(userRole)) {
            // Biến mất hoàn toàn khỏi màn hình (chim cút)
            btnCreateItem.setVisible(false);
            // Căn chỉnh lại bố cục, coi như nút này không tồn tại
            btnCreateItem.setManaged(false);
        }
    }

    @FXML
    void handleCreateItem(ActionEvent event) {
        // Gắn code mở Dialog/Popup FXML ở đây (dùng Stage hoặc Dialog của JavaFX)
    }
}