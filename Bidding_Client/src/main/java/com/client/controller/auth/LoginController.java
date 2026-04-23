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

import com.google.gson.Gson;
import javafx.application.Platform;

import com.shared.dto.*;

public class LoginController {

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
            showMessage("Vui lòng nhập đầy đủ Tài khoản và Mật khẩu!", Color.web("#e74c3c")); // Hiện chữ đỏ
            return; // Dừng lại, không chạy tiếp
        }

        // GỌI XUỐNG SERVICE & DATABASE (Sẽ thực hiện ở bước sau)
        // User loggedInUser = AuthService.login(username, password);

        // Gói dữ liệu
		LoginRequestDTO loginData = new LoginRequestDTO(username, password);

        Gson gson = new Gson();
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
                            ex.printStackTrace();
                            ClientSession.clear();
                        }
                    }
                    lblMessage.setText("Đang vào Dashboard...");
                    // Route to appropriate dashboard based on user role
                    String role = ClientSession.getRole();
                    String targetScene = "Dashboard.fxml"; // Mặc định, các sesion kia cho vào chức năng sau

                    SceneController.switchScene(event, targetScene);
                } else {
                    lblMessage.setText(res.getMessage());
                }
            });
        });
    }


    //   CHUYỂN SANG TRANG ĐĂNG KÝ

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