package com.client.controller.auth;

import com.client.util.*;


import javafx.animation.Animation;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.io.IOException;

public class ClientApp {

    @FXML
    private ImageView imgAnimatedCharacter;
    @FXML
    private ImageView Img1;
    @FXML
    private ImageView Img2;
    @FXML
    private ImageView Img3;
    @FXML
    private ImageView Img4;
    @FXML
    private ImageView Img5;


    @FXML
    public void initialize() {

        playCharacterAnimation(imgAnimatedCharacter, 64, 64, 4, 4);
        playCharacterAnimation(Img1, 64, 64, 4, 4);
        playCharacterAnimation(Img2, 64, 64, 4, 4);
        playCharacterAnimation(Img3, 64, 64, 4, 4);
        playCharacterAnimation(Img4, 64, 64, 4, 4);
        playCharacterAnimation(Img5, 64, 64, 4, 4);

    }


    private void playCharacterAnimation(ImageView imgView, int frameWidth, int frameHeight, int totalFrames, int columns) {

        // 1. Cắt cửa sổ vừa vặn với kích thước thật , index bat dau cat la 0, 0
        imgView.setViewport(new Rectangle2D(0, 0, frameWidth, frameHeight));

        // Đảm bảo ImageView không ép giãn ảnh lung tung
        imgView.setFitWidth(frameWidth);
        imgView.setFitHeight(frameHeight);

        // Pixel Art sắc nét
        imgView.setSmooth(false);

        // Chạy Animation
        final Animation animation = new SmallAnimation(
                imgView,
                Duration.millis(1000), // Thời gian chạy 1 vòng
                totalFrames, columns,
                0, 0,
                frameWidth, frameHeight
        );

        animation.setCycleCount(Animation.INDEFINITE);
        animation.play();
    }

    @FXML
    public void switchLogin(ActionEvent e) throws IOException {
        SceneController.switchScene(e, "Login.fxml");
    }

    @FXML
    public void switchRegister(ActionEvent e) throws IOException{
        SceneController.switchScene(e, "Register.fxml");
    }




}