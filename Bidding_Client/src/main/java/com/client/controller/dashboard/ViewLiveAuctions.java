package com.client.controller.dashboard;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.client.util.BidHistoryUtil;
import com.client.network.AuctionNetwork;
import com.client.session.ClientSession;
import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.AuctionUpdateDTO;
import com.shared.dto.AuctionWinnerDTO;
import com.shared.network.Response;
import com.client.network.MyWebSocketClient;
import com.shared.dto.BidHistoryDTO;
import com.client.util.ToastUtil; 
import com.client.util.WinnerBoardUtil;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.event.ActionEvent;

public class ViewLiveAuctions {

    private static final Logger logger = Logger.getLogger(ViewLiveAuctions.class.getName());

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
    private TextField customStepPriceField;
    @FXML
    private Label itemNameLabel;
    @FXML
    private Label itemTypeLabel;
    @FXML
    private Label itemDescriptionLabel;
    @FXML
    private Label sellerNameLabel;
    @FXML
    private VBox itemSpecificsBox;
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
    @FXML
    private VBox auctionsContainer;
    @FXML 
    private Button historyButton;
    @FXML
    private ImageView itemImageView;
    @FXML
    private LineChart<String, Number> priceChart;
    @FXML
    private XYChart.Series<String, Number> priceSeries;
	@FXML private VBox auctionListSidebar;
    // === SỔ TAY LƯU TRỮ LỊCH SỬ CỤC BỘ ===
    private Map<Long, List<BidHistoryDTO>> localBidLogs = new HashMap<>();
    
    @FXML
    private long currentAuctionId;

