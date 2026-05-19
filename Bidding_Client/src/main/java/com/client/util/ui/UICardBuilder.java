package com.client.util.ui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.net.URL;

public class UICardBuilder {
    private VBox card;
    private Class<?> resourceContext;

    public UICardBuilder(Class<?> resourceContext) {
        this.resourceContext = resourceContext;
        this.card = new VBox(10);
        this.card.setPadding(new Insets(15));
        this.card.setStyle("-fx-background-color: #242424; -fx-background-radius: 12; " +
                           "-fx-border-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 4);");
    }

    public UICardBuilder setCustomStyle(String css) {
        this.card.setStyle(this.card.getStyle() + " " + css);
        return this;
    }

    public UICardBuilder setImage(String imageUrl) {
        ImageView imageView = new ImageView();
        imageView.setFitHeight(140);
        imageView.setFitWidth(210);
        imageView.setPreserveRatio(true);

        Image cardImg = null;
        if (imageUrl != null && !imageUrl.isBlank()) {
            java.io.File f = new java.io.File(imageUrl);
            if (f.exists()) {
                cardImg = new Image(f.toURI().toString(), true);
            } else {
                URL res = resourceContext.getResource(imageUrl.startsWith("/") ? imageUrl : "/images/" + imageUrl);
                if (res != null) cardImg = new Image(res.toExternalForm(), true);
            }
        }
        if (cardImg == null) {
             try {
                 cardImg = new Image(resourceContext.getResource("/images/placeholder.png").toExternalForm());
             } catch (Exception e) {}
        }
        if (cardImg != null) imageView.setImage(cardImg);
        card.getChildren().add(imageView);
        return this;
    }

    public UICardBuilder setTitle(String title, String colorHex, int fontSize) {
        Label lbl = new Label(title);
        lbl.setFont(Font.font("System", FontWeight.BOLD, fontSize));
        lbl.setStyle("-fx-text-fill: " + colorHex + ";");
        lbl.setWrapText(true);
        card.getChildren().add(lbl);
        return this;
    }

    public UICardBuilder addInfoText(String text, String colorHex, boolean isBold) {
        Label lbl = new Label(text);
        String weight = isBold ? "-fx-font-weight: bold; " : "";
        lbl.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-size: 12px; " + weight);
        card.getChildren().add(lbl);
        return this;
    }

    public UICardBuilder addBadge(String text, String bgColor, String textColor, String borderColor) {
        Label badge = new Label(text);
        badge.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + 
                       "; -fx-border-color: " + borderColor + "; -fx-border-radius: 6; " +
                       "-fx-background-radius: 6; -fx-padding: 3 10; -fx-font-weight: bold; -fx-font-size: 11px;");
        card.getChildren().add(badge);
        return this;
    }

    public UICardBuilder setActionButton(String text, String gradientStyle, EventHandler<ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setStyle(gradientStyle + " -fx-text-fill: #121212; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(handler);
        card.getChildren().add(btn);
        return this;
    }
    
    public UICardBuilder setOnClick(Runnable action) {
        this.card.setOnMouseClicked(e -> action.run());
        this.card.setStyle(this.card.getStyle() + " -fx-cursor: hand;");
        return this;
    }

    public UICardBuilder setUserData(Object data) {
        this.card.setUserData(data);
        return this;
    }

    public VBox build() {
        return card;
    }
}