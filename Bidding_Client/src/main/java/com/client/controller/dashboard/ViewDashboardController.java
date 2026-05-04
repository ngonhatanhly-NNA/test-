package com.client.controller.dashboard;

import com.client.network.AuctionNetwork;
import com.client.network.ItemNetwork;
import com.client.util.DashboardNavigation;
import com.client.util.DashboardSearchBridge;
import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.ItemResponseDTO;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Dashboard: dữ liệu từ {@link ItemNetwork} → {@code GET /api/items} (ItemService / ItemResponseDTO).
 * Cũng hiển thị các phiên đấu giá đang hoạt động và sắp diễn ra.
 */
public class ViewDashboardController {

    @FXML
    private ScrollPane mainScrollPane;
    @FXML
    private FlowPane itemContainer;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private TextField searchField;
    @FXML
    private CheckBox chkElectronics;
    @FXML
    private CheckBox chkArt;
    @FXML
    private CheckBox chkVehicle;

    private final ItemNetwork itemNetwork = new ItemNetwork();
    private final AuctionNetwork auctionNetwork = new AuctionNetwork();
    private List<ItemResponseDTO> allItems = new ArrayList<>();

    private static ViewDashboardController instance;

    public static ViewDashboardController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;

        // Load auctions first
        loadAuctionsFromServer();

        DashboardSearchBridge.setOnSearch(q -> Platform.runLater(() -> {
            if (searchField != null) {
                searchField.setText(q);
                rebuildCards();
            }
        }));

