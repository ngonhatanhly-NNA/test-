package com.client.controller.dashboard;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MyInventoryController {

    private static final Logger logger = LoggerFactory.getLogger(MyInventoryController.class);

    @FXML private Button btnCreateItem;

    // Container cho Active Listings (sản phẩm đang bán)
    @FXML private FlowPane sellingItemsContainer;

    // [ĐÃ SỬA] Container cho Won Auctions (đấu giá đã thắng) - cần có trong FXML
    @FXML private FlowPane wonItemsContainer;

    private final ItemNetwork itemNetwork = new ItemNetwork();
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

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

        // [MỚI] Luôn tải Won Auctions dù là SELLER hay BIDDER
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
                    emptyLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");
                    sellingItemsContainer.getChildren().add(emptyLabel);
                }
            });
        });
    }

    // --- [MỚI] HÀM 2: TẢI WON AUCTIONS TỪ DATABASE ---
    private void loadWonAuctions() {
        long bidderId = ClientSession.getUserId();
        logger.info("Đang tải Won Auctions cho bidder ID = {}", bidderId);

        // Gọi API mới: GET /api/auctions/bidder/{bidderId}/won
        new Thread(() -> {
            try {
                String responseBody = AuctionNetwork.getWonAuctions(bidderId);
                com.shared.network.Response response = AuctionNetwork.parseResponse(responseBody);
                List<AuctionDetailDTO> wonAuctions = AuctionNetwork.parseActiveAuctionList(response);

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
                        emptyLabel.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 14px;");
                        wonItemsContainer.getChildren().add(emptyLabel);
                    }
                });
            } catch (Exception e) {
                logger.error("Lỗi tải won auctions: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    if (wonItemsContainer != null) {
                        wonItemsContainer.getChildren().clear();
                        Label errLabel = new Label("Không thể tải dữ liệu. Kiểm tra kết nối server.");
                        errLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 13px;");
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
        card.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");
        card.setPrefWidth(220);

        Label lblName = new Label(item.getName());
        lblName.setFont(Font.font("System", FontWeight.BOLD, 16));
        lblName.setStyle("-fx-text-fill: #1E293B;");

        Label lblType = new Label("Category: " + item.getType());
        lblType.setStyle("-fx-text-fill: #64748B;");

        BigDecimal price = item.getStartingPrice();
        String priceStr = price != null ? currencyFormat.format(price) + " VND" : "---";
        Label lblPrice = new Label("Start Price: " + priceStr);
        lblPrice.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblPrice.setStyle("-fx-text-fill: #D4AF37;");

        Button btnOpenAuction = new Button("Open Auction");
        btnOpenAuction.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
        btnOpenAuction.setMaxWidth(Double.MAX_VALUE);
        btnOpenAuction.setOnAction(e -> openCreateAuctionPopup(item.getId()));

        card.getChildren().addAll(lblName, lblType, lblPrice, btnOpenAuction);
        card.setUserData(item.getId());
        return card;
    }

    // --- [MỚI] HÀM 4: TẠO CARD CHO WON AUCTION ---
    private VBox createWonAuctionCard(AuctionDetailDTO auction) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #F0FDF4; -fx-border-color: #A7F3D0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(5,150,105,0.1), 10, 0, 0, 2);");
        card.setPrefWidth(220);

        // Tên sản phẩm
        Label lblName = new Label("🏆 " + (auction.getItemName() != null ? auction.getItemName() : "Item #" + auction.getItemId()));
        lblName.setFont(Font.font("System", FontWeight.BOLD, 15));
        lblName.setStyle("-fx-text-fill: #064E3B;");
        lblName.setWrapText(true);

        // Giá thắng
        String winPrice = auction.getCurrentPrice() != null
                ? currencyFormat.format(auction.getCurrentPrice()) + " VND"
                : "---";
        Label lblPrice = new Label("Giá thắng: " + winPrice);
        lblPrice.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblPrice.setStyle("-fx-text-fill: #059669;");

        // Tên người thắng (chính là user hiện tại)
        Label lblWinner = new Label("Người thắng: " + (auction.getHighestBidderName() != null ? auction.getHighestBidderName() : "Bạn"));
        lblWinner.setStyle("-fx-text-fill: #047857; -fx-font-size: 12px;");

        // Loại sản phẩm
        Label lblType = new Label("Loại: " + (auction.getItemType() != null ? auction.getItemType() : "---"));
        lblType.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");

        // Badge "ĐÃ THẮNG"
        Label lblBadge = new Label("✅ ĐÃ THẮNG");
        lblBadge.setStyle("-fx-background-color: #ECFDF5; -fx-text-fill: #047857; -fx-border-color: #A7F3D0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 3 10; -fx-font-weight: bold; -fx-font-size: 11px;");

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
