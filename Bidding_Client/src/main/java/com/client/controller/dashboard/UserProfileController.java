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

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

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
    @FXML private TextField txtWalletBalance;
    @FXML private TextField txtRoleLevel;
    @FXML private TextField txtLastLoginIp;
    @FXML private TextField txtCreditCardInfo;
    @FXML private TextField txtShopName;
    @FXML private TextField txtBankAccountNumber;

    @FXML private Label lblStatus;

    @FXML private HBox upgradeToSellerBox;
    @FXML private Button upgradeButton; 
    @FXML private Button btnDepositMoney; 
	@FXML
	private VBox mainContentBox;

    private final AuthNetwork authNetwork = new AuthNetwork();
    private final Gson gson = new Gson();

    private String currentUserRole = "BIDDER";
    private BaseProfileUpdateDTO currentProfile;

    private String loggedInUsername = "";

    @FXML
    public void initialize() {
        hideAllRoleSections();

        if (avatarView != null) {
            Circle clip = new Circle(105, 105, 105); 
            avatarView.setClip(clip);
        }
        
        if (upgradeButton != null) {
            upgradeButton.setOnAction(this::handleRequestSeller);
        }

        loggedInUsername = ClientSession.getUsername();

        if (loggedInUsername == null || loggedInUsername.trim().isEmpty()) {
            if(lblStatus != null) {
                lblStatus.setText("Bạn chưa đăng nhập");
                lblStatus.setStyle("-fx-text-fill: #dc3545;");
            }
            return;
        }
        loadUserDataFromAPI();
		
		// Tạo hiệu ứng Fade (mờ ảo hiện dần)
		FadeTransition fade = new FadeTransition(Duration.millis(800), mainContentBox);
		fade.setFromValue(0.0);
		fade.setToValue(1.0);

		// Tạo hiệu ứng trượt nhẹ từ dưới lên
		TranslateTransition translate = new TranslateTransition(Duration.millis(800), mainContentBox);
		translate.setFromY(40);
		translate.setToY(0);

		// Chạy song song cả hai hiệu ứng khi vừa mở
		ParallelTransition transition = new ParallelTransition(fade, translate);
		transition.play();
    }

    private void hideAllRoleSections() {
        if(vboxBidder != null) { vboxBidder.setVisible(false); vboxBidder.setManaged(false); }
        if(vboxSeller != null) { vboxSeller.setVisible(false); vboxSeller.setManaged(false); }
        if(vboxAdmin != null)  { vboxAdmin.setVisible(false);  vboxAdmin.setManaged(false); }
    }

    private void loadUserDataFromAPI() {
        if (lblStatus != null) {
            lblStatus.setText("Loading...");
            lblStatus.setStyle("-fx-text-fill: #E3B04B;");
        }

        authNetwork.getUserProfile(loggedInUsername).thenAccept(response -> {
            Platform.runLater(() -> {
                if ("SUCCESS".equals(response.getStatus())) {
                    if (lblStatus != null) lblStatus.setText("");

                    try {
                        JsonElement dataElement = gson.toJsonTree(response.getData());
                        JsonObject jsonData = dataElement.getAsJsonObject();

                        String role = jsonData.has("role") ? jsonData.get("role").getAsString() : "BIDDER";
                        currentUserRole = role.toUpperCase();

                        BaseProfileUpdateDTO profile = parseProfileByRole(jsonData, currentUserRole);
                        currentProfile = profile;

                        if (txtFullName != null) txtFullName.setText(profile.getFullName() != null ? profile.getFullName() : "");
                        if (txtEmail != null) txtEmail.setText(profile.getEmail() != null ? profile.getEmail() : "");
                        if (txtPhone != null) txtPhone.setText(profile.getPhoneNumber() != null ? profile.getPhoneNumber() : "");
                        if (txtAddress != null) txtAddress.setText(profile.getAddress() != null ? profile.getAddress() : "");

                        if (lblHeaderName != null) lblHeaderName.setText(profile.getFullName());
                        if (lblRoleTag != null) lblRoleTag.setText("Vai trò: " + role);

                        if (txtWalletBalance != null && jsonData.has("walletBalance") && !jsonData.get("walletBalance").isJsonNull()) {
                            double walletBalance = jsonData.get("walletBalance").getAsDouble();
                            txtWalletBalance.setText(String.format("%,.0f VNĐ", walletBalance));
                        }
                        
                        // Load Avatar URL từ Server
						if (jsonData.has("avatarUrl") && !jsonData.get("avatarUrl").isJsonNull()) {
							String avatarUrl = jsonData.get("avatarUrl").getAsString();
							
							try {
								Image imageToDisplay = null;

								// --- LỚP 1: QUÉT FILE VẬT LÝ TRÊN MÁY (Giống Item) ---
								java.io.File localFile = new java.io.File(avatarUrl);
								if (localFile.exists()) {
									imageToDisplay = new Image(localFile.toURI().toString(), true);
								} 
								
								// --- LỚP 2: QUÉT TRONG THƯ MỤC RESOURCES (Giống Item) ---
								if (imageToDisplay == null) {
									// Chuẩn hóa đường dẫn resources
									String resourcePath = avatarUrl.startsWith("/") ? avatarUrl : "/images/" + avatarUrl;
									java.net.URL resourceUrl = getClass().getResource(resourcePath);
									if (resourceUrl != null) {
										imageToDisplay = new Image(resourceUrl.toExternalForm(), true);
									}
								}

								// --- LỚP 3: ẢNH MẶC ĐỊNH (Nếu cả 2 cách trên đều tịt) ---
								if (imageToDisplay == null) {
									java.net.URL defaultUrl = getClass().getResource("/images/shopkeeper.png");
									if (defaultUrl != null) {
										imageToDisplay = new Image(defaultUrl.toExternalForm(), true);
									}
								}

								// Hiển thị lên ImageView
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
                            lblStatus.setText("Lỗi parse dữ liệu từ Server!");
                            lblStatus.setStyle("-fx-text-fill: #dc3545;");
                        }
                        e.printStackTrace();
                    }

                } else {
                    if (lblStatus != null) {
                        lblStatus.setText("Lỗi lấy dữ liệu: " + response.getMessage());
                        lblStatus.setStyle("-fx-text-fill: #dc3545;");
                    }
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                if (lblStatus != null) {
                    lblStatus.setText("Lỗi kết nối Server!");
                    lblStatus.setStyle("-fx-text-fill: #dc3545;");
                }
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
        } catch (Exception ignored) {}
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

        if ("ADMIN".equals(role)) {
            if (vboxAdmin != null) { vboxAdmin.setVisible(true); vboxAdmin.setManaged(true); }
            if (lblRoleTag != null) lblRoleTag.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10;");

        } else if ("SELLER".equals(role)) {
            if (vboxBidder != null) { vboxBidder.setVisible(true); vboxBidder.setManaged(true); }
            if (vboxSeller != null) { vboxSeller.setVisible(true); vboxSeller.setManaged(true); }
            if (lblRoleTag != null) lblRoleTag.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10;");

        } else { // BIDDER
            if (vboxBidder != null) { vboxBidder.setVisible(true); vboxBidder.setManaged(true); }
            if (lblRoleTag != null) lblRoleTag.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 10;");
        }
    }

    @FXML
    public void handleUpdateProfile() {
        if (lblStatus != null) {
            lblStatus.setText("Đang lưu thay đổi...");
            lblStatus.setStyle("-fx-text-fill: #E3B04B;");
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
                        lblStatus.setText("Cập nhật thông tin thành công!");
                        lblStatus.setStyle("-fx-text-fill: #28a745;");
                    }
                    if (lblHeaderName != null && txtFullName != null) lblHeaderName.setText(txtFullName.getText());
                    if (txtPassword != null) txtPassword.clear();
                } else {
                    if (lblStatus != null) {
                        lblStatus.setText("Cập nhật thất bại: " + response.getMessage());
                        lblStatus.setStyle("-fx-text-fill: #dc3545;");
                    }
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                if (lblStatus != null) {
                    lblStatus.setText("Mất kết nối tới Server!");
                    lblStatus.setStyle("-fx-text-fill: #dc3545;");
                }
            });
            return null;
        });
    }

    @FXML
    public void handleRequestSeller(ActionEvent actionEvent) {
        if (upgradeButton != null) {
            upgradeButton.setDisable(true);
        }
        if (lblStatus != null) {
            lblStatus.setText("Đang gửi yêu cầu...");
            lblStatus.setStyle("-fx-text-fill: #E3B04B;");
        }

        authNetwork.requestUpgradeToSeller(loggedInUsername).thenAccept(response -> {
            Platform.runLater(() -> {
                if ("SUCCESS".equals(response.getStatus())) {
                    if (lblStatus != null) {
                        lblStatus.setText("Role của bạn đã thay đổi, vui lòng đăng nhập lại.");
                        lblStatus.setStyle("-fx-text-fill: #28a745;");
                    }
                    if (upgradeButton != null) {
                        upgradeButton.setDisable(true);
                    }
                } else {
                    if (lblStatus != null) {
                        lblStatus.setText("Yêu cầu thất bại: " + response.getMessage());
                        lblStatus.setStyle("-fx-text-fill: #dc3545;");
                    }
                    if (upgradeButton != null) {
                        upgradeButton.setDisable(false);
                    }
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                if (lblStatus != null) {
                    lblStatus.setText("Lỗi kết nối Server khi gửi yêu cầu!");
                    lblStatus.setStyle("-fx-text-fill: #dc3545;");
                }
                if (upgradeButton != null) {
                    upgradeButton.setDisable(false);
                }
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
            depositStage.setTitle("Nạp Tiền");
            depositStage.setScene(new javafx.scene.Scene(depositRoot, 600, 700));
            depositStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            depositStage.setResizable(false);

            depositStage.showAndWait();
            loadUserDataFromAPI();

        } catch (Exception e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Lỗi",
                    "Không thể mở dialog nạp tiền: " + e.getMessage());
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
                showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Ảnh quá lớn, vui lòng chọn ảnh dưới 2MB");
                return;
            }

            try {
                // Hiển thị ảnh Local lên giao diện 
                Image newAvatar = new Image(selectedFile.toURI().toString());
                if (avatarView != null) avatarView.setImage(newAvatar);

                // GỬI THẲNG ĐƯỜNG DẪN Ổ CỨNG LÊN SERVER
                String imagePath = selectedFile.getAbsolutePath(); 
                uploadAvatarToServer(imagePath);

            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể thiết lập ảnh: " + e.getMessage());
            }
        }
    }
    
    private void uploadAvatarToServer(String base64Image) {
        if (lblStatus != null) {
            lblStatus.setText("Đang tải ảnh lên...");
            lblStatus.setStyle("-fx-text-fill: #E3B04B;");
        }
        
        authNetwork.updateAvatar(loggedInUsername, base64Image).thenAccept(response -> {
            Platform.runLater(() -> {
                if ("SUCCESS".equals(response.getStatus())) {
                    if (lblStatus != null) {
                        lblStatus.setText("Cập nhật ảnh đại diện thành công!");
                        lblStatus.setStyle("-fx-text-fill: #28a745;");
                    }
                } else {
                    if (lblStatus != null) {
                        lblStatus.setText("Cập nhật ảnh thất bại: " + response.getMessage());
                        lblStatus.setStyle("-fx-text-fill: #dc3545;");
                    }
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                if (lblStatus != null) {
                    lblStatus.setText("Lỗi kết nối khi tải ảnh lên!");
                    lblStatus.setStyle("-fx-text-fill: #dc3545;");
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