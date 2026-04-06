package com.client.controller.dashboard;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import java.io.IOException;

// Chú ý: Phải import thêm SceneController ở đây
import com.client.util.SceneController;

public class DashboardController {

    // Đang update dashboard phải hiện thông tin users và có chữ

    // KHAI BÁO CÁC THÀNH PHẦN GIAO DIỆN ---
    @FXML private BorderPane mainBorderPane; // Cái khung to nhất, lấy view là cái khung tô nhất, switch liên tục
    @FXML private Button btnDashboard;
    @FXML private Button btnLiveAuctions;
    @FXML private Button btnMyInventory;
    @FXML private Button btnLogout;

    // HÀM THAY RUỘT (Chỉ giữ lại 1 bản chuẩn BorderPane
    private void loadView(String fxmlFileName) {
        try {
            // Tải cái "ruột" mới từ file FXML
            Node view = FXMLLoader.load(getClass().getResource("/fxml/" + fxmlFileName)); // thay vi switch scene là
            // giữ cái khung và switch scene, ta gắn root node với 1 node kahcs

            // Lắp thẳng cái ruột mới vào vị trí Center của BorderPane
            mainBorderPane.setCenter(view);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi: Không tìm thấy file " + fxmlFileName);
        }
    }

    // HÀM TÔ MÀU NÚT CAM (Active State) ---
    private void setActiveButton(Button activeButton) {
        //
        String defaultStyle = "-fx-background-color: transparent; -fx-text-fill: #999999; -fx-alignment: BASELINE_LEFT; -fx-font-weight: bold;";
        btnDashboard.setStyle(defaultStyle);
        btnLiveAuctions.setStyle(defaultStyle);
        btnMyInventory.setStyle(defaultStyle);

        // Tô màu cam cho nút vừa được bấm
        String activeStyle = "-fx-background-color: #E3B04B; -fx-text-fill: white; -fx-alignment: BASELINE_LEFT; -fx-font-weight: bold; -fx-background-radius: 5;";
        activeButton.setStyle(activeStyle);
    }

    // CÁC SỰ KIỆN BẤM NÚT (Gọi cùng lúc 2 hàm trên) ---
    @FXML
    void handleBtnDashboard(ActionEvent event) {
        loadView("ViewDashboard.fxml");
        setActiveButton(btnDashboard);
    }

    @FXML
    void handleBtnLiveAuctions(ActionEvent event) {
        loadView("ViewLiveAuctions.fxml");
        setActiveButton(btnLiveAuctions);
    }

    @FXML
    void handleBtnMyInventory(ActionEvent event) {
        loadView("ViewMyInventory.fxml");
        setActiveButton(btnMyInventory);
    }

    @FXML
    void handleLogout(ActionEvent event) throws IOException {
        // Gọi quản gia đưa về lại màn hình Login
        SceneController.switchScene(event, "AuctionMenu.fxml");
    }

    // --- HÀM KHỞI TẠO (Chạy tự động khi load màn hình) ---
    @FXML
    public void initialize() {
        // Tự động "bấm" nút Dashboard ngay khi vừa vào app
        // Giúp đồng bộ màu cam và kích thước nút ngay từ đầu -> Fix lỗi UI
        handleBtnDashboard(null);
    }
}