package com.client.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class SwitchPane {
    private BorderPane currentPane;
    public SwitchPane (BorderPane currentPane){
        this.currentPane = currentPane;
    }
    public void loadView(String fxmlFileName) {
        try {
            Node view = FXMLLoader.load(getClass().getResource("/fxml/" + fxmlFileName));
            this.currentPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Lỗi: Không tìm thấy file " + fxmlFileName);
        }
    }
}
