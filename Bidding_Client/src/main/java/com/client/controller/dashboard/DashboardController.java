package com.client.controller.dashboard;

import com.client.util.DashboardNavigation;
import javafx.animation.TranslateTransition;
import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.AnchorPane; 
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.image.Image;     
import javafx.scene.image.ImageView;   
import javafx.util.Duration;           
import javafx.application.Platform;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.client.util.BiddingPet;
import com.client.util.SwitchPane;
import com.client.util.SceneController;
import com.client.session.ClientSession;

public class DashboardController {

    @FXML private BorderPane mainBorderPane;
    private SwitchPane mainPane; 

    // Nút Menu
    @FXML private Button btnDashboard;
    @FXML private Button btnLiveAuctions;
    @FXML private Button btnMyInventory;
    @FXML private Button btnLogout;
    @FXML private Button btnManagement;

    // Header
    @FXML private Label lblUsername;
    @FXML private TextField searchField;

    // Nút Categories
    @FXML private CheckBox chkElectronics;
    @FXML private CheckBox chkFineArts;
    @FXML private CheckBox chkVehicles;

    // --- GAME COMPONENTS ---
    @FXML private AnchorPane gameLayer; 
    @FXML private Label bidCounter;     
    private BiddingPet pet;
    private int caughtCount = 0;

    private List<ImageView> activeCoins = new ArrayList<>();
    private AnimationTimer gameLoop;
    public static DashboardController instance;

    // --- HÀM KHỞI TẠO ---
    @FXML
    public void initialize() {
        mainPane = new SwitchPane(mainBorderPane);

        if (ClientSession.isLoggedIn()) {
            lblUsername.setText(ClientSession.getUsername());
            String role = ClientSession.getRole();

            if (role == null || role.equalsIgnoreCase("BIDDER")) {
                if (btnManagement != null) {
                    btnManagement.setVisible(false);
                    btnManagement.setManaged(false);
                }
            }
        } else {
            lblUsername.setText("Khách");
            if (btnManagement != null) {
                btnManagement.setVisible(false);
                btnManagement.setManaged(false);
            }
        }

        instance = this;
        
        if (gameLayer != null) {
            pet = new BiddingPet(gameLayer);
            setupGameLoopAndControls(); // Cài đặt phím bấm và vòng lặp game
        }
        
        handleBtnDashboard(null);
        
        DashboardNavigation.setOpenLiveAuctions(() -> handleBtnLiveAuctions(null));
        DashboardNavigation.setNavigateToAuctionDetail(auctionId -> handleBtnLiveAuctions(null));
    }

