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

    public BiddingPet (Pane layer) {
        this.gameLayer = layer;
        
        this.mascotView = new ImageView();
        mascotView.setFitWidth(100);
        mascotView.setFitHeight(100);
        mascotView.setPreserveRatio(true);
        
        mascotView.setFitWidth(100);
        mascotView.setFitHeight(100);
        mascotView.setPreserveRatio(true); // 
        
        speechBubble = new Label();
        speechBubble.setFont(Font.font("System", FontWeight.BOLD, 14));
        speechBubble.setStyle("-fx-background-color: #242424; -fx-text-fill: #D4AF37; -fx-padding: 10px 15px; -fx-background-radius: 15px; -fx-border-color: #D4AF37; -fx-border-radius: 15px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 5);");
        speechBubble.setVisible(false); // Mặc định ẩn
        
        petContainer = new VBox(10); 
        petContainer.setAlignment(Pos.BOTTOM_CENTER); 
        petContainer.getChildren().addAll(speechBubble, mascotView);


        if (layer instanceof AnchorPane) {
            AnchorPane.setBottomAnchor(petContainer, 30.0);
            AnchorPane.setRightAnchor(petContainer, 30.0);
        }

        mascotView.setOnMouseClicked(e -> {
            jumpCelebrate();
            speak("Hi! I'm hungry, feed me?");
			changeState("/images/LuciusSpin.png", 8, 4, 64, 64);
        });
		
		idleTimer = new PauseTransition(Duration.seconds(30));
        idleTimer.setOnFinished(e -> {
            // Khi hết 30s không ai tương tác, chuyển sang animation đọc sách
            changeState("/images/Luciuscolumnpio.png", 16, 4, 64, 64);
            // Có thể cho 1 câu thoại nhỏ kiểu lẩm bẩm
            speechBubble.setText("Zzz... Boss is not interesting in me anymore :<...");
            speechBubble.setVisible(true);
        });
        
        mascotView.setStyle("-fx-cursor: hand;");
        gameLayer.getChildren().add(petContainer);
		changeState("/images/Lucius_sentado.png", 4, 4, 64, 64);
    }
	
	public void changeState(String spriteSheetPath, int frames, int cols, int width, int height) {
			if (currentAnimation != null) {
				currentAnimation.stop();
			}
			
			try {
				Image newSprite = new Image(getClass().getResourceAsStream(spriteSheetPath));
				mascotView.setImage(newSprite);
			   
				currentAnimation = new SmallAnimation(
					mascotView, 
					Duration.millis(800), // Tốc độ animation
					frames, cols, 0, 0, width, height
				);
				currentAnimation.setCycleCount(Animation.INDEFINITE);
				currentAnimation.play();
			} catch (Exception e) {
				System.err.println("Không tìm thấy ảnh: " + spriteSheetPath);
			}
		}
    // --- HÀM TRỢ LÝ ẢO NÓI CHUYỆN ---
    public void speak(String text) {
		resetIdleTimer();
		if (speakTimeline != null) speakTimeline.stop();
		
        speechBubble.setText(text);
        speechBubble.setVisible(true);

        speakTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            speechBubble.setVisible(false);
			changeState("/images/Lucius_libro.png", 4, 4, 64, 64);
        }));
        speakTimeline.play();
    }
	

	
	public void speak(String text, boolean isRandom) {
		if (isRandom && text.equals("Yummy! Thank you boss!")) {
			text = happyPhrases[rand.nextInt(happyPhrases.length)];
		}
		speak(text);
	}

    // --- HIỆU ỨNG NHẢY LÊN KHI CLICK ---
    public void jumpCelebrate() {
        TranslateTransition jump = new TranslateTransition(Duration.millis(150), mascotView);
        jump.setByY(-30);
        jump.setCycleCount(2);
        jump.setAutoReverse(true);
        jump.play();
    }

    // --- GETTER CHO MINI GAME MANAGER ---
    public VBox getContainer() {
        return petContainer;
    }
}