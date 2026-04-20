package com.client.controller.dashboard;

import com.client.session.ClientSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
// Import UserSession hoặc DTO chứa thông tin user đăng nhập

public class MyInventoryController {

    @FXML private Button btnCreateItem;

    @FXML
    public void initialize() {
        // Lấy được role của user đang đăng nhập từ Session
        String userRole = ClientSession.getRole(); //

        if (!"SELLER".equals(userRole)) {
            // Biến mất hoàn toàn khỏi màn hình
            btnCreateItem.setVisible(false);
            // Căn chỉnh lại bố cục, coi như nút này không tồn tại
            btnCreateItem.setManaged(false);
        }
    }

    @FXML
    void handleCreateItem(ActionEvent event) {
        // Code mở màn hình Popup CreateItemPopup.fxml
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CreateItemPopup.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL); // Bắt buộc người dùng xử lý popup xong mới quay lại màn chính
            stage.setTitle("Thêm Sản Phẩm Mới");
            stage.setScene(new Scene(root));
            stage.showAndWait(); // Đợi popup đóng lại


        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Không thể mở cửa sổ Tạo sản phẩm!");
        }
    }
}