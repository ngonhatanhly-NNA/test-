package com.client.controller.dashboard;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.client.network.AuctionNetwork;
import com.client.network.ItemNetwork;
import com.client.session.ClientSession;
import com.shared.dto.AuctionDetailDTO;
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

public class MyInventoryController {

    private static final Logger logger = LoggerFactory.getLogger(MyInventoryController.class);

    @FXML private Button btnCreateItem;

    // Container cho Active Listings (sản phẩm đang bán)
    @FXML private FlowPane sellingItemsContainer;

    // Container cho Won Auctions (đấu giá đã thắng)
    @FXML private FlowPane wonItemsContainer;

    private final ItemNetwork itemNetwork = new ItemNetwork();
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.of("vi", "VN"));

    @FXML
    public void initialize() {
        String userRole = ClientSession.getRole();

        if (!"SELLER".equals(userRole)) {
            // Ẩn nút tạo item nếu không phải seller
            if (btnCreateItem != null) {
                btnCreateItem.setVisible(false);
                btnCreateItem.setManaged(false);
            }
        } else {
            // Nếu là Seller: tải danh sách sản phẩm đang bán
            loadMyItems();
        }

        // Luôn tải Won Auctions dù là SELLER hay BIDDER
        loadWonAuctions();
    }

    // --- HÀM 1: TẢI ACTIVE LISTINGS ---
    private void loadMyItems() {
        logger.info("Đang tải danh sách sản phẩm từ Server...");

        long sellerId = ClientSession.getUserId();
        logger.info("MyInventoryController: Current seller ID = {}", sellerId);

        itemNetwork.getMyItems(sellerId).thenAccept(items -> {
            Platform.runLater(() -> {
                logger.info("MyInventoryController: Received {} items from server", items != null ? items.size() : 0);
                sellingItemsContainer.getChildren().clear();

                if (items != null && !items.isEmpty()) {
                    for (ItemResponseDTO item : items) {
                        VBox card = createItemCard(item);
                        sellingItemsContainer.getChildren().add(card);
                    }
                    logger.info("Đã tải và vẽ xong {} sản phẩm.", items.size());
                } else {
                    Label emptyLabel = new Label("Kho đồ của bạn đang trống. Hãy tạo sản phẩm mới!");
                    // Text xám nhạt cho nền đen
                    emptyLabel.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 14px;");
                    sellingItemsContainer.getChildren().add(emptyLabel);
                }
            });
        });
    }

    // --- HÀM 2: TẢI WON AUCTIONS TỪ DATABASE ---
    private void loadWonAuctions() {
        long bidderId = ClientSession.getUserId();
        logger.info("Đang tải Won Auctions cho bidder ID = {}", bidderId);

        new Thread(() -> {
            try {
                List<AuctionDetailDTO> wonAuctions = AuctionNetwork.getWonAuctions(bidderId);

                Platform.runLater(() -> {
                    if (wonItemsContainer == null) return;
                    wonItemsContainer.getChildren().clear();

                    if (wonAuctions != null && !wonAuctions.isEmpty()) {
                        for (AuctionDetailDTO auction : wonAuctions) {
                            VBox card = createWonAuctionCard(auction);
                            wonItemsContainer.getChildren().add(card);
                        }
                        logger.info("Đã tải {} won auctions.", wonAuctions.size());
                    } else {
                        Label emptyLabel = new Label("Bạn chưa thắng phiên đấu giá nào.");
                        emptyLabel.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 14px;");
                        wonItemsContainer.getChildren().add(emptyLabel);
                    }
                });
            } catch (Exception e) {
                logger.error("Lỗi tải won auctions: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    if (wonItemsContainer != null) {
                        wonItemsContainer.getChildren().clear();
                        Label errLabel = new Label("Không thể tải dữ liệu. Kiểm tra kết nối server.");
                        // Báo lỗi màu đỏ rực
                        errLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-size: 13px;");
                        wonItemsContainer.getChildren().add(errLabel);
                    }
                });
            }
        }, "load-won-auctions").start();
    }

    // --- HÀM 3: TẠO CARD CHO ITEM ĐANG BÁN ---
    private VBox createItemCard(ItemResponseDTO item) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        // Đổi màu thẻ thành nền xám đen, viền vàng kim loại
        card.setStyle("-fx-background-color: #181818; -fx-border-color: rgba(212,175,55,0.3); -fx-border-width: 1.5; -fx-border-radius: 12; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 10, 0, 0, 4);");
        card.setPrefWidth(220);

        Label lblName = new Label(item.getName());
        lblName.setFont(Font.font("System", FontWeight.BOLD, 16));
        lblName.setStyle("-fx-text-fill: #F5F5F5;"); // Chữ trắng sáng

        Label lblType = new Label("Category: " + item.getType());
        lblType.setStyle("-fx-text-fill: #A0A0A0;"); // Chữ xám nhạt

        BigDecimal price = item.getStartingPrice();
        String priceStr = price != null ? currencyFormat.format(price) + " VND" : "---";
        Label lblPrice = new Label("Start Price: " + priceStr);
        lblPrice.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblPrice.setStyle("-fx-text-fill: #D4AF37;"); // Màu vàng Gold

        Label lblAuctionStatus = new Label();
        lblAuctionStatus.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 3 10;");
        lblAuctionStatus.setVisible(false);
        lblAuctionStatus.setManaged(false);

        Button btnOpenAuction = new Button("Open Auction");
        // Nút bấm Gradient vàng đen
        btnOpenAuction.setStyle("-fx-background-color: linear-gradient(to right, #D4AF37, #FFD700); -fx-text-fill: #121212; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
        btnOpenAuction.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(lblName, lblType, lblPrice, lblAuctionStatus, btnOpenAuction);
        card.setUserData(item.getId());

        new Thread(() -> {
            try {
                String auctionStatus = AuctionNetwork.getAuctionStatusByItemId(item.getId());
                Platform.runLater(() -> {
                    if ("ACTIVE".equals(auctionStatus)) {
                        // Trạng thái Đang đấu giá: Nút màu xám tối, badge viền đỏ mờ
                        btnOpenAuction.setDisable(true);
                        btnOpenAuction.setStyle("-fx-background-color: #333333; -fx-text-fill: #888888; -fx-font-weight: bold; -fx-background-radius: 8;");
                        btnOpenAuction.setText("Đang đấu giá...");
                        lblAuctionStatus.setText("🔴 ĐANG ĐẤU GIÁ");
                        lblAuctionStatus.setStyle("-fx-background-color: rgba(220,38,38,0.1); -fx-text-fill: #E74C3C; -fx-border-color: rgba(220,38,38,0.4); -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 3 10; -fx-font-weight: bold; -fx-font-size: 11px;");
                        lblAuctionStatus.setVisible(true);
                        lblAuctionStatus.setManaged(true);
                    } else if ("FINISHED".equals(auctionStatus)) {
                        // Trạng thái Đã kết thúc: Nút màu xám tối, badge viền xanh mờ
                        btnOpenAuction.setDisable(true);
                        btnOpenAuction.setStyle("-fx-background-color: #333333; -fx-text-fill: #888888; -fx-font-weight: bold; -fx-background-radius: 8;");
                        btnOpenAuction.setText("Đã kết thúc");
                        lblAuctionStatus.setText("✅ ĐÃ ĐẤU GIÁ XONG");
                        lblAuctionStatus.setStyle("-fx-background-color: rgba(22,163,74,0.1); -fx-text-fill: #4CAF50; -fx-border-color: rgba(22,163,74,0.4); -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 3 10; -fx-font-weight: bold; -fx-font-size: 11px;");
                        lblAuctionStatus.setVisible(true);
                        lblAuctionStatus.setManaged(true);
                    }
                });
            } catch (Exception e) {
                logger.warn("Không thể kiểm tra auction status cho item {}: {}", item.getId(), e.getMessage());
            }
        }, "check-auction-status-" + item.getId()).start();

        btnOpenAuction.setOnAction(e -> openCreateAuctionPopup(item.getId()));
        return card;
    }

    // --- HÀM 4: TẠO CARD CHO WON AUCTION ---
    private VBox createWonAuctionCard(AuctionDetailDTO auction) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        // Thẻ màu xám đen, viền xanh lá Neon mờ để tôn vinh đồ đã thắng
        card.setStyle("-fx-background-color: #242424; -fx-border-color: rgba(76,175,80,0.5); -fx-border-width: 1.5; -fx-border-radius: 12; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 4);");
        card.setPrefWidth(220);

        Label lblName = new Label("🏆 " + (auction.getItemName() != null ? auction.getItemName() : "Item #" + auction.getItemId()));
        lblName.setFont(Font.font("System", FontWeight.BOLD, 15));
        lblName.setStyle("-fx-text-fill: #F5F5F5;"); // Chữ trắng sáng
        lblName.setWrapText(true);

        String winPrice = auction.getCurrentPrice() != null
                ? currencyFormat.format(auction.getCurrentPrice()) + " VND"
                : "---";
        Label lblPrice = new Label("Giá thắng: " + winPrice);
        lblPrice.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblPrice.setStyle("-fx-text-fill: #D4AF37;"); // Giá tiền vẫn bám theo tone Vàng Gold

        Label lblWinner = new Label("Người thắng: " + (auction.getHighestBidderName() != null ? auction.getHighestBidderName() : "Bạn"));
        lblWinner.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 12px;");

        Label lblType = new Label("Loại: " + (auction.getItemType() != null ? auction.getItemType() : "---"));
        lblType.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 12px;");

        // Badge "ĐÃ THẮNG" xanh lá trên nền tối
        Label lblBadge = new Label("✅ ĐÃ THẮNG");
        lblBadge.setStyle("-fx-background-color: rgba(76,175,80,0.1); -fx-text-fill: #4CAF50; -fx-border-color: rgba(76,175,80,0.4); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 3 10; -fx-font-weight: bold; -fx-font-size: 11px;");

        card.getChildren().addAll(lblName, lblPrice, lblWinner, lblType, lblBadge);
        return card;
    }

    private void addOrReplaceInventoryItem(ItemResponseDTO item) {
        if (item == null || sellingItemsContainer == null) {
            return;
        }

        sellingItemsContainer.getChildren().removeIf(node -> {
            Object data = node.getUserData();
            return data instanceof Long existingId && existingId == item.getId();
        });

        sellingItemsContainer.getChildren().removeIf(node ->
                node instanceof Label lbl && lbl.getText() != null && lbl.getText().contains("Kho đồ của bạn đang trống"));

        sellingItemsContainer.getChildren().add(0, createItemCard(item));
    }

    // --- HÀM 5: MỞ POPUP TẠO AUCTION ---
    private void openCreateAuctionPopup(long itemId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CreateAuctionPopup.fxml"));
            Parent root = loader.load();

            CreateAuctionController controller = loader.getController();
            controller.initData(itemId);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Open Live Auction");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Sau khi tạo auction xong, reload won auctions để đồng bộ
            loadWonAuctions();

        } catch (IOException e) {
            logger.error("Không thể mở cửa sổ tạo Auction: {}", e.getMessage(), e);
        }
    }

    // --- HÀM 6: MỞ POPUP TẠO SẢN PHẨM ---
    @FXML
    void handleCreateItem(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CreateItemPopup.fxml"));
            Parent root = loader.load();
            CreateItemController createItemController = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Thêm Sản Phẩm Mới");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (createItemController != null && createItemController.getCreatedItem() != null) {
                addOrReplaceInventoryItem(createItemController.getCreatedItem());
            }
            // Đồng bộ lại từ server để chắc chắn dữ liệu chuẩn DB
            loadMyItems();

        } catch (IOException e) {
            logger.error("Không thể mở cửa sổ Tạo sản phẩm: {}", e.getMessage(), e);
        }
    }
}