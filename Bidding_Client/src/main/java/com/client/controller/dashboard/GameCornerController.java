package com.client.controller.dashboard;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

public class GameCornerController {

    @FXML private AnchorPane gameLayer;
    @FXML private Label bidCounter;
    @FXML private Button btnGacha;
    @FXML private Button btnSpawn;

    private MiniGameManager gameManager;

    @FXML
    public void initialize() {
        // Khởi tạo game trực tiếp trên gameLayer của panel này
        gameManager = new MiniGameManager(gameLayer, bidCounter);
        
        // Cho trợ lý ảo chào sếp khi vừa vào phòng game
        gameManager.getPet().speak("Welcome to gaming zone!");
    }

    @FXML
    void handleTriggerFall(ActionEvent event) {
        if (gameManager != null) {
            // Thả ngẫu nhiên vài đồng xu rơi xuống
            for (int i = 0; i < 5; i++) {
                gameManager.triggerCoinFall();
            }
        }
    }

    @FXML
    void handleRollGacha(ActionEvent event) {
        if (gameManager != null) {
            gameManager.rollGacha();
        }
    }
}