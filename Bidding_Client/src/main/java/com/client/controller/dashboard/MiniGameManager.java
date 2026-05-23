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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MiniGameManager {
    private AnchorPane gameLayer;
    private Label bidCounter;
    private BiddingPet pet;
    
    private int totalBids = 0; // Tổng tài sản (Bids)
    
    // Quản lý Game Hứng Xu 30s
    private boolean isPlaying = false;
    private int timeLeft = 30;
    private Label timerLabel;
    private List<ImageView> activeCoins = new ArrayList<>();
    private AnimationTimer collisionTimer;

    // Danh sách đồ hiếm Gacha
    private final String[] GACHA_POOL = {
        "/images/item066.png", "/images/item081.png", "/images/item096.png", 
        "/images/item111.png", "/images/item120.png", "/images/GavelImg.png"
    };

    public MiniGameManager(AnchorPane gameLayer, Label bidCounter) {
        this.gameLayer = gameLayer;
        this.bidCounter = bidCounter;
        this.pet = new BiddingPet(gameLayer);
        
        gameLayer.toFront(); 
        pet.getContainer().setMouseTransparent(false);

        // Tạo sẵn một Label đếm ngược thời gian (ẩn đi khi không chơi)
        timerLabel = new Label("Thời gian: 30s");
        timerLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        timerLabel.setStyle("-fx-text-fill: #FF6B6B; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 5, 0, 0, 2);");
        timerLabel.setVisible(false);
        // Căn giữa phía trên màn hình
        timerLabel.layoutXProperty().bind(gameLayer.widthProperty().subtract(150).divide(2));
        timerLabel.setLayoutY(50);
        gameLayer.getChildren().add(timerLabel);
    }

    public BiddingPet getPet() {
        return pet;
    }

    // ==========================================
    // TÍNH NĂNG 1: GAME HỨNG XU 30 GIÂY
    // ==========================================
    public void start30sGame() {
        if (isPlaying) return;
        isPlaying = true;
        timeLeft = 30;
        timerLabel.setText("Thời gian: " + timeLeft + "s");
        timerLabel.setVisible(true);
        
        pet.speak("Bắt đầu! Di chuột để mình hứng xu sếp ơi!");

        // 1. Gỡ neo của Pet để nó có thể di chuyển tự do theo chuột
        AnchorPane.clearConstraints(pet.getContainer());
        pet.getContainer().setLayoutY(gameLayer.getHeight() - 150); // Giữ Pet ở sát đáy màn hình

        // 2. Bắt sự kiện chuột để Pet chạy theo
        gameLayer.setMouseTransparent(false);
        gameLayer.setOnMouseMoved(e -> {
            if (isPlaying) {
                // Đặt Pet ở ngay vị trí chuột (trừ đi 50px để căn giữa đầu Pet)
                pet.getContainer().setLayoutX(e.getX() - 50);
            }
        });

        // 3. Vòng lặp thả xu (mỗi 0.5s thả 1 đồng)
        Timeline coinSpawner = new Timeline(new KeyFrame(Duration.millis(500), e -> spawnFallingCoin()));
        coinSpawner.setCycleCount(60); // 30 giây * 2 = 60 đồng xu
        coinSpawner.play();

        // 4. Vòng lặp đếm ngược thời gian
        Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeLeft--;
            timerLabel.setText("Thời gian: " + timeLeft + "s");
            if (timeLeft <= 0) {
                end30sGame();
            }
        }));
        countdown.setCycleCount(30);
        countdown.play();

        // 5. Vòng lặp kiểm tra va chạm (rất nhanh, chạy ~60fps)
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
    }

    private void checkCollisions() {
        Iterator<ImageView> iter = activeCoins.iterator();
        while (iter.hasNext()) {
            ImageView coin = iter.next();
            // Nếu hình chữ nhật bao quanh Đồng xu chạm vào hình chữ nhật của Pikachu
            if (pet.getContainer().getBoundsInParent().intersects(coin.getBoundsInParent())) {
                gameLayer.getChildren().remove(coin); // Xóa xu khỏi màn hình
                iter.remove(); // Xóa khỏi danh sách theo dõi
                
                totalBids++; // Cộng điểm
                if (bidCounter != null) bidCounter.setText("Bids: " + totalBids);
            }
        }
    }

    private void end30sGame() {
        isPlaying = false;
        collisionTimer.stop();
        timerLabel.setVisible(false);
        gameLayer.setOnMouseMoved(null); // Tắt sự kiện chuột
        gameLayer.setMouseTransparent(true); // Tắt tương tác khung game
        
        // Neo Pet về lại góc phải dưới
        AnchorPane.setBottomAnchor(pet.getContainer(), 30.0);
        AnchorPane.setRightAnchor(pet.getContainer(), 30.0);
        
        pet.jumpCelebrate();
        pet.speak("Hết giờ! Chúng ta cày được " + totalBids + " Bids rồi sếp!");
    }

    // ==========================================
    // TÍNH NĂNG 2: NUÔI PET (CHO ĂN)
    // ==========================================
    public void feedPet() {
        if (isPlaying) {
            pet.speak("Đang bận hứng xu sếp ơi!");
            return;
        }
        if (totalBids < 5) {
            pet.speak("Em đói! Sếp chơi game kiếm thêm 5 Bids mua đồ ăn đi!");
            return;
        }
        
        // Trừ điểm
        totalBids -= 5;
        if (bidCounter != null) bidCounter.setText("Bids: " + totalBids);
        
        pet.speak("Măm măm... Ngon quá! Yêu sếp <3");
        pet.jumpCelebrate();
        
        // Hiệu ứng thả tim bay lên từ đầu Pet
        Label heart = new Label("❤️");
        heart.setStyle("-fx-text-fill: #FF4757; -fx-font-size: 30px;");
        heart.setLayoutX(pet.getContainer().getLayoutX() + 30);
        heart.setLayoutY(pet.getContainer().getLayoutY());
        gameLayer.getChildren().add(heart);

        TranslateTransition floatUp = new TranslateTransition(Duration.seconds(2), heart);
        floatUp.setByY(-80); // Bay lên 80px
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(2), heart);
        fadeOut.setToValue(0);

        ParallelTransition pt = new ParallelTransition(floatUp, fadeOut);
        pt.setOnFinished(e -> gameLayer.getChildren().remove(heart));
        pt.play();
    }

    // ==========================================
    // TÍNH NĂNG 3: VÒNG QUAY GACHA
    // ==========================================
    public void rollGacha() {
        if (isPlaying) {
            pet.speak("Chờ em hứng xu xong đã!");
            return;
        }
        if (totalBids < 10) {
            pet.speak("Không đủ 10 Bids mở rương rồi sếp ơi!");
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
                pet.speak("Wow!!! Trúng hàng kịch độc rồi!!");

            } catch (Exception e) {
                System.err.println("Lỗi Gacha: " + e.getMessage());
            }
        });
    }
}