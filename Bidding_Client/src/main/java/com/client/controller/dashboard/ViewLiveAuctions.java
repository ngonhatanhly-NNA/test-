package com.client.controller.dashboard;

import com.client.network.AuctionNetwork;
import com.client.session.ClientSession;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
    private ImageView itemImageView;
    @FXML
    private LineChart<String, Number> priceChart;
    @FXML
    private XYChart.Series<String, Number> priceSeries;
    @FXML
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

        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Prices over time");
        if (priceChart != null) {
            priceChart.getData().add(priceSeries);
            priceChart.setAnimated(false);
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
                    if (auctionsContainer != null) {
                        auctionsContainer.getChildren().clear();// Xóa thẻ mỗi lần refresh
                    }
                    if (list.isEmpty()) {
                        showError("Chưa có phiên đấu giá đang mở trên server (hoặc DB/cache trống).");
                        return;
                    }

                    for (AuctionDetailDTO a : list) {
                        if (auctionsContainer != null) {
                            auctionsContainer.getChildren().add(createAuctionCard(a));
                        }
                    }
                    // Auto load sản phẩm đầu tiên
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
        if (itemNameLabel != null) {
            itemNameLabel.setText(a.getItemName() != null ? a.getItemName() : ("Item #" + a.getItemId()));
        }
        if (itemTypeLabel != null) {
            itemTypeLabel.setText("Type: " + (a.getItemType() != null ? a.getItemType() : "GENERAL"));
        }
        if (itemDescriptionLabel != null) {
            itemDescriptionLabel.setText(a.getItemDescription() != null ? a.getItemDescription() : "(Không có mô tả)");
        }
        if (sellerNameLabel != null) {
            String seller = (a.getSellerName() != null && !a.getSellerName().isBlank())
                    ? a.getSellerName()
                    : ("Seller_" + a.getSellerId());
            sellerNameLabel.setText("Seller: " + seller);
        }
        renderItemSpecifics(a.getItemSpecifics());
        updateItemImage(a);
        if (currentPriceLabel != null && a.getCurrentPrice() != null) {
            currentPriceLabel.setText(formatMoney(a.getCurrentPrice()) + " VNĐ");
        }
        if (leaderLabel != null) {
            leaderLabel.setText("Người dẫn đầu: " + (a.getHighestBidderName() != null ? a.getHighestBidderName() : "—"));
        }
        
        // Update step hint with minimum bid amount
        if (stepHintLabel != null && a.getStepPrice() != null) {
            BigDecimal stepPrice = a.getStepPrice();
            BigDecimal currentPrice = a.getCurrentPrice();
            BigDecimal minimumBid;
            
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                minimumBid = stepPrice;
            } else {
                minimumBid = currentPrice.add(stepPrice);
            }
            
            stepHintLabel.setText("* Bước giá tối thiểu: " + formatMoney(stepPrice) + 
                                 " VNĐ | Giá tối thiểu để đặt: " + formatMoney(minimumBid) + " VNĐ");
        }
        
        // Pre-fill bid amount field with minimum bid
        if (bidAmountField != null && a.getStepPrice() != null) {
            BigDecimal currentPrice = a.getCurrentPrice();
            BigDecimal minimumBid;
            
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                minimumBid = a.getStepPrice();
            } else {
                minimumBid = currentPrice.add(a.getStepPrice());
            }
            
            bidAmountField.setText(formatMoney(minimumBid));
        }
        
        if (statusLabel != null) {
            statusLabel.setText("Tình trạng: ĐANG MỞ");
        }
        if (remainingLabel != null) {
            remainingLabel.setText("Còn lại: " + formatRemaining(a.getRemainingTime()));
        }
        if (priceSeries != null) {
            priceSeries.getData().clear();
            if (priceSeries != null) {
                priceSeries.getData().clear();
                String timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
               
                priceSeries.getData().add(new XYChart.Data<>(timeNow, a.getCurrentPrice() != null ? a.getCurrentPrice() : BigDecimal.ZERO));
            }
        }
    }

    private static String formatMoney(BigDecimal v) {
        return v == null ? "0" : v.toPlainString();
    }

    private static String formatRemaining(long millis) {
        if (millis <= 0) {
            return "Hết giờ đấu giá";
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
            showError("Chưa có phiên đấu giá. Bấm \"Refresh\" sau khi server đã chạy.");
            return;
        }
        if (!ClientSession.isLoggedIn()) {
            showError("Vui lòng đăng nhập trước khi đặt giá.");
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
        if (updateData == null ) {
            return;
        }
        Platform.runLater(() -> {
            if (updateData.getAuctionId() == currentAuctionId) {
                if (currentPriceLabel != null && updateData.getCurrentPrice() != null) {
                    currentPriceLabel.setText(formatMoney(updateData.getCurrentPrice()) + " VNĐ");
                }

                if (leaderLabel != null && updateData.getHighestBidderName() != null) {
                    leaderLabel.setText("Người dẫn đầu: " + updateData.getHighestBidderName());
                }

                if (remainingLabel != null) {
                    remainingLabel.setText("Còn lại: " + formatRemaining(updateData.getRemainingTime()));
                }

                if (priceSeries != null && updateData.getCurrentPrice() != null) {
                String timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                priceSeries.getData().add(new XYChart.Data<>(timeNow, updateData.getCurrentPrice()));
                
                // Chỉ giữ lại 15 mức giá gần nhất để biểu đồ không bị dồn cục lại
                if (priceSeries.getData().size() > 15) {
                    priceSeries.getData().remove(0);
                }
            }
            }
            
            // Update bid amount field hint to show new minimum bid (current + step)
            // Note: We need to fetch step price from somewhere - ideally from a cached auction detail
            // For now, we'll just update the UI labels
            
            if (auctionsContainer != null) {
				boolean isFound = false;
				
                for (var node : auctionsContainer.getChildren()) {
                    if (node.getUserData() instanceof Long id && id == updateData.getAuctionId()) {
                        isFound = true;
                        // Cập nhật thẻ đấu giá tương ứng trong danh sách
                        if (node instanceof VBox card) {
                            applyUpdateToCard(card, updateData);
                        }
                    }
                }
				
				if (!isFound) {
                    ioPool.execute(() -> {
                        try {
                            // Gọi API HTTP bằng file AuctionNetwork để lấy AuctionDetailDTO (chứa thông tin Item)
                            String rawDetail = AuctionNetwork.getAuctionDetail(updateData.getAuctionId());
                            Response res = AuctionNetwork.parseResponse(rawDetail);
                            AuctionDetailDTO fullDetail = AuctionNetwork.parseAuctionDetail(res);

                            if (fullDetail != null) {
                                // Lấy được "đống thông tin Item" rồi thì quay lại Thread UI để vẽ thẻ
                                Platform.runLater(() -> {
                                    addOrUpdateAuctionRealtime(fullDetail);
                                    
                                    // If this is the current auction being viewed, update the bid field
                                    if (fullDetail.getAuctionId() == currentAuctionId) {
                                        applyAuctionDetail(fullDetail);
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        });
    }

    public void addOrUpdateAuctionRealtime(AuctionDetailDTO detail) {
        if (detail == null || auctionsContainer == null) {
            return;
        }

        for (var node : auctionsContainer.getChildren()) {
            if (node.getUserData() instanceof Long id && id == detail.getAuctionId()) {
                if (node instanceof VBox card) {
                    AuctionUpdateDTO updateDTO = new AuctionUpdateDTO(
                            detail.getAuctionId(),
                            detail.getCurrentPrice(),
                            detail.getHighestBidderName(),
                            detail.getRemainingTime());
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
        if (auctionsContainer == null) {
            return;
        }

        auctionsContainer.getChildren().removeIf(node -> node.getUserData() instanceof Long id && id == auctionId);
        if (auctionId == currentAuctionId) {
            currentAuctionId = 0;
            if (statusLabel != null) {
                statusLabel.setText("Tình trạng: ĐÃ KẾT THÚC");
            }
            if (remainingLabel != null) {
                remainingLabel.setText("Còn lại: Hết giờ đấu giá");
            }
        }
    }

    private VBox createAuctionCard(AuctionDetailDTO a) {
        VBox card = new VBox(8);
        card.setStyle("-fx-border-color: #bdc3c7; -fx-border-radius: 8; -fx-padding: 15; -fx-background-color: #ffffff; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setMaxWidth(Double.MAX_VALUE);

        // Tên sản phẩm
        Label nameLbl = new Label(a.getItemName() != null ? a.getItemName() : "Item #" + a.getItemId());
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");
        nameLbl.setWrapText(true);

        Label typeLbl = new Label("Loại: " + (a.getItemType() != null ? a.getItemType() : "GENERAL"));
        typeLbl.setStyle("-fx-text-fill: #34495e; -fx-font-size: 12px;");

        Label specificsLbl = new Label(formatSpecificsPreview(a.getItemSpecifics()));
        specificsLbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;");
        specificsLbl.setWrapText(true);

        String sellerName = (a.getSellerName() != null && !a.getSellerName().isBlank())
                ? a.getSellerName()
                : ("Seller_" + a.getSellerId());
        Label sellerLbl = new Label("Người bán: " + sellerName);
        sellerLbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;");

        // Giá hiện tại
        Label priceLbl = new Label("Giá: " + formatMoney(a.getCurrentPrice()) + " đ");
        priceLbl.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px;");

        // Người dẫn đầu & Thời gian
        Label leaderLbl = new Label("Dẫn đầu: " + (a.getHighestBidderName() != null ? a.getHighestBidderName() : "—"));
        Label timeLbl = new Label("Còn lại: " + formatRemaining(a.getRemainingTime()));
        timeLbl.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

        // Nút bấm để xem chi tiết / Đặt giá
        Button btnSelect = new Button("Vào đấu giá");
        btnSelect.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        btnSelect.setMaxWidth(Double.MAX_VALUE);

        // LUỒNG QUAN TRỌNG: Khi bấm nút, bơm dữ liệu của thẻ này vào Form Detail hiện tại
        btnSelect.setOnAction(e -> {
            applyAuctionDetail(a);
            showInfo("Đã chọn: " + a.getItemName() + ". Bạn có thể đặt giá ngay bên cạnh!");
        });

        // Gắn tất cả vào Thẻ
        card.getChildren().addAll(nameLbl, typeLbl, sellerLbl, specificsLbl, priceLbl, leaderLbl, timeLbl, btnSelect);
        card.setUserData(a.getAuctionId()); // Lưu ID đấu giá vào userData để dễ truy xuất sau này nếu cần
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
                } else if (text.startsWith("Còn lại:")) {
                    lbl.setText("Còn lại: " + formatRemaining(updateData.getRemainingTime()));
                }
            }
        }
    }

    private void renderItemSpecifics(Map<String, String> specifics) {
        if (itemSpecificsBox == null) {
            return;
        }
        itemSpecificsBox.getChildren().clear();
        if (specifics == null || specifics.isEmpty()) {
            Label empty = new Label("Thuộc tính: -");
            empty.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;");
            itemSpecificsBox.getChildren().add(empty);
            return;
        }
        specifics.forEach((key, value) -> {
            Label line = new Label(key + ": " + (value != null ? value : "-"));
            line.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px;");
            line.setWrapText(true);
            itemSpecificsBox.getChildren().add(line);
        });
    }

    private String formatSpecificsPreview(Map<String, String> specifics) {
        if (specifics == null || specifics.isEmpty()) {
            return "Thuộc tính: -";
        }

        StringBuilder sb = new StringBuilder("Thuộc tính: ");
        int count = 0;
        for (Map.Entry<String, String> entry : specifics.entrySet()) {
            if (count > 0) {
                sb.append(" • ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue() != null ? entry.getValue() : "-");
            count++;
            if (count >= 2) {
                break;
            }
        }
        return sb.toString();
    }

    private void updateItemImage(AuctionDetailDTO a) {
        if (itemImageView == null || a == null) {
            return;
        }

        String itemName = a.getItemName();
        List<String> candidates = new ArrayList<>();
        if (a.getItemImageUrls() != null) {
            candidates.addAll(a.getItemImageUrls());
        }
        if (itemName != null && !itemName.isBlank()) {
            String trimmed = itemName.trim();
            candidates.add(trimmed + ".png");
            candidates.add(trimmed.replaceAll("\\s+", "") + ".png");
            candidates.add(trimmed.toUpperCase(Locale.ROOT) + ".png");
            candidates.add(capitalizeFirst(trimmed) + ".png");
        }
        if (a.getItemId() > 0) {
            candidates.add("item-" + a.getItemId() + ".png");
        }
        candidates.add("Gardevoir.png");

        URL chosen = null;
        for (String file : candidates) {
            if (file == null || file.isBlank()) {
                continue;
            }
            String normalized = file.startsWith("/") ? file.substring(1) : file;
            URL url = getClass().getResource(normalized.startsWith("images/") ? "/" + normalized : "/images/" + normalized);
            if (url != null) {
                chosen = url;
                break;
            }
        }

        if (chosen != null) {
            itemImageView.setImage(new Image(Objects.requireNonNull(chosen).toExternalForm(), true));
        }
    }

    private static String capitalizeFirst(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return "";
        }
        if (t.length() == 1) {
            return t.toUpperCase(Locale.ROOT);
        }
        return t.substring(0, 1).toUpperCase(Locale.ROOT) + t.substring(1);
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
