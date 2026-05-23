package com.client.util;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneController {

    /**
     * Hàm chuyển cảnh cơ bản nhất (Không có animation)
     * @param event Sự kiện click chuột
     * @param fxmlFile Tên file FXML muốn chuyển tới (VD: "Dashboard.fxml")
     */
    public static void switchScene(ActionEvent event, String fxmlFile) {
        try {
			FXMLLoader loader = new FXMLLoader(SceneController.class.getResource("/fxml/" + fxmlFile));
			Parent root = loader.load();
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			Scene scene = new Scene(root);
			stage.setScene(scene);

			// --- TỰ ĐỘNG KHÔI PHỤC SIZE CŨ Logout ---
			if ("Login.fxml".equals(fxmlFile) || "AuctionMenu.fxml".equals(fxmlFile)) {
				stage.setMaximized(false);
				// stage.setFullScreen(false); 
				stage.setResizable(false);
				stage.setWidth(1280);
				stage.setHeight(750);
				stage.centerOnScreen();
			}

			stage.show();

		} catch (IOException e) {
			System.err.println("LỖI CHUYỂN CẢNH: Không thể tải được file " + fxmlFile);
			e.printStackTrace();
		} catch (NullPointerException e) {
			System.err.println("LỖI ĐƯỜNG DẪN: File /fxml/" + fxmlFile + " không tồn tại!");
			e.printStackTrace();
		}
    }
}