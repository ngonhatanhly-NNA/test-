package com.client.util;

import com.client.controller.dashboard.BidHistoryController;
import com.shared.dto.BidHistoryDTO;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class BidHistoryUtil {

    public static void showBidHistoryBoard(long auctionId, List<BidHistoryDTO> history) {
        Platform.runLater(() -> {
            try {
                // Đọc giao diện từ file FXML
                FXMLLoader loader = new FXMLLoader(BidHistoryUtil.class.getResource("/fxml/BidHistory.fxml"));
                Parent root = loader.load();

                // Gọi Controller ra để nhét Data vào
                BidHistoryController controller = loader.getController();
                controller.setBidHistoryData(auctionId, history);

                // Hiển thị cái bảng lên màn hình
                Stage stage = new Stage();
                stage.setTitle("Detail Bid Hítory");
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setScene(new Scene(root, 500, 650));
                stage.setResizable(false);
                stage.show();
                
            } catch (Exception e) {
                e.printStackTrace();
                ToastUtil.showError("Lỗi hệ thống: Không thể mở bảng lịch sử!");
            }
        });
    }
}