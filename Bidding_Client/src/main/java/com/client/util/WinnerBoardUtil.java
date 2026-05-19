package com.client.util;

import com.shared.dto.AuctionWinnerDTO;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.math.BigDecimal;

public class WinnerBoardUtil {

    /**
     * Hiển thị bảng chúc mừng chiến thắng VIP 
     * @param winnerData Dữ liệu người chiến thắng
     * @param currentUserId ID của User đang đăng nhập trên Client
     */
    public static void showWinnerBoard(AuctionWinnerDTO winnerData, long currentUserId) {
        Platform.runLater(() -> {
            // 1. Tạo cửa sổ vô hình
            Stage dialog = new Stage();
            dialog.initStyle(StageStyle.TRANSPARENT);
            dialog.initModality(Modality.APPLICATION_MODAL); // Bắt buộc tương tác xong mới cho bấm chỗ khác

            // 2. Kiểm tra xem user hiện tại có phải người thắng không để đổi màu CSS
            boolean isWinner = (currentUserId == winnerData.getWinnerId());
            String bgColor = isWinner ? "linear-gradient(to bottom right, #f1c40f, #f39c12)" // Vàng hoàng kim
                                      : "linear-gradient(to bottom right, #ecf0f1, #bdc3c7)"; // Bạc/Xám
            
            String textColor = isWinner ? "#ffffff" : "#2c3e50";
            
            // 3. Khung chứa chính (VBox) và CSS
            VBox root = new VBox(15);
            root.setAlignment(Pos.CENTER);
            root.setStyle("-fx-background-color: " + bgColor + "; " +
                          "-fx-padding: 40; " +
                          "-fx-background-radius: 20; " +
                          "-fx-border-radius: 20; " +
                          "-fx-border-color: #d35400; " +
                          "-fx-border-width: 4; " +
                          "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 25, 0, 0, 10);");

            // 4. Các nhãn (Label)
            Label lblTitle = new Label(isWinner ? "🎉 CHÚC MỪNG BẠN ĐÃ CHIẾN THẮNG 🎉" : "KẾT QUẢ ĐẤU GIÁ");
            lblTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");

            Label lblItem = new Label("Sản phẩm: " + winnerData.getItemName());
            lblItem.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");

            Label lblWinner = new Label(isWinner ? "Tài sản này đã thuộc về bạn!" : "🏆 Người mua: " + winnerData.getWinnerName());
            lblWinner.setStyle("-fx-font-size: 16px; -fx-text-fill: " + textColor + ";");

            Label lblPrice = new Label(formatMoney(winnerData.getWinningPrice()) + " VNĐ");
            lblPrice.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #e74c3c; " + 
                              "-fx-background-color: #ffffff; -fx-padding: 10 20; -fx-background-radius: 10;");

            // 5. Nút đóng
            Button btnClose = new Button("Tuyệt vời!");
            btnClose.setStyle("-fx-background-color: " + (isWinner ? "#e74c3c" : "#34495e") + "; " +
                              "-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; " +
                              "-fx-padding: 10 30; -fx-background-radius: 10; -fx-cursor: hand;");
            
            btnClose.setOnMouseEntered(e -> btnClose.setOpacity(0.8));
            btnClose.setOnMouseExited(e -> btnClose.setOpacity(1.0));
            btnClose.setOnAction(e -> dialog.close());

            // Ráp các thành phần
            root.getChildren().addAll(lblTitle, lblItem, lblWinner, lblPrice, btnClose);

            // 6. Cài đặt Scene trong suốt
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            dialog.setScene(scene);
            
            // 7. Hiển thị với hiệu ứng Fade In
            root.setOpacity(0);
            dialog.show();
            
            FadeTransition ft = new FadeTransition(Duration.millis(500), root);
            ft.setToValue(1.0);
            ft.play();
        });
    }

    private static String formatMoney(BigDecimal v) {
        return v == null ? "0" : v.toPlainString();
    }
}