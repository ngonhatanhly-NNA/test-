package com.client.controller.auth;
import com.client.network.AuthNetwork;
import com.client.util.*;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

import com.google.gson.Gson;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javafx.application.Platform;

import com.shared.dto.*;
import com.shared.network.*;
public class LoginController {

    // Khai báo các biến khớp với fx:id bên file FXML
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblMessage; // label Message, in thong bao loi

    // Network
    private AuthNetwork authNetwork = new AuthNetwork();

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

        authNetwork.login(loginData).thenAccept(res -> {
					Platform.runLater(() -> {
						if ("SUCCESS".equals(res.getStatus())) {
							lblMessage.setText("Đang vào Dashboard...");
							// Chuyển cảnh sang Dashboard
							SceneController.switchScene(event, "Dashboard.fxml");
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