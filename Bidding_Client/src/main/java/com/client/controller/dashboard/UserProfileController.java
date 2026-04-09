package com.client.controller.dashboard;

import com.client.network.AuthNetwork;
import com.google.gson.Gson;
import com.shared.dto.BaseProfileUpdateDTO;
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

                    try {
                        String jsonData = gson.toJson(response.getData());

                        BaseProfileUpdateDTO profile = gson.fromJson(jsonData, BaseProfileUpdateDTO.class);

                        // Điền dữ liệu chung vào Form
                        txtFullName.setText(profile.getFullName());
                        txtEmail.setText(profile.getEmail());
                        txtPhone.setText(profile.getPhoneNumber());
                        txtAddress.setText(profile.getAddress());

                        lblHeaderName.setText(profile.getFullName());
                        lblRoleTag.setText("Vai trò: " + profile.getRole());

                        // Cấu hình form hiển thị theo Role
                        setupUIByRole(profile);
                    } catch (Exception e) {
                        lblStatus.setText("Lỗi parse dữ liệu từ Server!");
                        lblStatus.setStyle("-fx-text-fill: #dc3545;");
                        e.printStackTrace();
                    }

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

    // N chắc sa cũng thay dần Factory :)
    private void setupUIByRole(BaseProfileUpdateDTO profile) {
        String role = profile.getRole() != null ? profile.getRole().toUpperCase() : "BIDDER";

        if ("ADMIN".equals(role)) {
            vboxAdmin.setVisible(true); vboxAdmin.setManaged(true);
            lblRoleTag.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10;");

            // Lấy RoleLevel thật từ DB nếu có
            if (txtRoleLevel != null) {
                txtRoleLevel.setText(profile.getRoleLevel() != null ? profile.getRoleLevel() : "N/A");
            }

        } else if ("SELLER".equals(role)) {
            vboxBidder.setVisible(true); vboxBidder.setManaged(true);
            vboxSeller.setVisible(true); vboxSeller.setManaged(true);
            lblRoleTag.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10;");

            if (profile.getWalletBalance() != null) {
                txtWalletBalance.setText(String.format("%,.0f VNĐ", profile.getWalletBalance().doubleValue()));
            }

        } else { // BIDDER
            vboxBidder.setVisible(true); vboxBidder.setManaged(true);
            lblRoleTag.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10;");

            if (profile.getWalletBalance() != null) {
                txtWalletBalance.setText(String.format("%,.0f VNĐ", profile.getWalletBalance().doubleValue()));
            }
        }
    }

    @FXML
    public void handleUpdateProfile() {
        lblStatus.setText("Đang lưu thay đổi...");
        lblStatus.setStyle("-fx-text-fill: #E3B04B;");

        com.shared.dto.BaseProfileUpdateDTO updateData = new BaseProfileUpdateDTO() {
        };
        updateData.setFullName(txtFullName.getText()); // Bổ sung setFullName
        updateData.setEmail(txtEmail.getText());
        updateData.setPhoneNumber(txtPhone.getText());
        updateData.setAddress(txtAddress.getText());

        // Gọi API cập nhật (Giữ nguyên cú pháp authNetwork của bạn)

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