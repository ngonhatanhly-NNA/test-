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

    // GOij den network
    private AuthNetwork authNetwork = new AuthNetwork();
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

        // (Kiểm tra dữ liệu đầu vào)
        if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showMessage("Vui lòng điền đầy đủ các trường bắt buộc (*)", Color.web("#e74c3c")); // Màu đỏ
            return; // Dừng lại không làm tiếp
        }
        if (!password.equals(confirmPass)) {
            showMessage("Mật khẩu xác nhận không khớp!", Color.web("#e74c3c"));
            return;
        }
        if (password.length() < 6) {
            showMessage("Mật khẩu phải có ít nhất 6 ký tự!", Color.web("#e74c3c"));
            return; // Giới hạn độ đài mật khẩu
        }


		// [Connect voi DB] ném vào dto để bắt đầu dịch data sang dạng Json, Đóng gói object truyền đi
        RegisterRequestDTO dto = new RegisterRequestDTO(username, password, email, fullName); // default regis la Bidder, Admin se duoc tao 1 tai khoan rieng

        showMessage("Đang gửi API Đăng ký...", Color.web("#f39c12"));

        //  kết nối HTTP đến cổng 7070, giở jsonBody qua HttpClient
        authNetwork.register(dto)
                .thenAccept(res -> {
                    Platform.runLater(() -> {
                        if ("SUCCESS".equals(res.getStatus())) {
                            showMessage("Đăng ký thành công!", Color.GREEN);
                            SceneController.switchScene(event, "Login.fxml");
                        } else {
                            showMessage(res.getMessage(), Color.RED);
                        }
                    });
                })
                .exceptionally(e -> {
                    Platform.runLater(() -> showMessage("Không kết nối được tới Server!", Color.RED));
                    return null;
                });
		;
    }


      // Cac ham chuyen canh va xu li ben UI
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

