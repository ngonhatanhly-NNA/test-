package com.client.controller.dashboard;

import com.client.network.AuthNetwork;
import com.google.gson.Gson;
import com.shared.dto.UserProfileResponseDTO;
import com.shared.dto.UserProfileUpdateDTO;
import com.shared.network.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class UserProfileController {

    // --- Header ---
    @FXML private Label lblHeaderName;
    @FXML private Label lblRoleTag;

    // --- VBox Containers để ẩn/hiện theo Role ---
    @FXML private VBox vboxBidder;
    @FXML private VBox vboxSeller;
    @FXML private VBox vboxAdmin;

    // --- Form Fields ---
    @FXML private TextField txtUsername;
    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtAddress;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtWalletBalance;
    @FXML private TextField txtRoleLevel;

    @FXML private Label lblStatus;

    private final AuthNetwork authNetwork = new AuthNetwork();
    private final Gson gson = new Gson();

    //Temp for user đang đăng nhập
    private String loggedInUsername = "admin_team13";

    @FXML
    public void initialize() {
        hideAllRoleSections();
        loadUserDataFromAPI();
    }

    private void hideAllRoleSections() {
        if(vboxBidder != null) { vboxBidder.setVisible(false); vboxBidder.setManaged(false); }
        if(vboxSeller != null) { vboxSeller.setVisible(false); vboxSeller.setManaged(false); }
        if(vboxAdmin != null)  { vboxAdmin.setVisible(false);  vboxAdmin.setManaged(false); }
    }

    private void loadUserDataFromAPI() {
        lblStatus.setText("Đang tải dữ liệu...");
        lblStatus.setStyle("-fx-text-fill: #E3B04B;");

        // Gọi API qua mạng bất đồng bộ
        authNetwork.getUserProfile(loggedInUsername).thenAccept(response -> {

            Platform.runLater(() -> {
                if ("SUCCESS".equals(response.getStatus())) {
                    lblStatus.setText("");

                    // Parse data từ API sang DTO
                    String jsonData = gson.toJson(response.getData());
                    UserProfileResponseDTO profile = gson.fromJson(jsonData, UserProfileResponseDTO.class);

                    // Điền dữ liệu chung vào Form
                    txtUsername.setText(profile.getUsername());
                    txtFullName.setText(profile.getFullName()); // Nhớ thêm FullName vào DTO nếu cần update
                    txtEmail.setText(profile.getEmail());
                    txtPhone.setText(profile.getPhoneNumber());
                    txtAddress.setText(profile.getAddress());

                    lblHeaderName.setText(profile.getFullName());
                    lblRoleTag.setText("Vai trò: " + profile.getRole());

                    // Cấu hình form hiển thị theo Role
                    setupUIByRole(profile);

                } else {
                    lblStatus.setText("Lỗi lấy dữ liệu: " + response.getMessage());
                    lblStatus.setStyle("-fx-text-fill: #dc3545;");
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                lblStatus.setText("Lỗi kết nối Server!");
                lblStatus.setStyle("-fx-text-fill: #dc3545;");
            });
            return null;
        });
    }

    private void setupUIByRole(UserProfileResponseDTO profile) {
        String role = profile.getRole() != null ? profile.getRole().toUpperCase() : "BIDDER";

        if ("ADMIN".equals(role)) {
            vboxAdmin.setVisible(true); vboxAdmin.setManaged(true);
            lblRoleTag.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10;");
            // txtRoleLevel.setText("SUPER_ADMIN"); // Set data Admin nếu DTO trả về

        } else if ("SELLER".equals(role)) {
            vboxBidder.setVisible(true); vboxBidder.setManaged(true);
            vboxSeller.setVisible(true); vboxSeller.setManaged(true);
            lblRoleTag.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10;");
            txtWalletBalance.setText(String.format("%,.0f VNĐ", profile.getWalletBalance()));

        } else { // BIDDER
            vboxBidder.setVisible(true); vboxBidder.setManaged(true);
            lblRoleTag.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10;");
            txtWalletBalance.setText(String.format("%,.0f VNĐ", profile.getWalletBalance()));
        }
    }

    @FXML
    public void handleUpdateProfile() {
        lblStatus.setText("Đang lưu thay đổi...");
        lblStatus.setStyle("-fx-text-fill: #E3B04B;");

        // Đóng gói DTO // demo
        UserProfileUpdateDTO updateData = new UserProfileUpdateDTO(
                txtUsername.getText(),
                txtEmail.getText(),
                txtPhone.getText(),
                txtAddress.getText()
        );

        // Gọi API cập nhật
        authNetwork.updateProfile(updateData).thenAccept(response -> {
            Platform.runLater(() -> {
                if ("SUCCESS".equals(response.getStatus())) {
                    lblStatus.setText("Cập nhật thông tin thành công!");
                    lblStatus.setStyle("-fx-text-fill: #28a745;");
                    // Cập nhật lại UI header
                    lblHeaderName.setText(txtFullName.getText());
                    txtPassword.clear();
                } else {
                    lblStatus.setText("Cập nhật thất bại: " + response.getMessage());
                    lblStatus.setStyle("-fx-text-fill: #dc3545;");
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                lblStatus.setText("Mất kết nối tới Server!");
                lblStatus.setStyle("-fx-text-fill: #dc3545;");
            });
            return null;
        });
    }
}