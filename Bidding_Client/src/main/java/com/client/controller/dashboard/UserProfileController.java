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
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.util.Duration;
import java.nio.file.Files;
import java.util.Base64;
import java.io.File;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Modality;

public class UserProfileController {

    @FXML private Label lblHeaderName;
    @FXML private Label lblRoleTag;
    @FXML private ImageView avatarView;

    @FXML private VBox vboxBidder;
    @FXML private VBox vboxSeller;
    @FXML private VBox vboxAdmin;

    @FXML private TextField txtUsername;
    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtAddress;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtOldPassword;
    @FXML private TextField txtWalletBalance;
    @FXML private TextField txtRoleLevel;
    @FXML private TextField txtLastLoginIp;
    @FXML private TextField txtCreditCardInfo;
    @FXML private TextField txtShopName;
    @FXML private TextField txtBankAccountNumber;

    @FXML private Label lblRating;
    @FXML private Label lblReviews;
    @FXML private Label lblVerified;

    @FXML private Label lblStatus;

    @FXML private HBox upgradeToSellerBox;
    @FXML private Button upgradeButton;
    @FXML private Button btnDepositMoney;
    @FXML private HBox mainContentBox;

    private final AuthNetwork authNetwork = new AuthNetwork();
    private final Gson gson = new Gson();

    private String currentUserRole = "BIDDER";
    private BaseProfileUpdateDTO currentProfile;
    private String loggedInUsername = "";

    @FXML
    public void initialize() {
        hideAllRoleSections();

        if (avatarView != null) {
            Circle clip = new Circle(70, 70, 70);
            avatarView.setClip(clip);
        }

        if (upgradeButton != null) {
            upgradeButton.setOnAction(this::handleRequestSeller);
        }

        loggedInUsername = ClientSession.getUsername();

        if (loggedInUsername == null || loggedInUsername.trim().isEmpty()) {
            if(lblStatus != null) {
                lblStatus.setText("Please log in to continue.");
                lblStatus.setStyle("-fx-text-fill: #EF4444;");
            }
            return;
        }
        loadUserDataFromAPI();

        if(mainContentBox != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(800), mainContentBox);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);

            TranslateTransition translate = new TranslateTransition(Duration.millis(800), mainContentBox);
            translate.setFromY(40);
            translate.setToY(0);

