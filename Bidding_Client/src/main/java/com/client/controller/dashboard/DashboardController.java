package com.client.controller.dashboard;

import com.client.util.DashboardNavigation;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.AnchorPane; 
import javafx.scene.input.MouseEvent;

import java.io.IOException;

import com.client.util.SwitchPane;
import com.client.util.SceneController;
import com.client.session.ClientSession;

public class DashboardController {

    @FXML private BorderPane mainBorderPane;
    private SwitchPane mainPane; 

    @FXML private Button btnDashboard, btnLiveAuctions, btnMyInventory, btnLogout, btnManagement;
    @FXML private Label lblUsername;
    @FXML private TextField searchField;
    @FXML private CheckBox chkElectronics, chkFineArts, chkVehicles;

    @FXML private AnchorPane gameLayer; 
    @FXML private Label bidCounter;     
    
    private MiniGameManager gameManager; // Đã tách Game logic ra đây!
    public static DashboardController instance;

    @FXML
    public void initialize() {
        mainPane = new SwitchPane(mainBorderPane);

        if (ClientSession.isLoggedIn()) {
            lblUsername.setText(ClientSession.getUsername());
            if (btnManagement != null && "BIDDER".equalsIgnoreCase(ClientSession.getRole())) {
                btnManagement.setVisible(false);
                btnManagement.setManaged(false);
            }
        } else {
            lblUsername.setText("Khách");
            if (btnManagement != null) {
                btnManagement.setVisible(false);
                btnManagement.setManaged(false);
            }
        }

        instance = this;
        
        // Khởi động Game Manager
        if (gameLayer != null) {
            gameManager = new MiniGameManager(gameLayer, bidCounter);
            gameManager.start();
        }
        
        handleBtnDashboard(null);
        
        DashboardNavigation.setOpenLiveAuctions(() -> handleBtnLiveAuctions(null));
        DashboardNavigation.setNavigateToAuctionDetail(auctionId -> handleBtnLiveAuctions(null));
    }

    public void handleNewBidEvent() {
        if (gameManager != null) {
            gameManager.triggerCoinFall();
        }
    }

    private void setActiveButton(Button activeButton) {
        String defaultStyle = "-fx-background-color: transparent; -fx-text-fill: #333333; -fx-alignment: CENTER_LEFT; -fx-padding: 12; -fx-font-size: 14px;";
        if (btnDashboard != null) btnDashboard.setStyle(defaultStyle);
        if (btnLiveAuctions != null) btnLiveAuctions.setStyle(defaultStyle);
        if (btnMyInventory != null) btnMyInventory.setStyle(defaultStyle);
        if (btnManagement != null) btnManagement.setStyle(defaultStyle);

        if (activeButton != null) {
            activeButton.setStyle("-fx-background-color: #E3B04B; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 12; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 14px;");
        }
    }

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
        if ("SELLER".equalsIgnoreCase(role)) mainPane.loadView("SellerDashboard.fxml");
        else if ("ADMIN".equalsIgnoreCase(role)) mainPane.loadView("AdminDashboard.fxml");
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

    @FXML
    void handleCategoryFilter(ActionEvent event) {
        handleNewBidEvent(); // Gọi sang GameManager
    }
    
    @FXML
    void handleOpenProfile (MouseEvent event) {
        mainPane.loadView("UserProfile.fxml");
        setActiveButton(null);
    }
}