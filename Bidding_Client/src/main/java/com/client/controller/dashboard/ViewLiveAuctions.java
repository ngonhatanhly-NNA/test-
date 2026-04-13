package com.client.controller.dashboard;

import com.client.network.AuctionNetwork;
import com.client.util.ClientSession;
import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.AuctionUpdateDTO;
import com.shared.network.Response;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewLiveAuctions {

    @FXML
    private Label currentPriceLabel;
    @FXML
    private TextField bidAmountField;
    @FXML
    private CheckBox enableAutoBidCheckBox;
    @FXML
    private TextField maxAutoBidField;
    @FXML
    private Label autoBidStatusLabel;
    @FXML
    private Label itemNameLabel;
    @FXML
    private Label leaderLabel;
    @FXML
    private Label stepHintLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label remainingLabel;
    @FXML
    private Button refreshButton;

    private long currentAuctionId;
    private final ExecutorService ioPool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "auction-http");
        t.setDaemon(true);
        return t;
    });

    private static ViewLiveAuctions instance;

    public ViewLiveAuctions() {
        instance = this;
    }

    public static ViewLiveAuctions getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        if (maxAutoBidField != null) {
            maxAutoBidField.setVisible(false);
        }
        if (autoBidStatusLabel != null) {
            autoBidStatusLabel.setVisible(false);
        }
        if (enableAutoBidCheckBox != null) {
            enableAutoBidCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (maxAutoBidField != null) {
                    maxAutoBidField.setVisible(newVal);
                }
            });
        }
        handleRefreshAuctions();
    }

    @FXML
    public void handleRefreshAuctions() {
        if (refreshButton != null) {
            refreshButton.setDisable(true);
        }
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String body = AuctionNetwork.getActiveAuctions();
                Response res = AuctionNetwork.parseResponse(body);
                List<AuctionDetailDTO> list = AuctionNetwork.parseActiveAuctionList(res);
                Platform.runLater(() -> {
                    if (refreshButton != null) {
                        refreshButton.setDisable(false);
                    }
                    if (!"SUCCESS".equals(res.getStatus())) {
                        showError(res != null ? res.getMessage() : "Không tải được danh sách đấu giá.");
                        return;
                    }
                    if (list.isEmpty()) {
                        showError("Chưa có phiên đấu giá đang mở trên server (hoặc DB/cache trống).");
                        return;
                    }
                    applyAuctionDetail(list.get(0));
                });
                return null;
            }
        };
        task.setOnFailed(e -> Platform.runLater(() -> {
            if (refreshButton != null) {
                refreshButton.setDisable(false);
            }
            Throwable ex = task.getException();
            showError("Lỗi mạng: " + (ex != null ? ex.getMessage() : "unknown"));
            if (ex != null) {
                ex.printStackTrace();
            }
        }));
        ioPool.execute(task);
    }

    private void applyAuctionDetail(AuctionDetailDTO a) {
        currentAuctionId = a.getAuctionId();
        if (itemNameLabel != null) {
            itemNameLabel.setText(a.getItemName() != null ? a.getItemName() : ("Item #" + a.getItemId()));
        }
        if (currentPriceLabel != null && a.getCurrentPrice() != null) {
            currentPriceLabel.setText(formatMoney(a.getCurrentPrice()) + " VNĐ");
        }
        if (leaderLabel != null) {
            leaderLabel.setText("Người dẫn đầu: " + (a.getHighestBidderName() != null ? a.getHighestBidderName() : "—"));
        }
        if (stepHintLabel != null && a.getStepPrice() != null) {
            stepHintLabel.setText("* Bước giá tối thiểu: " + formatMoney(a.getStepPrice()) + " VNĐ");
        }
        if (statusLabel != null) {
            statusLabel.setText("Tình trạng: ĐANG MỞ");
        }
        if (remainingLabel != null) {
            remainingLabel.setText("Còn lại: " + formatRemaining(a.getRemainingTime()));
        }
    }

    private static String formatMoney(BigDecimal v) {
        return v == null ? "0" : v.toPlainString();
    }

    private static String formatRemaining(long millis) {
        if (millis <= 0) {
            return "hết giờ (kiểm tra server)";
        }
        long sec = millis / 1000;
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        if (h > 0) {
            return h + " giờ " + m + " phút";
        }
        if (m > 0) {
            return m + " phút " + s + " giây";
        }
        return s + " giây";
    }

    @FXML
    public void handlePlaceBid() {
        if (currentAuctionId <= 0) {
            showError("Chưa có phiên đấu giá. Bấm \"Làm mới\" sau khi server đã chạy.");
            return;
        }
        if (!ClientSession.isLoggedIn()) {
            showError("Vui lòng đăng nhập (Bidder) trước khi đặt giá.");
            return;
        }
        ioPool.execute(() -> {
            try {
                BigDecimal amount = new BigDecimal(bidAmountField.getText().trim());
                long bidderId = ClientSession.getUserId();
                String raw;
                if (enableAutoBidCheckBox != null && enableAutoBidCheckBox.isSelected()) {
                    BigDecimal maxAutoBid = new BigDecimal(maxAutoBidField.getText().trim());
                    if (maxAutoBid.compareTo(amount) < 0) {
                        Platform.runLater(() -> showError("Giá tối đa auto-bid phải ≥ giá đặt hiện tại."));
                        return;
                    }
                    raw = AuctionNetwork.placeBidWithAutoBid(currentAuctionId, bidderId, amount, maxAutoBid);
                } else {
                    raw = AuctionNetwork.placeBid(currentAuctionId, bidderId, amount);
                }
                Response res = AuctionNetwork.parseResponse(raw);
                Platform.runLater(() -> {
                    if ("SUCCESS".equals(res.getStatus())) {
                        showInfo(res.getMessage());
                        if (autoBidStatusLabel != null && enableAutoBidCheckBox != null && enableAutoBidCheckBox.isSelected()) {
                            autoBidStatusLabel.setVisible(true);
                            autoBidStatusLabel.setText("✓ Auto-bid bật");
                            autoBidStatusLabel.setStyle("-fx-text-fill: green;");
                        }
                    } else {
                        showError(res.getMessage());
                    }
                });
            } catch (NumberFormatException e) {
                Platform.runLater(() -> showError("Vui lòng nhập số tiền hợp lệ."));
            } catch (Exception e) {
                Platform.runLater(() -> showError("Lỗi khi đặt giá: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    @FXML
    public void handleCancelAutoBid() {
        if (currentAuctionId <= 0) {
            showError("Chưa có phiên đấu giá. Bấm \"Làm mới\".");
            return;
        }
        if (!ClientSession.isLoggedIn()) {
            showError("Vui lòng đăng nhập.");
            return;
        }
        ioPool.execute(() -> {
            try {
                String raw = AuctionNetwork.cancelAutoBid(currentAuctionId, ClientSession.getUserId());
                Response res = AuctionNetwork.parseResponse(raw);
                Platform.runLater(() -> {
                    if ("SUCCESS".equals(res.getStatus())) {
                        showInfo(res.getMessage());
                        if (autoBidStatusLabel != null) {
                            autoBidStatusLabel.setVisible(true);
                            autoBidStatusLabel.setText("✗ Auto-bid đã hủy");
                            autoBidStatusLabel.setStyle("-fx-text-fill: red;");
                        }
                        if (enableAutoBidCheckBox != null) {
                            enableAutoBidCheckBox.setSelected(false);
                        }
                    } else {
                        showError(res.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Lỗi khi hủy auto-bid: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    @FXML
    public void handleUpdateAutoBid() {
        if (currentAuctionId <= 0) {
            showError("Chưa có phiên đấu giá. Bấm \"Làm mới\".");
            return;
        }
        if (!ClientSession.isLoggedIn()) {
            showError("Vui lòng đăng nhập.");
            return;
        }
        ioPool.execute(() -> {
            try {
                BigDecimal newMaxBid = new BigDecimal(maxAutoBidField.getText().trim());
                String raw = AuctionNetwork.updateAutoBid(currentAuctionId, ClientSession.getUserId(), newMaxBid);
                Response res = AuctionNetwork.parseResponse(raw);
                Platform.runLater(() -> {
                    if ("SUCCESS".equals(res.getStatus())) {
                        showInfo(res.getMessage());
                        if (autoBidStatusLabel != null) {
                            autoBidStatusLabel.setVisible(true);
                            autoBidStatusLabel.setText("✓ Auto-bid cập nhật (tối đa: " + newMaxBid.toPlainString() + ")");
                            autoBidStatusLabel.setStyle("-fx-text-fill: green;");
                        }
                    } else {
                        showError(res.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Lỗi cập nhật auto-bid: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    public void updatePriceRealtime(AuctionUpdateDTO updateData) {
        if (updateData == null || updateData.getAuctionId() != currentAuctionId) {
            return;
        }
        Platform.runLater(() -> {
            if (currentPriceLabel != null && updateData.getCurrentPrice() != null) {
                currentPriceLabel.setText(formatMoney(updateData.getCurrentPrice()) + " VNĐ");
            }
            if (leaderLabel != null && updateData.getHighestBidderName() != null) {
                leaderLabel.setText("Người dẫn đầu: " + updateData.getHighestBidderName());
            }
            if (remainingLabel != null) {
                remainingLabel.setText("Còn lại: " + formatRemaining(updateData.getRemainingTime()));
            }
        });
    }

    private void showError(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Đấu giá");
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private void showInfo(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Đấu giá");
        a.setHeaderText(null);
        a.setContentText(message != null ? message : "Thành công");
        a.showAndWait();
    }
}
