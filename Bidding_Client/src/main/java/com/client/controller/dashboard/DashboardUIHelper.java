package com.client.controller.dashboard;

import com.client.util.DashboardNavigation;
import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.ItemResponseDTO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.InputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class DashboardUIHelper {

    public static VBox createAuctionSection(String title, List<AuctionDetailDTO> auctions, Class<?> resourceContext) {
        VBox section = new VBox();
        section.setSpacing(15);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        titleLabel.setStyle("-fx-text-fill: #D4AF37;");
        section.getChildren().add(titleLabel);

        HBox auctionContainer = new HBox();
        auctionContainer.setSpacing(15);
        auctionContainer.setPadding(new Insets(10));

        for (AuctionDetailDTO auction : auctions) {
            auctionContainer.getChildren().add(createAuctionCard(auction, resourceContext));
        }

        ScrollPane scrollPane = new ScrollPane(auctionContainer);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPrefHeight(280);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
        section.getChildren().add(scrollPane);
        return section;
    }

    private static VBox createAuctionCard(AuctionDetailDTO auction, Class<?> resourceContext) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: #242424; -fx-background-radius: 12; -fx-border-color: rgba(212,175,55,0.3); -fx-border-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 4);");
        card.setPrefWidth(240);

        ImageView imageView = new ImageView();
        imageView.setFitHeight(140);
        imageView.setFitWidth(210);
        imageView.setPreserveRatio(true);

        Image cardImg = null;
        if (auction.getItemImageUrls() != null && !auction.getItemImageUrls().isEmpty()) {
            String firstUrl = auction.getItemImageUrls().get(0);
            java.io.File f = new java.io.File(firstUrl);
            if (f.exists()) cardImg = new Image(f.toURI().toString(), true);
            else {
                URL res = resourceContext.getResource(firstUrl.startsWith("/") ? firstUrl : "/images/" + firstUrl);
                if (res != null) cardImg = new Image(res.toExternalForm(), true);
            }
        }
        imageView.setImage(cardImg != null ? cardImg : getDefaultImage(resourceContext));

        Label nameLabel = new Label(auction.getItemName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        nameLabel.setStyle("-fx-text-fill: #F5F5F5;");
        nameLabel.setWrapText(true);

        Label priceLabel = new Label("Giá hiện tại: " + formatCurrency(auction.getCurrentPrice().doubleValue()));
        priceLabel.setStyle("-fx-text-fill: #D4AF37; -fx-font-weight: bold;");

        VBox timeBox = new VBox(2);
        Label startLbl = new Label("📅 Từ: " + formatTimeDisplay(auction.getStartTime()));
        Label endLbl = new Label("🏁 Đến: " + formatTimeDisplay(auction.getEndTime()));
        startLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #A0A0A0;");
        endLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #E74C3C; -fx-font-weight: bold;");
        timeBox.getChildren().addAll(startLbl, endLbl);

        card.getChildren().addAll(imageView, nameLabel, priceLabel, timeBox);
        card.setOnMouseClicked(event -> DashboardNavigation.navigateToAuctionDetail(auction.getAuctionId()));

        return card;
    }

    public static VBox buildItemCard(ItemResponseDTO item, Class<?> resourceContext) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(220);
        card.setStyle("-fx-background-color: #181818; -fx-border-color: rgba(212,175,55,0.5); -fx-border-width: 1.5; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 15, 0, 0, 5);");

        ImageView img = new ImageView(loadItemImage(item, resourceContext));
        img.setFitHeight(120);
        img.setFitWidth(120);
        img.setPreserveRatio(true);

        Label title = new Label(item.getName() != null ? item.getName() : "Sản phẩm #" + item.getId());
        title.setFont(Font.font("System Bold", 16));
        title.setTextFill(javafx.scene.paint.Color.web("#F5F5F5"));
        title.setWrapText(true);

        String priceStr = item.getStartingPrice() != null ? item.getStartingPrice().toPlainString() : "0";
        Label price = new Label("Giá khởi điểm: " + priceStr + " VNĐ");
        price.setFont(Font.font("System Bold", 14));
        price.setTextFill(javafx.scene.paint.Color.web("#D4AF37"));

        Label type = new Label("Loại: " + displayType(item.getType()));
        type.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 11px;");

        Button goLive = new Button("Tới phòng đấu giá");
        goLive.setPrefWidth(150);
        goLive.setStyle("-fx-background-color: linear-gradient(to right, #D4AF37, #FFD700); -fx-text-fill: #121212; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
        goLive.setOnAction(e -> DashboardNavigation.openLiveAuctions());

        card.getChildren().addAll(img, title, price, type, goLive);
        return card;
    }

    private static String formatTimeDisplay(String isoTime) {
        if (isoTime == null || isoTime.isBlank()) return "---";
        try {
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(isoTime);
            return ldt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM"));
        } catch (Exception e) { return isoTime; }
    }

    private static String displayType(String raw) {
        if (raw == null) return "—";
        return switch (raw.toUpperCase(Locale.ROOT)) {
            case "ELECTRONICS" -> "Electronics";
            case "ART" -> "Art";
            case "VEHICLE" -> "Vehicle";
            default -> raw;
        };
    }

    private static Image loadItemImage(ItemResponseDTO item, Class<?> resourceContext) {
        String url = item.getThumbnailUrl();
        if (url == null || url.isBlank()) return getDefaultImage(resourceContext);
        try {
            java.io.File file = new java.io.File(url);
            if (file.exists()) return new Image(file.toURI().toString(), 120, 120, true, true);
            String path = url.startsWith("/") ? url : ("/images/" + url);
            InputStream is = resourceContext.getResourceAsStream(path);
            if (is != null) return new Image(is, 120, 120, true, true);
        } catch (Exception ignored) {}
        return getDefaultImage(resourceContext);
    }

    private static Image getDefaultImage(Class<?> resourceContext) {
        try {
            return new Image(resourceContext.getResource("/images/placeholder.png").toExternalForm());
        } catch (Exception e) {
            return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
        }
    }

    private static String formatCurrency(double amount) {
        return NumberFormat.getCurrencyInstance(Locale.of("vi", "VN")).format(amount);
    }
}