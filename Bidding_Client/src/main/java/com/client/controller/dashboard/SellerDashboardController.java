package com.client.controller.dashboard;

import com.client.network.AuctionNetwork;
import com.client.network.SellerNetwork;
import com.client.session.ClientSession;
import com.client.util.SceneController;
import javafx.stage.Modality;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.ItemResponseDTO;
import com.shared.network.Response;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SellerDashboardController - Giao diện quản trị cho Seller.
 * Hiển thị thông tin từ database: items, statistics, live auctions.
 */
public class SellerDashboardController {

    private static SellerDashboardController instance;

    // ---- Overview Tab ----
    @FXML private Label lblRating;
    @FXML private Label lblTotalItems;
    @FXML private ListView<String> listViewActivities;

    // ---- Item Management Tab ----
    @FXML private TableView<ItemResponseDTO> tableItems;
    @FXML private TableColumn<ItemResponseDTO, String> colItemId;
    @FXML private TableColumn<ItemResponseDTO, String> colItemName;
    @FXML private TableColumn<ItemResponseDTO, String> colStartingPrice;
    @FXML private TableColumn<ItemResponseDTO, String> colCategory;
    @FXML private TableColumn<ItemResponseDTO, String> colStatus;
    @FXML private TableColumn<ItemResponseDTO, String> colAction;
    @FXML private TextField txtSearchItem;

    // ---- Selling Analysis Tab ----
    @FXML private TextArea textAreaStatistics;

    // ---- Live Auction Monitoring Tab ----
    @FXML private ListView<String> listViewAuctions;
    @FXML private Label lblAuctionItemName;
    @FXML private Label lblCurrentAuctionPrice;
    @FXML private Label lblLeadingBidder;
    @FXML private Label lblTimeRemaining;
    @FXML private Label lblBidStep;

