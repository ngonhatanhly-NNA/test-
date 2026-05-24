package com.client.controller.dashboard;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class GameCornerController {

    private MiniGameManager gameManager;

    @FXML
    public void initialize() {
        this.gameManager = DashboardController.instance.getGameManager();
        
        if (this.gameManager != null) {
            this.gameManager.getPet().speak("Boss let's go and play some game");
        }
    }

    @FXML
    void handlePlayGame(ActionEvent event) {
        if (gameManager != null) gameManager.start30sGame();
    }

    @FXML
    void handleFeedPet(ActionEvent event) {
        if (gameManager != null) gameManager.feedPet();
    }

    @FXML
    void handleRollGacha(ActionEvent event) {
        if (gameManager != null) gameManager.rollGacha();
    }
}