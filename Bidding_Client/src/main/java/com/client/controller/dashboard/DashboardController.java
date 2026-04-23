package com.client.controller.dashboard;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.input.MouseEvent;

import java.io.IOException;

import com.client.util.SwitchPane;
import com.client.util.SceneController;
import com.client.session.ClientSession;

public class DashboardController {

    @FXML private BorderPane mainBorderPane;
    private SwitchPane mainPane; // qeen mat la chua khoi taoj :)

    // Nút Menu
    @FXML private Button btnDashboard;
    @FXML private Button btnLiveAuctions;
    @FXML private Button btnMyInventory;
    @FXML private Button btnLogout;
    @FXML private Button btnManagement; // Nút dành cho Admin/Seller

    // Header
    @FXML private Label lblUsername;
    @FXML private TextField searchField;

    // Nút Categories
    @FXML private CheckBox chkElectronics;
    @FXML private CheckBox chkFineArts;
    @FXML private CheckBox chkVehicles;

    // --- HÀM KHỞI TẠO ---
    @FXML
    public void initialize() {
        mainPane = new SwitchPane(mainBorderPane);

        if (ClientSession.isLoggedIn()) {
            lblUsername.setText(ClientSession.getUsername());
            // TODO: Phân quyền, hiện chỉ đang ẩn ở UI, nếu gọi api seller thì bidder vẫn thấy đc
            // Lấy vai trò của người dùng
            String role = ClientSession.getRole();

            if (role == null || role.equalsIgnoreCase("BIDDER")) {
                if (btnManagement != null) {
                    btnManagement.setVisible(false);
                    btnManagement.setManaged(false);
                }
            }
        } else {
            lblUsername.setText("Khách");
            // Khách cũng không được thấy nút Management
            if (btnManagement != null) {
                btnManagement.setVisible(false);
                btnManagement.setManaged(false);
            }
        }

        handleBtnDashboard(null);
    }

    // --- HÀM TÔ MÀU NÚT ---
    private void setActiveButton(Button activeButton) {
        // Trả tất cả về giao diện mặc định
        String defaultStyle = "-fx-background-color: transparent; -fx-text-fill: #333333; -fx-alignment: CENTER_LEFT; -fx-padding: 12; -fx-font-size: 14px;";

        if (btnDashboard != null) btnDashboard.setStyle(defaultStyle);
        if (btnLiveAuctions != null) btnLiveAuctions.setStyle(defaultStyle);
        if (btnMyInventory != null) btnMyInventory.setStyle(defaultStyle);
        if (btnManagement != null) btnManagement.setStyle(defaultStyle); // Cập nhật cả nút này

        String activeStyle = "-fx-background-color: #E3B04B; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 12; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 14px;";
        if (activeButton != null) {
            activeButton.setStyle(activeStyle);
        }
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

    // TODO: Tạm chưa hoàn thành phân quền, sau dùng token chỉnh sau
    // --- CHUYỂN CẢNH THEO ROLE ---
    @FXML
    void handleBtnManagement(ActionEvent event) {
        if (!ClientSession.isLoggedIn()) return;

        String role = ClientSession.getRole();

        if ("SELLER".equalsIgnoreCase(role)) {
            mainPane.loadView("SellerDashboard.fxml");
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            mainPane.loadView("AdminDashboard.fxml");
        } else {
            // Log lỗi hoặc hiện thông báo đề phòng trường hợp lọt quyền
            System.err.println("CẢNH BÁO: Tài khoản " + ClientSession.getUsername() + " cố truy cập Management trái phép!");
            return;
        }

        setActiveButton(btnManagement);
    }

    @FXML
    void handleBtnMyInventory(ActionEvent event) {
        mainPane.loadView("ViewMyInventory.fxml");
        setActiveButton(btnMyInventory);
    }

    @FXML
    void handleLogout(ActionEvent event) throws IOException {
        // Xóa thông tin đăng nhập trước khi văng ra ngoài
        ClientSession.clear();
        SceneController.switchScene(event, "AuctionMenu.fxml"); // Hoặc Login.fxml tuỳ bạn
    }

    // --- SỰ KIỆN CATEGORIES (Lọc sản phẩm) ---
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

        setActiveButton(null);
    }
}