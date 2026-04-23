package com.client.controller.dashboard;

import com.client.network.SellerNetwork;
import com.client.network.AuctionNetwork;
import com.client.session.ClientSession;
import com.client.util.SceneController;
import com.shared.network.Response;
import com.shared.dto.AuctionDetailDTO;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.scene.layout.GridPane;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SellerDashboardController - Giao diện quản trị cho Seller
 * Bao gồm chức năng bán hàng (seller) và đấu giá (bidder)
 */
public class SellerDashboardController {

    @FXML private Label lblShopName;
    @FXML private Label lblRating;
    @FXML private Label lblTotalItems;
    @FXML private Label lblBalance;
    @FXML private Label lblCurrentBalance;

    @FXML private ListView<String> listViewActivities;

    @FXML private TextArea textAreaStatistics;
    @FXML private TextArea textAreaTransactions;

    @FXML private TableView<?> tableItems;
    @FXML private TextField txtSearchItem;

    @FXML private TextField txtShopName;
    @FXML private TextField txtBankAccount;
    @FXML private TextField txtWithdrawAmount;

    // Bidding/Auction fields
    @FXML private ListView<String> listViewAuctions;
    @FXML private Label lblAuctionItemName;
    @FXML private Label lblCurrentAuctionPrice;
    @FXML private Label lblLeadingBidder;
    @FXML private Label lblTimeRemaining;
    @FXML private Label lblBidStep;
    @FXML private TextField txtBidAmount;
    @FXML private CheckBox chkAutoBid;
    @FXML private TextField txtMaxAutoBidAmount;
    @FXML private Label lblBidStatus;

