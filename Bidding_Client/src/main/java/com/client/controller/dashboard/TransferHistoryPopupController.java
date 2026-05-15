package com.client.controller.dashboard;

import com.client.network.SellerNetwork;
import com.client.session.ClientSession;
import com.shared.dto.TransferHistoryDTO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class TransferHistoryPopupController {

    @FXML private TableView<TransferHistoryDTO> tableTransferHistory;
    @FXML private TableColumn<TransferHistoryDTO, String> colTransferBuyerName;
    @FXML private TableColumn<TransferHistoryDTO, String> colTransferAmount;
    @FXML private TableColumn<TransferHistoryDTO, String> colTransferTime;
    @FXML private TableColumn<TransferHistoryDTO, String> colTransferItem;

    private final SellerNetwork sellerNetwork = new SellerNetwork();

    // Gson với TypeAdapter cho LocalDateTime — đọc cả dạng chuỗi ISO lẫn mảng số
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, ctx) -> {
                if (json.isJsonArray()) {
                    // Jackson mặc định serialize LocalDateTime thành mảng: [2025,5,15,10,30,0]
                    var arr = json.getAsJsonArray();
                    int year   = arr.get(0).getAsInt();
                    int month  = arr.get(1).getAsInt();
                    int day    = arr.get(2).getAsInt();
                    int hour   = arr.size() > 3 ? arr.get(3).getAsInt() : 0;
                    int minute = arr.size() > 4 ? arr.get(4).getAsInt() : 0;
                    int second = arr.size() > 5 ? arr.get(5).getAsInt() : 0;
                    return LocalDateTime.of(year, month, day, hour, minute, second);
                } else {
                    // Jackson với WRITE_DATES_AS_TIMESTAMPS=false serialize thành chuỗi ISO
                    return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
            })
            .create();

    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.of("vi", "VN"));
    private final ObservableList<TransferHistoryDTO> transferHistoryList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadTransferHistory();
    }

    private void setupTableColumns() {
        colTransferBuyerName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getBuyerName()));

        colTransferAmount.setCellValueFactory(data -> {
            BigDecimal amount = data.getValue().getAmount();
            return new SimpleStringProperty(currencyFormat.format(amount) + " VND");
        });

        colTransferTime.setCellValueFactory(data -> {
            LocalDateTime time = data.getValue().getTransferTime();
            if (time == null) return new SimpleStringProperty("N/A");
            return new SimpleStringProperty(time.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        });

        colTransferItem.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getItemName()));

        tableTransferHistory.setItems(transferHistoryList);
        tableTransferHistory.setPlaceholder(new Label("Chưa có giao dịch chuyển tiền nào."));
    }

    @FXML
    private void loadTransferHistory() {
        long sellerId = ClientSession.getUserId();
        sellerNetwork.getTransferHistory(sellerId)
                .thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        try {
                            String dataJson = gson.toJson(response.getData());
                            Type listType = new TypeToken<List<TransferHistoryDTO>>() {}.getType();
                            List<TransferHistoryDTO> history = gson.fromJson(dataJson, listType);

                            transferHistoryList.clear();
                            if (history != null && !history.isEmpty()) {
                                transferHistoryList.addAll(history);
                            }
                        } catch (Exception e) {
                            System.err.println("Lỗi parse lịch sử chuyển tiền: " + e.getMessage());
                        }
                    } else {
                        System.err.println("Lỗi lấy lịch sử chuyển tiền: " + response.getMessage());
                    }
                }))
                .exceptionally(e -> {
                    System.err.println("Lỗi kết nối lấy lịch sử chuyển tiền: " + e.getMessage());
                    return null;
                });
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadTransferHistory();
    }

    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}