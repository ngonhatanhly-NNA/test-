package com.client;

import com.client.network.MyWebSocketClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Trỏ đường dẫn tới file .fxml trong thư mục resources
        // get class hiện tại để get resource , nối
        URL url = getClass().getResource("/fxml/AuctionMenu.fxml");
        if (url == null) {
            System.out.println("Không tìm thấy file fxml. Check lại đường dẫn ");
            return;
        }

        Parent root = FXMLLoader.load(url);
        primaryStage.setTitle("App Đấu Giá Trực Tuyến");
        primaryStage.setScene(new Scene(root, 1200, 720)); // Kích thước cửa sổ
        primaryStage.setResizable(false); // nào sửa được đống frame thì thả :">
        primaryStage.show();
    }

    public static void main(String[] args) {
        MyWebSocketClient.connectToServer();
        launch(args);
    }
}