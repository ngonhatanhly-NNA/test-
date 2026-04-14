package com.client.util;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneController {

    /**
     * Hàm chuyển cảnh cơ bản nhất (Không có animation)
     * @param event Sự kiện click chuột
     * @param fxmlFile Tên file FXML muốn chuyển tới (VD: "Dashboard.fxml")
     */
    public static void switchScene(ActionEvent event, String fxmlFile) {
        try {
            // 1. Tải file giao diện (FXML) mới
            // Nhớ đảm bảo đường dẫn "/fxml/" khớp với thư mục resources của bạn
            FXMLLoader loader = new FXMLLoader(SceneController.class.getResource("/fxml/" + fxmlFile));
            Parent root = loader.load();

            // 2. Lấy thông tin Cửa sổ (Stage) hiện tại từ cái nút vừa bị bấm
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // 3. Tạo một Cảnh (Scene) mới chứa giao diện vừa tải
            Scene scene = new Scene(root);

            // 4. Đặt Cảnh mới lên Cửa sổ và hiển thị
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.err.println("LỖI CHUYỂN CẢNH: Không thể tải được file " + fxmlFile);
            e.printStackTrace(); // In chi tiết lỗi ra Console để dễ tìm
        } catch (NullPointerException e) {
            System.err.println("LỖI ĐƯỜNG DẪN: File /fxml/" + fxmlFile + " không tồn tại!");
            e.printStackTrace();
        }
    }
}