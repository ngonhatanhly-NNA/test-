package com.client.util.ui.state;

import javafx.scene.control.Button;

public class FinishedAuctionState implements AuctionUIState {
    @Override
    public void applyStyle(Button button) {
        button.setDisable(true);
        button.setText("Đã kết thúc");
        button.setStyle("-fx-background-color: #333333; -fx-text-fill: #888888; -fx-font-weight: bold; -fx-background-radius: 8;");
    }
}