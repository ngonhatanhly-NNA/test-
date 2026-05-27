package com.client.controller.dashboard;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.input.MouseEvent;
import javafx.application.Platform;
import java.io.IOException;
import com.client.util.DashboardSearchBridge;
import com.client.util.DashboardNavigation;
import com.client.util.SwitchPane;
import com.client.util.SceneController;
import com.client.session.ClientSession;

public class DashboardController {

    @FXML private BorderPane mainBorderPane;
    private SwitchPane mainPane;

    @FXML private Button btnDashboard, btnLiveAuctions, btnMyInventory, btnLogout, btnManagement, btnGameCorner;
    @FXML private Label lblUsername;
    @FXML private TextField searchField;
   
    // --- LINH VẬT TOÀN CỤC ---
    @FXML private AnchorPane gameLayer;
    @FXML private Label bidCounter;
    private MiniGameManager gameManager; 
    
    public static DashboardController instance;

    @FXML
    public void initialize() {
        instance = this;
        mainPane = new SwitchPane(mainBorderPane);
		
		if (searchField != null) {
			searchField.textProperty().addListener((obs, oldVal, newVal) -> {
				DashboardSearchBridge.publish(newVal); 
			});
		}
		
		if (ClientSession.isLoggedIn()) {
            lblUsername.setText(ClientSession.getUsername());
            if (btnManagement != null && "SELLER".equalsIgnoreCase(ClientSession.getRole())) {
                btnManagement.setVisible(true);
                btnManagement.setManaged(true);
            } else if (btnManagement != null && "ADMIN".equalsIgnoreCase(ClientSession.getRole())) {
                btnManagement.setVisible(true);
                btnManagement.setManaged(true);
            } else if (btnManagement != null) {
                btnManagement.setVisible(false);
                btnManagement.setManaged(false);
            }
        }
		
		DashboardNavigation.setOpenLiveAuctions(() -> {
            handleBtnLiveAuctions(null); 
        });

        DashboardNavigation.setNavigateToAuctionDetail(auctionId -> {
            handleBtnLiveAuctions(null); 
         
            Platform.runLater(() -> {
                if (ViewLiveAuctions.getInstance() != null) {
                    ViewLiveAuctions.getInstance().handleRefreshAuctions();
                }
            });
        });

        // KHỞI TẠO PET TOÀN CỤC
        if (gameLayer != null && bidCounter != null) {
            gameManager = new MiniGameManager(gameLayer, bidCounter);
            gameManager.getPet().speak("Welcome to T13 Auction System BOSS!");
        }

        mainPane.loadView("ViewDashboard.fxml");
        setActiveButton(btnDashboard);
    }

    public MiniGameManager getGameManager() {
        return gameManager;
    }

    public void setActiveButton(Button activeButton) {
        Button[] buttons = {btnDashboard, btnLiveAuctions, btnMyInventory, btnManagement, btnGameCorner};
        for (Button btn : buttons) {
            if (btn != null) {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #E0E0E0; -fx-alignment: CENTER_LEFT; -fx-padding: 0 20; -fx-background-radius: 12; -fx-cursor: hand;");
            }
        }
        if (activeButton != null) {
            activeButton.setStyle("-fx-background-color: linear-gradient(to right, #D4AF37, #FFD700); -fx-text-fill: #121212; -fx-background-radius: 12; -fx-alignment: CENTER_LEFT; -fx-padding: 0 20; -fx-effect: dropshadow(gaussian, rgba(212,175,55,0.5), 15, 0, 0, 0); -fx-font-weight: bold;");
        }
    }

    @FXML
    void handleBtnDashboard(ActionEvent event) {
        mainPane.loadView("ViewDashboard.fxml");
        setActiveButton(btnDashboard);
        if (gameManager != null) gameManager.getPet().speak("Boss it seems the menu got some updates :333!");
    }

    @FXML
    void handleBtnLiveAuctions(ActionEvent event) {
        mainPane.loadView("ViewLiveAuctions.fxml");
        setActiveButton(btnLiveAuctions);
        if (gameManager != null) gameManager.getPet().speak("Let's explore some interesting auction boss:3!");
    }

    @FXML
    void handleBtnManagement(ActionEvent event) {
        if (!ClientSession.isLoggedIn()) return;
        String role = ClientSession.getRole();
        if ("SELLER".equalsIgnoreCase(role)) mainPane.loadView("SellerDashboard.fxml");
        else if ("ADMIN".equalsIgnoreCase(role)) mainPane.loadView("AdminDashboard.fxml");
        setActiveButton(btnManagement);
        if (gameManager != null) gameManager.getPet().speak("Ongoing ,let;s catch some cheater boss...");
    }

    @FXML
    void handleBtnMyInventory(ActionEvent event) {
        mainPane.loadView("ViewMyInventory.fxml");
        setActiveButton(btnMyInventory);
        if (gameManager != null) gameManager.getPet().speak("Let's go to our inventory");
    }
    
    @FXML
    void handleBtnGameCorner(ActionEvent event) {
        mainPane.loadView("GameCorner.fxml");
        setActiveButton(btnGameCorner);
    }

    @FXML
    void handleLogout(ActionEvent event) throws IOException {
        ClientSession.clear();
        SceneController.switchScene(event, "AuctionMenu.fxml");
    }

    @FXML
    void handleCategoryFilter(ActionEvent event) {
    }

    @FXML
    void handleOpenProfile(MouseEvent event) {
        mainPane.loadView("UserProfile.fxml");
        setActiveButton(null);
        if (gameManager != null) gameManager.getPet().speak("Interesting, boss's privacy :>");
    }
}