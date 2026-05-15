package com.client.controller.dashboard;

import com.shared.dto.AuctionDetailDTO;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AuctionUIHelper {

    public static String formatMoney(BigDecimal v) {
        return v == null ? "0" : v.toPlainString();
    }

    public static String formatRemaining(long millis) {
        if (millis <= 0) return "Hết giờ đấu giá";
        long sec = millis / 1000;
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        if (h > 0) return h + " giờ " + m + " phút " + s + " giây";
        if (m > 0) return m + " phút " + s + " giây";
        return s + " giây";
    }

    public static String formatSpecificsPreview(Map<String, String> specifics) {
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

    public static void renderItemSpecifics(Map<String, String> specifics, VBox itemSpecificsBox) {
        if (itemSpecificsBox == null) return;
        itemSpecificsBox.getChildren().clear();
        if (specifics == null || specifics.isEmpty()) {
            Label empty = new Label("Thuộc tính: -");
            empty.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 12px;");
            itemSpecificsBox.getChildren().add(empty);
            return;
        }
        specifics.forEach((key, value) -> {
            Label line = new Label(key + ": " + (value != null ? value : "-"));
            line.setStyle("-fx-text-fill: #E0E0E0; -fx-font-size: 12px;");
            line.setWrapText(true);
            itemSpecificsBox.getChildren().add(line);
        });
    }

    public static void updateItemImage(AuctionDetailDTO a, ImageView itemImageView, Class<?> resourceClass) {
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
            URL resourceUrl = resourceClass.getResource(resourcePath);
            if (resourceUrl != null) {
                imageToDisplay = new Image(resourceUrl.toExternalForm(), true);
                break;
            }
        }
        if (imageToDisplay == null) {
            URL fallbackUrl = resourceClass.getResource("/images/Gardevoir.png");
            if (fallbackUrl != null) imageToDisplay = new Image(fallbackUrl.toExternalForm(), true);
        }
        if (imageToDisplay != null) {
            itemImageView.setImage(imageToDisplay);
        }
    }

    public static VBox createAuctionCard(AuctionDetailDTO a, Consumer<AuctionDetailDTO> onSelectCallback) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #242424; " +
                      "-fx-border-color: rgba(212,175,55,0.3); " +
                      "-fx-border-radius: 12; " +
                      "-fx-background-radius: 12; " +
                      "-fx-padding: 15; " +
                      "-fx-cursor: hand; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 4);");
        card.setMaxWidth(Double.MAX_VALUE);

        Label nameLbl = new Label(a.getItemName() != null ? a.getItemName() : "Item #" + a.getItemId());
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #F5F5F5;");
        nameLbl.setWrapText(true);

        Label typeLbl = new Label("Loại: " + (a.getItemType() != null ? a.getItemType() : "GENERAL"));
        typeLbl.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 12px;");

        Label specificsLbl = new Label(formatSpecificsPreview(a.getItemSpecifics()));
        specificsLbl.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 12px;");
        specificsLbl.setWrapText(true);

        String sellerName = (a.getSellerName() != null && !a.getSellerName().isBlank()) ? a.getSellerName() : ("Seller_" + a.getSellerId());
        Label sellerLbl = new Label("Người bán: " + sellerName);
        sellerLbl.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 12px;");

        Label priceLbl = new Label("Giá: " + formatMoney(a.getCurrentPrice()) + " đ");
        priceLbl.setStyle("-fx-text-fill: #D4AF37; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label leaderLbl = new Label("Dẫn đầu: " + (a.getHighestBidderName() != null ? a.getHighestBidderName() : "—"));
        leaderLbl.setStyle("-fx-text-fill: #E0E0E0;");

        Label timeLbl = new Label("Còn lại: " + formatRemaining(a.getRemainingTime()));
        timeLbl.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 12px;");

        Label scheduleInfoLbl = new Label("Bắt đầu: " + (a.getStartTime() != null ? a.getStartTime() : "—") +
                                          " | Kết thúc: " + (a.getEndTime() != null ? a.getEndTime() : "—"));
        scheduleInfoLbl.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px; -fx-font-weight: bold;");
        scheduleInfoLbl.setWrapText(true);

        Button btnSelect = new Button("Vào đấu giá");
        btnSelect.setStyle("-fx-background-color: linear-gradient(to right, #D4AF37, #FFD700); " +
                           "-fx-text-fill: #121212; " +
                           "-fx-cursor: hand; " +
                           "-fx-font-weight: bold; " +
                           "-fx-background-radius: 8;");
        btnSelect.setMaxWidth(Double.MAX_VALUE);

        btnSelect.setOnAction(e -> onSelectCallback.accept(a));

        card.getChildren().addAll(nameLbl, typeLbl, sellerLbl, specificsLbl, priceLbl, leaderLbl, timeLbl, scheduleInfoLbl, btnSelect);
        card.setUserData(a.getAuctionId()); 
        
        card.getProperties().put("endTime", System.currentTimeMillis() + a.getRemainingTime());
        card.getProperties().put("timeLabel", timeLbl);

        return card;
    }
}