package com.client.controller.dashboard;

import com.client.network.AdminNetwork;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import java.util.Map;

public class StatusCell extends TableCell<Map<String, String>, String> {

    private final Button statusButton = new Button();
    private final AdminDashboardController controller;

    public StatusCell(AdminDashboardController controller) {
        this.controller = controller;
        // Set a fixed width for the button to make the column uniform
        statusButton.setPrefWidth(80);
    }

    @Override
    protected void updateItem(String status, boolean empty) {
        super.updateItem(status, empty);

        if (empty || getTableRow() == null || getTableRow().getItem() == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        Map<String, String> rowData = getTableRow().getItem();
        String role = rowData.get("role");
        String username = rowData.get("username");

        // Admins cannot be banned, so just display their status as text.
        if ("ADMIN".equalsIgnoreCase(role)) {
            setText(status);
            setGraphic(null);
            return;
        }

        // For other roles, show a clickable button representing the CURRENT status
        if (status != null && username != null) {
            statusButton.setText(status.toUpperCase());

            if (status.equalsIgnoreCase("ACTIVE")) {
                // Green button for ACTIVE status
                statusButton.setStyle("-fx-background-color: #22C55E; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                // When clicked, perform the BAN action
                statusButton.setOnAction(event -> handleBan(username));
            } else if (status.equalsIgnoreCase("BANNED")) {
                // Red button for BANNED status
                statusButton.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                // When clicked, perform the UNBAN action
                statusButton.setOnAction(event -> handleUnban(username));
            } else {
                // For any other status, just show the text
                setText(status);
                setGraphic(null);
                return;
            }
            setGraphic(statusButton);
            setText(null);
        } else {
            setText(null);
            setGraphic(null);
        }
    }

    private void handleBan(String username) {
        AdminNetwork.banUser(username, "Banned by admin")
                .thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        showAlert("Thành Công", "Đã khóa tài khoản: " + username);
                        controller.loadAllUsers(); // Reload the table data
                    } else {
                        showAlert("Lỗi", "Không thể khóa tài khoản: " + response.getMessage());
                    }
                }));
    }

    private void handleUnban(String username) {
        AdminNetwork.unbanUser(username)
                .thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        showAlert("Thành Công", "Đã mở khóa tài khoản: " + username);
                        controller.loadAllUsers(); // Reload the table data
                    } else {
                        showAlert("Lỗi", "Không thể mở khóa tài khoản: " + response.getMessage());
                    }
                }));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
