package com.client.util.ui.state;

import javafx.scene.control.Button;

public class NotStartedAuctionState implements AuctionUIState {
    @Override
    public void applyStyle(Button button) {
        button.setDisable(false);
        button.setText("Open Auction");
        button.setStyle("-fx-background-color: linear-gradient(to right, #D4AF37, #FFD700); -fx-text-fill: #121212; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
    }
}