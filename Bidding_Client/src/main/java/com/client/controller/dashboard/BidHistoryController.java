package com.client.controller.dashboard;

import com.shared.dto.BidHistoryDTO;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BidHistoryController {

    @FXML
    private Label subtitleLabel;

    @FXML
    private VBox listContainer;

    public void setBidHistoryData(long auctionId, List<BidHistoryDTO> history) {
        subtitleLabel.setText("Phiên đấu giá #" + auctionId);
        listContainer.getChildren().clear();

        if (history == null || history.isEmpty()) {
            VBox emptyBox = new VBox();
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40, 0, 0, 0));
            Label emptyLabel = new Label("Chưa có lượt đặt giá nào \uD83D\uDE22");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748B; -fx-font-style: italic;");
            emptyBox.getChildren().add(emptyLabel);
            listContainer.getChildren().add(emptyBox);
            return;
        }

        for (int i = 0; i < history.size(); i++) {
            BidHistoryDTO bh = history.get(i);

            StackPane cardWrapper = new StackPane();
            cardWrapper.setAlignment(Pos.CENTER);
            
            // --- NỘI DUNG CHÍNH CỦA THẺ BÀI ---
            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            
            String name = (bh.getBidderName() != null && !bh.getBidderName().isBlank()) ? bh.getBidderName() : "Guest";
            String firstLetter = name.substring(0, 1).toUpperCase();
            Label avatar = new Label(firstLetter);
            avatar.setAlignment(Pos.CENTER);
            avatar.setMinSize(45, 45);
            avatar.setStyle("-fx-background-color: linear-gradient(to bottom right, #3B82F6, #8B5CF6); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 20px; -fx-background-radius: 25;");

            VBox userInfo = new VBox(3);
            Label nameLbl = new Label(name);
            nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #0F172A;");
            Label timeLbl = new Label(bh.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss - dd/MM/yyyy")));
            timeLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
            userInfo.getChildren().addAll(nameLbl, timeLbl);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            VBox priceInfo = new VBox(5);
            priceInfo.setAlignment(Pos.CENTER_RIGHT);
            Label priceLbl = new Label(formatMoney(bh.getBidAmount()) + " đ");
            
            Label typeBadge = new Label(bh.isAutoBid() ? "🤖 AUTO" : "🖐 MANUAL");
            if (bh.isAutoBid()) {
                typeBadge.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8; -fx-padding: 4 8; -fx-background-radius: 12; -fx-font-size: 10px; -fx-font-weight: bold;");
            } else {
                typeBadge.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-padding: 4 8; -fx-background-radius: 12; -fx-font-size: 10px; -fx-font-weight: bold;");
            }
            priceInfo.getChildren().addAll(priceLbl, typeBadge);

            row.getChildren().addAll(avatar, userInfo, spacer, priceInfo);

            // ========================================================
            // HIỆU ỨNG LED TRƯỢT VÔ CỰC DÀNH RIÊNG CHO TOP 1
            // ========================================================
            if (i == 0) {
                // 1. Chữ Top 1 cho màu Nổi bật
                priceLbl.setStyle("-fx-font-weight: 900; -fx-font-size: 19px; -fx-text-fill: #FF007A;");

                // 2. Tạo một dải màu cực to để xoay
                Rectangle ledBackground = new Rectangle(1200, 1200);
                ledBackground.setManaged(false); // Ngăn không cho nó làm phình giao diện

                // Mã màu Cyberpunk cực chất
                LinearGradient sweepGradient = new LinearGradient(
                        0, 0, 1, 1, true, CycleMethod.REFLECT,
                        new Stop(0, Color.web("#FF007A")),   // Hồng Neon
                        new Stop(0.3, Color.web("#7928CA")), // Tím
                        new Stop(0.5, Color.web("#38BDF8")), // Xanh Cyan
                        new Stop(0.7, Color.web("#7928CA")), 
                        new Stop(1, Color.web("#FF007A"))
                );
                ledBackground.setFill(sweepGradient);

                // 3. Cho mảng màu xoay vòng vòng mượt mà (3.5 giây / vòng)
                RotateTransition rt = new RotateTransition(Duration.seconds(3.5), ledBackground);
                rt.setByAngle(360);
                rt.setCycleCount(Animation.INDEFINITE);
                rt.setInterpolator(Interpolator.LINEAR);
                rt.play();

                // 4. Bọc mảng màu vào một cái hộp (Pane)
                javafx.scene.layout.Pane ledPane = new javafx.scene.layout.Pane(ledBackground);
                
                // Khóa kích thước hộp LED to hơn thẻ bài đúng 6px (Tạo viền dày 3px)
                ledPane.prefWidthProperty().bind(row.widthProperty().add(6));
                ledPane.prefHeightProperty().bind(row.heightProperty().add(6));
                ledPane.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                ledPane.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

                // Giữ mảng màu luôn ở giữa thẻ
                ledBackground.layoutXProperty().bind(ledPane.widthProperty().divide(2).subtract(600));
                ledBackground.layoutYProperty().bind(ledPane.heightProperty().divide(2).subtract(600));

                // 5. Cắt bo tròn cho hộp LED
                Rectangle clip = new Rectangle();
                clip.widthProperty().bind(ledPane.widthProperty());
                clip.heightProperty().bind(ledPane.heightProperty());
                clip.setArcWidth(28); // Bo góc to hơn thẻ một chút
                clip.setArcHeight(28);
                ledPane.setClip(clip);

                // 6. Hiệu ứng phát sáng lấp lánh nhẹ nhàng
                DropShadow glow = new DropShadow();
                glow.setColor(Color.web("#38BDF8").deriveColor(0, 1, 1, 0.5));
                glow.setRadius(15);
                glow.setSpread(0.15);
                ledPane.setEffect(glow);

                // Thẻ chính màu trắng đè lên trên lớp LED
                row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-padding: 15;");
                
                cardWrapper.getChildren().addAll(ledPane, row);
            } 
            // ========================================================
            // THẺ BÌNH THƯỜNG (TOP 2 TRỞ XUỐNG)
            // ========================================================
            else {
                priceLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #2C3E50;");
                
                row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-padding: 15; -fx-border-radius: 12; -fx-border-color: #CBD5E1; -fx-border-width: 1.5; -fx-effect: dropshadow(gaussian, rgba(15,23,42,0.08), 10, 0, 0, 4);");
                
                row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 12; -fx-padding: 15; -fx-border-radius: 12; -fx-border-color: #94A3B8; -fx-border-width: 1.5; -fx-effect: dropshadow(gaussian, rgba(15,23,42,0.15), 15, 0, 0, 6); -fx-cursor: hand;"));
                row.setOnMouseExited(e -> row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12; -fx-padding: 15; -fx-border-radius: 12; -fx-border-color: #CBD5E1; -fx-border-width: 1.5; -fx-effect: dropshadow(gaussian, rgba(15,23,42,0.08), 10, 0, 0, 4);"));
                
                cardWrapper.getChildren().add(row);
            }

            listContainer.getChildren().add(cardWrapper);
        }
    }

    private String formatMoney(BigDecimal v) {
        if (v == null) return "0";
        java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###");
        return formatter.format(v);
    }
}