    private final SellerNetwork sellerNetwork = new SellerNetwork();
    private final Gson gson = new Gson();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "seller-auction-thread");
        t.setDaemon(true);
        return t;
    });

    private long currentAuctionId = -1;
    private List<AuctionDetailDTO> currentAuctions;

    @FXML
    public void initialize() {
        // Setup auto bid visibility
        if (chkAutoBid != null && txtMaxAutoBidAmount != null) {
            txtMaxAutoBidAmount.setVisible(false);
            chkAutoBid.selectedProperty().addListener((obs, oldVal, newVal) -> {
                txtMaxAutoBidAmount.setVisible(newVal);
            });
        }

        // Tải dữ liệu dashboard khi khởi tạo
        loadDashboardData();
    }

    /**
     * Tải dữ liệu dashboard (thống kê)
     */
    @FXML
    private void loadDashboardData() {
        long sellerId = ClientSession.getUserId();
        sellerNetwork.getSellerById(sellerId)
                .thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        try {
                            JsonObject data = gson.toJsonTree(response.getData()).getAsJsonObject();
                            String shopName = data.has("shopName") ? data.get("shopName").getAsString() : "Cửa hàng";
                            double rating = data.has("rating") ? data.get("rating").getAsDouble() : 0.0;

                            lblShopName.setText(shopName);
                            lblRating.setText(String.format("%.1f ★", rating));

                            // Tải thống kê
                            loadSellerStatistics();
                        } catch (Exception e) {
                            showAlert("Lỗi", "Không thể phân tích dữ liệu");
                        }
                    }
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showAlert("Lỗi", "Không thể tải dữ liệu dashboard"));
                    return null;
                });
    }

    /**
     * Làm mới dữ liệu
     */
    @FXML
    private void refreshData(ActionEvent event) {
        loadDashboardData();
    }

    /**
     * Đăng xuất
     */
    @FXML
    private void logout(ActionEvent event) {
        ClientSession.clear();
        SceneController.switchScene(event, "Login.fxml");
    }

    /**
     * Hiển thị thông tin cửa hàng
     */
    @FXML
    private void showShopInfo(ActionEvent event) {
        loadShopInfo(null);
    }

    /**
     * Tải thông tin cửa hàng từ server
     */
    @FXML
    private void loadShopInfo(ActionEvent event) {
        long sellerId = ClientSession.getUserId();
        sellerNetwork.getSellerById(sellerId)
                .thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        try {
                            JsonObject data = gson.toJsonTree(response.getData()).getAsJsonObject();
                            String shopName = data.has("shopName") ? data.get("shopName").getAsString() : "";
                            String bankAccount = data.has("bankAccountNumber") ? data.get("bankAccountNumber").getAsString() : "";

                            txtShopName.setText(shopName);
                            txtBankAccount.setText(bankAccount);

                            showAlert("Thông Tin Cửa Hàng",
                                "Tên cửa hàng: " + shopName + "\n" +
                                "Ngân hàng: " + bankAccount);
                        } catch (Exception e) {
                            showAlert("Lỗi", "Không thể phân tích dữ liệu");
                        }
                    } else {
                        showAlert("Lỗi", response.getMessage());
                    }
                }));
    }

    /**
     * Cập nhật thông tin cửa hàng
     */
    @FXML
    private void updateShopInfo(ActionEvent event) {
        showUpdateShopDialog();
    }

    /**
     * Xử lý cập nhật thông tin cửa hàng
     */
    @FXML
    private void handleUpdateShopInfo(ActionEvent event) {
        String newShopName = txtShopName.getText().trim();
        String newBankAccount = txtBankAccount.getText().trim();

        if (newShopName.isEmpty()) {
            showAlert("Cảnh báo", "Tên cửa hàng không được để trống");
            return;
        }

        long sellerId = ClientSession.getUserId();
        com.shared.dto.SellerProfileUpdateDTO updateData = new com.shared.dto.SellerProfileUpdateDTO();
        updateData.setShopName(newShopName);
        updateData.setBankAccountNumber(newBankAccount);

        sellerNetwork.updateSellerProfile(sellerId, updateData)
                .thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        showAlert("Thành Công", "Cập nhật thông tin cửa hàng thành công!");
                        loadShopInfo(null);
                    } else {
                        showAlert("Lỗi", response.getMessage());
                    }
                }));
    }

    /**
     * Hiển thị dialog cập nhật cửa hàng
     */
    private void showUpdateShopDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Cập Nhật Cửa Hàng");
        dialog.setHeaderText("Cập nhật thông tin cửa hàng");

        TextField tfShopName = new TextField();
        tfShopName.setPromptText("Tên cửa hàng");
        TextField tfBankAccount = new TextField();
        tfBankAccount.setPromptText("Số tài khoản ngân hàng");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Tên Cửa Hàng:"), 0, 0);
        grid.add(tfShopName, 1, 0);
        grid.add(new Label("Số Tài Khoản:"), 0, 1);
        grid.add(tfBankAccount, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return tfShopName.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(shopName -> {
            if (!shopName.isEmpty()) {
                long sellerId = ClientSession.getUserId();
                com.shared.dto.SellerProfileUpdateDTO updateData = new com.shared.dto.SellerProfileUpdateDTO();
                updateData.setShopName(shopName);
                updateData.setBankAccountNumber(tfBankAccount.getText());

                sellerNetwork.updateSellerProfile(sellerId, updateData)
                        .thenAccept(response -> Platform.runLater(() -> {
                            if ("SUCCESS".equals(response.getStatus())) {
                                showAlert("Thành Công", "Cập nhật thông tin cửa hàng thành công!");
                                loadShopInfo(null);
                            } else {
                                showAlert("Lỗi", response.getMessage());
                            }
                        }));
            }
        });
    }

    /**
     * Hiển thị danh sách sản phẩm
     */
    @FXML
    private void showAllItems(ActionEvent event) {
        long sellerId = ClientSession.getUserId();
        sellerNetwork.getSellerItems(sellerId)
                .thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        System.out.println("Items loaded: " + response.getData());
                        // TODO: Populate tableItems với dữ liệu
                    }
                }));
    }

    /**
     * Tìm kiếm sản phẩm
     */
    @FXML
    private void handleSearchItem(ActionEvent event) {
        String itemName = txtSearchItem.getText().trim();
        if (itemName.isEmpty()) {
            showAlert("Cảnh báo", "Vui lòng nhập tên sản phẩm");
            return;
        }
        // TODO: Gọi API tìm kiếm sản phẩm theo tên
        showAlert("Tìm Kiếm", "Tìm sản phẩm: " + itemName);
    }

    /**
     * Tạo sản phẩm mới
     */
    @FXML
    private void createNewItem(ActionEvent event) {
        // TODO: Mở dialog tạo sản phẩm mới
        showAlert("Tạo Sản Phẩm", "Mở form tạo sản phẩm mới");
    }

    /**
     * Hiển thị ví tiền
     */
    @FXML
    private void showBalance(ActionEvent event) {
        long sellerId = ClientSession.getUserId();
        sellerNetwork.getSellerById(sellerId)
                .thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        try {
                            JsonObject data = gson.toJsonTree(response.getData()).getAsJsonObject();
                            // Xử lý dữ liệu ví tiền
                            showAlert("Ví Tiền", "Dữ liệu: " + data.toString());
                        } catch (Exception e) {
                            showAlert("Lỗi", "Không thể phân tích dữ liệu");
                        }
                    }
                }));
    }

    /**
     * Rút tiền
     */
    @FXML
    private void withdrawMoney(ActionEvent event) {
        showWithdrawDialog();
    }

    /**
     * Xử lý rút tiền
     */
    @FXML
    private void handleWithdraw(ActionEvent event) {
        String amountStr = txtWithdrawAmount.getText().trim();
        if (amountStr.isEmpty()) {
            showAlert("Cảnh báo", "Vui lòng nhập số tiền cần rút");
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(amountStr);
            long sellerId = ClientSession.getUserId();

            sellerNetwork.withdrawMoney(sellerId, amount)
                    .thenAccept(response -> Platform.runLater(() -> {
                        if ("SUCCESS".equals(response.getStatus())) {
                            showAlert("Thành Công", "Rút tiền thành công!");
                            txtWithdrawAmount.clear();
                            loadDashboardData();
                        } else {
                            showAlert("Lỗi", response.getMessage());
                        }
                    }));
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Số tiền không hợp lệ");
        }
    }

    /**
     * Hiển thị dialog rút tiền
     */
    private void showWithdrawDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Rút Tiền");
        dialog.setHeaderText("Nhập số tiền cần rút (VND)");

        TextField tfAmount = new TextField();
        tfAmount.setPromptText("Số tiền");

        GridPane grid = new GridPane();
        grid.add(new Label("Số Tiền (VND):"), 0, 0);
        grid.add(tfAmount, 1, 0);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return tfAmount.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(amount -> {
            if (!amount.isEmpty()) {
                try {
                    BigDecimal withdrawAmount = new BigDecimal(amount);
                    long sellerId = ClientSession.getUserId();

                    sellerNetwork.withdrawMoney(sellerId, withdrawAmount)
                            .thenAccept(response -> Platform.runLater(() -> {
                                if ("SUCCESS".equals(response.getStatus())) {
                                    showAlert("Thành Công", "Rút tiền thành công!");
                                    loadDashboardData();
                                } else {
                                    showAlert("Lỗi", response.getMessage());
                                }
                            }));
                } catch (NumberFormatException e) {
                    showAlert("Lỗi", "Số tiền không hợp lệ");
                }
            }
        });
    }

    /**
     * Tải thống kê bán hàng
     */
    @FXML
    private void loadStatistics() {
        long sellerId = ClientSession.getUserId();
        sellerNetwork.getSellerStatistics(sellerId)
                .thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        textAreaStatistics.setText(
                                "Thống Kê Bán Hàng:\n\n" +
                                response.getData().toString()
                        );
                    } else {
                        textAreaStatistics.setText("Lỗi: " + response.getMessage());
                    }
                }));
    }

    /**
     * Hiển thị thống kê
     */
    @FXML
    private void showStatistics(ActionEvent event) {
        loadStatistics();
    }

    /**
     * Tải lịch sử giao dịch
     */
    @FXML
    private void loadTransactionHistory(ActionEvent event) {
        long sellerId = ClientSession.getUserId();
        sellerNetwork.getSellerStatistics(sellerId)
                .thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        textAreaTransactions.setText(
                                "Lịch Sử Giao Dịch:\n\n" +
                                response.getData().toString()
                        );
                    } else {
                        textAreaTransactions.setText("Lỗi: " + response.getMessage());
                    }
                }));
    }

    /**
     * Tải thống kê seller (gọi riêng cho dashboard)
     */
    private void loadSellerStatistics() {
        long sellerId = ClientSession.getUserId();
        sellerNetwork.getSellerStatistics(sellerId)
                .thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        try {
                            JsonObject data = gson.toJsonTree(response.getData()).getAsJsonObject();
                            // Cập nhật các label thống kê
                            System.out.println("Statistics loaded: " + data);
                        } catch (Exception e) {
                            System.out.println("Lỗi phân tích thống kê");
                        }
                    }
                }));
    }

    // ============================================================
    // BIDDING/AUCTION FUNCTIONS (Seller as Bidder)
    // ============================================================

    /**
     * Tải danh sách phiên đấu giá đang diễn ra
     */
    @FXML
    private void loadLiveAuctions() {
        executorService.submit(() -> {
            try {
                String responseBody = AuctionNetwork.getActiveAuctions();
                Response response = AuctionNetwork.parseResponse(responseBody);
                currentAuctions = AuctionNetwork.parseActiveAuctionList(response);

                Platform.runLater(() -> {
                    if (currentAuctions != null && !currentAuctions.isEmpty()) {
                        javafx.collections.ObservableList<String> auctionList = FXCollections.observableArrayList();
                        for (AuctionDetailDTO auction : currentAuctions) {
                            String displayText = String.format(
                                "[ID: %d] %s - Giá: %s VND - Trạng thái: %s",
                                auction.getAuctionId(),
                                auction.getItemName(),
                                auction.getCurrentPrice()
                            );
                            auctionList.add(displayText);
                        }
                        listViewAuctions.setItems(auctionList);
                        listViewAuctions.setCellFactory(lv -> new javafx.scene.control.ListCell<String>() {
                            @Override
                            protected void updateItem(String item, boolean empty) {
                                super.updateItem(item, empty);
                                setText(empty ? null : item);
                                setStyle(empty ? "" : "-fx-padding: 8px; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");
                            }
                        });

                        // Add click listener
                        listViewAuctions.setOnMouseClicked(e -> {
                            int selectedIndex = listViewAuctions.getSelectionModel().getSelectedIndex();
                            if (selectedIndex >= 0 && selectedIndex < currentAuctions.size()) {
                                displayAuctionDetails(currentAuctions.get(selectedIndex));
                            }
                        });
                    } else {
                        showAlert("Thông Báo", "Không có phiên đấu giá nào đang diễn ra");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi", "Không thể tải danh sách phiên đấu giá: " + e.getMessage()));
            }
        });
    }

    /**
     * Hiển thị chi tiết phiên đấu giá
     */
    private void displayAuctionDetails(AuctionDetailDTO auction) {
        currentAuctionId = auction.getAuctionId();

        lblAuctionItemName.setText(auction.getItemName());
        lblCurrentAuctionPrice.setText(String.format("%s VND", auction.getCurrentPrice()));
        lblLeadingBidder.setText(auction.getHighestBidderName() != null ? auction.getHighestBidderName() : "Chưa có ai đấu giá");
        lblBidStep.setText(String.format("%s VND", auction.getStepPrice() != null ? auction.getStepPrice() : "Không xác định"));

        lblBidStatus.setText("");
        txtBidAmount.clear();
        txtMaxAutoBidAmount.clear();
        chkAutoBid.setSelected(false);
    }

    /**
     * Đặt giá thầu
     */
    @FXML
    private void handlePlaceBid(ActionEvent event) {
        if (currentAuctionId <= 0) {
            showAlert("Cảnh báo", "Vui lòng chọn một phiên đấu giá");
            return;
        }

        String bidAmountStr = txtBidAmount.getText().trim();
        if (bidAmountStr.isEmpty()) {
            showAlert("Cảnh báo", "Vui lòng nhập số tiền cần đấu giá");
            return;
        }

        try {
            BigDecimal bidAmount = new BigDecimal(bidAmountStr);
            long bidderId = ClientSession.getUserId();

            executorService.submit(() -> {
                try {
                    String responseBody;

                    // Check if auto bid is enabled
                    if (chkAutoBid.isSelected()) {
                        String maxAutoBidStr = txtMaxAutoBidAmount.getText().trim();
                        if (maxAutoBidStr.isEmpty()) {
                            Platform.runLater(() -> showAlert("Cảnh báo", "Vui lòng nhập giá thầu tối đa cho đấu giá tự động"));
                            return;
                        }
                        BigDecimal maxAutoBid = new BigDecimal(maxAutoBidStr);
                        responseBody = AuctionNetwork.placeBidWithAutoBid(currentAuctionId, bidderId, bidAmount, maxAutoBid);
                    } else {
                        responseBody = AuctionNetwork.placeBid(currentAuctionId, bidderId, bidAmount);
                    }

                    Response response = AuctionNetwork.parseResponse(responseBody);
                    Platform.runLater(() -> {
                        if ("SUCCESS".equals(response.getStatus())) {
                            lblBidStatus.setText("✓ Đấu giá thành công!");
                            lblBidStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                            showAlert("Thành Công", "Đặt giá thầu thành công!");
                            txtBidAmount.clear();
                            txtMaxAutoBidAmount.clear();
                            chkAutoBid.setSelected(false);
                            loadLiveAuctions(); // Refresh list
                        } else {
                            lblBidStatus.setText("✗ " + response.getMessage());
                            lblBidStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                            showAlert("Lỗi", response.getMessage());
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        lblBidStatus.setText("✗ Lỗi: " + e.getMessage());
                        lblBidStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        showAlert("Lỗi", "Không thể đặt giá thầu: " + e.getMessage());
                    });
                }
            });
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Số tiền không hợp lệ");
        }
    }

    /**
     * Hủy đấu giá tự động
     */
    @FXML
    private void handleCancelAutoBid(ActionEvent event) {
        if (currentAuctionId <= 0) {
            showAlert("Cảnh báo", "Vui lòng chọn một phiên đấu giá");
            return;
        }

        long bidderId = ClientSession.getUserId();

        executorService.submit(() -> {
            try {
                String responseBody = AuctionNetwork.cancelAutoBid(currentAuctionId, bidderId);
                Response response = AuctionNetwork.parseResponse(responseBody);
                Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        lblBidStatus.setText("✓ Hủy đấu giá tự động thành công!");
                        lblBidStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        showAlert("Thành Công", "Hủy đấu giá tự động thành công!");
                    } else {
                        lblBidStatus.setText("✗ " + response.getMessage());
                        lblBidStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        showAlert("Lỗi", response.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblBidStatus.setText("✗ Lỗi: " + e.getMessage());
                    lblBidStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    showAlert("Lỗi", "Không thể hủy đấu giá tự động: " + e.getMessage());
                });
            }
        });
    }

    // ============================================================
    // END BIDDING FUNCTIONS
    // ============================================================

    /**
     * Hiển thị dialog thông báo
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}


