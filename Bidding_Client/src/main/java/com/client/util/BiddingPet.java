package com.client.util;

import javafx.animation.*;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class BiddingPet {
    private ImageView mascotView;
    private SpriteAnimation walkAnim;
    private Pane gameLayer;
    
    // --- BIẾN ĐIỀU KHIỂN ---
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private double speed = 8.0; // Tốc độ chạy của Pet

    public BiddingPet (Pane layer) {
        this.gameLayer = layer;
        
        Image spriteSheet = new Image(getClass().getResourceAsStream("/images/Pikachu.png"));
        this.mascotView = new ImageView(spriteSheet);
        
        int cols = 4; int rows = 4;
        double frameW = spriteSheet.getWidth() / cols;
        double frameH = spriteSheet.getHeight() / rows;

        mascotView.setViewport(new Rectangle2D(0, 0, frameW, frameH));
        
        walkAnim = new SpriteAnimation(mascotView, Duration.millis(600), cols, cols, rows);
        walkAnim.setCycleCount(Animation.INDEFINITE);
        
        gameLayer.getChildren().add(mascotView);
        
        // Đặt tọa độ mặc định ở GIỮA màn hình
        mascotView.setY(layer.getPrefHeight() - frameH - 50); 
        mascotView.setTranslateX(layer.getPrefWidth() / 2 - frameW / 2);
    }

    // --- HÀM NHẬN LỆNH TỪ BÀN PHÍM ---
    public void setMoveLeft(boolean b) { 
        moveLeft = b; 
        updateAnimationState(); 
    }
    public void setMoveRight(boolean b) { 
        moveRight = b; 
        updateAnimationState(); 
    }

    private void updateAnimationState() {
        if (moveLeft) mascotView.setScaleX(-1); // Quay mặt trái
        if (moveRight) mascotView.setScaleX(1); // Quay mặt phải
        
        if (moveLeft || moveRight) walkAnim.play();
        else walkAnim.pause(); // Nhả phím là đứng yên
    }

    // --- HÀM CẬP NHẬT TỌA ĐỘ (Được gọi liên tục trong Game Loop) ---
    public void updateMovement() {
        double currentX = mascotView.getTranslateX();
        // Không cho chạy tọt ra ngoài mép màn hình
        if (moveLeft && currentX > 0) {
            mascotView.setTranslateX(currentX - speed);
        }
        if (moveRight && currentX < gameLayer.getWidth() - 60) {
            mascotView.setTranslateX(currentX + speed);
        }
    }

    // --- KIỂM TRA VA CHẠM ---
    public boolean intersects(ImageView coin) {
        // Trả về true nếu hình vuông của Pet đè lên hình vuông của Coin
        return mascotView.getBoundsInParent().intersects(coin.getBoundsInParent());
    }

    // --- HIỆU ỨNG NHẢY LÊN KHI ĂN ĐIỂM ---
    public void jumpCelebrate() {
        TranslateTransition jump = new TranslateTransition(Duration.millis(150), mascotView);
        jump.setByY(-30);
        jump.setCycleCount(2);
        jump.setAutoReverse(true);
        jump.play();
    }
}