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
import javafx.scene.input.KeyCode;
import javafx.scene.effect.Glow;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;

import com.client.util.SmallAnimation;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MiniGameManager {
    private AnchorPane gameLayer;
    private Label bidCounter;
    private BiddingPet pet;
	
	private AudioClip coinSound;
	private AudioClip specialSound;
	private AudioClip gameEndSound;
	private int combo = 0;
	private Label comboLabel;
	private Rectangle flashRect;
	private Timeline magicSpawner;
    
    private int totalBids = 0; 
    private boolean isPlaying = false;
    private int timeLeft = 30;
    private Label timerLabel;
    private List<ImageView> activeCoins = new ArrayList<>();
    private AnimationTimer collisionTimer;
    
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private AnimationTimer movementTimer;
    
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
		
		try {
			coinSound = new AudioClip(getClass().getResource("/sounds/coin.mp3").toString());
			specialSound = new AudioClip(getClass().getResource("/sounds/special.mp3").toString());
			gameEndSound = new AudioClip(getClass().getResource("/sounds/gameend.mp3").toString());
		} catch (Exception e) { 
			System.err.println("LỖI TẢI ÂM THANH:");
			e.printStackTrace(); 
		}

		comboLabel = new Label();
		comboLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
		comboLabel.setTextFill(Color.GOLD);
		comboLabel.setVisible(false);
		gameLayer.getChildren().add(comboLabel);
		
		flashRect = new Rectangle(gameLayer.getWidth(), gameLayer.getHeight());
		flashRect.setFill(Color.WHITE);
		flashRect.setOpacity(0);
		flashRect.setMouseTransparent(true);
		gameLayer.getChildren().add(flashRect);

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
		pet.setGameModeActive(true);
		pet.stopWalking(); 
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
				boolean isMoving = false;

				if (moveLeft && pet.getContainer().getLayoutX() > 0) {
					pet.getContainer().setLayoutX(pet.getContainer().getLayoutX() - 10);
					pet.walkLeft(); // Bật chạy animation quay sang trái
					isMoving = true;
				}
				if (moveRight && pet.getContainer().getLayoutX() < gameLayer.getWidth() - 100) {
					pet.getContainer().setLayoutX(pet.getContainer().getLayoutX() + 10);
					pet.walkRight(); // Bật chạy animation quay sang phải
					isMoving = true;
				}
				
				// Nếu không bấm phím nào, trả về đứng im nhìn xuống dưới
				if (!isMoving) {
					pet.stopWalking();
				}
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
		int type = Math.random() < 0.8 ? 1 : (Math.random() < 0.5 ? 2 : 3); // 1: thường, 2: vàng (x2), 3: tím (x5)
		String imagePath = type == 1 ? "/icons/coin.png" : (type == 2 ? "/icons/goldcoin.png" : "/icons/purplecoin.png");
		int value = type == 1 ? 1 : (type == 2 ? 2 : 5);
		
		try {
			ImageView coin = new ImageView(new Image(getClass().getResourceAsStream(imagePath)));
			coin.setFitWidth(45); coin.setFitHeight(45);
			coin.setLayoutX(startX); coin.setLayoutY(-50);
			coin.setUserData(value); // lưu giá trị
			gameLayer.getChildren().add(coin);
			activeCoins.add(coin);
			
			TranslateTransition fall = new TranslateTransition(Duration.seconds(3.5), coin);
			fall.setFromY(0); fall.setToY(gameLayer.getHeight() + 100);
			fall.setOnFinished(e -> { gameLayer.getChildren().remove(coin); activeCoins.remove(coin); });
			fall.play();
		} catch (Exception e) {e.printStackTrace();}
	}

    private void checkCollisions() {
    Iterator<ImageView> iter = activeCoins.iterator();
    while (iter.hasNext()) {
        ImageView coin = iter.next();
        if (pet.getContainer().getBoundsInParent().intersects(coin.getBoundsInParent())) {
            int value = (int) coin.getUserData();
            totalBids += value;
            combo++;
            if (bidCounter != null) bidCounter.setText("Bids: " + totalBids);
            
            if (combo >= 3) {
                comboLabel.setText("COMBO x" + combo + "!");
                comboLabel.setLayoutX(coin.getLayoutX());
                comboLabel.setLayoutY(coin.getLayoutY());
                comboLabel.setVisible(true);
                Timeline hideCombo = new Timeline(new KeyFrame(Duration.seconds(0.8), ev -> comboLabel.setVisible(false)));
                hideCombo.play();
            }
            
            if (value == 1 && coinSound != null) coinSound.play();
            else if (specialSound != null) specialSound.play();
            
            Glow glow = new Glow(0.8);
            coin.setEffect(glow);
            
            gameLayer.getChildren().remove(coin);
            iter.remove();
        } else {
            combo = 0; 
        }
    }
}

	private void end30sGame() {
		isPlaying = false;
		if (collisionTimer != null) collisionTimer.stop();
		if (movementTimer != null) movementTimer.stop();
		timerLabel.setVisible(false);
		if (gameEndSound != null) gameEndSound.play();
		
		// Rung màn hình
		TranslateTransition shake = new TranslateTransition(Duration.millis(50), flashRect);
		shake.setByX(10);
		shake.setCycleCount(6);
		shake.setAutoReverse(true);
		shake.play();
		flashRect.setOpacity(0.3);
		Timeline fadeFlash = new Timeline(new KeyFrame(Duration.seconds(0.5), ev -> flashRect.setOpacity(0)));
		fadeFlash.play();
		
		if (onGameEnd != null) onGameEnd.run();
		AnchorPane.setBottomAnchor(pet.getContainer(), 30.0);
		AnchorPane.setRightAnchor(pet.getContainer(), 30.0);
		
		// TẮT CHẾ ĐỘ GAME TRẢ PET VỀ MẶC ĐỊNH
		pet.setGameModeActive(false); 
		
		pet.jumpCelebrate();
		pet.speak("Time's up! You earned " + totalBids + " Bids! " + (totalBids > 50 ? "Amazing!" : "Try again!"));	
	}
	
    public void feedPet() {
        if (isPlaying) { pet.speak("Busy catching coins boss!"); return; }
        if (totalBids < 5) { pet.speak("I'm hungry! Please play game to earn 5 Bids!"); return; }
        totalBids -= 5;
        if (bidCounter != null) bidCounter.setText("Bids: " + totalBids);
        pet.speak("Yummy! Thank you boss!", true);
        pet.jumpCelebrate();
    }

    public void rollGacha() {
		if (isPlaying) {
			pet.speak("Wait for me to finish catching coins!");
			return;
		}
		if (totalBids < 10) {
			pet.speak("Boss is so poor! You need 10 Bids to unlock the Gacha!");
			return;
		}

		totalBids -= 10;
		if (bidCounter != null) bidCounter.setText("Bids: " + totalBids);

		String randomItemPath = GACHA_POOL[(int) (Math.random() * GACHA_POOL.length)];
		boolean isFail = randomItemPath.toLowerCase().contains("fail");

		Platform.runLater(() -> {
			try {
				ImageView rewardView = new ImageView(new Image(getClass().getResourceAsStream(randomItemPath)));
				rewardView.setFitWidth(200);
				rewardView.setFitHeight(200);
				rewardView.setLayoutX((gameLayer.getWidth() - 200) / 2);
				rewardView.setLayoutY((gameLayer.getHeight() - 200) / 2);
				rewardView.setScaleX(0); // Ban đầu ẩn
				rewardView.setScaleY(0);
				gameLayer.getChildren().add(rewardView);

				ScaleTransition scaleUp = new ScaleTransition(Duration.seconds(0.6), rewardView);
				scaleUp.setToX(2.0);
				scaleUp.setToY(2.0);
				scaleUp.setInterpolator(Interpolator.EASE_BOTH);

				if (isFail) {
					pet.speak("Oh no... Better luck next time, boss :(", false);
					pet.changeState("/images/Lucius_sentado.png", 4, 4, 0, 0, 64, 96, 0);
					scaleUp.play(); // Chỉ chạy animation phóng to cho item fail
				} else {
					DropShadow glow = new DropShadow();
					glow.setColor(Color.GOLD);
					glow.setRadius(50);
					glow.setSpread(0.8);
					rewardView.setEffect(glow);

					RotateTransition wobble = new RotateTransition(Duration.millis(70), rewardView);
					wobble.setByAngle(12);
					wobble.setCycleCount(8);
					wobble.setAutoReverse(true);

					pet.speak("Wow! Great reward, boss!", true);
					pet.jumpCelebrate();
					pet.changeState("/images/LuciusSpin.png", 8, 4, 0, 0, 96, 96, Animation.INDEFINITE);

					// Animation cho đồ xịn
					PauseTransition delay = new PauseTransition(Duration.seconds(2.5));
					ParallelTransition fadeAndShrink = new ParallelTransition();
					FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.6), rewardView);
					fadeOut.setToValue(0);
					ScaleTransition shrink = new ScaleTransition(Duration.seconds(0.6), rewardView);
					shrink.setToX(0);
					shrink.setToY(0);
					fadeAndShrink.getChildren().addAll(fadeOut, shrink);

					SequentialTransition seq = new SequentialTransition(scaleUp, wobble, delay, fadeAndShrink);
					seq.setOnFinished(e -> {
						gameLayer.getChildren().remove(rewardView);
						pet.setToDefaultState(); // Trả pet về bình thường sau khi xong
					});
					seq.play();
				}
			} catch (Exception e) {
				System.err.println("Lỗi load Gacha: " + e.getMessage());
			}
		});
	}
}