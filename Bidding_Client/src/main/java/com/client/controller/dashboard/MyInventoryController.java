package com.client.controller.dashboard;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.client.network.AuctionNetwork;
import com.client.network.ItemNetwork;
import com.client.session.ClientSession;
import com.client.util.ui.CardFactory;
import com.client.util.ui.state.StateFactory;
import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.ItemResponseDTO;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MyInventoryController {

    private static final Logger logger = LoggerFactory.getLogger(MyInventoryController.class);

    @FXML private Button btnCreateItem;
    @FXML private FlowPane sellingItemsContainer;
    @FXML private FlowPane wonItemsContainer;
	@FXML private VBox sellerSection;
	@FXML private VBox cuteImageSection;

    private final ItemNetwork itemNetwork = new ItemNetwork();

    @FXML
    public void initialize() {
        if (!"SELLER".equals(ClientSession.getRole())) {
            if (btnCreateItem != null) {
                btnCreateItem.setVisible(false);
                btnCreateItem.setManaged(false);
            }
			if (sellerSection != null) {
				sellerSection.setVisible(false);
				sellerSection.setManaged(false);
			}
			if (cuteImageSection != null) {
				cuteImageSection.setVisible(true);
				cuteImageSection.setManaged(true);
			}
        } else {
			if (cuteImageSection != null) {
				cuteImageSection.setVisible(false);
				cuteImageSection.setManaged(false);
			}
            loadMyItems();
        }
        loadWonAuctions();
    }

    private void loadMyItems() {
        logger.info("Đang tải danh sách sản phẩm từ Server...");
        long sellerId = ClientSession.getUserId();

        itemNetwork.getMyItems(sellerId).thenAccept(items -> {
            Platform.runLater(() -> {
                sellingItemsContainer.getChildren().clear();
                if (items != null && !items.isEmpty()) {
                    for (ItemResponseDTO item : items) {
                        sellingItemsContainer.getChildren().add(createItemCard(item));
                    }
                } else {
                    Label emptyLabel = new Label("No products found in your inventory. Create a new one now!");
                    emptyLabel.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 14px;");
                    sellingItemsContainer.getChildren().add(emptyLabel);
                }
            });
        });
    }

    private void loadWonAuctions() {
        long bidderId = ClientSession.getUserId();
        CompletableFuture.supplyAsync(() -> {
            try { return AuctionNetwork.getWonAuctions(bidderId); }
            catch (Exception e) { return null; }
        }).thenAcceptAsync(wonAuctions -> {
            if (wonItemsContainer == null) return;
            wonItemsContainer.getChildren().clear();
            if (wonAuctions != null && !wonAuctions.isEmpty()) {
                wonAuctions.forEach(auction -> wonItemsContainer.getChildren().add(CardFactory.createInventoryWonCard(auction, getClass())));
            } else {
                Label emptyLabel = new Label("You haven't won any auctions yet. Keep bidding and good luck!");
                emptyLabel.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 14px;");
                wonItemsContainer.getChildren().add(emptyLabel);
            }
        }, Platform::runLater);
    }

    private VBox createItemCard(ItemResponseDTO item) {
        VBox[] cardHolder = new VBox[1];
        cardHolder[0] = CardFactory.createInventorySellingCard(
                item,
                getClass(),
                e -> openCreateAuctionPopup(item.getId()),
                e -> handleDeleteItem(item.getId(), cardHolder[0])
        );
        VBox card = cardHolder[0];
        Button btnOpenAuction = (Button) card.getChildren().get(card.getChildren().size() - 2);

        // KẾT HỢP BƯỚC 3 (CompletableFuture) và BƯỚC 4 (State Pattern)
        CompletableFuture.supplyAsync(() -> {
            try { return AuctionNetwork.getAuctionStatusByItemId(item.getId()); }
            catch (Exception e) { return "NONE"; }
        }).thenAcceptAsync(status -> {
            // Thay vì if-else mệt mỏi, ta gọi StateFactory khoác áo cho nút bấm
            StateFactory.getButtonState(status).applyStyle(btnOpenAuction);
        }, Platform::runLater);

        return card;
    }

    private void handleDeleteItem(long itemId, VBox card) {
        // Hiển thị hộp thoại xác nhận trước khi xóa
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Product Deletion");
        confirm.setHeaderText("Are you sure you want to delete this product?");
        confirm.setContentText("This action cannot be undone.");

        java.util.Optional<javafx.scene.control.ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != javafx.scene.control.ButtonType.OK) {
            return; // Người dùng chọn Cancel, không làm gì
        }

        long sellerId = ClientSession.getUserId();
        itemNetwork.deleteItem(sellerId, itemId).thenAccept(response -> Platform.runLater(() -> {
            if ("SUCCESS".equals(response.getStatus())) {
                // Xóa card khỏi giao diện ngay lập tức, không cần reload trang
                sellingItemsContainer.getChildren().remove(card);
                // Nếu kho trống, hiển thị thông báo
                if (sellingItemsContainer.getChildren().isEmpty()) {
                    Label emptyLabel = new Label("No products found in your inventory. Create a new one now!");
                    emptyLabel.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 14px;");
                    sellingItemsContainer.getChildren().add(emptyLabel);
                }
            } else {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Lỗi xóa sản phẩm");
                alert.setHeaderText(null);
                alert.setContentText(response.getMessage());
                alert.showAndWait();
            }
        })).exceptionally(e -> {
            Platform.runLater(() -> {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Connection error");
                alert.setHeaderText(null);
                alert.setContentText("Failed to connect to the server");
                alert.showAndWait();
            });
            return null;
        });
    }

    private void addOrReplaceInventoryItem(ItemResponseDTO item) {
        if (item == null || sellingItemsContainer == null) return;
        sellingItemsContainer.getChildren().removeIf(node -> node.getUserData() instanceof Long id && id == item.getId());
        sellingItemsContainer.getChildren().removeIf(node -> node instanceof Label);
        VBox newCard = createItemCard(item);
        sellingItemsContainer.getChildren().add(0, newCard);
    }

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
            loadWonAuctions();
        } catch (IOException e) {
            logger.error("Không thể mở cửa sổ tạo Auction: {}", e.getMessage(), e);
        }
    }

    @FXML
    void handleCreateItem(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CreateItemPopup.fxml"));
            Parent root = loader.load();
            CreateItemController createItemController = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Create Product");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (createItemController != null && createItemController.getCreatedItem() != null) {
                addOrReplaceInventoryItem(createItemController.getCreatedItem());
            }
            loadMyItems();
        } catch (IOException e) {
            logger.error("Không thể mở cửa sổ Tạo sản phẩm: {}", e.getMessage(), e);
        }
    }
}