        if (sortCombo != null) {
            sortCombo.getItems().setAll(
                    "Mới nhất (theo ID)",
                    "Giá khởi điểm: thấp → cao",
                    "Giá khởi điểm: cao → thấp",
                    "Tên A → Z"
            );
            sortCombo.getSelectionModel().selectFirst();
            sortCombo.setOnAction(e -> rebuildCards());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> rebuildCards());
        }
        if (chkElectronics != null) {
            chkElectronics.selectedProperty().addListener((o, a, b) -> rebuildCards());
        }
        if (chkArt != null) {
            chkArt.selectedProperty().addListener((o, a, b) -> rebuildCards());
        }
        if (chkVehicle != null) {
            chkVehicle.selectedProperty().addListener((o, a, b) -> rebuildCards());
        }

        loadItemsFromServer();
    }

    /** Gọi từ {@link com.client.controller.dashboard.DashboardController} khi user bấm category ở sidebar. */
    public void syncSidebarCategories(boolean electronics, boolean fineArts, boolean vehicles) {
        if (chkElectronics != null) {
            chkElectronics.setSelected(electronics);
        }
        if (chkArt != null) {
            chkArt.setSelected(fineArts);
        }
        if (chkVehicle != null) {
            chkVehicle.setSelected(vehicles);
        }
        rebuildCards();
    }

    @FXML
    public void handleRefreshItems() {
        loadItemsFromServer();
    }

    private void loadAuctionsFromServer() {
        new Thread(() -> {
            try {
                List<AuctionDetailDTO> liveAuctions = auctionNetwork.getActiveAuctions();
                List<AuctionDetailDTO> upcomingAuctions = auctionNetwork.getUpcomingAuctions();

                Platform.runLater(() -> {
                    if (mainScrollPane == null) return;
                    
                    VBox mainContent = new VBox();
                    mainContent.setSpacing(30);
                    mainContent.setPadding(new Insets(20));

                    // Live Auctions Section
                    if (liveAuctions != null && !liveAuctions.isEmpty()) {
                        mainContent.getChildren().add(createAuctionSection("💎 Live Auctions", liveAuctions));
                    }

                    // Upcoming Auctions Section
                    if (upcomingAuctions != null && !upcomingAuctions.isEmpty()) {
                        mainContent.getChildren().add(createAuctionSection("⏰ Upcoming Auctions", upcomingAuctions));
                    }

                    mainScrollPane.setContent(mainContent);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private VBox createAuctionSection(String title, List<AuctionDetailDTO> auctions) {
        VBox section = new VBox();
        section.setSpacing(15);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        titleLabel.setStyle("-fx-text-fill: #333333;");
        section.getChildren().add(titleLabel);

        HBox auctionContainer = new HBox();
        auctionContainer.setSpacing(15);
        auctionContainer.setPadding(new Insets(10));

        for (AuctionDetailDTO auction : auctions) {
            auctionContainer.getChildren().add(createAuctionCard(auction));
        }

        ScrollPane scrollPane = new ScrollPane(auctionContainer);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPrefHeight(280);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        section.getChildren().add(scrollPane);
        return section;
    }

    private VBox createAuctionCard(AuctionDetailDTO auction) {
        VBox card = new VBox();
        card.setSpacing(10);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-cursor: hand;");
        card.setPrefWidth(220);

        ImageView imageView = new ImageView();
        imageView.setFitHeight(140);
        imageView.setFitWidth(200);
        imageView.setPreserveRatio(true);
        if (auction.getItemImageUrls() != null && !auction.getItemImageUrls().isEmpty()) {
            try {
                imageView.setImage(new Image(auction.getItemImageUrls().get(0)));
            } catch (Exception e) {
                imageView.setImage(getDefaultImage());
            }
        } else {
            imageView.setImage(getDefaultImage());
        }

        Label nameLabel = new Label(auction.getItemName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(200);

        Label priceLabel = new Label(formatCurrency(auction.getCurrentPrice().doubleValue()));
        priceLabel.setFont(Font.font("System", 12));
        priceLabel.setStyle("-fx-text-fill: #ff6b6b;");

        Label timeLabel = new Label(formatRemainingTime(auction.getRemainingTime()));
        timeLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        timeLabel.setStyle("-fx-text-fill: #d9534f;");

        card.getChildren().addAll(imageView, nameLabel, priceLabel, timeLabel);
        card.setOnMouseClicked(event -> {
            DashboardNavigation.navigateToAuctionDetail(auction.getAuctionId());
        });

        return card;
    }

    private Image getDefaultImage() {
        try {
            return new Image(getClass().getResource("/images/placeholder.png").toExternalForm());
        } catch (Exception e) {
            return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
        }
    }

    private String formatCurrency(double amount) {
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return currencyFormatter.format(amount);
    }

    private String formatRemainingTime(long millis) {
        if (millis <= 0) {
            return "Ended";
        }
        Duration duration = Duration.ofMillis(millis);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        if (days > 0) {
            return String.format("%d days %d h", days, hours);
        } else if (hours > 0) {
            return String.format("%d hours", hours);
        } else {
            return String.format("%d min", minutes);
        }
    }

    private void loadItemsFromServer() {
        itemNetwork.getAllItems().thenAccept(items -> Platform.runLater(() -> {
            allItems = items != null ? items : new ArrayList<>();
            rebuildCards();
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Dashboard");
                a.setHeaderText(null);
                a.setContentText("Không tải được danh sách sản phẩm: " +
                        (ex != null ? ex.getMessage() : "lỗi mạng"));
                a.showAndWait();
            });
            return null;
        });
    }

    private void rebuildCards() {
        if (itemContainer == null) {
            return;
        }
        itemContainer.getChildren().clear();

        List<ItemResponseDTO> filtered = allItems.stream()
                .filter(this::matchesCategory)
                .filter(this::matchesSearch)
                .collect(Collectors.toList());

        Comparator<ItemResponseDTO> cmp = comparatorForSort();
        filtered.sort(cmp);

        if (filtered.isEmpty()) {
            Label empty = new Label("Chưa có sản phẩm hoặc không khớp bộ lọc. Bấm \"Tải lại\" hoặc kiểm tra server /api/items.");
            empty.setWrapText(true);
            empty.setStyle("-fx-text-fill: #666;");
            itemContainer.getChildren().add(empty);
            return;
        }

        for (ItemResponseDTO item : filtered) {
            itemContainer.getChildren().add(buildCard(item));
        }
    }

    private boolean matchesCategory(ItemResponseDTO item) {
        String t = item.getType() != null ? item.getType().toUpperCase(Locale.ROOT) : "";
        boolean any = (chkElectronics == null || !chkElectronics.isSelected())
                && (chkArt == null || !chkArt.isSelected())
                && (chkVehicle == null || !chkVehicle.isSelected());
        if (any) {
            return true;
        }
        boolean match = false;
        if (chkElectronics != null && chkElectronics.isSelected() && "ELECTRONICS".equals(t)) {
            match = true;
        }
        if (chkArt != null && chkArt.isSelected() && "ART".equals(t)) {
            match = true;
        }
        if (chkVehicle != null && chkVehicle.isSelected() && "VEHICLE".equals(t)) {
            match = true;
        }
        return match;
    }

    private boolean matchesSearch(ItemResponseDTO item) {
        if (searchField == null) {
            return true;
        }
        String q = searchField.getText() != null ? searchField.getText().trim().toLowerCase(Locale.ROOT) : "";
        if (q.isEmpty()) {
            return true;
        }
        String name = item.getName() != null ? item.getName().toLowerCase(Locale.ROOT) : "";
        String desc = item.getDescription() != null ? item.getDescription().toLowerCase(Locale.ROOT) : "";
        return name.contains(q) || desc.contains(q);
    }

    private Comparator<ItemResponseDTO> comparatorForSort() {
        if (sortCombo == null || sortCombo.getSelectionModel().getSelectedItem() == null) {
            return Comparator.comparingLong(ItemResponseDTO::getId).reversed();
        }
        return switch (sortCombo.getSelectionModel().getSelectedIndex()) {
            case 1 -> Comparator.comparing(this::safeStartPrice, Comparator.nullsLast(Comparator.naturalOrder()));
            case 2 -> Comparator.comparing(this::safeStartPrice, Comparator.nullsLast(Comparator.reverseOrder()));
            case 3 -> Comparator.comparing(i -> i.getName() != null ? i.getName() : "", String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparingLong(ItemResponseDTO::getId).reversed();
        };
    }

    private BigDecimal safeStartPrice(ItemResponseDTO i) {
        return i.getStartingPrice() != null ? i.getStartingPrice() : BigDecimal.ZERO;
    }

    private VBox buildCard(ItemResponseDTO item) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(220);
        card.setStyle("-fx-background-color: white; -fx-border-color: #E3B04B; -fx-border-width: 2; " +
                "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        ImageView img = new ImageView(loadItemImage(item));
        img.setFitHeight(120);
        img.setFitWidth(120);
        img.setPreserveRatio(true);

        Label title = new Label(item.getName() != null ? item.getName() : "Sản phẩm #" + item.getId());
        title.setFont(Font.font("System Bold", 16));
        title.setTextFill(javafx.scene.paint.Color.web("#333333"));
        title.setWrapText(true);

        String priceStr = item.getStartingPrice() != null ? item.getStartingPrice().toPlainString() : "0";
        Label price = new Label("Giá khởi điểm: " + priceStr + " VNĐ");
        price.setFont(Font.font("System Bold", 14));
        price.setTextFill(javafx.scene.paint.Color.web("#e3b04b"));

        Label type = new Label("Loại: " + displayType(item.getType()));
        type.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");

        Button goLive = new Button("Tới phòng đấu giá");
        goLive.setPrefWidth(150);
        goLive.setStyle("-fx-background-color: #E3B04B; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 5; -fx-cursor: hand;");
        goLive.setOnAction(e -> DashboardNavigation.openLiveAuctions());

        card.getChildren().addAll(img, title, price, type, goLive);
        return card;
    }

    private static String displayType(String raw) {
        if (raw == null) {
            return "—";
        }
        return switch (raw.toUpperCase(Locale.ROOT)) {
            case "ELECTRONICS" -> "Điện tử";
            case "ART" -> "Mỹ thuật";
            case "VEHICLE" -> "Xe / phương tiện";
            default -> raw;
        };
    }

    private Image loadItemImage(ItemResponseDTO item) {
        String u = item.getThumbnailUrl();
        try {
            if (u != null && (u.startsWith("http://") || u.startsWith("https://"))) {
                return new Image(u, 120, 120, true, true, true);
            }
            if (u != null && !u.isBlank()) {
                String path = u.startsWith("/") ? u : ("/images/" + u);
                InputStream local = getClass().getResourceAsStream(path);
                if (local != null) {
                    return new Image(local, 120, 120, true, true);
                }
            }
        } catch (Exception ignored) {
        }
        InputStream fallback = getClass().getResourceAsStream("/images/ABRA.png");
        if (fallback != null) {
            return new Image(fallback, 120, 120, true, true);
        }
        return new Image(getClass().getResourceAsStream("/images/Gardevoir.png"), 120, 120, true, true);
    }
}

