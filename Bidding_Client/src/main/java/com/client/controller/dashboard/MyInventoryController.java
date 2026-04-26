package com.client.controller.dashboard;

import com.client.network.ItemNetwork;
import com.client.session.ClientSession;
import com.shared.dto.ItemResponseDTO;
import com.shared.dto.ItemResponseDTO;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class MyInventoryController {

    private static final Logger logger = LoggerFactory.getLogger(MyInventoryController.class);

    @FXML private Button btnCreateItem;

    // Bắt cái Container từ file FXML để nhét Thẻ sản phẩm vào
    @FXML private FlowPane sellingItemsContainer;

    private final ItemNetwork itemNetwork = new ItemNetwork();

    @FXML
    public void initialize() {
        String userRole = ClientSession.getRole();

        if (!"SELLER".equals(userRole)) {
            btnCreateItem.setVisible(false);
            btnCreateItem.setManaged(false);
        } else {
            // Nếu đúng là Seller, lập tức tải danh sách sản phẩm từ kho về!
            loadMyItems();
        }
    }

    // --- HÀM 1: GỌI MẠNG ĐỂ LẤY DỮ LIỆU ---
    private void loadMyItems() {
        logger.info("Đang tải danh sách sản phẩm từ Server...");

        long sellerId = ClientSession.getUserId();
        logger.info("MyInventoryController: Current seller ID = {}", sellerId);

        itemNetwork.getMyItems(sellerId).thenAccept(items -> {
            // Có dữ liệu rồi, quay lại luồng UI để vẽ
            Platform.runLater(() -> {
                logger.info("MyInventoryController: Received {} items from server", items != null ? items.size() : 0);
                sellingItemsContainer.getChildren().clear(); // Xóa sạch đồ cũ trước khi bày đồ mới

                if (items != null && !items.isEmpty()) {
                    for (ItemResponseDTO item : items) {
                        // Nặn ra một cái Thẻ và nhét vào Container
                        VBox card = createItemCard(item);
                        sellingItemsContainer.getChildren().add(card);
                    }
                    logger.info("Đã tải và vẽ xong {} sản phẩm.", items.size());
                } else {
                    Label emptyLabel = new Label("Kho đồ của bạn đang trống. Hãy tạo sản phẩm mới!");
                    emptyLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");
                    sellingItemsContainer.getChildren().add(emptyLabel);
                }
            });
        });
    }

    // --- HÀM 2: DÙNG JAVA NẶN RA GIAO DIỆN "THẺ SẢN PHẨM" ---
    private VBox createItemCard(ItemResponseDTO item) {
        VBox card = new VBox(10); // Khoảng cách giữa các phần tử là 10
        card.setPadding(new Insets(15));
        // Thêm shadow và border cho giống thẻ Card xịn xò
        card.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");
        card.setPrefWidth(220);

        // 1. Tên sản phẩm
        Label lblName = new Label(item.getName());
        lblName.setFont(Font.font("System", FontWeight.BOLD, 16));
        lblName.setStyle("-fx-text-fill: #1E293B;");

        // 2. Loại và Giá khởi điểm
        Label lblType = new Label("Category: " + item.getType());
        lblType.setStyle("-fx-text-fill: #64748B;");

        Label lblPrice = new Label("Start Price: $" + item.getStartingPrice());
        lblPrice.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblPrice.setStyle("-fx-text-fill: #D4AF37;");

        // 3. NÚT QUAN TRỌNG NHẤT: MỞ PHIÊN ĐẤU GIÁ
        Button btnOpenAuction = new Button("Open Auction");
        btnOpenAuction.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
        btnOpenAuction.setMaxWidth(Double.MAX_VALUE); // Cho nút phình to bằng chiều ngang của thẻ

        // Đính kèm ID của sản phẩm này vào sự kiện bấm nút
        btnOpenAuction.setOnAction(e -> openCreateAuctionPopup(item.getId()));

        card.getChildren().addAll(lblName, lblType, lblPrice, btnOpenAuction);
        return card;
    }

    // --- HÀM 3: MỞ CỬA SỔ SET NGÀY GIỜ VÀ TRUYỀN ID SẢN PHẨM SANG ---
    private void openCreateAuctionPopup(long itemId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CreateAuctionPopup.fxml"));
            Parent root = loader.load();

            // Tóm lấy thằng Controller của cửa sổ Popup
            CreateAuctionController controller = loader.getController();
            // Ném cái ID sản phẩm cho nó ngậm lấy!
            controller.initData(itemId);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Open Live Auction");
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            logger.error("Không thể mở cửa sổ tạo Auction: {}", e.getMessage(), e);
        }
    }

    // --- HÀM 4: MỞ CỬA SỔ TẠO SẢN PHẨM (ĐÃ CHỈNH SỬA ĐỂ AUTO-REFRESH) ---
    @FXML
    void handleCreateItem(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CreateItemPopup.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Thêm Sản Phẩm Mới");
            stage.setScene(new Scene(root));
            stage.showAndWait(); // Lệnh này làm luồng UI tạm dừng, chờ Popup đóng lại

            // BÙM! Khi popup tắt đi, tự động tải lại danh sách để hiện sản phẩm vừa tạo
            loadMyItems();

        } catch (IOException e) {
            logger.error("Không thể mở cửa sổ Tạo sản phẩm: {}", e.getMessage(), e);
        }
    }
}