            ParallelTransition transition = new ParallelTransition(fade, translate);
            transition.play();
        }
    }

    private void hideAllRoleSections() {
        if(vboxBidder != null) { vboxBidder.setVisible(false); vboxBidder.setManaged(false); }
        if(vboxSeller != null) { vboxSeller.setVisible(false); vboxSeller.setManaged(false); }
        if(vboxAdmin != null)  { vboxAdmin.setVisible(false);  vboxAdmin.setManaged(false); }
    }

    private void loadUserDataFromAPI() {
        if (lblStatus != null) {
            lblStatus.setText("Loading...");
            lblStatus.setStyle("-fx-text-fill: #D4AF37;");
        }

        authNetwork.getUserProfile(loggedInUsername).thenAccept(response -> {
            Platform.runLater(() -> {
                if ("SUCCESS".equals(response.getStatus())) {
                    if (lblStatus != null) lblStatus.setText("");

                    try {
                        JsonElement dataElement = gson.toJsonTree(response.getData());
                        JsonObject jsonData = dataElement.getAsJsonObject();

                        System.out.println("=== DATA TỪ SERVER CHÍNH XÁC LÀ: " + jsonData.toString());

                        String role = jsonData.has("role") ? jsonData.get("role").getAsString() : "BIDDER";
                        currentUserRole = role.toUpperCase();

                        BaseProfileUpdateDTO profile = parseProfileByRole(jsonData, currentUserRole);
                        currentProfile = profile;

                        if (txtFullName != null) txtFullName.setText(profile.getFullName() != null ? profile.getFullName() : "");
                        if (txtEmail != null) txtEmail.setText(profile.getEmail() != null ? profile.getEmail() : "");
                        if (txtPhone != null) txtPhone.setText(profile.getPhoneNumber() != null ? profile.getPhoneNumber() : "");
                        if (txtAddress != null) txtAddress.setText(profile.getAddress() != null ? profile.getAddress() : "");
                        if (txtUsername != null) txtUsername.setText(loggedInUsername);

                        if (lblHeaderName != null) lblHeaderName.setText(profile.getFullName());

                        if (txtWalletBalance != null && jsonData.has("walletBalance") && !jsonData.get("walletBalance").isJsonNull()) {
                            double walletBalance = jsonData.get("walletBalance").getAsDouble();
                            txtWalletBalance.setText(String.format("%,.0f VNĐ", walletBalance));
                        }

                        // ==========================================
                        // PHẦN LOAD AVATAR ĐÃ ĐƯỢC CHUẨN HOÁ GỌN GÀNG
                        // ==========================================
                        if (jsonData.has("avatarUrl") && !jsonData.get("avatarUrl").isJsonNull()) {
                            String avatarUrl = jsonData.get("avatarUrl").getAsString();
                            try {
                                Image imageToDisplay = null;

                                // ---Xử lý URL trả về từ Server ---
                                if (avatarUrl.startsWith("/api/images/")) {

                                    String fullServerUrl = "http://localhost:8080" + avatarUrl;

                                    // Tham số 'true' giúp ảnh load ngầm (background loading) không làm đơ giao diện
                                    imageToDisplay = new Image(fullServerUrl, true);
                                }

                                else {
                                    java.io.File localFile = new java.io.File(avatarUrl);
                                    if (localFile.exists()) {
                                        imageToDisplay = new Image(localFile.toURI().toString(), true);
                                    }
                                    if (imageToDisplay == null) {
                                        String resourcePath = avatarUrl.startsWith("/") ? avatarUrl : "/images/" + avatarUrl;
                                        java.net.URL resourceUrl = getClass().getResource(resourcePath);
                                        if (resourceUrl != null) {
                                            imageToDisplay = new Image(resourceUrl.toExternalForm(), true);
                                        }
                                    }
                                }

                                // Ảnh mặc định nếu không load được gì
                                if (imageToDisplay == null || imageToDisplay.isError()) {
                                    java.net.URL defaultUrl = getClass().getResource("/images/shopkeeper.png");
                                    if (defaultUrl != null) {
                                        imageToDisplay = new Image(defaultUrl.toExternalForm(), true);
                                    }
                                }

                                if (avatarView != null && imageToDisplay != null) {
                                    avatarView.setImage(imageToDisplay);
                                }
                            } catch (Exception e) {
                                System.err.println("Lỗi quét ảnh Profile: " + e.getMessage());
                            }
                        }

                        setupUIByRoleStrategy(profile, role);
                    } catch (Exception e) {
                        if (lblStatus != null) {
                            lblStatus.setText("Data parsing error!");
                            lblStatus.setStyle("-fx-text-fill: #EF4444;");
                        }
                        e.printStackTrace();
                    }

                } else {
                    if (lblStatus != null) {
                        lblStatus.setText("Failed to load data: " + response.getMessage());
                        lblStatus.setStyle("-fx-text-fill: #EF4444;");
                    }
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                if (lblStatus != null) {
                    lblStatus.setText("Server connection failed!");
                    lblStatus.setStyle("-fx-text-fill: #EF4444;");
                }
            });
            return null;
        });
    }

    private void loadDefaultAvatar() {
        Platform.runLater(() -> {
            try {
                java.net.URL defaultUrl = getClass().getResource("/images/shopkeeper.png");
                if (defaultUrl != null && avatarView != null) {
                    avatarView.setImage(new Image(defaultUrl.toExternalForm(), true));
                }
            } catch (Exception ignored) {}
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
        }
        catch (Exception ignored) {}
        return 0L;
    }

    private String getStringValue(JsonObject jsonObject, String key) {
        if (jsonObject.has(key) && !jsonObject.get(key).isJsonNull()) {
            return jsonObject.get(key).getAsString();
        }
        return "";
    }

    private void setupUIByRoleStrategy(BaseProfileUpdateDTO profile, String role) {
        IProfileUIStrategy strategy = ProfileUIStrategyFactory.getStrategy(role);
        strategy.displayProfile(this, profile);

        if (upgradeToSellerBox != null) {
            upgradeToSellerBox.setVisible(true);
            upgradeToSellerBox.setManaged(true);
        }

        if (lblRoleTag != null) {
            lblRoleTag.setText("Role: " + role);
        }

        if ("ADMIN".equals(role)) {
            if (vboxAdmin != null) { vboxAdmin.setVisible(true); vboxAdmin.setManaged(true); }
            if (lblRoleTag != null) lblRoleTag.setStyle("-fx-background-color: rgba(239,68,68,0.15); -fx-text-fill: #EF4444; -fx-padding: 5 15; -fx-background-radius: 20; -fx-border-color: #EF4444; -fx-border-radius: 20;");
        } else if ("SELLER".equals(role)) {
            if (vboxBidder != null) { vboxBidder.setVisible(true); vboxBidder.setManaged(true); }
            if (vboxSeller != null) { vboxSeller.setVisible(true); vboxSeller.setManaged(true); }
            if (lblRoleTag != null) lblRoleTag.setStyle("-fx-background-color: rgba(16,185,129,0.15); -fx-text-fill: #10B981; -fx-padding: 5 15; -fx-background-radius: 20; -fx-border-color: #10B981; -fx-border-radius: 20;");
        } else { // BIDDER
            if (vboxBidder != null) { vboxBidder.setVisible(true); vboxBidder.setManaged(true); }
            if (lblRoleTag != null) lblRoleTag.setStyle("-fx-background-color: rgba(59,130,246,0.15); -fx-text-fill: #3B82F6; -fx-padding: 5 15; -fx-background-radius: 20; -fx-border-color: #3B82F6; -fx-border-radius: 20;");
        }
    }

    @FXML
    public void handleUpdateProfile() {
        if (lblStatus != null) {
            lblStatus.setText("Saving changes...");
            lblStatus.setStyle("-fx-text-fill: #D4AF37;");
        }

        IProfileUIStrategy strategy = ProfileUIStrategyFactory.getStrategy(currentUserRole);
        BaseProfileUpdateDTO updateData = strategy.collectData(this);

        long id = (currentProfile != null && currentProfile.getId() > 0) ? currentProfile.getId() : ClientSession.getUserId();
        if (id > 0) {
            updateData.setId(id);
        }

        authNetwork.updateProfile(updateData).thenAccept(response -> {
            Platform.runLater(() -> {
                if ("SUCCESS".equals(response.getStatus())) {
                    if (lblStatus != null) {
                        lblStatus.setText("Profile updated successfully!");
                        lblStatus.setStyle("-fx-text-fill: #10B981;");
                    }
                    if (txtPassword != null) txtPassword.clear();
                    // Reload lại dữ liệu từ server để hiển thị thông tin mới nhất
                    // (bao gồm shopName và bankAccountNumber vừa lưu)
                    loadUserDataFromAPI();
                } else {
                    if (lblStatus != null) {
                        lblStatus.setText("Update failed: " + response.getMessage());
                        lblStatus.setStyle("-fx-text-fill: #EF4444;");
                    }
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                if (lblStatus != null) {
                    lblStatus.setText("Lost connection to server!"); // [ĐÃ SỬA]
                    lblStatus.setStyle("-fx-text-fill: #EF4444;");
                }
            });
            return null;
        });
    }

    @FXML
    public void handleChangePassword() {
        String oldPass = txtOldPassword != null ? txtOldPassword.getText() : "";
        String newPass = txtPassword != null ? txtPassword.getText() : "";

        if (oldPass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Information", "Please enter your current password.");
            return;
        }
        if (newPass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Information", "Please enter your new password.");
            return;
        }
        if (newPass.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Weak Password", "The new password must be at least 6 characters long.");
            return;
        }

        if (lblStatus != null) {
            lblStatus.setText("Updating password...");
            lblStatus.setStyle("-fx-text-fill: #D4AF37;");
        }

        authNetwork.changePassword(oldPass, newPass).thenAccept(response -> {
            Platform.runLater(() -> {
                if ("SUCCESS".equals(response.getStatus())) {
                    if (lblStatus != null) {
                        lblStatus.setText("Password updated successfully!");
                        lblStatus.setStyle("-fx-text-fill: #10B981;");
                    }
                    if (txtOldPassword != null) txtOldPassword.clear();
                    if (txtPassword != null) txtPassword.clear();
                } else {
                    if (lblStatus != null) {
                        lblStatus.setText("Update password failed: " + response.getMessage());
                        lblStatus.setStyle("-fx-text-fill: #EF4444;");
                    }
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                if (lblStatus != null) {
                    lblStatus.setText("Lost connection while updating password!");
                    lblStatus.setStyle("-fx-text-fill: #EF4444;");
                }
            });
            return null;
        });
    }

    @FXML
    public void handleRequestSeller(ActionEvent actionEvent) {
        if (upgradeButton != null) upgradeButton.setDisable(true);
        if (lblStatus != null) {
            lblStatus.setText("Sending request...");
            lblStatus.setStyle("-fx-text-fill: #D4AF37;");
        }

        authNetwork.requestUpgradeToSeller(loggedInUsername).thenAccept(response -> {
            Platform.runLater(() -> {
                if ("SUCCESS".equals(response.getStatus())) {
                    if (lblStatus != null) {
                        lblStatus.setText("Request successfully sent, waiting for Admin approval!");
                        lblStatus.setStyle("-fx-text-fill: #10B981;");
                    }
                } else {
                    if (lblStatus != null) {
                        lblStatus.setText("Request failed: " + response.getMessage());
                        lblStatus.setStyle("-fx-text-fill: #EF4444;");
                    }
                    if (upgradeButton != null) upgradeButton.setDisable(false);
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                if (lblStatus != null) {
                    lblStatus.setText("Lost connection while sending request!");
                    lblStatus.setStyle("-fx-text-fill: #EF4444;");
                }
                if (upgradeButton != null) upgradeButton.setDisable(false);
            });
            return null;
        });
    }

    @FXML
    public void handleOpenDepositDialog() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/DepositPopup.fxml"));
            javafx.scene.Parent depositRoot = loader.load();

            javafx.stage.Stage depositStage = new javafx.stage.Stage();
            depositStage.setTitle("Deposit");
            depositStage.setScene(new javafx.scene.Scene(depositRoot, 550, 650));
            depositStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            depositStage.setResizable(false);

            depositStage.showAndWait();
            loadUserDataFromAPI();

        } catch (Exception e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Error",
                    "Unable to open deposit dialog:" + e.getMessage());
        }
    }

    @FXML
    public void handleChangeAvatar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose your profile image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File selectedFile = fileChooser.showOpenDialog(avatarView.getScene().getWindow());

        if (selectedFile != null) {
            if (selectedFile.length() > 2 * 1024 * 1024) {
                showAlert(Alert.AlertType.WARNING, "Warning ", "Image size too large. Please select an image under 2MB.");
                return;
            }

            try {
                //Hiển thị tạm thời trên giao diện cho người dùng thấy
                Image newAvatar = new Image(selectedFile.toURI().toString());
                if (avatarView != null) avatarView.setImage(newAvatar);

                //Dùng File Builder đọc file và chuyển thành Base64
                byte[] fileContent = Files.readAllBytes(selectedFile.toPath());
                String base64String = Base64.getEncoder().encodeToString(fileContent);

                uploadAvatarToServer(base64String);

            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", " Failed to upload image: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleViewTransferHistory(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TransferHistoryPopup.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Transaction History");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to open transaction history window: " + e.getMessage());
        }
    }

    private void uploadAvatarToServer(String base64Image) {
        if (lblStatus != null) {
            lblStatus.setText("Updating Image...");
            lblStatus.setStyle("-fx-text-fill: #D4AF37;");
        }
        authNetwork.updateAvatar(loggedInUsername, base64Image).thenAccept(response -> {
            Platform.runLater(() -> {
                if ("SUCCESS".equals(response.getStatus())) {
                    if (lblStatus != null) {
                        lblStatus.setText("Update avatar successfully!");
                        lblStatus.setStyle("-fx-text-fill: #10B981;");
                    }
                } else {
                    if (lblStatus != null) {
                        lblStatus.setText("Failed to updae avatar: " + response.getMessage());
                        lblStatus.setStyle("-fx-text-fill: #EF4444;");
                    }
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                if (lblStatus != null) {
                    lblStatus.setText("Lost connection while updating avatar!");
                    lblStatus.setStyle("-fx-text-fill: #EF4444;");
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