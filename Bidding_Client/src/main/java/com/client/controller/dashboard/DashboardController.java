package com.client.controller.dashboard;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.input.MouseEvent;

import java.io.IOException;

import com.client.util.SwitchPane;
import com.client.util.SceneController;
import com.client.util.ClientSession;

public class DashboardController {

    @FXML private BorderPane mainBorderPane;
    private SwitchPane mainPane; // qeen mat la chua khoi taoj :)

    // Nút Menu
    @FXML private Button btnDashboard;
    @FXML private Button btnLiveAuctions;
    @FXML private Button btnMyInventory;
    @FXML private Button btnLogout;

    // Header
    @FXML private Label lblUsername;
    @FXML private TextField searchField; // Nếu bạn có đặt fx:id="searchField" cho ô tìm kiếm

    // Nút Categories
    //  <CheckBox fx:id="chkElectronics" text="Electronics" />
    @FXML private CheckBox chkElectronics;
    @FXML private CheckBox chkFineArts;
    @FXML private CheckBox chkVehicles;

    // --- HÀM KHỞI TẠO ---
    @FXML
    public void initialize() { // Tự động
        // Cập nhật tên user đăng nhập (Sau này lấy từ Utils/Session)
         mainPane = new SwitchPane(mainBorderPane);
        if (ClientSession.isLoggedIn()) {
            lblUsername.setText(ClientSession.getUsername());
        } else {
            lblUsername.setText("Khách");
        }

        // Tự động load màn hình Dashboard đầu tiên
        handleBtnDashboard(null);
    }

    // --- HÀM TÔ MÀU NÚT ---
    private void setActiveButton(Button activeButton) {
        // Trả tất cả về giao diện mặc định
        String defaultStyle = "-fx-background-color: transparent; -fx-text-fill: #333333; -fx-alignment: CENTER_LEFT; -fx-padding: 12; -fx-font-size: 14px;";
        btnDashboard.setStyle(defaultStyle);
        btnLiveAuctions.setStyle(defaultStyle);
        btnMyInventory.setStyle(defaultStyle);

        String activeStyle = "-fx-background-color: #E3B04B; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 12; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 14px;";
        activeButton.setStyle(activeStyle);
    }

    // --- SỰ KIỆN MENU CHÍNH ---
    @FXML
    void handleBtnDashboard(ActionEvent event) {
        mainPane.loadView("ViewDashboard.fxml");
        setActiveButton(btnDashboard);
    }

    @FXML
    void handleBtnLiveAuctions(ActionEvent event) {
        mainPane.loadView("ViewLiveAuctions.fxml");
        setActiveButton(btnLiveAuctions);
    }

    @FXML
    void handleBtnMyInventory(ActionEvent event) {
        mainPane.loadView("ViewMyInventory.fxml");
        setActiveButton(btnMyInventory);
    }

    @FXML
    void handleLogout(ActionEvent event) throws IOException {
        SceneController.switchScene(event, "AuctionMenu.fxml");
    }

    // --- SỰ KIỆN CATEGORIES (Lọc sản phẩm) ---
    // Bạn gắn hàm này vào onAction của các CheckBox trong FXML
    @FXML
    void handleCategoryFilter(ActionEvent event) {
        System.out.println("Lọc sản phẩm...");
        if (chkElectronics != null && chkElectronics.isSelected()) System.out.println("- Đồ điện tử");
        if (chkFineArts != null && chkFineArts.isSelected()) System.out.println("- Nghệ thuật");
        if (chkVehicles != null && chkVehicles.isSelected()) System.out.println("- Xe cộ");

        // TODO: Nối với ViewDashboardController để lọc danh sách hiển thị
    }

    @FXML
    void handleOpenProfile (MouseEvent event) {
        mainPane.loadView("UserProfile.fxml");
    }
}