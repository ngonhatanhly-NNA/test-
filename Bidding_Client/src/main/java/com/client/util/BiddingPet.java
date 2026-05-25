package com.client.util;
import java.util.Random;
import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class BiddingPet {
    private ImageView mascotView;
    private Pane gameLayer;
    private Label speechBubble;
    private VBox petContainer; 
    private Timeline speakTimeline;
    private SmallAnimation currentAnimation;
    private PauseTransition idleTimer;
    
    private String[] happyPhrases = {"Great!", "Awesome!", "You're the best!", "So cool!"};
    private String[] hungryPhrases = {"Feed me, boss!", "I'm starving!", "Bids please~"};
    private Random rand = new Random();

    private String currentMovementState = "none";

    public BiddingPet (Pane layer) {
        this.gameLayer = layer;
        
        this.mascotView = new ImageView();
        mascotView.setFitWidth(100);
        mascotView.setFitHeight(100);
        mascotView.setPreserveRatio(true);
        
        speechBubble = new Label();
        speechBubble.setFont(Font.font("System", FontWeight.BOLD, 14));
        speechBubble.setStyle("-fx-background-color: #242424; -fx-text-fill: #D4AF37; -fx-padding: 10px 15px; -fx-background-radius: 15px; -fx-border-color: #D4AF37; -fx-border-radius: 15px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 5);");
        speechBubble.setVisible(false);
        
        petContainer = new VBox(10); 
        petContainer.setAlignment(Pos.BOTTOM_CENTER); 
        petContainer.getChildren().addAll(speechBubble, mascotView);

        if (layer instanceof AnchorPane) {
            AnchorPane.setBottomAnchor(petContainer, 30.0);
            AnchorPane.setRightAnchor(petContainer, 30.0);
        }

        mascotView.setOnMouseClicked(e -> {
            if ("game".equals(currentMovementState)) return; 
            resetIdleTimer();
            jumpCelebrate();
            speak("Hi! I'm hungry, feed me?");
            changeState("/images/LuciusSpin.png", 8, 4, 0, 0, 96, 96, 2);
            if (currentAnimation != null) {
                currentAnimation.setOnFinished(ev -> setToDefaultState());
            }
        });
        
        idleTimer = new PauseTransition(Duration.seconds(30));
        idleTimer.setOnFinished(e -> {
            if (currentMovementState.startsWith("walk") || "game".equals(currentMovementState)) return;
            changeState("/images/Luciuscolumpio.png", 4, 4, 0, 0, 96, 96, Animation.INDEFINITE);
            speechBubble.setText("Zzz... Boss is not interesting in me anymore :<...");
            speechBubble.setVisible(true);
			
        });
        
        mascotView.setStyle("-fx-cursor: hand;");
        gameLayer.getChildren().add(petContainer);
        setToDefaultState();
    }
    
    public void setToDefaultState() {
        currentMovementState = "idle";
        changeState("/images/Lucius_sentado.png", 4, 4, 0, 0, 64, 96, Animation.INDEFINITE);
        resetIdleTimer();
    }

    // --- CÁC HÀM ĐỔI HIỆU ỨNG DI CHUYỂN DÙNG CHO MINIGAME ---
    public void walkLeft() {
        if ("walk_left".equals(currentMovementState)) return; // 
        currentMovementState = "walk_left";
        // File Lucius.png, chạy 4 frame hàng 2 (offsetY = 96), kích thước frame 128x96
        changeState("/images/Lucius.png", 4, 4, 0, 96, 128, 96, Animation.INDEFINITE);
    }

    public void walkRight() {
        if ("walk_right".equals(currentMovementState)) return;
        currentMovementState = "walk_right";
        // File Lucius.png, chạy 4 frame hàng 3 (offsetY = 192), kích thước frame 128x96
        changeState("/images/Lucius.png", 4, 4, 0, 192, 128, 96, Animation.INDEFINITE);
    }

    public void stopWalking() {
        if ("game_idle".equals(currentMovementState)) return;
        currentMovementState = "game_idle";
        changeState("/images/Lucius.png", 4, 4, 0, 0, 128, 96, Animation.INDEFINITE);
    }

    public void setGameModeActive(boolean active) {
        if (active) {
            currentMovementState = "game_idle";
            if (idleTimer != null) idleTimer.stop();
        } else {
            setToDefaultState();
        }
    }
    
    public void resetIdleTimer() {
        if (idleTimer != null && !"game".equals(currentMovementState)) {
            idleTimer.playFromStart(); 
        }
    }
    
    public void changeState(String spriteSheetPath, int frames, int cols, int offsetX, int offsetY, int width, int height, int cycleCount) {
        if (currentAnimation != null) {
            currentAnimation.stop();
        }
        
        try {
            Image newSprite = new Image(getClass().getResourceAsStream(spriteSheetPath));
            mascotView.setImage(newSprite);
           
            currentAnimation = new SmallAnimation(
                mascotView, 
                Duration.millis(600), // Tốc độ bước đi, càng nhỏ chân bước càng nhanh
                frames, cols, offsetX, offsetY, width, height
            );
            currentAnimation.setCycleCount(cycleCount); 
            currentAnimation.play();
        } catch (Exception e) {
            System.err.println("Không tìm thấy ảnh: " + spriteSheetPath);
        }
    }

    public void speak(String text) {
        resetIdleTimer(); 
        if (speakTimeline != null) speakTimeline.stop();
        
        speechBubble.setText(text);
        speechBubble.setVisible(true);

        speakTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            speechBubble.setVisible(false);
            if (currentAnimation != null && currentAnimation.getStatus() == Animation.Status.STOPPED) {
                setToDefaultState();
            }
        }));
        speakTimeline.play();
    }
    
    public void speak(String text, boolean isRandom) {
        if (isRandom && text.equals("Yummy! Thank you boss!")) {
            text = happyPhrases[rand.nextInt(happyPhrases.length)];
        }
        speak(text);
    }

    public void jumpCelebrate() {
        TranslateTransition jump = new TranslateTransition(Duration.millis(150), mascotView);
        jump.setByY(-30);
        jump.setCycleCount(2);
        jump.setAutoReverse(true);
        jump.play();
    }

    public VBox getContainer() { return petContainer; }
}