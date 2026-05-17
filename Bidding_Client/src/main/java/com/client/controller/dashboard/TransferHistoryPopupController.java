package com.client.controller.dashboard;

import com.client.network.BidderNetwork;
import com.client.session.ClientSession;
import com.shared.dto.WalletTransactionDTO;
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

/**
 * Controller cho popup Lịch sử giao dịch ví.
 * Hiển thị lịch sử nạp tiền và trừ tiền khi thắng đấu giá của người dùng hiện tại.
 */
public class TransferHistoryPopupController {

    @FXML private TableView<WalletTransactionDTO> tableTransferHistory;
    @FXML private TableColumn<WalletTransactionDTO, String> colTransferType;
    @FXML private TableColumn<WalletTransactionDTO, String> colTransferAmount;
    @FXML private TableColumn<WalletTransactionDTO, String> colTransferTime;
    @FXML private TableColumn<WalletTransactionDTO, String> colTransferDescription;

    // Dùng BidderNetwork vì endpoint wallet-transactions nằm ở /api/bidders/{id}/wallet-transactions
    // (Seller cũng được map vào cùng endpoint này ở server)
    private final BidderNetwork bidderNetwork = new BidderNetwork();

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
    private final ObservableList<WalletTransactionDTO> transactionList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadWalletTransactions();
    }

    private void setupTableColumns() {
        // Cột loại giao dịch: DEPOSIT = Nạp tiền, DEDUCT = Trừ tiền
        colTransferType.setCellValueFactory(data -> {
            String type = data.getValue().getType();
            String display = "DEPOSIT".equals(type) ? "⬆ Nạp tiền" : "⬇ Trừ tiền";
            return new SimpleStringProperty(display);
        });

        // Cột số tiền (màu xanh nếu nạp, đỏ nếu trừ)
        colTransferAmount.setCellValueFactory(data -> {
            BigDecimal amount = data.getValue().getAmount();
            String type = data.getValue().getType();
            String prefix = "DEPOSIT".equals(type) ? "+" : "-";
            return new SimpleStringProperty(prefix + currencyFormat.format(amount) + " VNĐ");
        });

        // Cột thời gian
        colTransferTime.setCellValueFactory(data -> {
            LocalDateTime time = data.getValue().getCreatedAt();
            if (time == null) return new SimpleStringProperty("N/A");
            return new SimpleStringProperty(time.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        });

        // Cột mô tả giao dịch
        colTransferDescription.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDescription()));

        tableTransferHistory.setItems(transactionList);
        tableTransferHistory.setPlaceholder(new Label("Chưa có giao dịch nào."));
    }

    @FXML
    private void loadWalletTransactions() {
        long userId = ClientSession.getUserId();
        bidderNetwork.getWalletTransactions(userId)
                .thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        try {
                            String dataJson = gson.toJson(response.getData());
                            Type listType = new TypeToken<List<WalletTransactionDTO>>() {}.getType();
                            List<WalletTransactionDTO> transactions = gson.fromJson(dataJson, listType);

                            transactionList.clear();
                            if (transactions != null && !transactions.isEmpty()) {
                                transactionList.addAll(transactions);
                            }
                        } catch (Exception e) {
                            System.err.println("Lỗi parse lịch sử giao dịch ví: " + e.getMessage());
                        }
                    } else {
                        System.err.println("Lỗi lấy lịch sử giao dịch ví: " + response.getMessage());
                    }
                }))
                .exceptionally(e -> {
                    System.err.println("Lỗi kết nối lấy lịch sử giao dịch ví: " + e.getMessage());
                    return null;
                });
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadWalletTransactions();
    }

    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}