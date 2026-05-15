package com.client.controller.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.client.network.AuctionNetwork;
import com.client.network.MyWebSocketClient;
import com.client.session.ClientSession;
import com.client.util.BidHistoryUtil;
import com.client.util.ToastUtil; 
import com.client.util.WinnerBoardUtil;
import com.shared.dto.*;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class ViewLiveAuctions {

    private static final Logger logger = Logger.getLogger(ViewLiveAuctions.class.getName());

    // --- FXML BINDINGS ---
    @FXML private Label currentPriceLabel, itemNameLabel, itemTypeLabel, itemDescriptionLabel;
    @FXML private Label sellerNameLabel, leaderLabel, stepHintLabel, statusLabel, remainingLabel, autoBidStatusLabel;
    @FXML private TextField bidAmountField, maxAutoBidField, customStepPriceField;
    @FXML private CheckBox enableAutoBidCheckBox;
    @FXML private VBox itemSpecificsBox, auctionsContainer, auctionListSidebar;
    @FXML private Button refreshButton, historyButton;
    @FXML private ImageView itemImageView;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private XYChart.Series<String, Number> priceSeries;

    // --- SERVICES & STATE ---
    private final AuctionActionService actionService = new AuctionActionService();
    private final LiveAuctionViewModel viewModel = new LiveAuctionViewModel();
    private final Map<Long, List<BidHistoryDTO>> localBidLogs = new HashMap<>();
    
    @FXML private long currentAuctionId;
    private Timeline countdownTimeline;
    private static ViewLiveAuctions instance;

    public ViewLiveAuctions() { instance = this; }
    public static ViewLiveAuctions getInstance() { return instance; }
    public static ViewLiveAuctions getExistingInstance() { return instance; }

    @FXML
    public void initialize() {
        if (maxAutoBidField != null) { maxAutoBidField.setVisible(false); maxAutoBidField.setManaged(false); }
        if (customStepPriceField != null) { customStepPriceField.setVisible(false); customStepPriceField.setManaged(false); }
        if (autoBidStatusLabel != null) autoBidStatusLabel.setVisible(false);
        
        if (enableAutoBidCheckBox != null) {
            enableAutoBidCheckBox.selectedProperty().addListener((obs, o, n) -> {
                if (maxAutoBidField != null) { maxAutoBidField.setVisible(n); maxAutoBidField.setManaged(n); }
                if (customStepPriceField != null) { customStepPriceField.setVisible(n); customStepPriceField.setManaged(n); }
            });
        }

        // --- MVVM DATA BINDING ---
        if (currentPriceLabel != null) currentPriceLabel.textProperty().bind(viewModel.currentPriceProperty());
        if (leaderLabel != null) leaderLabel.textProperty().bind(viewModel.leaderProperty());
        if (itemNameLabel != null) itemNameLabel.textProperty().bind(viewModel.itemNameProperty());
        if (itemTypeLabel != null) itemTypeLabel.textProperty().bind(viewModel.itemTypeProperty());
        if (statusLabel != null) statusLabel.textProperty().bind(viewModel.statusProperty());
        if (remainingLabel != null) remainingLabel.textProperty().bind(viewModel.remainingTimeProperty());
        if (stepHintLabel != null) stepHintLabel.textProperty().bind(viewModel.stepHintProperty());
        if (bidAmountField != null) bidAmountField.disableProperty().bind(viewModel.isAuctionActiveProperty().not());
        if (enableAutoBidCheckBox != null) enableAutoBidCheckBox.disableProperty().bind(viewModel.isAuctionActiveProperty().not());

        viewModel.remainingTimeProperty().addListener((obs, o, n) -> {
            if (remainingLabel == null) return;
            long rem = viewModel.getRemainingTimeMillis();
            remainingLabel.setStyle(rem > 0 && rem < 60000 ? "-fx-text-fill: #e74c3c; -fx-font-weight: bold;" : "-fx-text-fill: #A0A0A0;");
        });

        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Prices over time");
        if (priceChart != null) { priceChart.getData().add(priceSeries); priceChart.setAnimated(false); }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdownRealtime()));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();

        handleRefreshAuctions();
    }

    private void updateCountdownRealtime() {
        long now = System.currentTimeMillis();
        if (currentAuctionId > 0) viewModel.updateCountdown();

        if (auctionsContainer != null) {
            auctionsContainer.getChildren().forEach(node -> {
                if (node instanceof VBox card && card.getProperties().get("endTime") instanceof Long end 
                    && card.getProperties().get("timeLabel") instanceof Label lbl) {
                    lbl.setText("Còn lại: " + AuctionUIHelper.formatRemaining(end - now));
                }
            });
        }
    }

    @FXML
    public void handleRefreshAuctions() {
        if (refreshButton != null) refreshButton.setDisable(true);
        actionService.fetchActiveAuctions(list -> {
            if (refreshButton != null) refreshButton.setDisable(false);
            if (list == null || list.isEmpty()) { showError("Auction is not open"); return; }
            if (auctionsContainer != null) {
                auctionsContainer.getChildren().clear();
                list.forEach(a -> {
                    VBox card = AuctionUIHelper.createAuctionCard(a, selectedA -> {
                        applyAuctionDetail(selectedA);
                        showInfo("Đã chọn: " + selectedA.getItemName() + ". Bạn có thể đặt giá ngay bên cạnh!");
                    });
                    auctionsContainer.getChildren().add(card);
                });
            }
            applyAuctionDetail(list.get(0));
        }, err -> {
            if (refreshButton != null) refreshButton.setDisable(false);
            showError("Lỗi mạng: " + err.getMessage());
        });
    }

    public void updateConnectionStatus(boolean isConnected, String message) {
        Platform.runLater(() -> {
            if (statusLabel != null) statusLabel.setStyle(isConnected ? "-fx-text-fill: #28a745; -fx-font-weight: bold;" : "-fx-text-fill: #dc3545; -fx-font-weight: bold;");
        });
    }

    private void applyAuctionDetail(AuctionDetailDTO a) {
        currentAuctionId = a.getAuctionId();
        try { if (MyWebSocketClient.getInstance() != null) MyWebSocketClient.getInstance().joinRoom(currentAuctionId); } catch (Exception ignored) {}
        
        viewModel.setAuctionDetail(a); 

        if (itemDescriptionLabel != null) itemDescriptionLabel.setText(a.getItemDescription() != null ? a.getItemDescription() : "(Không có mô tả)");
        if (sellerNameLabel != null) sellerNameLabel.setText("Seller: " + (a.getSellerName() != null ? a.getSellerName() : "Seller_" + a.getSellerId()) + " | Kết thúc: " + a.getEndTime());
        
        AuctionUIHelper.renderItemSpecifics(a.getItemSpecifics(), itemSpecificsBox);
        AuctionUIHelper.updateItemImage(a, itemImageView, getClass());
        if (bidAmountField != null && a.getStepPrice() != null && bidAmountField.getText().trim().isEmpty()) bidAmountField.setText(AuctionUIHelper.formatMoney(a.getStepPrice()));
        
        updateCountdownRealtime();

        // 🚀 BƯỚC 3: DÙNG COMPLETABLE FUTURE TẢI LỊCH SỬ GIÁ CỰC GỌN
        CompletableFuture.supplyAsync(() -> {
            try { return AuctionNetwork.getBidHistory(a.getAuctionId()); } catch (Exception e) { return null; }
        }, actionService.getIoPool()).thenAcceptAsync(history -> {
            if (priceSeries == null) return;
            priceSeries.getData().clear();
            if (history != null && !history.isEmpty()) {
                localBidLogs.put(a.getAuctionId(), new ArrayList<>(history));
                history.forEach(bh -> priceSeries.getData().add(new XYChart.Data<>(bh.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")), bh.getBidAmount())));
            } else {
                priceSeries.getData().add(new XYChart.Data<>(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), a.getCurrentPrice() != null ? a.getCurrentPrice() : BigDecimal.ZERO));
            }
        }, Platform::runLater);
    }

    private boolean validateAction() {
        if (currentAuctionId <= 0) { showError("Chưa có phiên đấu giá. Bấm \"Làm mới\"."); return false; }
        if (!ClientSession.isLoggedIn()) { showError("Vui lòng đăng nhập."); return false; }
        return true;
    }

    @FXML
    private void handlePlaceBid() {
        if (!validateAction()) return;
        boolean isAuto = enableAutoBidCheckBox != null && enableAutoBidCheckBox.isSelected();
        actionService.placeBid(currentAuctionId, bidAmountField.getText(), isAuto, isAuto ? maxAutoBidField.getText() : null, isAuto && customStepPriceField != null ? customStepPriceField.getText() : null, 
            res -> {
                if ("SUCCESS".equals(res.getStatus())) {
                    showInfo("Đặt giá thành công!"); 
                    if (isAuto && autoBidStatusLabel != null) { autoBidStatusLabel.setVisible(true); autoBidStatusLabel.setText("✓ Auto-bid enter"); autoBidStatusLabel.setStyle("-fx-text-fill: green;"); }
                } else showError(res.getMessage());
            }, this::showError);
    }

    @FXML
    public void handleCancelAutoBid() {
        if (!validateAction()) return;
        actionService.cancelAutoBid(currentAuctionId, res -> {
            if ("SUCCESS".equals(res.getStatus())) {
                showInfo(res.getMessage());
                if (autoBidStatusLabel != null) { autoBidStatusLabel.setVisible(true); autoBidStatusLabel.setText("✗ Auto-bid cancelled"); autoBidStatusLabel.setStyle("-fx-text-fill: red;"); }
                if (enableAutoBidCheckBox != null) enableAutoBidCheckBox.setSelected(false);
            } else showError(res.getMessage());
        }, this::showError);
    }

    @FXML
    public void handleUpdateAutoBid() {
        if (!validateAction()) return;
        actionService.updateAutoBid(currentAuctionId, maxAutoBidField.getText(), customStepPriceField != null ? customStepPriceField.getText() : null, 
            res -> {
                if ("SUCCESS".equals(res.getStatus())) {
                    showInfo(res.getMessage());
                    if (autoBidStatusLabel != null) { autoBidStatusLabel.setVisible(true); autoBidStatusLabel.setText("✓ Auto-bid cập nhật"); autoBidStatusLabel.setStyle("-fx-text-fill: green;"); }
                } else showError(res.getMessage());
            }, this::showError);
    }
    
    @FXML
    private void handleOpenBidHistory() {
        if (currentAuctionId <= 0) { ToastUtil.showError("Vui lòng chọn một phiên đấu giá trước."); return; }
        Platform.runLater(() -> BidHistoryUtil.showBidHistoryBoard(currentAuctionId, localBidLogs.getOrDefault(currentAuctionId, new ArrayList<>())));
    }

    public void updatePriceRealtime(AuctionUpdateDTO updateData) {
        if (updateData == null) return;
        Platform.runLater(() -> {
            if (updateData.getAuctionId() == currentAuctionId) {
                viewModel.updateRealtime(updateData);
                localBidLogs.computeIfAbsent(updateData.getAuctionId(), k -> new ArrayList<>()).add(0, new BidHistoryDTO(0L, updateData.getHighestBidderName() != null ? updateData.getHighestBidderName() : "Khách", updateData.getCurrentPrice(), LocalDateTime.now(), false));
                updateCountdownRealtime();
                
                if (priceSeries != null && updateData.getCurrentPrice() != null) {
                    priceSeries.getData().add(new XYChart.Data<>(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), updateData.getCurrentPrice()));
                    if (priceSeries.getData().size() > 88) priceSeries.getData().remove(0);
                }
                updateBidAmountFieldHint(updateData);
            }
            if (auctionsContainer != null && !auctionsContainer.getChildren().isEmpty()) updateAuctionCardInList(updateData);
        });
    }

    // 🚀 BƯỚC 3: HÀM HELPER TẢI CHI TIẾT AUCTION BẰNG COMPLETABLE FUTURE
    private CompletableFuture<AuctionDetailDTO> fetchAuctionDetailAsync(long auctionId) {
        return CompletableFuture.supplyAsync(() -> {
            try { return AuctionNetwork.parseAuctionDetail(AuctionNetwork.parseResponse(AuctionNetwork.getAuctionDetail(auctionId))); } 
            catch (Exception e) { throw new RuntimeException(e); }
        }, actionService.getIoPool());
    }

    private void updateBidAmountFieldHint(AuctionUpdateDTO updateData) {
        if (bidAmountField == null || updateData.getCurrentPrice() == null) return;
        fetchAuctionDetailAsync(updateData.getAuctionId()).thenAcceptAsync(detail -> {
            if (detail != null && detail.getStepPrice() != null && stepHintLabel != null) {
                stepHintLabel.setText("* Bước giá: " + AuctionUIHelper.formatMoney(detail.getStepPrice()) + " VNĐ (Nhập số tiền muốn CỘNG THÊM)");
            }
        }, Platform::runLater).exceptionally(e -> null);
    }

    private void updateAuctionCardInList(AuctionUpdateDTO updateData) {
        boolean found = false;
        for (var node : auctionsContainer.getChildren()) {
            if (node.getUserData() instanceof Long id && id == updateData.getAuctionId() && node instanceof VBox card) {
                card.getChildren().stream().filter(c -> c instanceof Label).map(c -> (Label) c).forEach(lbl -> {
                    if (lbl.getText().startsWith("Giá:")) lbl.setText("Giá: " + AuctionUIHelper.formatMoney(updateData.getCurrentPrice()) + " đ");
                    if (lbl.getText().startsWith("Dẫn đầu:")) lbl.setText("Dẫn đầu: " + (updateData.getHighestBidderName() != null ? updateData.getHighestBidderName() : "—"));
                });
                card.getProperties().put("endTime", System.currentTimeMillis() + updateData.getRemainingTime());
                found = true; break;
            }
        }
        if (!found) {
            fetchAuctionDetailAsync(updateData.getAuctionId()).thenAcceptAsync(detail -> {
                if (detail != null) {
                    addOrUpdateAuctionRealtime(detail);
                    if (detail.getAuctionId() == currentAuctionId) applyAuctionDetail(detail);
                }
            }, Platform::runLater).exceptionally(e -> null);
        }
    }

    public void addOrUpdateAuctionRealtime(AuctionDetailDTO detail) {
        if (detail == null || auctionsContainer == null) return;
        for (var node : auctionsContainer.getChildren()) {
            if (node.getUserData() instanceof Long id && id == detail.getAuctionId() && node instanceof VBox card) {
                updateAuctionCardInList(new AuctionUpdateDTO(detail.getAuctionId(), detail.getCurrentPrice(), detail.getHighestBidderName(), detail.getRemainingTime()));
                return;
            }
        }
        VBox newCard = AuctionUIHelper.createAuctionCard(detail, selectedA -> { applyAuctionDetail(selectedA); showInfo("Đã chọn: " + selectedA.getItemName()); });
        auctionsContainer.getChildren().add(0, newCard);
        if (currentAuctionId <= 0) applyAuctionDetail(detail);
    }

    public void removeAuctionRealtime(long auctionId) {
        if (auctionsContainer == null) return;
        auctionsContainer.getChildren().removeIf(node -> node.getUserData() instanceof Long id && id == auctionId);
        if (auctionId == currentAuctionId) { currentAuctionId = 0; viewModel.lockAuction(); }
    }
    
    public void handleAuctionFinishedNoWinner(long auctionId) {
        boolean isViewing = (currentAuctionId == auctionId);
        removeAuctionRealtime(auctionId);
        if (isViewing) { ToastUtil.showInfo("Pathetic, no one bid this item!"); lockAuctionUI(); }
    }
    
    public void showWinnerNotification(AuctionWinnerDTO winnerData) {
        boolean isViewing = (currentAuctionId == winnerData.getAuctionId());
        removeAuctionRealtime(winnerData.getAuctionId());
        if (isViewing) { WinnerBoardUtil.showWinnerBoard(winnerData, ClientSession.getUserId()); lockAuctionUI(); }
    }
    
    @FXML private void handleToggleSidebar(ActionEvent event) {
        auctionListSidebar.setVisible(!auctionListSidebar.isVisible());
        auctionListSidebar.setManaged(auctionListSidebar.isVisible()); 
    }

    public void lockAuctionUI() {
        viewModel.lockAuction();
        if (statusLabel != null) statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
    }
    
    private void showError(String msg) { ToastUtil.showError(msg); }
    private void showInfo(String msg) { ToastUtil.showInfo(msg != null ? msg : "Thành công"); }
}