    private long currentAuctionEndTimeMillis = 0;
    private Timeline countdownTimeline;

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
        if (instance == null) {
            throw new IllegalStateException("ViewLiveAuctions instance is not initialized yet.");
        }
        return instance;
    }

    public static ViewLiveAuctions getExistingInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        if (maxAutoBidField != null) {
            maxAutoBidField.setVisible(false);
            maxAutoBidField.setManaged(false);
        }
        if (customStepPriceField != null) {
            customStepPriceField.setVisible(false);
            customStepPriceField.setManaged(false);
        }
        if (autoBidStatusLabel != null) {
            autoBidStatusLabel.setVisible(false);
        }
        if (enableAutoBidCheckBox != null) {
            enableAutoBidCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (maxAutoBidField != null) {
                    maxAutoBidField.setVisible(newVal);
                    maxAutoBidField.setManaged(newVal);
                }
                if (customStepPriceField != null) {
                    customStepPriceField.setVisible(newVal);
                    customStepPriceField.setManaged(newVal);
                }
            });
        }

        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Prices over time");
        if (priceChart != null) {
            priceChart.getData().add(priceSeries);
            priceChart.setAnimated(false);
        }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateCountdownRealtime()));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();

        handleRefreshAuctions();
    }

    private void updateCountdownRealtime() {
        long now = System.currentTimeMillis();

        if (currentAuctionId > 0 && remainingLabel != null) {
            long remaining = currentAuctionEndTimeMillis - now;
            if (remaining < 0) remaining = 0;
            
            remainingLabel.setText("Còn lại: " + formatRemaining(remaining));
            
            if (remaining > 0 && remaining < 60000) {
                remainingLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); 
            } else {
                remainingLabel.setStyle("-fx-text-fill: #A0A0A0;");
            }
        }

        if (auctionsContainer != null) {
            for (var node : auctionsContainer.getChildren()) {
                if (node instanceof VBox card) {
                    Object endTimeObj = card.getProperties().get("endTime");
                    Object timeLblObj = card.getProperties().get("timeLabel");

                    if (endTimeObj instanceof Long && timeLblObj instanceof Label) {
                        long endTime = (Long) endTimeObj;
                        Label timeLbl = (Label) timeLblObj;
                        
                        long remaining = endTime - now;
                        if (remaining < 0) remaining = 0;
                        timeLbl.setText("Còn lại: " + formatRemaining(remaining));
                    }
                }
            }
        }
    }

    @FXML
    public void handleRefreshAuctions() {
        if (refreshButton != null) {
            refreshButton.setDisable(true);
        }
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<AuctionDetailDTO> list = AuctionNetwork.getActiveAuctions();
                Platform.runLater(() -> {
                    if (refreshButton != null) {
                        refreshButton.setDisable(false);
                    }
                    if (list == null || list.isEmpty()) {
                        showError("Auction is not open");
                        return;
                    }

                    if (auctionsContainer != null) {
                        auctionsContainer.getChildren().clear();
                    }

                    for (AuctionDetailDTO a : list) {
                        if (auctionsContainer != null) {
                            auctionsContainer.getChildren().add(createAuctionCard(a));
                        }
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
            if (ex != null) ex.printStackTrace();
        }));
        ioPool.execute(task);
    }

    public void updateConnectionStatus(boolean isConnected, String message) {
        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(message);
                if (isConnected) {
                    statusLabel.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                } else {
                    statusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                }
            }
        });
    }

    private void applyAuctionDetail(AuctionDetailDTO a) {
        currentAuctionId = a.getAuctionId();
        
        try {
            if (MyWebSocketClient.getInstance() != null) {
                MyWebSocketClient.getInstance().joinRoom(currentAuctionId);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Không thể kết nối WebSocket Room", e);
        }
        
        currentAuctionEndTimeMillis = System.currentTimeMillis() + a.getRemainingTime();

        if (itemNameLabel != null) itemNameLabel.setText(a.getItemName() != null ? a.getItemName() : ("Item #" + a.getItemId()));
        if (itemTypeLabel != null) itemTypeLabel.setText("Type: " + (a.getItemType() != null ? a.getItemType() : "GENERAL"));
        if (itemDescriptionLabel != null) itemDescriptionLabel.setText(a.getItemDescription() != null ? a.getItemDescription() : "(Không có mô tả)");
        if (sellerNameLabel != null) {
            String seller = (a.getSellerName() != null && !a.getSellerName().isBlank()) ? a.getSellerName() : ("Seller_" + a.getSellerId());
            sellerNameLabel.setText("Seller: " + seller + " | Bắt đầu: " + (a.getStartTime() != null ? a.getStartTime() : "—") +
                                   " | Kết thúc: " + (a.getEndTime() != null ? a.getEndTime() : "—"));
        }
        renderItemSpecifics(a.getItemSpecifics());
        updateItemImage(a);
        
        if (currentPriceLabel != null && a.getCurrentPrice() != null) {
            currentPriceLabel.setText(formatMoney(a.getCurrentPrice()) + " VNĐ");
        }
        if (leaderLabel != null) {
            leaderLabel.setText("Người dẫn đầu: " + (a.getHighestBidderName() != null ? a.getHighestBidderName() : "—"));
        }
        
        if (stepHintLabel != null && a.getStepPrice() != null) {
            stepHintLabel.setText("* Bước giá: " + formatMoney(a.getStepPrice()) + " VNĐ (Nhập số tiền muốn CỘNG THÊM)");
        }
        
        if (bidAmountField != null && a.getStepPrice() != null) {
            if (bidAmountField.getText().trim().isEmpty()) {
                bidAmountField.setText(formatMoney(a.getStepPrice()));
            }
        }
        
        if (statusLabel != null) statusLabel.setText("Tình trạng: ĐANG MỞ");
        
        updateCountdownRealtime();

        ioPool.execute(() -> {
            try {
                List<BidHistoryDTO> history = AuctionNetwork.getBidHistory(a.getAuctionId());
                
                // --- LƯU LỊCH SỬ CŨ VÀO SỔ TAY ---
                if (history != null) {
                    localBidLogs.put(a.getAuctionId(), new ArrayList<>(history));
                }

                Platform.runLater(() -> {
                    if (priceSeries != null) {
                        priceSeries.getData().clear();
                        if (history != null && !history.isEmpty()) {
                            for (BidHistoryDTO bh : history) {
                                String time = bh.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                                priceSeries.getData().add(new XYChart.Data<>(time, bh.getBidAmount()));
                            }
                        } else {
                            String timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                            priceSeries.getData().add(new XYChart.Data<>(timeNow, a.getCurrentPrice() != null ? a.getCurrentPrice() : BigDecimal.ZERO));
                        }
                    }
                });
            } catch (Exception e) {
                // Thay vì chỉ in ra dòng chữ vô thưởng vô phạt
                // Ta truyền thêm biến 'e' vào logger để nó in ra toàn bộ cây lỗi (Stack Trace)
                // Từ đó ta sẽ biết nó lỗi do Parse JSON, do HTTP 404, hay do đứt mạng.
                logger.log(Level.WARNING, "Cant load bid history: " + e.getMessage(), e);

                // Ép hệ thống in thẳng lỗi ra màn hình Terminal bằng màu đỏ
                e.printStackTrace();
            }
        });
    }

    private static String formatMoney(BigDecimal v) {
        return v == null ? "0" : v.toPlainString();
    }

    private static String formatRemaining(long millis) {
        if (millis <= 0) return "Hết giờ đấu giá";
        long sec = millis / 1000;
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        if (h > 0) return h + " giờ " + m + " phút " + s + " giây";
        if (m > 0) return m + " phút " + s + " giây";
        return s + " giây";
    }

    @FXML
    private void handlePlaceBid() {
        if (currentAuctionId <= 0) {
            showError("There isn't any auction. Enter /Refresh/");
            return;
        }
        if (!ClientSession.isLoggedIn()) {
            showError("Please log in before place bid");
            return;
        }

        ioPool.execute(() -> {
            try {
                String rawDetail = AuctionNetwork.getAuctionDetail(currentAuctionId);
                Response res = AuctionNetwork.parseResponse(rawDetail);
                AuctionDetailDTO detail = AuctionNetwork.parseAuctionDetail(res);

                Platform.runLater(() -> {
                    if (detail != null && detail.getStartTime() != null) {
                        try {
                            LocalDateTime startTime = LocalDateTime.parse(detail.getStartTime());
                            if (startTime.isAfter(LocalDateTime.now())) {
                                showError("Auction is not open: Time starts at " + detail.getStartTime());
                                return;
                            }
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Error parsing start time", e);
                        }
                    }

                    try {
                        BigDecimal addedAmount = new BigDecimal(bidAmountField.getText().trim());
                        BigDecimal stepPrice = (detail != null && detail.getStepPrice() != null) ? detail.getStepPrice() : BigDecimal.ZERO;
                        
                        if (addedAmount.compareTo(stepPrice) < 0) {
                            showError("Adding bid must be greater than: " + formatMoney(stepPrice) + " đ");
                            return;
                        }

                        long bidderId = ClientSession.getUserId();
                        String raw2;

                        if (enableAutoBidCheckBox != null && enableAutoBidCheckBox.isSelected()) {
                            BigDecimal maxAutoBid = new BigDecimal(maxAutoBidField.getText().trim());
                            BigDecimal customStep = null;
                            
                            if (customStepPriceField != null && !customStepPriceField.getText().trim().isEmpty()) {
                                customStep = new BigDecimal(customStepPriceField.getText().trim());
                            }
                            
                            raw2 = AuctionNetwork.placeBidWithAutoBid(currentAuctionId, bidderId, addedAmount, maxAutoBid, customStep);
                        } else {
                            raw2 = AuctionNetwork.placeBid(currentAuctionId, bidderId, addedAmount);
                        }
                        
                        Response res2 = AuctionNetwork.parseResponse(raw2);
                        
                        if ("SUCCESS".equals(res2.getStatus())) {
                            showInfo("Đặt giá thành công!"); 
                            if (autoBidStatusLabel != null && enableAutoBidCheckBox != null && enableAutoBidCheckBox.isSelected()) {
                                autoBidStatusLabel.setVisible(true);
                                autoBidStatusLabel.setText("✓ Auto-bid enter");
                                autoBidStatusLabel.setStyle("-fx-text-fill: green;");
                            }
                        } else {
                            showError(res2.getMessage());
                        }
                    } catch (NumberFormatException e) {
                        showError("Please enter correct number");
                    } catch (Exception e) {
                        showError("Error in bidding: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Error in checking auction: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        });
    }

    @FXML
    public void handleCancelAutoBid() {
        if (currentAuctionId <= 0) {
            showError("There isn't any Auction ->Refresh");
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
                            autoBidStatusLabel.setText("✗ Auto-bid cancelled");
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
                Platform.runLater(() -> showError("Error in quit auto-bid: " + e.getMessage()));
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
                BigDecimal customStep = null;
                if (customStepPriceField != null && !customStepPriceField.getText().trim().isEmpty()) {
                    customStep = new BigDecimal(customStepPriceField.getText().trim());
                }
                String raw = AuctionNetwork.updateAutoBid(currentAuctionId, ClientSession.getUserId(), newMaxBid, customStep);
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
            }
        });
    }
    
    // === XEM LỊCH SỬ TỪ SỔ TAY ===
    @FXML
    private void handleOpenBidHistory() {
        if (currentAuctionId <= 0) {
            ToastUtil.showError("Vui lòng chọn một phiên đấu giá trước.");
            return;
        }

        List<BidHistoryDTO> history = localBidLogs.getOrDefault(currentAuctionId, new ArrayList<>());
        Platform.runLater(() -> {
            BidHistoryUtil.showBidHistoryBoard(currentAuctionId, history);
        });
    }

    public void updatePriceRealtime(AuctionUpdateDTO updateData) {
        if (updateData == null) return;

        Platform.runLater(() -> {
            if (updateData.getAuctionId() == currentAuctionId) {
                updateCurrentAuctionDisplay(updateData);
            }
            if (auctionsContainer != null && auctionsContainer.getChildren().size() > 0) {
                updateAuctionCardInList(updateData);
            }
        });
    }

    private void updateCurrentAuctionDisplay(AuctionUpdateDTO updateData) {
        if (currentPriceLabel != null && updateData.getCurrentPrice() != null) {
            currentPriceLabel.setText(formatMoney(updateData.getCurrentPrice()) + " VNĐ");
        }
        if (leaderLabel != null && updateData.getHighestBidderName() != null) {
            leaderLabel.setText("Người dẫn đầu: " + updateData.getHighestBidderName());
            leaderLabel.setStyle("-fx-text-fill: #E0E0E0; -fx-font-weight: bold;");
        }

        // === GHI LOG REAL-TIME VÀO SỔ TAY ===
        List<BidHistoryDTO> logs = localBidLogs.computeIfAbsent(updateData.getAuctionId(), k -> new ArrayList<>());
        
        BidHistoryDTO newLog = new BidHistoryDTO(
            0L, // Tạm thời để ID bằng 0 vì DTO chỉ hỗ trợ bidderId
            updateData.getHighestBidderName() != null ? updateData.getHighestBidderName() : "Khách",
            updateData.getCurrentPrice(),
            LocalDateTime.now(),
            false
        );
        logs.add(0, newLog);
        // ======================================

        currentAuctionEndTimeMillis = System.currentTimeMillis() + updateData.getRemainingTime();
        updateCountdownRealtime();

        if (priceSeries != null && updateData.getCurrentPrice() != null) {
            String timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            priceSeries.getData().add(new XYChart.Data<>(timeNow, updateData.getCurrentPrice()));
            if (priceSeries.getData().size() > 88) {
                priceSeries.getData().remove(0);
            }
        }
        updateBidAmountFieldHint(updateData);
    }

    private void updateBidAmountFieldHint(AuctionUpdateDTO updateData) {
        if (bidAmountField == null || updateData.getCurrentPrice() == null) return;

        ioPool.execute(() -> {
            try {
                String rawDetail = AuctionNetwork.getAuctionDetail(updateData.getAuctionId());
                Response res = AuctionNetwork.parseResponse(rawDetail);
                AuctionDetailDTO fullDetail = AuctionNetwork.parseAuctionDetail(res);

                if (fullDetail != null && fullDetail.getStepPrice() != null) {
                    BigDecimal stepPrice = fullDetail.getStepPrice();
                    Platform.runLater(() -> {
                        if (stepHintLabel != null) {
                            stepHintLabel.setText("* Bước giá: " + formatMoney(stepPrice) + " VNĐ (Nhập số tiền muốn CỘNG THÊM)");
                        }
                    });
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to update bid hint: " + e.getMessage());
            }
        });
    }

    private void updateAuctionCardInList(AuctionUpdateDTO updateData) {
        boolean foundAndUpdated = false;
        for (var node : auctionsContainer.getChildren()) {
            if (node.getUserData() instanceof Long id && id == updateData.getAuctionId()) {
                if (node instanceof VBox card) {
                    applyUpdateToCard(card, updateData);
                    foundAndUpdated = true;
                    break;
                }
            }
        }

        if (!foundAndUpdated) {
            ioPool.execute(() -> {
                try {
                    String rawDetail = AuctionNetwork.getAuctionDetail(updateData.getAuctionId());
                    Response res = AuctionNetwork.parseResponse(rawDetail);
                    AuctionDetailDTO fullDetail = AuctionNetwork.parseAuctionDetail(res);

                    if (fullDetail != null) {
                        Platform.runLater(() -> {
                            addOrUpdateAuctionRealtime(fullDetail);
                            if (fullDetail.getAuctionId() == currentAuctionId) {
                                applyAuctionDetail(fullDetail);
                            }
                        });
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to fetch auction detail: " + e.getMessage());
                }
            });
        }
    }

    public void addOrUpdateAuctionRealtime(AuctionDetailDTO detail) {
        if (detail == null || auctionsContainer == null) return;

        for (var node : auctionsContainer.getChildren()) {
            if (node.getUserData() instanceof Long id && id == detail.getAuctionId()) {
                if (node instanceof VBox card) {
                    AuctionUpdateDTO updateDTO = new AuctionUpdateDTO(
                            detail.getAuctionId(), detail.getCurrentPrice(),
                            detail.getHighestBidderName(), detail.getRemainingTime());
                    applyUpdateToCard(card, updateDTO);
                }
                return;
            }
        }

        VBox newCard = createAuctionCard(detail);
        auctionsContainer.getChildren().add(0, newCard);
        if (currentAuctionId <= 0) {
            applyAuctionDetail(detail);
        }
    }

    public void removeAuctionRealtime(long auctionId) {
        if (auctionsContainer == null) return;
        auctionsContainer.getChildren().removeIf(node -> node.getUserData() instanceof Long id && id == auctionId);
        if (auctionId == currentAuctionId) {
            currentAuctionId = 0;
            if (statusLabel != null) statusLabel.setText("Tình trạng: ĐÃ KẾT THÚC");
            if (remainingLabel != null) remainingLabel.setText("Còn lại: Hết giờ đấu giá");
        }
    }

    private VBox createAuctionCard(AuctionDetailDTO a) {
        VBox card = new VBox(8);
        // Nhuộm đen nền thẻ, viền vàng nhạt bo góc cực mượt
        card.setStyle("-fx-background-color: #242424; " +
                      "-fx-border-color: rgba(212,175,55,0.3); " +
                      "-fx-border-radius: 12; " +
                      "-fx-background-radius: 12; " +
                      "-fx-padding: 15; " +
                      "-fx-cursor: hand; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 4);");
        card.setMaxWidth(Double.MAX_VALUE);

        Label nameLbl = new Label(a.getItemName() != null ? a.getItemName() : "Item #" + a.getItemId());
        // Tên vật phẩm màu trắng sáng
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #F5F5F5;");
        nameLbl.setWrapText(true);

        Label typeLbl = new Label("Loại: " + (a.getItemType() != null ? a.getItemType() : "GENERAL"));
        typeLbl.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 12px;"); // Xám nhạt

        Label specificsLbl = new Label(formatSpecificsPreview(a.getItemSpecifics()));
        specificsLbl.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 12px;");
        specificsLbl.setWrapText(true);

        String sellerName = (a.getSellerName() != null && !a.getSellerName().isBlank()) ? a.getSellerName() : ("Seller_" + a.getSellerId());
        Label sellerLbl = new Label("Người bán: " + sellerName);
        sellerLbl.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 12px;");

        Label priceLbl = new Label("Giá: " + formatMoney(a.getCurrentPrice()) + " đ");
        // Chữ Giá màu Vàng Gold
        priceLbl.setStyle("-fx-text-fill: #D4AF37; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label leaderLbl = new Label("Dẫn đầu: " + (a.getHighestBidderName() != null ? a.getHighestBidderName() : "—"));
        leaderLbl.setStyle("-fx-text-fill: #E0E0E0;"); // Xám sáng

        Label timeLbl = new Label("Còn lại: " + formatRemaining(a.getRemainingTime()));
        timeLbl.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 12px;");

        Label scheduleInfoLbl = new Label("Bắt đầu: " + (a.getStartTime() != null ? a.getStartTime() : "—") +
                                          " | Kết thúc: " + (a.getEndTime() != null ? a.getEndTime() : "—"));
        scheduleInfoLbl.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px; -fx-font-weight: bold;"); // Xanh lá sáng để hợp nền đen
        scheduleInfoLbl.setWrapText(true);

        Button btnSelect = new Button("Vào đấu giá");
        // Nút bấm Gradient vàng đen sang trọng
        btnSelect.setStyle("-fx-background-color: linear-gradient(to right, #D4AF37, #FFD700); " +
                           "-fx-text-fill: #121212; " +
                           "-fx-cursor: hand; " +
                           "-fx-font-weight: bold; " +
                           "-fx-background-radius: 8;");
        btnSelect.setMaxWidth(Double.MAX_VALUE);

        btnSelect.setOnAction(e -> {
            applyAuctionDetail(a);
            showInfo("Đã chọn: " + a.getItemName() + ". Bạn có thể đặt giá ngay bên cạnh!");
        });

        card.getChildren().addAll(nameLbl, typeLbl, sellerLbl, specificsLbl, priceLbl, leaderLbl, timeLbl, scheduleInfoLbl, btnSelect);
        card.setUserData(a.getAuctionId()); 
        
        card.getProperties().put("endTime", System.currentTimeMillis() + a.getRemainingTime());
        card.getProperties().put("timeLabel", timeLbl);

        return card;
    }

    private void applyUpdateToCard(VBox card, AuctionUpdateDTO updateData) {
        for (var child : card.getChildren()) {
            if (child instanceof Label lbl) {
                String text = lbl.getText();
                if (text.startsWith("Giá:")) {
                    lbl.setText("Giá: " + formatMoney(updateData.getCurrentPrice()) + " đ");
                } else if (text.startsWith("Dẫn đầu:")) {
                    lbl.setText("Dẫn đầu: " + (updateData.getHighestBidderName() != null ? updateData.getHighestBidderName() : "—"));
                }
            }
        }
        card.getProperties().put("endTime", System.currentTimeMillis() + updateData.getRemainingTime());
    }

    private void renderItemSpecifics(Map<String, String> specifics) {
        if (itemSpecificsBox == null) return;
        itemSpecificsBox.getChildren().clear();
        if (specifics == null || specifics.isEmpty()) {
            Label empty = new Label("Thuộc tính: -");
            empty.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 12px;"); // Đổi thành màu xám nhạt
            itemSpecificsBox.getChildren().add(empty);
            return;
        }
        specifics.forEach((key, value) -> {
            Label line = new Label(key + ": " + (value != null ? value : "-"));
            line.setStyle("-fx-text-fill: #E0E0E0; -fx-font-size: 12px;"); // Đổi thành màu xám sáng
            line.setWrapText(true);
            itemSpecificsBox.getChildren().add(line);
        });
    }

    private String formatSpecificsPreview(Map<String, String> specifics) {
        if (specifics == null || specifics.isEmpty()) return "Thuộc tính: -";
        StringBuilder sb = new StringBuilder("Thuộc tính: ");
        int count = 0;
        for (Map.Entry<String, String> entry : specifics.entrySet()) {
            if (count > 0) sb.append(" • ");
            sb.append(entry.getKey()).append("=").append(entry.getValue() != null ? entry.getValue() : "-");
            count++;
            if (count >= 2) break;
        }
        return sb.toString();
    }

    private void updateItemImage(AuctionDetailDTO a) {
        if (itemImageView == null || a == null) return;
        List<String> candidates = new ArrayList<>();
        if (a.getItemImageUrls() != null && !a.getItemImageUrls().isEmpty()) {
            candidates.addAll(a.getItemImageUrls());
        }

        Image imageToDisplay = null;
        for (String fileStr : candidates) {
            if (fileStr == null || fileStr.isBlank()) continue;
            java.io.File localFile = new java.io.File(fileStr);
            if (localFile.exists()) {
                imageToDisplay = new Image(localFile.toURI().toString(), true);
                break;
            }
            String resourcePath = fileStr.startsWith("/") ? fileStr : "/images/" + fileStr;
            URL resourceUrl = getClass().getResource(resourcePath);
            if (resourceUrl != null) {
                imageToDisplay = new Image(resourceUrl.toExternalForm(), true);
                break;
            }
        }
        if (imageToDisplay == null) {
            URL fallbackUrl = getClass().getResource("/images/Gardevoir.png");
            if (fallbackUrl != null) imageToDisplay = new Image(fallbackUrl.toExternalForm(), true);
        }
        if (imageToDisplay != null) {
            itemImageView.setImage(imageToDisplay);
        }
    }
    
    public void handleAuctionFinishedNoWinner (long auctionId) {
        boolean isCurrentlyViewing = (currentAuctionId == auctionId);
        
        removeAuctionRealtime(auctionId);
        
        if (isCurrentlyViewing) {
            ToastUtil.showInfo("Pathetic, no one bid this item!");
            lockAuctionUI(); 
        }
    }
    
    public void showWinnerNotification(AuctionWinnerDTO winnerData) {
        boolean isCurrentlyViewing = (currentAuctionId == winnerData.getAuctionId());
        
        removeAuctionRealtime(winnerData.getAuctionId());
        
        if (isCurrentlyViewing) {
            WinnerBoardUtil.showWinnerBoard(winnerData, ClientSession.getUserId());
            lockAuctionUI(); 
        }
    }
	
	@FXML
    private void handleToggleSidebar(ActionEvent event) {
        boolean isCurrentlyVisible = auctionListSidebar.isVisible();
        auctionListSidebar.setVisible(!isCurrentlyVisible);
        auctionListSidebar.setManaged(!isCurrentlyVisible); 
    }

    public void lockAuctionUI() {
        if (statusLabel != null) {
            statusLabel.setText("FINISHED");
            statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }
        
        if (remainingLabel != null) remainingLabel.setText("TIME UP");
        if (bidAmountField != null) bidAmountField.setDisable(true);
        if (enableAutoBidCheckBox != null) enableAutoBidCheckBox.setDisable(true);
    }
    
    private void showError(String message) {
        ToastUtil.showError(message);
    }

    private void showInfo(String message) {
        ToastUtil.showInfo(message != null ? message : "Thành công");
    }
}