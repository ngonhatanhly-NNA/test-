package com.client.controller.dashboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import java.util.List;

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
	// Giao dịch
	@FXML private TableView<Map<String, String>> tableTransactions;
    @FXML private TableColumn<Map<String, String>, String> colTransId;
    @FXML private TableColumn<Map<String, String>, String> colTransUser;
    @FXML private TableColumn<Map<String, String>, String> colTransType;
    @FXML private TableColumn<Map<String, String>, String> colTransAmount;
    @FXML private TableColumn<Map<String, String>, String> colTransTime;
	
	// Lịch sử đấu giá
	@FXML private TableView<Map<String, String>> tableAdminAuctions;
    @FXML private TableColumn<Map<String, String>, String> colAucId;
    @FXML private TableColumn<Map<String, String>, String> colAucItem;
    @FXML private TableColumn<Map<String, String>, String> colAucStatus;
    @FXML private TableColumn<Map<String, String>, String> colAucEndTime;
    @FXML private TableColumn<Map<String, String>, Void> colAucAction; // Cột này chứa Nút bấm
	
    private final Gson gson = new GsonBuilder()
    .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, context) ->
        LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, type, context) ->
        new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
    .create();
    private boolean usersLoaded = false;
    private boolean itemAnalyticsLoaded = false;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.of("vi", "VN"));

    @FXML
    public void initialize() {
        // 1. Setup Table Columns cho Giao dịch
        colTransId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().get("id")));
        colTransUser.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().get("username")));
        colTransType.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().get("type")));
        colTransAmount.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().get("amount")));
        colTransTime.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().get("time")));

        // 2. Setup Table Columns cho Đấu giá
        colAucId.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().get("id")));
        colAucItem.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().get("itemName")));
        colAucStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().get("status")));
        colAucEndTime.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().get("endTime")));
        
        // 3. Setup Nút "Xem lịch sử" cho bảng Đấu giá
        setupAuctionHistoryButton();

        // 4. Setup các cột khác
        setupUserTableColumns();
        setupItemTableColumns();
        
        // 5. Setup Listener cho TabPane (Tự động load khi chuyển tab)
        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            if (newTab == null) return;
            String text = newTab.getText();
            if ("Transaction Logs".equals(text)) {
                loadGlobalTransactions();
            } else if ("Auction History".equals(text)) {
                loadAdminAuctions();
            } else if ("User Management".equals(text) && !usersLoaded) {
                loadAllUsers();
                usersLoaded = true;
            } else if ("Item Analysis".equals(text) && !itemAnalyticsLoaded) {
                loadProductAnalytics(null);
                itemAnalyticsLoaded = true;
            }
        });

        // 6. Load dữ liệu ban đầu
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
                            userData.put("isVerified", user.get("isVerified").getAsBoolean() ? "Verified" : "Unverified");
                        } else {
                            userData.put("isVerified", "N/A");
                        }
                        usersList.add(userData);
                    }
                    tableUsers.setItems(usersList);
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Parsing Error", "Failed to parse user list: " + e.getMessage());
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
                                    item.setStatus("In bidding process");
                                } else {
                                    item.setStatus("No auctions available");
                                }
                                itemsList.add(item);
                            }
                            tableItems.setItems(itemsList);
                        } catch (Exception e) {
                            e.printStackTrace();
                            showAlert("Parsing Error", "Failed to parse user list: " + e.getMessage());
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
	
	@FXML
    private void loadGlobalTransactions() {
        System.out.println(">>> Đang gọi API lấy Lịch sử Giao dịch...");
        com.client.network.AdminNetwork.getGlobalTransactions().thenAcceptAsync(res -> {
            System.out.println(">>> [API Giao Dịch] Status: " + res.getStatus());
            System.out.println(">>> [API Giao Dịch] Message: " + res.getMessage());
            System.out.println(">>> [API Giao Dịch] Data: " + res.getData());
            
            if ("SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                try {
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, String>>>(){}.getType();
                    java.util.List<java.util.Map<String, String>> list = new com.google.gson.Gson().fromJson(new com.google.gson.Gson().toJson(res.getData()), listType);
                    System.out.println(">>> [API Giao Dịch] Đã Parse được: " + list.size() + " dòng!");
                    if (tableTransactions != null) tableTransactions.getItems().setAll(list);
                } catch (Exception e) {
                    System.out.println(">>> [Lỗi Parse JSON Giao Dịch]: " + e.getMessage());
                }
            }
        }, javafx.application.Platform::runLater);
    }

    @FXML
    private void loadAdminAuctions() {
        System.out.println(">>> Đang gọi API lấy Lịch sử Đấu giá...");
        com.client.network.AdminNetwork.getAllAuctionsForAdmin().thenAcceptAsync(res -> {
            System.out.println(">>> [API Đấu Giá] Status: " + res.getStatus());
            System.out.println(">>> [API Đấu Giá] Message: " + res.getMessage());
            System.out.println(">>> [API Đấu Giá] Data: " + res.getData());
            
            if ("SUCCESS".equals(res.getStatus()) && res.getData() != null) {
                try {
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, String>>>(){}.getType();
                    java.util.List<java.util.Map<String, String>> list = new com.google.gson.Gson().fromJson(new com.google.gson.Gson().toJson(res.getData()), listType);
                    System.out.println(">>> [API Đấu Giá] Đã Parse được: " + list.size() + " dòng!");
                    if (tableAdminAuctions != null) tableAdminAuctions.getItems().setAll(list);
                } catch (Exception e) {
                    System.out.println(">>> [Lỗi Parse JSON Đấu Giá]: " + e.getMessage());
                }
            }
        }, javafx.application.Platform::runLater);
    }
	
	private void setupAuctionHistoryButton() {
		colAucAction.setCellFactory(param -> new TableCell<>() {
			private final Button btn = new Button("📜 Xem Lịch Sử");
			{
				btn.setOnAction(e -> {
					Map<String, String> row = getTableView().getItems().get(getIndex());
					String idStr = row.get("id");
					if (idStr == null) {
						Platform.runLater(() -> showAlert("Lỗi", "Không tìm thấy ID phiên đấu giá."));
						return;
					}
					long auctionId = Long.parseLong(idStr);
					AdminNetwork.getAuctionDetailedHistory(auctionId).thenAccept(res -> {
						if ("SUCCESS".equals(res.getStatus()) && res.getData() != null) {
							try {
								TypeToken<List<com.shared.dto.BidHistoryDTO>> token = new TypeToken<List<com.shared.dto.BidHistoryDTO>>() {};
								List<com.shared.dto.BidHistoryDTO> history = gson.fromJson(gson.toJson(res.getData()), token.getType());
								if (history.isEmpty()) {
									Platform.runLater(() -> showAlert("Thông báo", "Phiên đấu giá chưa có lượt đấu nào."));
								} else {
									Platform.runLater(() -> com.client.util.BidHistoryUtil.showBidHistoryBoard(auctionId, history));
								}
							} catch (Exception ex) {
								ex.printStackTrace();
								Platform.runLater(() -> showAlert("Lỗi", "Không thể parse dữ liệu: " + ex.getMessage()));
							}
						} else {
							Platform.runLater(() -> showAlert("Lỗi", "Không lấy được lịch sử: " + (res.getMessage() != null ? res.getMessage() : "Unknown")));
						}
					}).exceptionally(ex -> {
						ex.printStackTrace();
						Platform.runLater(() -> showAlert("Lỗi", "Lỗi kết nối: " + ex.getMessage()));
						return null;
					});
				});
			}
			@Override
			protected void updateItem(Void item, boolean empty) {
				super.updateItem(item, empty);
				setGraphic(empty ? null : btn);
			}
		});
	}
    @FXML private void refreshData(ActionEvent event) { /* ... */ }
    @FXML private void logout(ActionEvent event) { /* ... */ }
    @FXML private void handleSearchUser(ActionEvent event) { /* ... */ }
    @FXML private void showAllUsers(ActionEvent event) { loadAllUsers(); }
    @FXML private void loadRevenueData(ActionEvent event) { /* ... */ }
    @FXML private void showApproveSellerDialog(ActionEvent event) {}
    @FXML private void showActivityLog(ActionEvent event) {}
}
