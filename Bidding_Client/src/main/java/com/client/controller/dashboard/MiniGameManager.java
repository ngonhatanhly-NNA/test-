package com.client.controller.dashboard;

import com.client.util.BiddingPet;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class MiniGameManager {
    private AnchorPane gameLayer;
    private Label bidCounter;
    private BiddingPet pet;
    private int caughtCount = 0;
    
    // Đếm số lượng vật phẩm đang rớt trên màn hình
    private int activeItemsCount = 0; 

    // Danh sách hình ảnh các món đồ cực hiếm
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
    }

    public BiddingPet getPet() {
        return pet;
    }

    public void triggerCoinFall() {
        if (gameLayer == null || pet == null) return;
        
        Platform.runLater(() -> {
            activeItemsCount++;
            // Bật tương tác chuột cho gameLayer khi có đồ rớt
            gameLayer.setMouseTransparent(false); 
            
            double startX = 100 + Math.random() * (gameLayer.getWidth() - 200);
            try {
                ImageView coin = new ImageView(new Image(getClass().getResourceAsStream("/icons/coin.png")));
                coin.setFitWidth(50);
                coin.setFitHeight(50);
                coin.setLayoutX(startX);
                coin.setLayoutY(-60); 
                coin.setStyle("-fx-cursor: hand;"); 
                
                gameLayer.getChildren().add(coin);

                TranslateTransition fall = new TranslateTransition(Duration.seconds(4.0), coin);
                fall.setFromY(0);
                fall.setToY(gameLayer.getHeight() + 100); 
                fall.setOnFinished(e -> {
                    gameLayer.getChildren().remove(coin);
                    checkAndDisableGameLayer(); // Kiểm tra xem còn đồ nào rớt không
                });
                
                // SỰ KIỆN CLICK VÀO VẬT PHẨM
                coin.setOnMouseClicked(e -> {
                    fall.stop();
                    gameLayer.getChildren().remove(coin);
                    
                    caughtCount++;
                    if (bidCounter != null) bidCounter.setText("Bids: " + caughtCount);
                    
                    pet.jumpCelebrate();
                    pet.speak("Hurray!");
                    
                    checkAndDisableGameLayer();
                });

                fall.play();
            } catch (Exception e) {
                System.err.println("Lỗi tạo vật phẩm rơi: " + e.getMessage());
                checkAndDisableGameLayer();
            }
        });
    }

    // --- HÀM VÒNG QUAY NHÂN PHẨM (GACHA) ---
    public void rollGacha() {
        // 1. Kiểm tra xem có đủ tiền (10 Bids) không
        if (caughtCount < 10) {
            pet.jumpCelebrate();
            pet.speak("Not enough bids, get more!");
            return;
        }

        // 2. Đủ tiền -> Trừ 10 Bids
        caughtCount -= 10;
        if (bidCounter != null) bidCounter.setText("Bids: " + caughtCount);

        // Bật lớp tương tác lên để hiện đồ ở giữa màn hình
        activeItemsCount++;
        gameLayer.setMouseTransparent(false);

        // 3. Random lấy 1 món đồ từ danh sách
        String randomItemPath = GACHA_POOL[(int) (Math.random() * GACHA_POOL.length)];

        Platform.runLater(() -> {
            try {
                ImageView rewardView = new ImageView(new Image(getClass().getResourceAsStream(randomItemPath)));
                rewardView.setFitWidth(150);
                rewardView.setFitHeight(150);
                
                // Căn chính giữa màn hình (giả sử AnchorPane của bạn khá rộng)
                rewardView.setLayoutX((gameLayer.getWidth() - 150) / 2);
                rewardView.setLayoutY((gameLayer.getHeight() - 150) / 2);
                
                // Hiệu ứng vầng hào quang Vàng chói lóa
                DropShadow glow = new DropShadow();
                glow.setColor(Color.GOLD);
                glow.setRadius(40);
                glow.setSpread(0.6);
                rewardView.setEffect(glow);

                // Bắt đầu từ kích thước 0 (tàng hình)
                rewardView.setScaleX(0);
                rewardView.setScaleY(0);

                gameLayer.getChildren().add(rewardView);

                // Animation 1: Phóng to bùm ra (Scale)
                ScaleTransition scaleUp = new ScaleTransition(Duration.seconds(0.7), rewardView);
                scaleUp.setToX(1.5); 
                scaleUp.setToY(1.5);
                
                // Animation 2: Lắc lư mừng rỡ (Rotate)
                RotateTransition wobble = new RotateTransition(Duration.millis(80), rewardView);
                wobble.setByAngle(15);
                wobble.setCycleCount(6); // Lắc 6 lần
                wobble.setAutoReverse(true);
                
                // Animation 3: Dừng lại cho người dùng ngắm 3 giây
                PauseTransition delay = new PauseTransition(Duration.seconds(3));
                
                // Animation 4: Mờ dần rồi biến mất
                FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), rewardView);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> {
                    gameLayer.getChildren().remove(rewardView);
                    checkAndDisableGameLayer(); 
                });

                // Chắp nối 4 hiệu ứng lại chạy liên tiếp
                SequentialTransition seq = new SequentialTransition(scaleUp, wobble, delay, fadeOut);
                seq.play();

                // Pikachu phản ứng ngạc nhiên
                pet.jumpCelebrate();
                pet.speak("Wow!!! Rare Item!!!");

            } catch (Exception e) {
                System.err.println("Lỗi load ảnh Gacha: " + e.getMessage());
                checkAndDisableGameLayer();
            }
        });
    }

    // Hàm phụ: Tắt tương tác chuột của gameLayer nếu không còn vật phẩm nào đang rớt
    private void checkAndDisableGameLayer() {
        activeItemsCount--;
        if (activeItemsCount <= 0) {
            activeItemsCount = 0;
            gameLayer.setMouseTransparent(true);
        }
    }
}