package com.client.controller.dashboard;

import com.client.network.AdminNetwork;
import com.client.session.ClientSession;
import com.client.util.SceneController;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.shared.network.Response;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

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
    @FXML private TableView<Map<String, String>> tableCategoryAnalytics;
    @FXML private TableColumn<Map<String, String>, String> colCategoryName;
    @FXML private TableColumn<Map<String, String>, String> colCategoryCount;
    @FXML private TableView<Map<String, String>> tableConditionAnalytics;
    @FXML private TableColumn<Map<String, String>, String> colConditionName;
    @FXML private TableColumn<Map<String, String>, String> colConditionCount;
    @FXML private TableView<Map<String, String>> tablePriceAnalytics;
    @FXML private TableColumn<Map<String, String>, String> colPriceStatName;
    @FXML private TableColumn<Map<String, String>, String> colPriceStatValue;
    
    // Financial Report
    @FXML private TextArea textAreaRevenue;

    private final Gson gson = new Gson();
    private boolean usersLoaded = false;
    private boolean itemAnalyticsLoaded = false;

    @FXML
    public void initialize() {
        setupUserTableColumns();
        setupItemAnalyticsTableColumns();
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
        colStatus.setCellFactory(param -> new StatusCell(this));
        colShopName.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getOrDefault("shopName", "N/A")));
        colRating.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getOrDefault("rating", "N/A")));
        colVerified.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getOrDefault("isVerified", "N/A")));
    }

    private void setupItemAnalyticsTableColumns() {
        colCategoryName.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().get("name")));
        colCategoryCount.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().get("count")));
        colConditionName.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().get("name")));
        colConditionCount.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().get("count")));
        colPriceStatName.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().get("name")));
        colPriceStatValue.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().get("value")));
    }

    @FXML
    private void loadDashboardData() {
        AdminNetwork.getAllUsers().thenAccept(userResponse -> Platform.runLater(() -> {
            if ("SUCCESS".equals(userResponse.getStatus()) && userResponse.getData() != null) {
                try {
                    JsonArray usersArray = gson.toJsonTree(userResponse.getData()).getAsJsonArray();
                    lblTotalUsers.setText(String.valueOf(usersArray.size()));
                } catch (Exception e) {
                    lblTotalUsers.setText("Lỗi");
                }
            }
        }));
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
        AdminNetwork.getProductAnalytics().thenAccept(response -> Platform.runLater(() -> {
            if ("SUCCESS".equals(response.getStatus()) && response.getData() != null) {
                try {
                    JsonObject data = gson.toJsonTree(response.getData()).getAsJsonObject();
                    
                    ObservableList<Map<String, String>> categoryList = FXCollections.observableArrayList();
                    if (data.has("phanLoai")) {
                        JsonObject phanLoai = data.getAsJsonObject("phanLoai");
                        for (String key : phanLoai.keySet()) {
                            Map<String, String> row = new HashMap<>();
                            row.put("name", key);
                            row.put("count", phanLoai.get(key).getAsString());
                            categoryList.add(row);
                        }
                    }
                    tableCategoryAnalytics.setItems(categoryList);

                    ObservableList<Map<String, String>> conditionList = FXCollections.observableArrayList();
                    if (data.has("tinhTrang")) {
                        JsonObject tinhTrang = data.getAsJsonObject("tinhTrang");
                        for (String key : tinhTrang.keySet()) {
                            Map<String, String> row = new HashMap<>();
                            row.put("name", key);
                            row.put("count", tinhTrang.get(key).getAsString());
                            conditionList.add(row);
                        }
                    }
                    tableConditionAnalytics.setItems(conditionList);
                    
                    ObservableList<Map<String, String>> priceList = FXCollections.observableArrayList();
                    if (data.has("thongKeGia")) {
                        JsonObject thongKeGia = data.getAsJsonObject("thongKeGia");
                        priceList.add(createPriceRow("Giá cao nhất", thongKeGia.get("giaCaoNhat")));
                        priceList.add(createPriceRow("Giá thấp nhất", thongKeGia.get("giaThapNhat")));
                        priceList.add(createPriceRow("Giá trung bình", thongKeGia.get("giaTrungBinh")));
                    }
                    tablePriceAnalytics.setItems(priceList);

                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Lỗi Phân Tích", "Không thể phân tích dữ liệu sản phẩm: " + e.getMessage());
                }
            }
        }));
    }
    
    private Map<String, String> createPriceRow(String name, JsonElement value) {
        Map<String, String> row = new HashMap<>();
        row.put("name", name);
        if (value != null && !value.isJsonNull()) {
            BigDecimal number = value.getAsBigDecimal();
            row.put("value", String.format("%,.0f", number));
        } else {
            row.put("value", "N/A");
        }
        return row;
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

    @FXML private void refreshData(ActionEvent event) { /* ... */ }
    @FXML private void logout(ActionEvent event) { /* ... */ }
    @FXML private void handleSearchUser(ActionEvent event) { /* ... */ }
    @FXML private void showAllUsers(ActionEvent event) { loadAllUsers(); }
    @FXML private void loadRevenueData(ActionEvent event) { /* ... */ }
    @FXML private void showApproveSellerDialog(ActionEvent event) {}
    @FXML private void showActivityLog(ActionEvent event) {}
}