    // --- CÀI ĐẶT GAME LOOP VÀ BÀN PHÍM ---
    private void setupGameLoopAndControls() {
        // Đợi UI load xong mới gắn sự kiện phím
        Platform.runLater(() -> {
            gameLayer.getScene().setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.A || e.getCode() == KeyCode.LEFT) pet.setMoveLeft(true);
                if (e.getCode() == KeyCode.D || e.getCode() == KeyCode.RIGHT) pet.setMoveRight(true);
            });
            gameLayer.getScene().setOnKeyReleased(e -> {
                if (e.getCode() == KeyCode.A || e.getCode() == KeyCode.LEFT) pet.setMoveLeft(false);
                if (e.getCode() == KeyCode.D || e.getCode() == KeyCode.RIGHT) pet.setMoveRight(false);
            });
        });

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (pet != null) pet.updateMovement();
                checkCollisions();
            }
        };
        gameLoop.start();
    }

    // --- XỬ LÝ VA CHẠM (HỨNG ĐỒ) ---
    private void checkCollisions() {
        if (pet == null) return;
        Iterator<ImageView> it = activeCoins.iterator();
        while (it.hasNext()) {
            ImageView coin = it.next();
            if (pet.intersects(coin)) { // Nếu Pikachu chạm vào đồng tiền
                gameLayer.getChildren().remove(coin);
                it.remove();
                
                caughtCount++;
                if (bidCounter != null) bidCounter.setText("Bids: " + caughtCount);
                pet.jumpCelebrate(); // Nhảy ăn mừng
            }
        }
    }

    // --- SỰ KIỆN RƠI ĐỒ ---
    public void handleNewBidEvent() {
        if (gameLayer == null || pet == null) return;

        double startX = 100 + Math.random() * (gameLayer.getWidth() - 300);
        
        try {
            ImageView coin = new ImageView(new Image(getClass().getResourceAsStream("/images/item066.png")));
            coin.setFitWidth(40);
            coin.setFitHeight(40);
            coin.setLayoutX(startX);
            coin.setLayoutY(-50); 
            
            gameLayer.getChildren().add(coin);
            activeCoins.add(coin); // Lưu vào danh sách để xét va chạm

            TranslateTransition fall = new TranslateTransition(Duration.seconds(2.5), coin);
            fall.setFromY(0);
            fall.setToY(gameLayer.getHeight() + 50); // Rơi tụt xuống đất
            
            fall.setOnFinished(e -> {
                // Rớt xuống đất mà không ai hứng thì xóa đi
                gameLayer.getChildren().remove(coin);
                activeCoins.remove(coin);
            });
            fall.play();
        } catch (Exception e) {
            System.err.println("Không thể tạo vật phẩm: " + e.getMessage());
        }
    }

    // --- HÀM TÔ MÀU NÚT ---
    private void setActiveButton(Button activeButton) {
        String defaultStyle = "-fx-background-color: transparent; -fx-text-fill: #333333; -fx-alignment: CENTER_LEFT; -fx-padding: 12; -fx-font-size: 14px;";

        if (btnDashboard != null) btnDashboard.setStyle(defaultStyle);
        if (btnLiveAuctions != null) btnLiveAuctions.setStyle(defaultStyle);
        if (btnMyInventory != null) btnMyInventory.setStyle(defaultStyle);
        if (btnManagement != null) btnManagement.setStyle(defaultStyle);

        String activeStyle = "-fx-background-color: #E3B04B; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 12; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 14px;";
        if (activeButton != null) {
            activeButton.setStyle(activeStyle);
        }
    }

    // --- SỰ KIỆN MENU CHÍNH ---
    @FXML
    void handleBtnDashboard(ActionEvent event) {
        mainPane.loadView("ViewDashboard.fxml");
        setActiveButton(btnDashboard);
    }

    @FXML
    void handleBtnLiveAuctions(ActionEvent event) {
        mainPane.loadView("ViewLiveAuctions.fxml");
        setActiveButton(btnLiveAuctions);
    }

    @FXML
    void handleBtnManagement(ActionEvent event) {
        if (!ClientSession.isLoggedIn()) return;

        String role = ClientSession.getRole();

        if ("SELLER".equalsIgnoreCase(role)) {
            mainPane.loadView("SellerDashboard.fxml");
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            mainPane.loadView("AdminDashboard.fxml");
        } else {
            System.err.println("CẢNH BÁO: Tài khoản " + ClientSession.getUsername() + " cố truy cập Management trái phép!");
            return;
        }
        setActiveButton(btnManagement);
    }

    @FXML
    void handleBtnMyInventory(ActionEvent event) {
        mainPane.loadView("ViewMyInventory.fxml");
        setActiveButton(btnMyInventory);
    }

    @FXML
    void handleLogout(ActionEvent event) throws IOException {
        ClientSession.clear();
        SceneController.switchScene(event, "AuctionMenu.fxml"); 
    }

    // --- SỰ KIỆN CATEGORIES ---
    @FXML
    void handleCategoryFilter(ActionEvent event) {
        System.out.println("Lọc sản phẩm...");
        if (chkElectronics != null && chkElectronics.isSelected()) System.out.println("- Đồ điện tử");
        if (chkFineArts != null && chkFineArts.isSelected()) System.out.println("- Nghệ thuật");
        if (chkVehicles != null && chkVehicles.isSelected()) System.out.println("- Xe cộ");
    
        handleNewBidEvent(); // Kích hoạt sự kiện rơi đồ để test
    }
    
    @FXML
    void handleOpenProfile (MouseEvent event) {
        mainPane.loadView("UserProfile.fxml");
        setActiveButton(null);
    }
}