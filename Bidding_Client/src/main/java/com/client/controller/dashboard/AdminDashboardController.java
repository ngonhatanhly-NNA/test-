package com.client.controller.dashboard;

import com.client.network.AdminNetwork;
import com.client.session.ClientSession;
import com.client.util.SceneController;
import com.shared.network.Response;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.scene.layout.GridPane;

/**
 * AdminDashboardController - Giao diện quản trị cho Admin
 */
public class AdminDashboardController {

    @FXML private Label lblTotalUsers;
    @FXML private Label lblTotalProducts;
    @FXML private Label lblTotalAuctions;
    @FXML private Label lblRevenue;

    @FXML private ListView<String> listViewActivities;

    @FXML private TableView<?> tableUsers;
    @FXML private TextField txtSearchUsername;

    @FXML private TextArea textAreaAnalytics;
    @FXML private TextArea textAreaRevenue;

    @FXML
    public void initialize() {
        // Tải dữ liệu dashboard khi khởi tạo
        loadDashboardData();
    }

    /**
     * Tải dữ liệu dashboard (thống kê)
     */
    @FXML
    private void loadDashboardData() {
        AdminNetwork.getDashboardData()
                .thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        // Xử lý dữ liệu từ response
                        System.out.println("Dashboard data loaded: " + response.getData());
                        // TODO: Parse data và cập nhật các label
                    } else {
                        showAlert("Lỗi", response.getMessage());
                    }
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> showAlert("Lỗi", "Không thể tải dữ liệu dashboard"));
                    return null;
                });
    }

    /**
     * Làm mới dữ liệu
     */
    @FXML
    private void refreshData(ActionEvent event) {
        loadDashboardData();
    }

    /**
     * Đăng xuất
     */
    @FXML
    private void logout(ActionEvent event) {
        ClientSession.clear();
        SceneController.switchScene(event, "Login.fxml");
    }

    /**
     * Hiển thị tất cả người dùng
     */
    @FXML
    private void showAllUsers(ActionEvent event) {
        AdminNetwork.getAllUsers()
                .thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        System.out.println("Users loaded: " + response.getData());
                        // TODO: Populate tableUsers với dữ liệu
                    }
                }));
    }

    /**
     * Tìm kiếm người dùng
     */
    @FXML
    private void showSearchUser(ActionEvent event) {
        showSearchUserDialog();
    }

    @FXML
    private void handleSearchUser(ActionEvent event) {
        String username = txtSearchUsername.getText().trim();
        if (username.isEmpty()) {
            showAlert("Cảnh báo", "Vui lòng nhập username");
            return;
        }

        AdminNetwork.searchUser(username)
                .thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        System.out.println("User found: " + response.getData());
                        // TODO: Hiển thị thông tin user
                    } else {
                        showAlert("Không Tìm Thấy", response.getMessage());
                    }
                }));
    }

    /**
     * Hiển thị dialog tìm kiếm
     */
    private void showSearchUserDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Tìm Kiếm Người Dùng");
        dialog.setHeaderText("Nhập username để tìm kiếm");

        TextField tfUsername = new TextField();
        tfUsername.setPromptText("Username");

        GridPane grid = new GridPane();
        grid.add(new Label("Username:"), 0, 0);
        grid.add(tfUsername, 1, 0);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return tfUsername.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(username -> {
            AdminNetwork.searchUser(username)
                    .thenAccept(response -> Platform.runLater(() -> {
                        if ("SUCCESS".equals(response.getStatus())) {
                            showAlert("Thành Công", "Người dùng: " + username + "\nDữ liệu: " + response.getData());
                        } else {
                            showAlert("Lỗi", response.getMessage());
                        }
                    }));
        });
    }

    /**
     * Hiển thị dialog phê duyệt seller
     */
    @FXML
    private void showApproveSellerDialog(ActionEvent event) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Phê Duyệt Seller");
        dialog.setHeaderText("Phê duyệt Bidder thành Seller");

        TextField tfUsername = new TextField();
        tfUsername.setPromptText("Username");
        TextField tfShopName = new TextField();
        tfShopName.setPromptText("Tên cửa hàng");
        TextField tfBankAccount = new TextField();
        tfBankAccount.setPromptText("Số tài khoản ngân hàng");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Username:"), 0, 0);
        grid.add(tfUsername, 1, 0);
        grid.add(new Label("Tên Cửa Hàng:"), 0, 1);
        grid.add(tfShopName, 1, 1);
        grid.add(new Label("Số Tài Khoản:"), 0, 2);
        grid.add(tfBankAccount, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return tfUsername.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(username -> {
            AdminNetwork.approveSeller(username, tfShopName.getText(), tfBankAccount.getText())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if ("SUCCESS".equals(response.getStatus())) {
                            showAlert("Thành Công", "Phê duyệt seller thành công!");
                        } else {
                            showAlert("Lỗi", response.getMessage());
                        }
                    }));
        });
    }

    /**
     * Hiển thị lịch hoạt động
     */
    @FXML
    private void showActivityLog(ActionEvent event) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Lịch Hoạt Động");
        dialog.setHeaderText("Xem lịch hoạt động người dùng");

        TextField tfUsername = new TextField();
        tfUsername.setPromptText("Username");

        GridPane grid = new GridPane();
        grid.add(new Label("Username:"), 0, 0);
        grid.add(tfUsername, 1, 0);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return tfUsername.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(username -> {
            AdminNetwork.getUserActivityLog(username)
                    .thenAccept(response -> Platform.runLater(() -> {
                        if ("SUCCESS".equals(response.getStatus())) {
                            showAlert("Lịch Hoạt Động", "Dữ liệu: " + response.getData());
                        } else {
                            showAlert("Lỗi", response.getMessage());
                        }
                    }));
        });
    }

    /**
     * Tải dữ liệu phân tích sản phẩm
     */
    @FXML
    private void loadProductAnalytics(ActionEvent event) {
        AdminNetwork.getProductAnalytics()
                .thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        textAreaAnalytics.setText(
                                "Phân Tích Sản Phẩm:\n\n" +
                                response.getData().toString()
                        );
                    } else {
                        textAreaAnalytics.setText("Lỗi: " + response.getMessage());
                    }
                }));
    }

    /**
     * Hiển thị dialog phân tích sản phẩm
     */
    @FXML
    private void showProductAnalytics(ActionEvent event) {
        loadProductAnalytics(event);
    }

    /**
     * Tải dữ liệu doanh thu
     */
    @FXML
    private void loadRevenueData(ActionEvent event) {
        AdminNetwork.getRevenueEstimate()
                .thenAccept(response -> Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        textAreaRevenue.setText(
                                "Ước Tính Doanh Thu:\n\n" +
                                response.getData().toString()
                        );
                    } else {
                        textAreaRevenue.setText("Lỗi: " + response.getMessage());
                    }
                }));
    }

    /**
     * Hiển thị dialog doanh thu
     */
    @FXML
    private void showRevenueEstimate(ActionEvent event) {
        loadRevenueData(event);
    }

    /**
     * Hiển thị dialog thông báo
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

