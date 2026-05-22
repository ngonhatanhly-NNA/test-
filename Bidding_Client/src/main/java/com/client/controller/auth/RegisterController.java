package com.client.controller.auth;

import com.client.network.AuthNetwork;
import com.client.util.*;
import com.shared.network.*;
import com.shared.dto.*;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

public class RegisterController {

    // Liên kết với các ô nhập liệu bên FXML
    @FXML private TextField txtUsername;
    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;

    // Label dùng để in ra thông báo lỗi hoặc thành công
    @FXML private Label lblMessage;

    // Gọi đến network
    private AuthNetwork authNetwork = new AuthNetwork();

    // Hàm khởi tạo để bắt sự kiện Enter
    @FXML
    public void initialize() {
        // Gắn sự kiện Enter cho tất cả các ô nhập liệu
        txtUsername.setOnAction(this::handleRegister);
        txtFullName.setOnAction(this::handleRegister);
        txtEmail.setOnAction(this::handleRegister);
        txtPassword.setOnAction(this::handleRegister);
        txtConfirmPassword.setOnAction(this::handleRegister);
    }

    // XỬ LÝ KHI NGƯỜI DÙNG BẤM NÚT ĐĂNG KÝ, XỬ LÍ GIAO DIỆN
    @FXML
    public void handleRegister(ActionEvent event) {
        // Lấy dữ liệu từ giao diện Kiểm tra và liên kết với DB
        String username = txtUsername.getText().trim();
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText();

        // Confirm thôi
        String confirmPass = txtConfirmPassword.getText();

        // (Kiểm tra dữ liệu đầu vào - In thông báo bằng tiếng Anh)
        if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showMessage("Please fill in all required fields (*)", Color.web("#e74c3c")); // Màu đỏ
            return; // Dừng lại không làm tiếp
        }
        if (!password.equals(confirmPass)) {
            showMessage("Passwords do not match!", Color.web("#e74c3c"));
            return;
        }
        if (password.length() < 6) {
            showMessage("Password must be at least 6 characters!", Color.web("#e74c3c"));
            return; // Giới hạn độ dài mật khẩu
        }

        // [Connect voi DB] ném vào dto để bắt đầu dịch data sang dạng Json, Đóng gói object truyền đi
        RegisterRequestDTO dto = new RegisterRequestDTO(username, password, email, fullName);

        showMessage("Sending registration request...", Color.web("#f39c12"));

        // kết nối HTTP đến cổng 7070, gửi jsonBody qua HttpClient
        authNetwork.register(dto)
                .thenAccept(res -> {
                    Platform.runLater(() -> {
                        if ("SUCCESS".equals(res.getStatus())) {
                            showMessage("Registration successful!", Color.GREEN);
                            SceneController.switchScene(event, "Login.fxml");
                        } else {
                            // Lỗi do server trả về, có thể server còn cứng tiếng Việt
                            // Tạm thời nếu server trả message tiếng Việt thì UI vẫn hiện lên
                            // (Muốn sạch hẳn thì phải sang tầng Server sửa các AuthException)
                            showMessage(res.getMessage(), Color.RED);
                        }
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> showMessage("Failed to connect to Server!", Color.RED));
                    return null;
                });
    }

    // Các hàm chuyển cảnh và xử lí bên UI
    @FXML
    public void switchLogin (ActionEvent event) {
        SceneController.switchScene(event, "Login.fxml");
    }

    // Hàm phụ trợ đổi màu và nội dung chữ
    @FXML
    private void showMessage(String message, Color color) {
        lblMessage.setText(message);
        lblMessage.setTextFill(color);
    }
}