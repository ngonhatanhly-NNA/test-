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
	
	private String[] happyPhrases = {"Great!", "Awesome!", "You're the best!", "So cool!"};
	private String[] hungryPhrases = {"Feed me, boss!", "I'm starving!", "Bids please~"};
	private Random rand = new Random();

    public BiddingPet (Pane layer) {
        this.gameLayer = layer;
        
        Image iconImage = new Image(getClass().getResourceAsStream("/icons/icon.png"));
        this.mascotView = new ImageView(iconImage);
        
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
        });
        
        mascotView.setStyle("-fx-cursor: hand;");
        gameLayer.getChildren().add(petContainer);
    }

    // --- HÀM TRỢ LÝ ẢO NÓI CHUYỆN ---
    public void speak(String text) {
		if (speakTimeline != null) speakTimeline.stop();
		
        speechBubble.setText(text);
        speechBubble.setVisible(true);

        speakTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            speechBubble.setVisible(false);
        }));
        speakTimeline.play();
    }
	
	public void speak(String text, boolean isRandom) {
		if (isRandom && text.equals("Yummy! Thank you boss!")) {
			text = happyPhrases[rand.nextInt(happyPhrases.length)];
		}
		if (speakTimeline != null) speakTimeline.stop();
		speechBubble.setText(text);
		speechBubble.setVisible(true);
		speakTimeline = new Timeline(new KeyFrame(Duration.seconds(4), e -> speechBubble.setVisible(false)));
		speakTimeline.play();
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