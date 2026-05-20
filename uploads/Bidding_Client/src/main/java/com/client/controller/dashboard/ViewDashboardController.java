package com.client.controller.dashboard;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.client.network.AuctionNetwork;
import com.client.network.ItemNetwork;
import com.client.util.DashboardNavigation;
import com.client.util.DashboardSearchBridge;
import com.client.util.ui.CardFactory;
import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.ItemResponseDTO;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ViewDashboardController {

    @FXML private ScrollPane mainScrollPane;
    @FXML private FlowPane itemContainer;
    @FXML private ComboBox<String> sortCombo;
    @FXML private TextField searchField;
    @FXML private CheckBox chkElectronics, chkArt, chkVehicle;

    private final ItemNetwork itemNetwork = new ItemNetwork();
    private List<ItemResponseDTO> allItems = new ArrayList<>();

    private static ViewDashboardController instance;

    public static ViewDashboardController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        
        loadAuctionsFromServer();

        DashboardSearchBridge.setOnSearch(q -> Platform.runLater(() -> {
            if (searchField != null) {
                searchField.setText(q);
                rebuildCards();
            }
        }));

        if (sortCombo != null) {
            sortCombo.getItems().setAll("Mới nhất (theo ID)", "Giá khởi điểm: thấp → cao", "Giá khởi điểm: cao → thấp", "Tên A → Z");
            sortCombo.getSelectionModel().selectFirst();
            sortCombo.setOnAction(e -> rebuildCards());
        }
        
        if (searchField != null) searchField.textProperty().addListener((obs, o, n) -> rebuildCards());
        if (chkElectronics != null) chkElectronics.selectedProperty().addListener((o, a, b) -> rebuildCards());
        if (chkArt != null) chkArt.selectedProperty().addListener((o, a, b) -> rebuildCards());
        if (chkVehicle != null) chkVehicle.selectedProperty().addListener((o, a, b) -> rebuildCards());

        loadItemsFromServer();
    }

    public void syncSidebarCategories(boolean electronics, boolean fineArts, boolean vehicles) {
        if (chkElectronics != null) chkElectronics.setSelected(electronics);
        if (chkArt != null) chkArt.setSelected(fineArts);
        if (chkVehicle != null) chkVehicle.setSelected(vehicles);
        rebuildCards();
    }

    @FXML
    public void handleRefreshItems() {
        loadItemsFromServer();
    }

    private void loadAuctionsFromServer() {
        new Thread(() -> {
            List<AuctionDetailDTO> tempLive = new ArrayList<>();
            List<AuctionDetailDTO> tempUp = new ArrayList<>();
            try {
                tempLive = AuctionNetwork.getActiveAuctions();
                tempUp = AuctionNetwork.getUpcomingAuctions();
            } catch (Exception e) {
                System.out.println("Cảnh báo: Không thể tải phiên đấu giá: " + e.getMessage());
            }

            final List<AuctionDetailDTO> liveAuctions = tempLive;
            final List<AuctionDetailDTO> upcomingAuctions = tempUp;

            Platform.runLater(() -> {
                if (mainScrollPane == null) return;
                VBox mainContent = new VBox();
                mainContent.setSpacing(30);
                mainContent.setPadding(new Insets(20));

                if (!liveAuctions.isEmpty()) mainContent.getChildren().add(createAuctionSection("🔥 Đang Diễn Ra (Live)", liveAuctions));
                if (!upcomingAuctions.isEmpty()) mainContent.getChildren().add(createAuctionSection("⏰ Sắp Diễn Ra", upcomingAuctions));

                if (itemContainer != null) {
                    VBox itemSection = new VBox(15);
                    Label titleLabel = new Label("📦 Khám Phá Sản Phẩm");
                    titleLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
                    titleLabel.setStyle("-fx-text-fill: #D4AF37;");
                    itemSection.getChildren().addAll(titleLabel, itemContainer);
                    mainContent.getChildren().add(itemSection);
                }
                mainScrollPane.setContent(mainContent);
            });
        }).start();
    }
    
    private VBox createAuctionSection(String title, List<AuctionDetailDTO> auctions) {
        VBox section = new VBox(15);
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        titleLabel.setStyle("-fx-text-fill: #D4AF37;");

        HBox auctionContainer = new HBox(15);
        auctionContainer.setPadding(new Insets(10));
        for (AuctionDetailDTO auction : auctions) {
            auctionContainer.getChildren().add(CardFactory.createDashboardAuctionCard(auction, getClass(), 
                () -> DashboardNavigation.navigateToAuctionDetail(auction.getAuctionId())));
        }

        ScrollPane scrollPane = new ScrollPane(auctionContainer);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPrefHeight(280);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
        
        section.getChildren().addAll(titleLabel, scrollPane);
        return section;
    }

    private void loadItemsFromServer() {
        itemNetwork.getAllItems().thenAccept(items -> Platform.runLater(() -> {
            allItems = items != null ? items : new ArrayList<>();
            rebuildCards();
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Lỗi tải dữ liệu");
                a.setHeaderText(null);
                a.setContentText("Không tải được danh sách sản phẩm.");
                a.showAndWait();
            });
            return null;
        });
    }

    private void rebuildCards() {
        if (itemContainer == null) return;
        itemContainer.getChildren().clear();

        List<ItemResponseDTO> filtered = allItems.stream()
                .filter(this::matchesCategory)
                .filter(this::matchesSearch)
                .sorted(comparatorForSort())
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Label empty = new Label("Chưa có sản phẩm hoặc không khớp bộ lọc.");
            empty.setStyle("-fx-text-fill: #A0A0A0;");
            itemContainer.getChildren().add(empty);
            return;
        }

        for (ItemResponseDTO item : filtered) {
            itemContainer.getChildren().add(CardFactory.createExploreItemCard(item, getClass(), e -> DashboardNavigation.openLiveAuctions()));
        }
    }

    private boolean matchesCategory(ItemResponseDTO item) {
        String t = item.getType() != null ? item.getType().toUpperCase(Locale.ROOT) : "";
        boolean any = (chkElectronics == null || !chkElectronics.isSelected()) && (chkArt == null || !chkArt.isSelected()) && (chkVehicle == null || !chkVehicle.isSelected());
        if (any) return true;
        if (chkElectronics != null && chkElectronics.isSelected() && "ELECTRONICS".equals(t)) return true;
        if (chkArt != null && chkArt.isSelected() && "ART".equals(t)) return true;
        if (chkVehicle != null && chkVehicle.isSelected() && "VEHICLE".equals(t)) return true;
        return false;
    }

    private boolean matchesSearch(ItemResponseDTO item) {
        if (searchField == null) return true;
        String q = searchField.getText() != null ? searchField.getText().trim().toLowerCase(Locale.ROOT) : "";
        if (q.isEmpty()) return true;
        String name = item.getName() != null ? item.getName().toLowerCase(Locale.ROOT) : "";
        String desc = item.getDescription() != null ? item.getDescription().toLowerCase(Locale.ROOT) : "";
        return name.contains(q) || desc.contains(q);
    }

    private Comparator<ItemResponseDTO> comparatorForSort() {
        if (sortCombo == null || sortCombo.getSelectionModel().getSelectedItem() == null) return Comparator.comparingLong(ItemResponseDTO::getId).reversed();
        return switch (sortCombo.getSelectionModel().getSelectedIndex()) {
            case 1 -> Comparator.comparing(i -> i.getStartingPrice() != null ? i.getStartingPrice() : BigDecimal.ZERO, Comparator.nullsLast(Comparator.naturalOrder()));
            case 2 -> Comparator.comparing(i -> i.getStartingPrice() != null ? i.getStartingPrice() : BigDecimal.ZERO, Comparator.nullsLast(Comparator.reverseOrder()));
            case 3 -> Comparator.comparing(i -> i.getName() != null ? i.getName() : "", String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparingLong(ItemResponseDTO::getId).reversed();
        };
    }
}