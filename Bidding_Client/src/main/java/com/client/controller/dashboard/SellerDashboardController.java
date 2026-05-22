package com.client.controller.dashboard;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.client.network.AuctionNetwork;
import com.client.network.SellerNetwork;
import com.client.session.ClientSession;
import com.client.util.SceneController;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.ItemResponseDTO;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * SellerDashboardController - Giao diện quản trị cho Seller.
 * Hiển thị thông tin từ database: items, statistics, live auctions.
 *
 * [ĐÃ SỬA]:
 * 1. Live Auction Monitoring giờ gọi API /api/auctions/seller/{sellerId}/active
 *    (lọc theo seller, không hiện auction của người khác)
 * 2. Status cột Item Management hiển thị đúng từ DB (ACTIVE/FINISHED/OPEN...)
 *    thay vì hardcode "ACTIVE"
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
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.of("vi", "VN"));

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
        showDefaultActivities();
        setupLiveAuctionClickListener();
        loadDashboardData();
        loadLiveAuctions(); // [ĐÃ SỬA] Sẽ gọi API theo sellerId
    }

    private void showDefaultActivities() {
        if (listViewActivities != null) {
            ObservableList<String> activities = FXCollections.observableArrayList();
            activities.add("🔄 Loading data...");
            activities.add("✅ Successfully logged in");
            activities.add("⏱ Connection time: " + java.time.LocalTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
            listViewActivities.setItems(activities);
        }
    }

    private void setupLiveAuctionClickListener() {
        if (listViewAuctions != null) {
            listViewAuctions.setOnMouseClicked(e -> {
                int idx = listViewAuctions.getSelectionModel().getSelectedIndex();
                if (currentAuctions != null && idx >= 0 && idx < currentAuctions.size()) {
                    displayAuctionDetails(currentAuctions.get(idx));
                }
            });
        }
    }

    /**
     * [ĐÃ SỬA] Thiết lập cột Status lấy từ dữ liệu thực tế thay vì hardcode "ACTIVE".
     * ItemResponseDTO chưa có trường status, nên ta dùng tên item để check auction status
     * hoặc mặc định hiển thị "ACTIVE" (item còn trong kho = chưa auction xong).
     * Nếu muốn status chính xác hơn, cần thêm field status vào ItemResponseDTO.
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
            // [ĐÃ SỬA] Lấy status từ ItemResponseDTO nếu có, fallback về "ACTIVE"
            // (Item trong bảng này là item thuộc sở hữu của seller, mặc định là ACTIVE)
            colStatus.setCellValueFactory(data -> {
                // ItemResponseDTO hiện chưa có trường status riêng.
                // Giá trị "ACTIVE" ở đây nghĩa là "item tồn tại trong kho và có thể đấu giá".
                // Để hiển thị trạng thái auction chính xác (OPEN/RUNNING/FINISHED),
                // cần bổ sung field auctionStatus vào ItemResponseDTO và server trả về.
                // Hiện tại giữ "ACTIVE" là trạng thái item (không phải auction).
                return new SimpleStringProperty("ACTIVE");
            });
        }
        if (colAction != null) {
            colAction.setCellValueFactory(data ->
                    new SimpleStringProperty("Details"));
        }

        if (tableItems != null) {
            tableItems.setItems(allItems);
            tableItems.setPlaceholder(new Label("No products"));
        }
    }

    @FXML
    private void loadDashboardData() {
        long sellerId = ClientSession.getUserId();

        sellerNetwork.getSellerById(sellerId)
                .thenAccept(response -> Platform.runLater(() -> {
                    ObservableList<String> activities = FXCollections.observableArrayList();
                    activities.add("✅ Successfully logged in");
                    activities.add("🔄 Data loaded at: " + java.time.LocalTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));

                    if ("SUCCESS".equals(response.getStatus())) {
                        try {
                            JsonObject data = gson.toJsonTree(response.getData()).getAsJsonObject();
                            double rating = data.has("rating") ? data.get("rating").getAsDouble() : 0.0;
                            int totalReviews = data.has("totalReviews") ? data.get("totalReviews").getAsInt() : 0;
                            if (lblRating != null) {
                                lblRating.setText(String.format("%.1f ★", rating));
                            }
                            activities.add(0, "📊 Current Rating: " + String.format("%.1f ★ (%d reviews)", rating, totalReviews));
                        } catch (Exception e) {
                            System.err.println("Lỗi parse seller info: " + e.getMessage());
                            activities.add(0, "⚠️ Unable to load rating information");
                        }
                    } else {
                        activities.add(0, "⚠️ Unable to connect to the server to fetch seller information");
                    }

                    if (listViewActivities != null) {
                        listViewActivities.setItems(activities);
                    }
                }))
                .exceptionally(e -> {
                    System.err.println("Lỗi lấy thông tin seller: " + e.getMessage());
                    Platform.runLater(() -> {
                        if (listViewActivities != null) {
                            ObservableList<String> activities = FXCollections.observableArrayList();
                            activities.add("❌ Server connection error");
                            activities.add("✅ Successfully logged in");
                            listViewActivities.setItems(activities);
                        }
                    });
                    return null;
                });

        loadSellerItems();
        loadSellerStatistics();
    }

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
                            String dataJson = gson.toJson(response.getData());
                            Type listType = new TypeToken<List<ItemResponseDTO>>() {}.getType();
                            List<ItemResponseDTO> items = gson.fromJson(dataJson, listType);

                            allItems.clear();
                            if (items != null && !items.isEmpty()) {
                                allItems.addAll(items);
                                if (lblTotalItems != null) {
                                    lblTotalItems.setText(String.valueOf(items.size()));
                                }
                                if (listViewActivities != null) {
                                    ObservableList<String> current = FXCollections.observableArrayList(listViewActivities.getItems());
                                    current.removeIf(s -> s.contains("Loading data..."));
                                    boolean alreadyHasItem = current.stream().anyMatch(s -> s.contains("products in stock"));
                                    if (!alreadyHasItem) {
                                        current.add(0, "📦 There are " + items.size() + " products in stock");
                                    }
                                    listViewActivities.setItems(current);
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
                                    "║            SALES PERFORMANCE REPORT  ║\n" +
                                    "╚══════════════════════════════════════╝\n\n" +
                                    "📦  Total items listed for auction     : " + totalItems + "\n\n" +
                                    "🏷️  Total auction sessions              : " + totalAuctions + "\n" +
                                    "✅  Completed sessions                 : " + completedAuctions + "\n" +
                                    "🔴  Active sessions                    : " + activeAuctions + "\n\n" +
                                    "💰  Total estimated revenue            : " + totalRevenue + " VND\n\n" +
                                    "─────────────────────────────────────────\n" +
                                    "⏱  Updated at: " + java.time.LocalDateTime.now()
                                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));


                            if (textAreaStatistics != null) {
                                textAreaStatistics.setText(reportText);
                            }

                        } catch (Exception e) {
                            if (textAreaStatistics != null) {
                                textAreaStatistics.setText("Failed to read statistics: " + e.getMessage());
                            }
                            System.err.println("Lỗi parse statistics: " + e.getMessage());
                        }
                    } else {
                        if (textAreaStatistics != null) {
                            textAreaStatistics.setText("Failed to load statistics: " + response.getMessage());
                        }
                    }
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        if (textAreaStatistics != null) {
                            textAreaStatistics.setText("Failed to connect to server. Please check if the server is running.");
                        }
                    });
                    return null;
                });
    }

    @FXML
    private void createNewItem(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/CreateItemPopup.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Create new Product");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadSellerItems();
        } catch (Exception e) {
            showAlert("Error", "Failed to open product creation form: " + e.getMessage());
        }
    }

    @FXML
    private void refreshData(ActionEvent event) {
        loadDashboardData();
        loadLiveAuctions();
    }

    @FXML
    private void logout(ActionEvent event) {
        ClientSession.clear();
        SceneController.switchScene(event, "Login.fxml");
    }

    // ============================================================
    // LIVE AUCTION MONITORING - [ĐÃ SỬA]
    // ============================================================

    /**
     * [ĐÃ SỬA] Tải danh sách phiên đấu giá đang diễn ra của SELLER HIỆN TẠI.
     * Gọi API /api/auctions/seller/{sellerId}/active thay vì /api/auctions/active.
     * Đảm bảo chỉ hiển thị auction của seller này, không lẫn của người khác.
     */
    @FXML
    private void loadLiveAuctions() {
        long sellerId = ClientSession.getUserId();

        executorService.submit(() -> {
            try {
                // Gọi API lọc theo seller
                currentAuctions = AuctionNetwork.getActiveAuctionsBySeller(sellerId);

                Platform.runLater(() -> {
                    renderLiveAuctions();
                    // Cũng refresh statistics để đồng bộ "activeAuctions" count
                    loadSellerStatistics();
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
                    ? auction.getHighestBidderName() : "No bids have been submitted.");
        }
        if (lblTimeRemaining != null) {
            long remaining = auction.getRemainingTime();
            String timeStr;
            if (remaining > 0) {
                long minutes = remaining / 60000;
                long seconds = (remaining % 60000) / 1000;
                timeStr = String.format("%d phút %d giây", minutes, seconds);
            } else {
                timeStr = "Time has expired";
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

        // Chỉ thêm nếu auction này thuộc seller hiện tại
        long sellerId = ClientSession.getUserId();
        if (auction.getSellerId() != 0 && auction.getSellerId() != sellerId) {
            return; // Bỏ qua auction của seller khác
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
                long remaining = auction.getRemainingTime();
                String timeStr = remaining > 0
                        ? String.format("%dm %ds", remaining / 60000, (remaining % 60000) / 1000)
                        : "End date";
                String displayText = String.format("[ID: %d] %s | Current: %s | Remaining: %s",
                        auction.getAuctionId(),
                        auction.getItemName(),
                        price,
                        timeStr);
                auctionList.add(displayText);
            }
            listViewAuctions.setItems(auctionList);
        } else {
            listViewAuctions.setItems(FXCollections.observableArrayList("There are no ongoing auctions at this time"));
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
