package com.client.controller.dashboard;

import com.client.network.AdminNetwork;
import com.client.network.AuctionNetwork;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.ItemResponseDTO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminDashboardController {

    // FXML fields
    @FXML private TabPane tabPane;
    @FXML private Label lblTotalUsers;
    @FXML private Label lblTotalProducts;
    @FXML private Label lblTotalAuctions;
    @FXML private Label lblRevenue;

    // User Management
    @FXML private TableView<Map<String, String>> tableUsers;
    @FXML private TableColumn<Map<String, String>, String> colId;
    @FXML private TableColumn<Map<String, String>, String> colUsername;
    @FXML private TableColumn<Map<String, String>, String> colEmail;
    @FXML private TableColumn<Map<String, String>, String> colRole;
    @FXML private TableColumn<Map<String, String>, String> colStatus;
    @FXML private TableColumn<Map<String, String>, String> colShopName;
    @FXML private TableColumn<Map<String, String>, String> colRating;
    @FXML private TableColumn<Map<String, String>, String> colVerified;

    // Item Analysis
    @FXML private TableView<ItemResponseDTO> tableItems;
    @FXML private TableColumn<ItemResponseDTO, Long> colItemId;
    @FXML private TableColumn<ItemResponseDTO, String> colItemName;
    @FXML private TableColumn<ItemResponseDTO, String> colItemDescription;
    @FXML private TableColumn<ItemResponseDTO, BigDecimal> colItemStartingPrice;
    @FXML private TableColumn<ItemResponseDTO, String> colItemStatus;
    @FXML private TableColumn<ItemResponseDTO, Void> colItemAction;

    // Financial Report
    @FXML private TextArea textAreaRevenue;

    private final Gson gson = new Gson();
    private boolean usersLoaded = false;
    private boolean itemAnalyticsLoaded = false;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    @FXML
    public void initialize() {
        setupUserTableColumns();
        setupItemTableColumns();
        setupTabPaneListener();
        loadDashboardData();
    }

    private void setupTabPaneListener() {
        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            if (newTab != null) {
                if (newTab.getText().equals("User Management") && !usersLoaded) {
                    loadAllUsers();
                    usersLoaded = true;
                } else if (newTab.getText().equals("Item Analysis") && !itemAnalyticsLoaded) {
                    loadProductAnalytics(null);
                    itemAnalyticsLoaded = true;
                }
            }
        });
    }

    private void setupUserTableColumns() {
        colId.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().get("id")));
        colUsername.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().get("username")));
        colEmail.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().get("email")));
        colRole.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().get("role")));
        colStatus.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().get("status")));
        colShopName.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getOrDefault("shopName", "N/A")));
        colRating.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getOrDefault("rating", "N/A")));
        colVerified.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getOrDefault("isVerified", "N/A")));
        colStatus.setCellFactory(param -> new StatusCell(this));
    }

    private void setupItemTableColumns() {
        colItemId.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getId()));
        colItemName.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getName()));
        colItemDescription.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescription()));
        colItemStartingPrice.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getStartingPrice()));
        colItemStatus.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus()));
        addButtonToTable();
    }

    private void addButtonToTable() {
        Callback<TableColumn<ItemResponseDTO, Void>, TableCell<ItemResponseDTO, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<ItemResponseDTO, Void> call(final TableColumn<ItemResponseDTO, Void> param) {
                final TableCell<ItemResponseDTO, Void> cell = new TableCell<>() {
                    private final Button btn = new Button("Delete");
                    {
                        btn.setOnAction((ActionEvent event) -> {
                            ItemResponseDTO item = getTableView().getItems().get(getIndex());
                            deleteItem(item.getId());
                        });
                    }
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };
        colItemAction.setCellFactory(cellFactory);
    }

    private void deleteItem(long itemId) {
        AdminNetwork.deleteItem(itemId).thenAccept(response -> {
            Platform.runLater(() -> {
                if ("SUCCESS".equals(response.getStatus())) {
                    loadProductAnalytics(null);
                } else {
                    showAlert("Error", "Failed to delete item.");
                }
            });
        });
    }

    @FXML
    private void loadDashboardData() {
        // Load total users
        AdminNetwork.getAllUsers().thenAccept(userResponse -> {
            if ("SUCCESS".equals(userResponse.getStatus()) && userResponse.getData() != null) {
                Platform.runLater(() -> {
                    try {
                        JsonArray usersArray = gson.toJsonTree(userResponse.getData()).getAsJsonArray();
                        lblTotalUsers.setText(String.valueOf(usersArray.size()));
                    } catch (Exception e) {
                        lblTotalUsers.setText("Lỗi");
                    }
                });
            }
        });

        // Load total products
        AdminNetwork.getProductAnalytics().thenAccept(itemResponse -> {
            if ("SUCCESS".equals(itemResponse.getStatus()) && itemResponse.getData() != null) {
                Platform.runLater(() -> {
                    try {
                        JsonArray itemsArray = gson.toJsonTree(itemResponse.getData()).getAsJsonArray();
                        lblTotalProducts.setText(String.valueOf(itemsArray.size()));
                    } catch (Exception e) {
                        lblTotalProducts.setText("Lỗi");
                    }
                });
            }
        });

        // Load total auctions
        AuctionNetwork.getActiveAuctionsAsync().thenAccept(auctionResponse -> {
            if ("SUCCESS".equals(auctionResponse.getStatus()) && auctionResponse.getData() != null) {
                Platform.runLater(() -> {
                    try {
                        JsonArray auctionsArray = gson.toJsonTree(auctionResponse.getData()).getAsJsonArray();
                        lblTotalAuctions.setText(String.valueOf(auctionsArray.size()));
                    } catch (Exception e) {
                        lblTotalAuctions.setText("Lỗi");
                    }
                });
            }
        });

        // Load revenue
        AdminNetwork.getRevenueEstimate().thenAccept(revenueResponse -> {
            if ("SUCCESS".equals(revenueResponse.getStatus()) && revenueResponse.getData() != null) {
                Platform.runLater(() -> {
                    try {
                        JsonObject revenueData = gson.toJsonTree(revenueResponse.getData()).getAsJsonObject();
                        BigDecimal revenueValue = revenueData.get("phiNenTang_8%").getAsBigDecimal();
                        lblRevenue.setText(currencyFormat.format(revenueValue));
                    } catch (Exception e) {
                        lblRevenue.setText("Lỗi");
                    }
                });
            }
        });
    }

    public void loadAllUsers() {
        AdminNetwork.getAllUsers().thenAccept(response -> Platform.runLater(() -> {
            if ("SUCCESS".equals(response.getStatus()) && response.getData() != null) {
                try {
                    JsonArray usersArray = gson.toJsonTree(response.getData()).getAsJsonArray();
                    ObservableList<Map<String, String>> usersList = FXCollections.observableArrayList();
                    for (JsonElement userElement : usersArray) {
                        JsonObject user = userElement.getAsJsonObject();
                        Map<String, String> userData = new HashMap<>();
                        userData.put("id", getJsonStringAsInteger(user, "id"));
                        userData.put("username", getJsonString(user, "username"));
                        userData.put("email", getJsonString(user, "email"));
                        userData.put("role", getJsonString(user, "role"));
                        userData.put("status", getJsonString(user, "status"));
                        userData.put("shopName", getJsonString(user, "shopName"));
                        userData.put("rating", getJsonStringAsDouble(user, "rating", 1));
                        if (user.has("isVerified") && !user.get("isVerified").isJsonNull()) {
                            userData.put("isVerified", user.get("isVerified").getAsBoolean() ? "Đã xác thực" : "Chưa");
                        } else {
                            userData.put("isVerified", "N/A");
                        }
                        usersList.add(userData);
                    }
                    tableUsers.setItems(usersList);
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Lỗi Phân Tích", "Không thể phân tích danh sách người dùng: " + e.getMessage());
                }
            }
        }));
    }

    @FXML
    private void loadProductAnalytics(ActionEvent event) {
        AuctionNetwork.getActiveAuctionsAsync().thenAccept(auctionResponse -> {
            if ("SUCCESS".equals(auctionResponse.getStatus()) && auctionResponse.getData() != null) {
                JsonArray auctionsArray = gson.toJsonTree(auctionResponse.getData()).getAsJsonArray();
                Set<Long> auctionedItemIds = FXCollections.observableSet();
                for (JsonElement auctionElement : auctionsArray) {
                    AuctionDetailDTO auction = gson.fromJson(auctionElement, AuctionDetailDTO.class);
                    auctionedItemIds.add(auction.getItemId());
                }

                AdminNetwork.getProductAnalytics().thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus()) && response.getData() != null) {
                        try {
                            JsonArray itemsArray = gson.toJsonTree(response.getData()).getAsJsonArray();
                            ObservableList<ItemResponseDTO> itemsList = FXCollections.observableArrayList();
                            for (JsonElement itemElement : itemsArray) {
                                ItemResponseDTO item = gson.fromJson(itemElement, ItemResponseDTO.class);
                                if (auctionedItemIds.contains(item.getId())) {
                                    item.setStatus("Đang đấu giá");
                                } else {
                                    item.setStatus("Chưa có phiên đấu giá");
                                }
                                itemsList.add(item);
                            }
                            tableItems.setItems(itemsList);
                        } catch (Exception e) {
                            e.printStackTrace();
                            showAlert("Lỗi Phân Tích", "Không thể phân tích dữ liệu sản phẩm: " + e.getMessage());
                        }
                    }
                }));
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String getJsonString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "N/A";
    }

    private String getJsonStringAsInteger(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsBigDecimal().toPlainString();
        }
        return "N/A";
    }

    private String getJsonStringAsDouble(JsonObject obj, String key, int decimalPlaces) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            double value = obj.get(key).getAsDouble();
            return String.format("%." + decimalPlaces + "f", value);
        }
        return "N/A";
    }

    // Lớp nội để hiển thị nút Ban/Unban
    private static class StatusCell extends TableCell<Map<String, String>, String> {
        private final HBox hBox = new HBox(5);
        private final Label statusLabel = new Label();
        private final Button actionButton = new Button();
        private final AdminDashboardController controller;

        public StatusCell(AdminDashboardController controller) {
            this.controller = controller;
            hBox.getChildren().addAll(statusLabel, actionButton);
            actionButton.setOnAction(event -> {
                Map<String, String> user = getTableView().getItems().get(getIndex());
                String username = user.get("username");
                String currentStatus = user.get("status");
                if ("ACTIVE".equals(currentStatus)) {
                    AdminNetwork.banUser(username, "Banned by admin").thenAccept(response -> {
                        if ("SUCCESS".equals(response.getStatus())) {
                            Platform.runLater(() -> controller.loadAllUsers());
                        }
                    });
                } else if ("BANNED".equals(currentStatus)) {
                    AdminNetwork.unbanUser(username).thenAccept(response -> {
                        if ("SUCCESS".equals(response.getStatus())) {
                            Platform.runLater(() -> controller.loadAllUsers());
                        }
                    });
                }
            });
        }

        @Override
        protected void updateItem(String status, boolean empty) {
            super.updateItem(status, empty);

            if (empty || status == null || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                setGraphic(null);
                return;
            }

            Map<String, String> user = getTableView().getItems().get(getIndex());
            String role = user.get("role");

            statusLabel.setText(status);
            actionButton.setVisible(true); // Hiển thị nút theo mặc định

            if ("ADMIN".equals(role)) {
                actionButton.setVisible(false); // Ẩn nút nếu là ADMIN
            } else if ("ACTIVE".equals(status)) {
                actionButton.setText("Ban");
                actionButton.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white;");
            } else if ("BANNED".equals(status)) {
                actionButton.setText("Unban");
                actionButton.setStyle("-fx-background-color: #10B981; -fx-text-fill: white;");
            } else {
                actionButton.setVisible(false); // Ẩn nút cho các trạng thái khác
            }
            setGraphic(hBox);
        }
    }

    @FXML private void refreshData(ActionEvent event) { /* ... */ }
    @FXML private void logout(ActionEvent event) { /* ... */ }
    @FXML private void handleSearchUser(ActionEvent event) { /* ... */ }
    @FXML private void showAllUsers(ActionEvent event) { loadAllUsers(); }
    @FXML private void loadRevenueData(ActionEvent event) { /* ... */ }
    @FXML private void showApproveSellerDialog(ActionEvent event) {}
    @FXML private void showActivityLog(ActionEvent event) {}
}
