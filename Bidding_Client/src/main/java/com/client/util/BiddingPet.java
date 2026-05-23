package com.client.util;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    
    private PauseTransition speechTimer;

    public BiddingPet (Pane layer) {
        this.gameLayer = layer;
        
        // 1. TẠO HÌNH ẢNH PIKACHU
        Image spriteSheet = new Image(getClass().getResourceAsStream("/images/pikachu.png"));
        this.mascotView = new ImageView(spriteSheet);
        int cols = 4; int rows = 4;
        double frameW = spriteSheet.getWidth() / cols;
        double frameH = spriteSheet.getHeight() / rows;
        mascotView.setViewport(new Rectangle2D(0, 0, frameW, frameH));
        
        speechBubble = new Label();
        speechBubble.setFont(Font.font("System", FontWeight.BOLD, 14));
        speechBubble.setStyle(
            "-fx-background-color: #242424; -fx-text-fill: #D4AF37;" +
            "-fx-padding: 10px 15px; -fx-background-radius: 15px;" +
            "-fx-border-color: #D4AF37; -fx-border-radius: 15px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 5);"
        );
        speechBubble.setVisible(false);
        
        // 3. ĐƯA PIKACHU VÀ BONG BÓNG VÀO VBOX (Cách này giúp chúng luôn dính lấy nhau)
        petContainer = new VBox(10); // Khoảng cách 10px giữa bong bóng và Pikachu
        petContainer.setAlignment(Pos.BOTTOM_CENTER); // Căn giữa
        petContainer.getChildren().addAll(speechBubble, mascotView);

        // 4. GHIM VBOX VÀO GÓC DƯỚI BÊN PHẢI
        if (layer instanceof javafx.scene.layout.AnchorPane) {
			javafx.scene.layout.AnchorPane.setBottomAnchor(petContainer, 30.0); // Cách lề dưới 30px
			javafx.scene.layout.AnchorPane.setRightAnchor(petContainer, 30.0);  // Cách lề phải 30px
		}

		gameLayer.getChildren().add(petContainer);
        // 5. EVENT CLICK
        mascotView.setOnMouseClicked(e -> {
            jumpCelebrate();
            speak("Hi! Is there any item you want to get?");
        });
        
        mascotView.setStyle("-fx-cursor: hand;");
    }

    // --- HÀM TRỢ LÝ ẢO NÓI CHUYỆN ---
	public void speak(String text) {
        speechBubble.setText(text);
        speechBubble.setVisible(true);
 
        if (speechTimer != null) {
            speechTimer.stop();
        }
 
        speechTimer = new PauseTransition(Duration.seconds(7));
        speechTimer.setOnFinished(e -> speechBubble.setVisible(false));
        speechTimer.play();
    }


    // --- HIỆU ỨNG NHẢY LÊN ---
    public void jumpCelebrate() {
        TranslateTransition jump = new TranslateTransition(Duration.millis(200), petContainer);
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