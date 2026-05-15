package com.client.controller.dashboard;

import com.client.util.BiddingPet;
import javafx.animation.AnimationTimer;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MiniGameManager {
    private AnchorPane gameLayer;
    private Label bidCounter;
    private BiddingPet pet;
    private int caughtCount = 0;
    private List<ImageView> activeCoins = new ArrayList<>();
    private AnimationTimer gameLoop;

    public MiniGameManager(AnchorPane gameLayer, Label bidCounter) {
        this.gameLayer = gameLayer;
        this.bidCounter = bidCounter;
        this.pet = new BiddingPet(gameLayer);
    }

    public void start() {
        setupControls();
        startGameLoop();
    }

    private void setupControls() {
        Platform.runLater(() -> {
            if (gameLayer.getScene() != null) {
                gameLayer.getScene().setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.A || e.getCode() == KeyCode.LEFT) pet.setMoveLeft(true);
                    if (e.getCode() == KeyCode.D || e.getCode() == KeyCode.RIGHT) pet.setMoveRight(true);
                });
                gameLayer.getScene().setOnKeyReleased(e -> {
                    if (e.getCode() == KeyCode.A || e.getCode() == KeyCode.LEFT) pet.setMoveLeft(false);
                    if (e.getCode() == KeyCode.D || e.getCode() == KeyCode.RIGHT) pet.setMoveRight(false);
                });
            }
        });
    }

    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (pet != null) pet.updateMovement();
                checkCollisions();
            }
        };
        gameLoop.start();
    }

    private void checkCollisions() {
        if (pet == null) return;
        Iterator<ImageView> it = activeCoins.iterator();
        while (it.hasNext()) {
            ImageView coin = it.next();
            if (pet.intersects(coin)) { 
                gameLayer.getChildren().remove(coin);
                it.remove();
                caughtCount++;
                if (bidCounter != null) bidCounter.setText("Bids: " + caughtCount);
                pet.jumpCelebrate(); 
            }
        }
    }

    public void triggerCoinFall() {
        if (gameLayer == null || pet == null) return;
        double startX = 100 + Math.random() * (gameLayer.getWidth() - 300);
        try {
            ImageView coin = new ImageView(new Image(getClass().getResourceAsStream("/images/item066.png")));
            coin.setFitWidth(40);
            coin.setFitHeight(40);
            coin.setLayoutX(startX);
            coin.setLayoutY(-50); 
            
            gameLayer.getChildren().add(coin);
            activeCoins.add(coin); 

            TranslateTransition fall = new TranslateTransition(Duration.seconds(2.5), coin);
            fall.setFromY(0);
            fall.setToY(gameLayer.getHeight() + 50); 
            fall.setOnFinished(e -> {
                gameLayer.getChildren().remove(coin);
                activeCoins.remove(coin);
            });
            fall.play();
        } catch (Exception e) {
            System.err.println("Không thể tạo vật phẩm: " + e.getMessage());
        }
    }
}