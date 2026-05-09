package com.client.util;

import com.shared.dto.BidHistoryDTO;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BidHistoryUtil {

    /**
     * Hiển thị bảng lịch sử đặt giá
     * @param auctionId ID của phiên đấu giá
     * @param history Danh sách lịch sử lấy từ Server
     */
    public static void showBidHistoryBoard(long auctionId, List<BidHistoryDTO> history) {
        Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.setTitle("Lịch sử đấu giá chi tiết");
            stage.initModality(Modality.APPLICATION_MODAL); // Bắt buộc tương tác xong mới cho bấm chỗ khác

            VBox root = new VBox(15);
            root.setStyle("-fx-padding: 20; -fx-background-color: #f8f9fa;");

            Label header = new Label("Lịch sử phiên #" + auctionId);
            header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            root.getChildren().add(header);

            // Xử lý nếu chưa có ai đặt giá
            if (history == null || history.isEmpty()) {
                Label emptyLabel = new Label("No bid yet.");
                emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-font-style: italic;");
                root.getChildren().add(emptyLabel);
            } else {
                // In danh sách các lượt đặt giá
                for (BidHistoryDTO bh : history) {
                    String type = bh.isAutoBid() ? " [Auto]" : " [Manual]";
                    String text = String.format("%s: %s has placed %s VNĐ%s",
                            bh.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                            bh.getBidderName(),
                            formatMoney(bh.getBidAmount()),
                            type);
                    Label row = new Label(text);
                    row.setStyle("-fx-border-color: #dee2e6; -fx-border-width: 0 0 1 0; -fx-padding: 5;");
                    root.getChildren().add(row);
                }
            }

            Scene scene = new Scene(root, 450, 500);
            stage.setScene(scene);
            stage.show();
        });
    }

    // Hàm format tiền tệ (Dùng chung trong Util)
    private static String formatMoney(BigDecimal v) {
        return v == null ? "0" : v.toPlainString();
    }
}