package com.client.controller.dashboard;

import com.client.util.BiddingPet;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameCornerController {

    @FXML private AnchorPane gameLayer;
    @FXML private Label bidCounter;
    @FXML private Button btnPlay;
    @FXML private Button btnFeed;
    @FXML private Button btnGacha;

    // --- CÁC BIẾN QUAN TRỌNG ---
    private MiniGameManager gameManager;
    private BiddingPet pet;
    private int totalBids = 0;
    
    // --- QUẢN LÝ GAME 30S ---
    private boolean isPlaying = false;
    private int timeLeft = 30;
    private Label timerLabel;
    private List<ImageView> activeCoins = new ArrayList<>();
    private AnimationTimer collisionTimer;

    private final String[] GACHA_POOL = {
        "/images/item066.png", "/images/item081.png", "/images/item096.png", 
        "/images/item111.png", "/images/item120.png", "/images/GavelImg.png"
    };

    @FXML
    public void initialize() {
        this.gameManager = DashboardController.instance.getGameManager();
        
        if (this.gameManager != null) {
            this.pet = this.gameManager.getPet(); 
            this.pet.speak("Khu giải trí đây rồi! Bấm chơi để kiếm xu nha sếp!");
        }

        // Khởi tạo Nhãn đếm ngược thời gian (Ẩn mặc định)
        timerLabel = new Label("Thời gian: 30s");
        timerLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        timerLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 5, 0, 0, 2);");
        timerLabel.setVisible(false);
        timerLabel.layoutXProperty().bind(gameLayer.widthProperty().subtract(150).divide(2));
        timerLabel.setLayoutY(90);
        gameLayer.getChildren().add(timerLabel);
    }

    // =================================================================================
    // 1. GAME HỨNG XU 30S
    // =================================================================================
    @FXML
    void handlePlayGame(ActionEvent event) {
        if (isPlaying || pet == null) return;
        isPlaying = true;
        timeLeft = 30;
        
        timerLabel.setText("Thời gian: " + timeLeft + "s");
        timerLabel.setVisible(true);
        btnPlay.setDisable(true); 
        
        pet.speak("Di chuột để mình hứng xu sếp ơi!");

        AnchorPane.clearConstraints(pet.getContainer());
        pet.getContainer().setLayoutY(gameLayer.getHeight() - 150); 

        gameLayer.setMouseTransparent(false);
        gameLayer.setOnMouseMoved(e -> {
            if (isPlaying) {
                pet.getContainer().setLayoutX(e.getX() - 50); 
            }
        });

        Timeline coinSpawner = new Timeline(new KeyFrame(Duration.millis(500), e -> spawnFallingCoin()));
        coinSpawner.setCycleCount(60); 
        coinSpawner.play();

        Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeLeft--;
            timerLabel.setText("Thời gian: " + timeLeft + "s");
            if (timeLeft <= 0) {
                end30sGame();
            }
        }));
        countdown.setCycleCount(30);
        countdown.play();

        collisionTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                checkCollisions();
            }
        };
        collisionTimer.start();
    }

    private void spawnFallingCoin() {
        double startX = 50 + Math.random() * (gameLayer.getWidth() - 100);
        try {
            ImageView coin = new ImageView(new Image(getClass().getResourceAsStream("/icons/coin.png")));
            coin.setFitWidth(45);
            coin.setFitHeight(45);
            coin.setLayoutX(startX);
            coin.setLayoutY(-50); 
            
            gameLayer.getChildren().add(coin);
            activeCoins.add(coin);

            TranslateTransition fall = new TranslateTransition(Duration.seconds(3.5), coin);
            fall.setFromY(0);
            fall.setToY(gameLayer.getHeight() + 100); 
            fall.setOnFinished(e -> {
                gameLayer.getChildren().remove(coin);
                activeCoins.remove(coin);
            });
            fall.play();
        } catch (Exception e) {
            System.err.println("Lỗi load icon xu: " + e.getMessage());
        }
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
        timerLabel.setVisible(false);
        gameLayer.setOnMouseMoved(null);
        gameLayer.setMouseTransparent(true); 
        btnPlay.setDisable(false); 
        
        AnchorPane.setBottomAnchor(pet.getContainer(), 30.0);
        AnchorPane.setRightAnchor(pet.getContainer(), 30.0);
        
        pet.jumpCelebrate();
        pet.speak("Hết giờ! Chúng ta đã kiếm được " + totalBids + " Bids!");
    }

    // =================================================================================
    // 2. NUÔI PET
    // =================================================================================
    @FXML
    void handleFeedPet(ActionEvent event) {
        if (pet == null) return;
        if (isPlaying) {
            pet.speak("Đang bận hứng xu sếp ơi!");
            return;
        }
        if (totalBids < 5) {
            pet.speak("Không đủ tiền mua đồ ăn (Cần 5 Bids)!");
            return;
        }
        
        totalBids -= 5;
        if (bidCounter != null) bidCounter.setText("Bids: " + totalBids);
        
        pet.speak("Măm măm... Ngon quá! Yêu sếp <3");
        pet.jumpCelebrate();
        
        Label heart = new Label("❤️");
        heart.setStyle("-fx-text-fill: #FF4757; -fx-font-size: 30px;");
        heart.setLayoutX(pet.getContainer().getLayoutX() + 30);
        heart.setLayoutY(pet.getContainer().getLayoutY());
        gameLayer.getChildren().add(heart);

        TranslateTransition floatUp = new TranslateTransition(Duration.seconds(2), heart);
        floatUp.setByY(-80);
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(2), heart);
        fadeOut.setToValue(0);

        ParallelTransition pt = new ParallelTransition(floatUp, fadeOut);
        pt.setOnFinished(e -> gameLayer.getChildren().remove(heart));
        pt.play();
    }

    // =================================================================================
    // 3. GACHA
    // =================================================================================
    @FXML
    void handleRollGacha(ActionEvent event) {
        if (pet == null) return;
        if (isPlaying) {
            pet.speak("Chờ em hứng xu xong đã!");
            return;
        }
        if (totalBids < 10) {
            pet.speak("Cần 10 Bids mới mở được rương!");
            return;
        }

        totalBids -= 10;
        if (bidCounter != null) bidCounter.setText("Bids: " + totalBids);

        String randomItemPath = GACHA_POOL[(int) (Math.random() * GACHA_POOL.length)];

        Platform.runLater(() -> {
            try {
                ImageView rewardView = new ImageView(new Image(getClass().getResourceAsStream(randomItemPath)));
                rewardView.setFitWidth(150);
                rewardView.setFitHeight(150);
                rewardView.setLayoutX((gameLayer.getWidth() - 150) / 2);
                rewardView.setLayoutY((gameLayer.getHeight() - 150) / 2);
                
                DropShadow glow = new DropShadow();
                glow.setColor(Color.GOLD);
                glow.setRadius(40); glow.setSpread(0.6);
                rewardView.setEffect(glow);

                rewardView.setScaleX(0); rewardView.setScaleY(0);
                gameLayer.getChildren().add(rewardView);

                ScaleTransition scaleUp = new ScaleTransition(Duration.seconds(0.7), rewardView);
                scaleUp.setToX(1.5); scaleUp.setToY(1.5);
                
                RotateTransition wobble = new RotateTransition(Duration.millis(80), rewardView);
                wobble.setByAngle(15); wobble.setCycleCount(6); wobble.setAutoReverse(true);
                
                PauseTransition delay = new PauseTransition(Duration.seconds(3));
                
                FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), rewardView);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> gameLayer.getChildren().remove(rewardView));

                SequentialTransition seq = new SequentialTransition(scaleUp, wobble, delay, fadeOut);
                seq.play();

                pet.jumpCelebrate();
                pet.speak("Wow!!! Sếp nhân phẩm tốt quá!!");

            } catch (Exception e) {
                System.err.println("Lỗi Gacha: " + e.getMessage());
            }
        });
    }
}