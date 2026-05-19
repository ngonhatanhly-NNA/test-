package com.client.util;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

public class ToastUtil {

    // 1. Toast Thông báo Lỗi (Màu đỏ)
    public static void showError(String message) {
        showToast(message, "#e74c3c"); 
    }

    // 2. Toast Thông báo Thành công (Màu xanh lá)
    public static void showSuccess(String message) {
        showToast(message, "#2ecc71");
    }

    // 3. Toast Thông báo Bình thường (Màu tối)
    public static void showInfo(String message) {
        showToast(message, "#34495e");
    }

    // --- HÀM LÕI XỬ LÝ GIAO DIỆN ---
    private static void showToast(String message, String bgColor) {
        Platform.runLater(() -> {
            // Tìm Cửa sổ (Window/Stage) đang được người dùng chọn (Focus)
            Stage mainStage = null;
            for (Window window : Window.getWindows()) {
                if (window instanceof Stage) {
                    mainStage = (Stage) window;
					
					if (mainStage.isFocused()){
						break;
					}
                }
            }

            if (mainStage == null) return;

            // Khởi tạo Popup (Nổi trên mọi FXML)
            Popup popup = new Popup();
            popup.setAutoFix(true);
            popup.setAutoHide(true);

            // Thiết kế Label hiển thị
            Label label = new Label(message);
            label.setStyle("-fx-background-color: " + bgColor + "; "
                         + "-fx-text-fill: white; "
                         + "-fx-padding: 12px 24px; "
                         + "-fx-background-radius: 8px; "
                         + "-fx-font-size: 14px; "
                         + "-fx-font-weight: bold; "
                         + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 5);");

            popup.getContent().add(label);

            // Tính toán để Popup luôn nằm ở CHÍNH GIỮA CẠNH DƯỚI màn hình
            Stage finalMainStage = mainStage;
            popup.setOnShown(e -> {
                popup.setX(finalMainStage.getX() + finalMainStage.getWidth() / 2 - popup.getWidth() / 2);
                popup.setY(finalMainStage.getY() + finalMainStage.getHeight() - 100); // Cách mép dưới 100px
            });

            // Hiệu ứng hiện rõ dần (Fade In)
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), label);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            // Hiệu ứng mờ dần rồi biến mất (Fade Out)
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), label);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setDelay(Duration.seconds(1.5)); // ⏱️ Tự động ẩn sau 2.5 giây

            // Luồng chạy: Hiện -> Chờ 1.5s -> Mờ -> Đóng Popup
            fadeIn.setOnFinished(e -> fadeOut.play());
            fadeOut.setOnFinished(e -> popup.hide());

            popup.show(mainStage);
            fadeIn.play();
        });
    }
}