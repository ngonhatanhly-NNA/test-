package com.client.controller.auth;

import com.client.network.AuthNetwork;
import com.client.session.ClientSession;
import com.client.util.*;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;

import com.google.gson.Gson;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shared.dto.*;
import java.io.IOException;

public class LoginController {

    // Logger
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    // Khai báo các biến khớp với fx:id bên file FXML
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMessage; // label Message, in thong bao loi
    @FXML private TextField txtPasswordVisible; // Ô Text đè lên PasswordField
    @FXML private ImageView iconEye;

    private boolean isPasswordVisible = false;
    private Image eyeOpen = new Image(getClass().getResourceAsStream("/icons/eye-open.png"));
    private Image eyeClosed = new Image(getClass().getResourceAsStream("/icons/eye-close.png"));
    // Network
    private AuthNetwork authNetwork = new AuthNetwork();

    @FXML
    public void initialize() {
        // Đồng bộ dữ liệu 2 chiều giữa ô ẩn và ô hiện
        txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());

        // Mặc định: Ẩn ô text rõ chữ, hiện ô password (dấu chấm)
        txtPasswordVisible.setVisible(false);
        txtPasswordVisible.setManaged(false);

        // Cài ảnh mặc định cho icon
        if (iconEye != null) {
            iconEye.setImage(eyeClosed);
        }

        // Bắt sự kiện Enter (gọi thẳng hàm handleLogin)
        txtUsername.setOnAction(this::handleLogin);
        txtPassword.setOnAction(this::handleLogin);
        txtPasswordVisible.setOnAction(this::handleLogin); // Cho trường hợp đang hiện password
    }

    @FXML
    public void togglePasswordVisibility(MouseEvent event) {
        isPasswordVisible = !isPasswordVisible;

        if (isPasswordVisible) {
            // Đang ẩn -> Mở lên cho thấy chữ
            txtPasswordVisible.setVisible(true);
            txtPasswordVisible.setManaged(true);
            txtPassword.setVisible(false);
            txtPassword.setManaged(false);
            iconEye.setImage(eyeOpen);

            // Focus lại con trỏ chuột
            txtPasswordVisible.requestFocus();
            txtPasswordVisible.positionCaret(txtPasswordVisible.getText().length());
        } else {
            // Đang hiện -> Giấu đi thành dấu chấm
            txtPassword.setVisible(true);
            txtPassword.setManaged(true);
            txtPasswordVisible.setVisible(false);
            txtPasswordVisible.setManaged(false);
            iconEye.setImage(eyeClosed);

            // Focus lại con trỏ chuột
            txtPassword.requestFocus();
            txtPassword.positionCaret(txtPassword.getText().length());
        }
    }

    //  XỬ LÝ KHI BẤM NÚT ĐĂNG NHẬP
    @FXML
    public void handleLogin(ActionEvent event) {
        // 1. Lấy dữ liệu và xóa khoảng trắng 2 đầu
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        // Validate dữ liệu cơ bản
        if (username.isEmpty() || password.isEmpty()) {
            // UI CHO USER (Tiếng Anh)
            showMessage("⚠️ Please enter both Username and Password!", Color.web("#e74c3c"));
            return;
        }

        // Xóa thông báo cũ
        lblMessage.setText("");

        // Gói dữ liệu
        LoginRequestDTO loginData = new LoginRequestDTO(username, password);

        Gson gson = new Gson();

        // Hiển thị thông báo loading trên UI (Tiếng Anh)
        lblMessage.setText("⏳ Checking credentials...");
        lblMessage.setTextFill(Color.web("#3B82F6")); // Màu xanh

        authNetwork.login(loginData).thenAccept(res -> {
            Platform.runLater(() -> {
                if ("SUCCESS".equals(res.getStatus())) {
                    if (res.getData() != null) {
                        try {
                            LoginResponseDTO loginResponse = gson.fromJson(
                                    gson.toJson(res.getData()),
                                    LoginResponseDTO.class);
                            if (loginResponse != null) {
                                ClientSession.setSession(loginResponse.getProfile(), loginResponse.getToken());
                            } else {
                                ClientSession.clear();
                            }
                        } catch (Exception ex) {
                            // Giữ nguyên dòng in lỗi kỹ thuật
                            ex.printStackTrace();
                            ClientSession.clear();
                        }
                    }
                    // UI CHO USER (Tiếng Anh)
                    lblMessage.setText("✓ Login successful! Redirecting to Dashboard...");
                    lblMessage.setTextFill(Color.web("#10B981")); // Màu xanh lá

                    String targetScene = "Dashboard.fxml";
                    switchToDashboard(targetScene);
                } else {
                    // Logic xử lý thông báo lỗi từ Server (Có thể message từ server vẫn là tiếng Việt,
                    // ta dùng logic Contains tiếng Việt nhưng in ra UI bằng tiếng Anh)
                    String errorMessage = res.getMessage();

                    if (errorMessage.contains("không tồn tại")) {
                        showMessage("❌ Account does not exist!", Color.web("#e74c3c"));
                    } else if (errorMessage.contains("Sai mật khẩu")) {
                        showMessage("❌ Incorrect password! Please try again.", Color.web("#e74c3c"));
                    } else if (errorMessage.contains("khóa")) {
                        showMessage("🔒 Your account has been banned!", Color.web("#EF4444"));
                    } else {
                        // Trả nguyên lỗi server nếu không khớp các case trên
                        // (hoặc nếu server đã nâng cấp trả tiếng Anh thì nó sẽ hiện luôn)
                        showMessage("❌ " + errorMessage, Color.web("#e74c3c"));
                    }

                    // Xóa trường mật khẩu
                    txtPassword.clear();
                    txtPasswordVisible.clear();
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                // LOG CHO DEV (Tiếng Việt)
                logger.error("Lỗi kết nối server: {}", ex.getMessage());
                // UI CHO USER (Tiếng Anh)
                showMessage("❌ Connection failed! Please check your network.", Color.web("#e74c3c"));
            });
            return null;
        });
    }

    // Phương thức riêng để chuyển scene, không cần ActionEvent
    private void switchToDashboard(String fxmlFile) {
        try {
            Stage stage = (Stage) lblMessage.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
			
			stage.setResizable(true); 
            stage.setMaximized(true);
        } catch (IOException e) {
            // LOG CHO DEV
            System.err.println("LỖI CHUYỂN CẢNH: Không thể tải được file " + fxmlFile);
            e.printStackTrace();
        }
    }


    // CHUYỂN SANG TRANG ĐĂNG KÝ
    @FXML
    public void switchRegister(ActionEvent event) {
        // Gọi SceneController để đổi màn hình sang file Register.fxml
        SceneController.switchScene(event, "Register.fxml");
    }


    // HIỂN THỊ THÔNG BÁO LỖI
    private void showMessage(String message, Color color) {
        lblMessage.setText(message);
        lblMessage.setTextFill(color);
    }
}