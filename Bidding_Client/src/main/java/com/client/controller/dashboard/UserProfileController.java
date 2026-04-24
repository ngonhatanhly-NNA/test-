package com.client.controller.dashboard;

import com.client.network.AuthNetwork;
import com.client.controller.dashboard.strategy.ProfileUIStrategyFactory;
import com.client.controller.dashboard.strategy.IProfileUIStrategy;
import com.client.session.ClientSession;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.shared.dto.BaseProfileUpdateDTO;
import com.shared.dto.AdminProfileUpdateDTO;
import com.shared.dto.BidderProfileUpdateDTO;
import com.shared.dto.SellerProfileUpdateDTO;
import com.shared.network.Response;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
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
    @FXML private TextField txtLastLoginIp;
    @FXML private TextField txtCreditCardInfo;
    @FXML private TextField txtShopName;
    @FXML private TextField txtBankAccountNumber;

    @FXML private Label lblStatus;

    // --- Box và Nút nâng cấp ---
    @FXML private HBox upgradeToSellerBox;
    @FXML private Button upgradeButton; // <-- Thêm khai báo cho nút

    private final AuthNetwork authNetwork = new AuthNetwork();
    private final Gson gson = new Gson();

    private String currentUserRole = "BIDDER";
    private BaseProfileUpdateDTO currentProfile;

    // Temp for user đang đăng nhập
    private String loggedInUsername = "";

    @FXML
    public void initialize() {
        hideAllRoleSections();

        // --- GÁN SỰ KIỆN BẰNG CODE JAVA ---
        // Cách này đảm bảo sự kiện được liên kết chính xác
        if (upgradeButton != null) {
            upgradeButton.setOnAction(this::handleRequestSeller);
        }
        // ---------------------------------

        // Lấy user thật từ session (được set sau khi Login thành công)
        loggedInUsername = ClientSession.getUsername();

        if (loggedInUsername == null || loggedInUsername.trim().isEmpty()) {
            lblStatus.setText("Bạn chưa đăng nhập (không có session username)");
            lblStatus.setStyle("-fx-text-fill: #dc3545;");
            return;
        }
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

        authNetwork.getUserProfile(loggedInUsername).thenAccept(response -> {
            Platform.runLater(() -> {
                if ("SUCCESS".equals(response.getStatus())) {
                    lblStatus.setText("");

                    try {
                        JsonElement dataElement = gson.toJsonTree(response.getData());
                        JsonObject jsonData = dataElement.getAsJsonObject();

                        String role = jsonData.has("role") ? jsonData.get("role").getAsString() : "BIDDER";
                        currentUserRole = role.toUpperCase();

                        // Parse DTO theo role
                        BaseProfileUpdateDTO profile = parseProfileByRole(jsonData, currentUserRole);
                        currentProfile = profile;

                        // Điền dữ liệu chung vào Form
                        txtFullName.setText(profile.getFullName() != null ? profile.getFullName() : "");
                        txtEmail.setText(profile.getEmail() != null ? profile.getEmail() : "");
                        txtPhone.setText(profile.getPhoneNumber() != null ? profile.getPhoneNumber() : "");
                        txtAddress.setText(profile.getAddress() != null ? profile.getAddress() : "");

                        lblHeaderName.setText(profile.getFullName());
                        lblRoleTag.setText("Vai trò: " + role);

                        // Display wallet balance if available
                        if (jsonData.has("walletBalance") && !jsonData.get("walletBalance").isJsonNull()) {
                            double walletBalance = jsonData.get("walletBalance").getAsDouble();
                            txtWalletBalance.setText(String.format("%,.0f VNĐ", walletBalance));
                        }

                        // Sử dụng Strategy Pattern để setup UI
                        setupUIByRoleStrategy(profile, role);
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

    private BaseProfileUpdateDTO parseProfileByRole(JsonObject jsonData, String role) {
        long id = getLongValue(jsonData, "id");

        switch(role) {
            case "ADMIN":
                AdminProfileUpdateDTO adminProfile = new AdminProfileUpdateDTO();
                adminProfile.setId(id);
                adminProfile.setFullName(getStringValue(jsonData, "fullName"));
                adminProfile.setEmail(getStringValue(jsonData, "email"));
                adminProfile.setPhoneNumber(getStringValue(jsonData, "phoneNumber"));
                adminProfile.setAddress(getStringValue(jsonData, "address"));
                adminProfile.setRoleLevel(getStringValue(jsonData, "roleLevel"));
                adminProfile.setLastLoginIp(getStringValue(jsonData, "lastLoginIp"));
                return adminProfile;

            case "SELLER":
                SellerProfileUpdateDTO sellerProfile = new SellerProfileUpdateDTO();
                sellerProfile.setId(id);
                sellerProfile.setFullName(getStringValue(jsonData, "fullName"));
                sellerProfile.setEmail(getStringValue(jsonData, "email"));
                sellerProfile.setPhoneNumber(getStringValue(jsonData, "phoneNumber"));
                sellerProfile.setAddress(getStringValue(jsonData, "address"));
                sellerProfile.setCreditCardInfo(getStringValue(jsonData, "creditCardInfo"));
                sellerProfile.setShopName(getStringValue(jsonData, "shopName"));
                sellerProfile.setBankAccountNumber(getStringValue(jsonData, "bankAccountNumber"));
                return sellerProfile;

            case "BIDDER":
            default:
                BidderProfileUpdateDTO bidderProfile = new BidderProfileUpdateDTO();
                bidderProfile.setId(id);
                bidderProfile.setFullName(getStringValue(jsonData, "fullName"));
                bidderProfile.setEmail(getStringValue(jsonData, "email"));
                bidderProfile.setPhoneNumber(getStringValue(jsonData, "phoneNumber"));
                bidderProfile.setAddress(getStringValue(jsonData, "address"));
                bidderProfile.setCreditCardInfo(getStringValue(jsonData, "creditCardInfo"));
                return bidderProfile;
        }
    }

    private long getLongValue(JsonObject jsonObject, String key) {
        try {
            if (jsonObject.has(key) && !jsonObject.get(key).isJsonNull()) {
                return jsonObject.get(key).getAsLong();
            }
        } catch (Exception ignored) {
        }
        return 0L;
    }

    private String getStringValue(JsonObject jsonObject, String key) {
        if (jsonObject.has(key) && !jsonObject.get(key).isJsonNull()) {
            return jsonObject.get(key).getAsString();
        }
        return "";
    }

    private void setupUIByRoleStrategy(BaseProfileUpdateDTO profile, String role) {
        // Lấy strategy từ Factory
        IProfileUIStrategy strategy = ProfileUIStrategyFactory.getStrategy(role);

        // Hiển thị dữ liệu theo strategy
        strategy.displayProfile(this, profile);

        // --- LOGIC HIỂN THỊ NÚT NÂNG CẤP ---
        // Để kiểm tra giao diện, tạm thời luôn hiện nút này.
        if (upgradeToSellerBox != null) {
             upgradeToSellerBox.setVisible(true);
             upgradeToSellerBox.setManaged(true);
        }
        // --- KẾT THÚC THAY ĐỔI ---

        // Setup VBox visibility và styling
        if ("ADMIN".equals(role)) {
            vboxAdmin.setVisible(true); vboxAdmin.setManaged(true);
            lblRoleTag.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10;");

        } else if ("SELLER".equals(role)) {
            vboxBidder.setVisible(true); vboxBidder.setManaged(true);
            vboxSeller.setVisible(true); vboxSeller.setManaged(true);
            lblRoleTag.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10;");

        } else { // BIDDER
            vboxBidder.setVisible(true); vboxBidder.setManaged(true);
            lblRoleTag.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10;");
        }
    }

    @FXML
    public void handleUpdateProfile() {
        lblStatus.setText("Đang lưu thay đổi...");
        lblStatus.setStyle("-fx-text-fill: #E3B04B;");

        // Sử dụng Strategy để collect data
        IProfileUIStrategy strategy = ProfileUIStrategyFactory.getStrategy(currentUserRole);
        BaseProfileUpdateDTO updateData = strategy.collectData(this);

        // Backward-compatible: nếu backend cần id (AuthService.updateProfile), thì gửi kèm id.
        // Với backend mới dùng session, id có/không đều OK.
        long id = (currentProfile != null && currentProfile.getId() > 0) ? currentProfile.getId() : ClientSession.getUserId();
        if (id > 0) {
            updateData.setId(id);
        }

        authNetwork.updateProfile(updateData).thenAccept(response -> {
            Platform.runLater(() -> {
                if ("SUCCESS".equals(response.getStatus())) {
                    lblStatus.setText("Cập nhật thông tin thành công!");
                    lblStatus.setStyle("-fx-text-fill: #28a745;");
                    lblHeaderName.setText(txtFullName.getText());
                    txtPassword.clear();
                } else {
                    lblStatus.setText("Cập nhật thất bại: " + response.getMessage());
                    lblStatus.setStyle("-fx-text-fill: #dc3545;");
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                lblStatus.setText("Mất kết nối tới Server! (" + (ex.getMessage() != null ? ex.getMessage() : "unknown") + ")");
                lblStatus.setStyle("-fx-text-fill: #dc3545;");
            });
            return null;
        });
    }

    @FXML
    public void handleRequestSeller(ActionEvent actionEvent) {
        // Vô hiệu hóa nút để tránh người dùng nhấn nhiều lần
        if (upgradeButton != null) {
            upgradeButton.setDisable(true);
        }
        lblStatus.setText("Đang gửi yêu cầu...");
        lblStatus.setStyle("-fx-text-fill: #E3B04B;");

        // Gọi đến AuthNetwork để gửi yêu cầu
        authNetwork.requestUpgradeToSeller(loggedInUsername).thenAccept(response -> {
            // Callback này sẽ được thực thi sau khi nhận được phản hồi từ Server
            // Quan trọng: Phải chạy trên luồng của JavaFX để cập nhật UI
            Platform.runLater(() -> {
                if ("SUCCESS".equals(response.getStatus())) {
                    lblStatus.setText("Nâng cấp thành công! Đang tải lại thông tin...");
                    lblStatus.setStyle("-fx-text-fill: #28a745;");
                    // Tải lại toàn bộ dữ liệu profile để giao diện được cập nhật (hiện thêm các trường của Seller)
                    loadUserDataFromAPI();
                } else {
                    lblStatus.setText("Yêu cầu thất bại: " + response.getMessage());
                    lblStatus.setStyle("-fx-text-fill: #dc3545;");
                }
                // Bật lại nút sau khi xử lý xong (nếu cần)
                if (upgradeButton != null) {
                    upgradeButton.setDisable(false);
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                lblStatus.setText("Lỗi kết nối Server khi gửi yêu cầu!");
                lblStatus.setStyle("-fx-text-fill: #dc3545;");
                if (upgradeButton != null) {
                    upgradeButton.setDisable(false);
                }
            });
            return null;
        });
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Getters cho các TextField - dùng bởi Strategy classes
    public TextField getTxtFullName() { return txtFullName; }
    public TextField getTxtEmail() { return txtEmail; }
    public TextField getTxtPhone() { return txtPhone; }
    public TextField getTxtAddress() { return txtAddress; }
    public TextField getTxtCreditCardInfo() { return txtCreditCardInfo; }
    public TextField getTxtRoleLevel() { return txtRoleLevel; }
    public TextField getTxtLastLoginIp() { return txtLastLoginIp; }
    public TextField getTxtShopName() { return txtShopName; }
    public TextField getTxtBankAccountNumber() { return txtBankAccountNumber; }
}
