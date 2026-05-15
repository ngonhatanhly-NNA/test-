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

    private final ItemNetwork itemNetwork = new ItemNetwork();

    @FXML
    public void initialize() {
        if (!"SELLER".equals(ClientSession.getRole())) {
            if (btnCreateItem != null) {
                btnCreateItem.setVisible(false);
                btnCreateItem.setManaged(false);
            }
        } else {
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
                    Label emptyLabel = new Label("Kho đồ của bạn đang trống. Hãy tạo sản phẩm mới!");
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
                Label emptyLabel = new Label("Bạn chưa thắng phiên đấu giá nào.");
                emptyLabel.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 14px;");
                wonItemsContainer.getChildren().add(emptyLabel);
            }
        }, Platform::runLater);
    }

    private VBox createItemCard(ItemResponseDTO item) {
        VBox card = CardFactory.createInventorySellingCard(item, getClass(), e -> openCreateAuctionPopup(item.getId()));
        Button btnOpenAuction = (Button) card.getChildren().get(card.getChildren().size() - 1);

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

    private void addOrReplaceInventoryItem(ItemResponseDTO item) {
        if (item == null || sellingItemsContainer == null) return;
        sellingItemsContainer.getChildren().removeIf(node -> node.getUserData() instanceof Long id && id == item.getId());
        sellingItemsContainer.getChildren().removeIf(node -> node instanceof Label);
        sellingItemsContainer.getChildren().add(0, createItemCard(item));
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
            stage.setTitle("Thêm Sản Phẩm Mới");
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