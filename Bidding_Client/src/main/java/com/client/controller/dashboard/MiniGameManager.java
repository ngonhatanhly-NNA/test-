package com.client.controller.dashboard;

import com.client.util.BiddingPet;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import javafx.scene.input.KeyCode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MiniGameManager {
    private AnchorPane gameLayer;
    private Label bidCounter;
    private BiddingPet pet;
    
    private int totalBids = 0; 
    private boolean isPlaying = false;
    private int timeLeft = 30;
    private Label timerLabel;
    private List<ImageView> activeCoins = new ArrayList<>();
    private AnimationTimer collisionTimer;
    
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private AnimationTimer movementTimer;
    
    // Callback để báo cho Controller khi game kết thúc
    private Runnable onGameEnd;

    private final String[] GACHA_POOL = {
        "/images/phoebe.png", "/images/cute.png", "/images/voucher.png", 
        "/images/fail3.png", "/images/fail2.png", "/images/fail.png", "/images/dog.png", "/images/hams.png"
    };

    public MiniGameManager(AnchorPane gameLayer, Label bidCounter) {
        this.gameLayer = gameLayer;
        this.bidCounter = bidCounter;
        this.pet = new BiddingPet(gameLayer);
        
        gameLayer.toFront(); 
        pet.getContainer().setMouseTransparent(false);

        timerLabel = new Label("Time: 30s");
        timerLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        timerLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 5, 0, 0, 2);");
        timerLabel.setVisible(false);
        timerLabel.setLayoutX(550);
        timerLabel.setLayoutY(50);
        gameLayer.getChildren().add(timerLabel);
    }

    public BiddingPet getPet() { return pet; }
    
    public void setOnGameEnd(Runnable callback) { this.onGameEnd = callback; }

    public void start30sGame() {
        if (isPlaying) return;
        isPlaying = true;
        timeLeft = 30;
        timerLabel.setText("Time: " + timeLeft + "s");
        timerLabel.setVisible(true);
        
        pet.speak("Start! Use A and D keys to catch coins!");

        AnchorPane.clearConstraints(pet.getContainer());
        pet.getContainer().setLayoutY(gameLayer.getHeight() - 150);

        gameLayer.setFocusTraversable(true);
        gameLayer.requestFocus();
        gameLayer.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.A) moveLeft = true;
            if (e.getCode() == KeyCode.D) moveRight = true;
        });
        gameLayer.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.A) moveLeft = false;
            if (e.getCode() == KeyCode.D) moveRight = false;
        });

        movementTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (moveLeft && pet.getContainer().getLayoutX() > 0) 
                    pet.getContainer().setLayoutX(pet.getContainer().getLayoutX() - 10);
                if (moveRight && pet.getContainer().getLayoutX() < gameLayer.getWidth() - 100) 
                    pet.getContainer().setLayoutX(pet.getContainer().getLayoutX() + 10);
            }
        };
        movementTimer.start();

        Timeline coinSpawner = new Timeline(new KeyFrame(Duration.millis(500), e -> spawnFallingCoin()));
        coinSpawner.setCycleCount(60);
        coinSpawner.play();

        Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeLeft--;
            timerLabel.setText("Time: " + timeLeft + "s");
            if (timeLeft <= 0) end30sGame();
        }));
        countdown.setCycleCount(30);
        countdown.play();

        collisionTimer = new AnimationTimer() {
            @Override
            public void handle(long now) { checkCollisions(); }
        };
        collisionTimer.start();
    }

    private void spawnFallingCoin() {
        double startX = 50 + Math.random() * (gameLayer.getWidth() - 100);
        try {
            ImageView coin = new ImageView(new Image(getClass().getResourceAsStream("/icons/coin.png")));
            coin.setFitWidth(45); coin.setFitHeight(45);
            coin.setLayoutX(startX); coin.setLayoutY(-50); 
            gameLayer.getChildren().add(coin);
            activeCoins.add(coin);
            TranslateTransition fall = new TranslateTransition(Duration.seconds(3.5), coin);
            fall.setFromY(0); fall.setToY(gameLayer.getHeight() + 100); 
            fall.setOnFinished(e -> { gameLayer.getChildren().remove(coin); activeCoins.remove(coin); });
            fall.play();
        } catch (Exception ignored) {}
    }

    private void checkCollisions() {
        Iterator<ImageView> iter = activeCoins.iterator();
        while (iter.hasNext()) {
            ImageView coin = iter.next();
            if (pet.getContainer().getBoundsInParent().intersects(coin.getBoundsInParent())) {
                gameLayer.getChildren().remove(coin);
                iter.remove(); 
                totalBids++;
                if (bidCounter != null) bidCounter.setText("Bids: " + totalBids);
            }
        }
    }

    private void end30sGame() {
        isPlaying = false;
        collisionTimer.stop();
        movementTimer.stop(); 
        timerLabel.setVisible(false);
        if (onGameEnd != null) onGameEnd.run();
        
        AnchorPane.setBottomAnchor(pet.getContainer(), 30.0);
        AnchorPane.setRightAnchor(pet.getContainer(), 30.0);
        
        pet.jumpCelebrate();
        pet.speak("Time's up! We earned " + totalBids + " Bids! Great job boss!");
    }

    public void feedPet() {
        if (isPlaying) { pet.speak("Busy catching coins boss!"); return; }
        if (totalBids < 5) { pet.speak("I'm hungry! Please play game to earn 5 Bids!"); return; }
        totalBids -= 5;
        if (bidCounter != null) bidCounter.setText("Bids: " + totalBids);
        pet.speak("Yummy! Thank you boss!");
        pet.jumpCelebrate();
    }

    public void rollGacha() {
        if (isPlaying) { pet.speak("Wait for me to finish catching coins!"); return; }
        if (totalBids < 10) { pet.speak("Boss is so poor! You need 10 Bids to unlock the Gacha!"); return; }
        totalBids -= 10;
        if (bidCounter != null) bidCounter.setText("Bids: " + totalBids);

        String randomItemPath = GACHA_POOL[(int) (Math.random() * GACHA_POOL.length)];
        Platform.runLater(() -> {
            try {
                ImageView rewardView = new ImageView(new Image(getClass().getResourceAsStream(randomItemPath)));
                rewardView.setFitWidth(150); rewardView.setFitHeight(150);
                rewardView.setLayoutX((gameLayer.getWidth() - 150) / 2);
                rewardView.setLayoutY((gameLayer.getHeight() - 150) / 2);
				
				DropShadow glow = new DropShadow(); 
                glow.setColor(Color.GOLD); 
                glow.setRadius(40); 
                glow.setSpread(0.6);
                
				rewardView.setScaleX(0); 
                rewardView.setScaleY(0);
                gameLayer.getChildren().add(rewardView);
				
                ScaleTransition scaleUp = new ScaleTransition(Duration.seconds(0.7), rewardView);
                scaleUp.setToX(1.5); 
                scaleUp.setToY(1.5);
				
				RotateTransition wobble = new RotateTransition(Duration.millis(80), rewardView);
                wobble.setByAngle(15); 
                wobble.setCycleCount(6); 
                wobble.setAutoReverse(true);
				
				PauseTransition delay = new PauseTransition(Duration.seconds(3));
				FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), rewardView);
                fadeOut.setToValue(0);

                SequentialTransition seq = new SequentialTransition(scaleUp, wobble, delay, fadeOut);
                seq.setOnFinished(e -> gameLayer.getChildren().remove(rewardView));
                seq.play();

                pet.jumpCelebrate();
                pet.speak("SOS ! Rare item WOROWO!!");
            } catch (Exception e) {
				System.err.println("Lỗi load ảnh Gacha: " + e.getMessage());
			}
        });
    }
}