    private final SellerNetwork sellerNetwork = new SellerNetwork();
    private final Gson gson = new Gson();
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "seller-thread");
        t.setDaemon(true);
        return t;
    });

    private List<AuctionDetailDTO> currentAuctions;
    private ObservableList<ItemResponseDTO> allItems = FXCollections.observableArrayList();

    public SellerDashboardController() {
        instance = this;
    }

    public static SellerDashboardController getExistingInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        loadDashboardData();
        loadLiveAuctions();
    }

    /**
     * Thiết lập các cột của TableView với đúng kiểu dữ liệu ItemResponseDTO.
     */
    private void setupTableColumns() {
        if (colItemId != null) {
            colItemId.setCellValueFactory(data ->
                    new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        }
        if (colItemName != null) {
            colItemName.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getName()));
        }
        if (colStartingPrice != null) {
            colStartingPrice.setCellValueFactory(data -> {
                BigDecimal price = data.getValue().getStartingPrice();
                String formatted = (price != null) ? currencyFormat.format(price) + " VND" : "---";
                return new SimpleStringProperty(formatted);
            });
        }
        if (colCategory != null) {
            colCategory.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getType()));
        }
        if (colStatus != null) {
            // Items lấy từ auctions nên đang được đấu giá = ACTIVE
            colStatus.setCellValueFactory(data ->
                    new SimpleStringProperty("ACTIVE"));
        }
        if (colAction != null) {
            colAction.setCellValueFactory(data ->
                    new SimpleStringProperty("Xem chi tiết"));
        }

        if (tableItems != null) {
            tableItems.setItems(allItems);
            tableItems.setPlaceholder(new Label("Chưa có sản phẩm nào"));
        }
    }

    /**
     * Tải dữ liệu tổng quan khi khởi tạo.
     */
    @FXML
    private void loadDashboardData() {
        long sellerId = ClientSession.getUserId();

        // 1. Lấy thông tin seller (shopName, rating)
        sellerNetwork.getSellerById(sellerId)
                .thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        try {
                            JsonObject data = gson.toJsonTree(response.getData()).getAsJsonObject();
                            double rating = data.has("rating") ? data.get("rating").getAsDouble() : 0.0;
                            int totalReviews = data.has("totalReviews") ? data.get("totalReviews").getAsInt() : 0;
                            if (lblRating != null) {
                                lblRating.setText(String.format("%.1f ★", rating));
                            }
                            if (listViewActivities != null) {
                                ObservableList<String> activities = FXCollections.observableArrayList();
                                activities.add("📊 Rating hiện tại: " + String.format("%.1f ★ (%d đánh giá)", rating, totalReviews));
                                activities.add("✅ Đã đăng nhập thành công");
                                activities.add("🔄 Dữ liệu đã được tải lúc: " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
                                listViewActivities.setItems(activities);
                            }
                        } catch (Exception e) {
                            System.err.println("Lỗi parse seller info: " + e.getMessage());
                        }
                    }
                }))
                .exceptionally(e -> {
                    System.err.println("Lỗi lấy thông tin seller: " + e.getMessage());
                    return null;
                });

        // 2. Lấy items và statistics song song
        loadSellerItems();
        loadSellerStatistics();
    }

    /**
     * Tải danh sách items của seller và đổ vào TableView.
     */
    @FXML
    private void showAllItems(ActionEvent event) {
        loadSellerItems();
    }

    private void loadSellerItems() {
        long sellerId = ClientSession.getUserId();
        sellerNetwork.getSellerItems(sellerId)
                .thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        try {
                            // Parse List<ItemResponseDTO> từ response.getData()
                            String dataJson = gson.toJson(response.getData());
                            Type listType = new TypeToken<List<ItemResponseDTO>>() {}.getType();
                            List<ItemResponseDTO> items = gson.fromJson(dataJson, listType);

                            allItems.clear();
                            if (items != null && !items.isEmpty()) {
                                allItems.addAll(items);
                                if (lblTotalItems != null) {
                                    lblTotalItems.setText(String.valueOf(items.size()));
                                }
                                // Thêm vào activities
                                if (listViewActivities != null) {
                                    listViewActivities.getItems().add(
                                            "📦 Đã tải " + items.size() + " sản phẩm");
                                }
                            } else {
                                if (lblTotalItems != null) lblTotalItems.setText("0");
                            }
                        } catch (Exception e) {
                            System.err.println("Lỗi parse items: " + e.getMessage());
                        }
                    } else {
                        System.err.println("Lỗi lấy items: " + response.getMessage());
                    }
                }))
                .exceptionally(e -> {
                    System.err.println("Lỗi kết nối lấy items: " + e.getMessage());
                    return null;
                });
    }

    /**
     * Tìm kiếm item theo tên trong danh sách đã tải.
     */
    @FXML
    private void handleSearchItem(ActionEvent event) {
        if (txtSearchItem == null) return;
        String keyword = txtSearchItem.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            tableItems.setItems(allItems);
            return;
        }
        ObservableList<ItemResponseDTO> filtered = FXCollections.observableArrayList();
        for (ItemResponseDTO item : allItems) {
            if (item.getName() != null && item.getName().toLowerCase().contains(keyword)) {
                filtered.add(item);
            }
        }
        tableItems.setItems(filtered);
    }

    /**
     * Tải và hiển thị thống kê bán hàng.
     */
    @FXML
    private void loadStatistics() {
        loadSellerStatistics();
    }

    private void loadSellerStatistics() {
        long sellerId = ClientSession.getUserId();
        sellerNetwork.getSellerStatistics(sellerId)
                .thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        try {
                            JsonObject data = gson.toJsonTree(response.getData()).getAsJsonObject();

                            int totalAuctions = data.has("totalAuctions") ? data.get("totalAuctions").getAsInt() : 0;
                            int completedAuctions = data.has("completedAuctions") ? data.get("completedAuctions").getAsInt() : 0;
                            int activeAuctions = data.has("activeAuctions") ? data.get("activeAuctions").getAsInt() : 0;
                            String totalRevenue = data.has("totalRevenue") ? data.get("totalRevenue").getAsString() : "0";
                            int totalItems = data.has("totalItems") ? data.get("totalItems").getAsInt() : 0;

                            String reportText = "╔══════════════════════════════════════╗\n" +
                                    "║       BÁO CÁO HIỆU SUẤT BÁN HÀNG     ║\n" +
                                    "╚══════════════════════════════════════╝\n\n" +
                                    "📦  Tổng số sản phẩm đã đưa ra đấu giá : " + totalItems + "\n\n" +
                                    "🏷️  Tổng số phiên đấu giá              : " + totalAuctions + "\n" +
                                    "✅  Phiên đã hoàn thành                : " + completedAuctions + "\n" +
                                    "🔴  Phiên đang hoạt động               : " + activeAuctions + "\n\n" +
                                    "💰  Tổng doanh thu ước tính            : " + totalRevenue + " VND\n\n" +
                                    "─────────────────────────────────────────\n" +
                                    "⏱  Cập nhật lúc: " + java.time.LocalDateTime.now()
                                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

                            if (textAreaStatistics != null) {
                                textAreaStatistics.setText(reportText);
                            }

                        } catch (Exception e) {
                            if (textAreaStatistics != null) {
                                textAreaStatistics.setText("Lỗi khi đọc dữ liệu thống kê: " + e.getMessage());
                            }
                            System.err.println("Lỗi parse statistics: " + e.getMessage());
                        }
                    } else {
                        if (textAreaStatistics != null) {
                            textAreaStatistics.setText("Không thể tải thống kê: " + response.getMessage());
                        }
                    }
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        if (textAreaStatistics != null) {
                            textAreaStatistics.setText("Lỗi kết nối server. Hãy kiểm tra server đang chạy.");
                        }
                    });
                    return null;
                });
    }

    /**
     * Tạo item mới - chuyển sang màn hình tạo item.
     */
    @FXML
    private void createNewItem(ActionEvent event) {
        // Mở popup tạo item mới trong cửa sổ mới
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/CreateItemPopup.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Tạo sản phẩm mới");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            // Sau khi đóng popup, reload lại danh sách items
            loadSellerItems();
        } catch (Exception e) {
            showAlert("Lỗi", "Không thể mở form tạo sản phẩm: " + e.getMessage());
        }
    }

    /**
     * Làm mới toàn bộ dữ liệu.
     */
    @FXML
    private void refreshData(ActionEvent event) {
        loadDashboardData();
    }

    /**
     * Đăng xuất.
     */
    @FXML
    private void logout(ActionEvent event) {
        ClientSession.clear();
        SceneController.switchScene(event, "Login.fxml");
    }

    // ============================================================
    // LIVE AUCTION MONITORING
    // ============================================================

    /**
     * Tải danh sách phiên đấu giá đang diễn ra.
     */
    @FXML
    private void loadLiveAuctions() {
        executorService.submit(() -> {
            try {
                String responseBody = AuctionNetwork.getActiveAuctions();
                Response response = AuctionNetwork.parseResponse(responseBody);
                currentAuctions = AuctionNetwork.parseActiveAuctionList(response);

                Platform.runLater(() -> {
                    renderLiveAuctions();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (listViewAuctions != null) {
                        listViewAuctions.setItems(FXCollections.observableArrayList(
                                "Lỗi kết nối server: " + e.getMessage()));
                    }
                });
            }
        });
    }

    /**
     * Hiển thị chi tiết phiên đấu giá được chọn.
     */
    private void displayAuctionDetails(AuctionDetailDTO auction) {
        if (lblAuctionItemName != null) {
            lblAuctionItemName.setText(auction.getItemName() != null ? auction.getItemName() : "---");
        }
        if (lblCurrentAuctionPrice != null) {
            String price = auction.getCurrentPrice() != null
                    ? currencyFormat.format(auction.getCurrentPrice()) + " VNĐ"
                    : "0 VNĐ";
            lblCurrentAuctionPrice.setText(price);
        }
        if (lblLeadingBidder != null) {
            lblLeadingBidder.setText(auction.getHighestBidderName() != null
                    ? auction.getHighestBidderName() : "Chưa có ai đấu giá");
        }
        if (lblTimeRemaining != null) {
            long remaining = auction.getRemainingTime();
            String timeStr;
            if (remaining > 0) {
                long minutes = remaining / 60;
                long seconds = remaining % 60;
                timeStr = String.format("%d phút %d giây", minutes, seconds);
            } else {
                timeStr = "Đã kết thúc";
            }
            lblTimeRemaining.setText(timeStr);
        }
        if (lblBidStep != null) {
            String step = auction.getStepPrice() != null
                    ? currencyFormat.format(auction.getStepPrice()) + " VND"
                    : "---";
            lblBidStep.setText(step);
        }
    }

    public void addOrUpdateAuctionRealtime(AuctionDetailDTO auction) {
        if (auction == null) {
            return;
        }
        if (currentAuctions == null) {
            currentAuctions = FXCollections.observableArrayList();
        }

        for (int i = 0; i < currentAuctions.size(); i++) {
            if (currentAuctions.get(i).getAuctionId() == auction.getAuctionId()) {
                currentAuctions.set(i, auction);
                renderLiveAuctions();
                return;
            }
        }

        currentAuctions.add(0, auction);
        renderLiveAuctions();
    }

    public void removeAuctionRealtime(long auctionId) {
        if (currentAuctions == null || currentAuctions.isEmpty()) {
            return;
        }

        currentAuctions.removeIf(a -> a.getAuctionId() == auctionId);
        renderLiveAuctions();
    }

    private void renderLiveAuctions() {
        if (listViewAuctions == null) {
            return;
        }

        if (currentAuctions != null && !currentAuctions.isEmpty()) {
            ObservableList<String> auctionList = FXCollections.observableArrayList();
            for (AuctionDetailDTO auction : currentAuctions) {
                String price = auction.getCurrentPrice() != null
                        ? currencyFormat.format(auction.getCurrentPrice()) + " VND"
                        : "---";
                String displayText = String.format("[ID: %d] %s - Giá: %s",
                        auction.getAuctionId(),
                        auction.getItemName(),
                        price);
                auctionList.add(displayText);
            }
            listViewAuctions.setItems(auctionList);
            listViewAuctions.setOnMouseClicked(e -> {
                int idx = listViewAuctions.getSelectionModel().getSelectedIndex();
                if (idx >= 0 && idx < currentAuctions.size()) {
                    displayAuctionDetails(currentAuctions.get(idx));
                }
            });
        } else {
            listViewAuctions.setItems(FXCollections.observableArrayList("Không có phiên đấu giá nào đang diễn ra"));
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}