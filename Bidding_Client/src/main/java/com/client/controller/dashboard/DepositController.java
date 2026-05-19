package com.client.controller.dashboard;

import com.client.network.BidderNetwork;
import com.client.session.ClientSession;
import com.google.gson.Gson;
import com.shared.network.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * DepositController: Xử lý logic UI của màn hình nạp tiền
 * Tương tác với BidderNetwork để gọi API nạp tiền
 */
public class DepositController {
    private static final Logger logger = LoggerFactory.getLogger(DepositController.class);
    private static final Gson gson = new Gson();

    @FXML private Label lblCurrentBalance;
    @FXML private ComboBox<String> cmbPaymentMethod;
    @FXML private TextField txtDepositAmount;
    @FXML private Label lblAmountError;
    @FXML private TextField txtRecipient;
    @FXML private TextField txtAccountNumber;
    @FXML private Button btnDeposit;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label lblStatus;

    private BidderNetwork bidderNetwork;

    @FXML
    public void initialize() {
        logger.info("Khởi tạo DepositController");
        bidderNetwork = new BidderNetwork();

        // Khởi tạo ComboBox với các hình thức thanh toán
        cmbPaymentMethod.getItems().addAll("Thẻ Tín Dụng", "Chuyển Khoản Ngân Hàng", "Ví Điện Tử");
        cmbPaymentMethod.setValue("Thẻ Tín Dụng");

        // Set thông tin người nhận (tên cửa hàng)
        txtRecipient.setText("Team13 Auction System");
        txtAccountNumber.setText("0123456789");

        // Load số dư hiện tại
        loadCurrentBalance();

        // Xóa lỗi khi user nhập
        txtDepositAmount.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                lblAmountError.setText("");
            }
        });
    }

    /**
     * Tải số dư ví hiện tại từ server
     */
    private void loadCurrentBalance() {
        long bidderId = ClientSession.getUserId();

        bidderNetwork.getWalletBalance(bidderId).thenAccept(response -> {
            Platform.runLater(() -> {
                if ("SUCCESS".equals(response.getStatus())) {
                    // Extract wallet balance từ response
                    try {
                        String walletJson = gson.toJson(response.getData());
                        WalletInfo walletInfo = gson.fromJson(walletJson, WalletInfo.class);

                        // Format tiền tệ
                        double balance = walletInfo.getWalletBalance() != null
                            ? Double.parseDouble(walletInfo.getWalletBalance().toString())
                            : 0.0;

                        String balanceText = String.format("%,.0f VNĐ", balance);
                        lblCurrentBalance.setText(balanceText);
                        logger.info("Số dư ví: {}", balanceText);
                    } catch (Exception e) {
                        logger.error("Lỗi parse wallet balance: {}", e.getMessage());
                    }
                } else {
                    lblCurrentBalance.setText("Lỗi tải dữ liệu");
                    logger.error("Lỗi tải số dư: {}", response.getMessage());
                }
            });
        }).exceptionally(e -> {
            Platform.runLater(() -> {
                lblCurrentBalance.setText("Lỗi kết nối");
                logger.error("Lỗi kết nối server: {}", e.getMessage());
            });
            return null;
        });
    }

    /**
     * XỬ LÝ NẠP TIỀN - Phương thức chính
     */
    @FXML
    private void handleDeposit() {
        String amountText = txtDepositAmount.getText().trim();

        // Validate input
        if (amountText.isEmpty()) {
            lblAmountError.setText("❌ Vui lòng nhập số tiền");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                lblAmountError.setText("❌ Số tiền phải lớn hơn 0");
                return;
            }
            if (amount < 10000) {
                lblAmountError.setText("❌ Số tiền tối thiểu: 10.000 VNĐ");
                return;
            }

            // Hiển thị loading
            progressIndicator.setVisible(true);
            btnDeposit.setDisable(true);
            lblStatus.setText("⏳ Đang xử lý...");
            lblStatus.setStyle("-fx-text-fill: #F59E0B;");

            // Gọi API nạp tiền
            long bidderId = ClientSession.getUserId();
            BigDecimal depositAmount = BigDecimal.valueOf(amount);

            bidderNetwork.depositMoney(bidderId, depositAmount).thenAccept(response -> {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    btnDeposit.setDisable(false);

                    if ("SUCCESS".equals(response.getStatus())) {
                        // Nạp tiền thành công
                        lblStatus.setText("✅ " + response.getMessage());
                        lblStatus.setStyle("-fx-text-fill: #059669;");
                        logger.info("Nạp tiền thành công: {} VNĐ", amount);

                        // Reset form
                        txtDepositAmount.clear();
                        lblAmountError.setText("");

                        // Reload số dư
                        loadCurrentBalance();

                        // Đóng dialog sau 2 giây
                        new java.util.Timer().schedule(new java.util.TimerTask() {
                            @Override
                            public void run() {
                                Platform.runLater(this::closeDialog);
                            }

                            private void closeDialog() {
                                Stage stage = (Stage) btnDeposit.getScene().getWindow();
                                stage.close();
                            }
                        }, 2000);
                    } else {
                        // Lỗi nạp tiền
                        lblStatus.setText("❌ " + response.getMessage());
                        lblStatus.setStyle("-fx-text-fill: #DC2626;");
                        logger.warn("Nạp tiền thất bại: {}", response.getMessage());
                    }
                });
            }).exceptionally(e -> {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    btnDeposit.setDisable(false);
                    lblStatus.setText("❌ Lỗi kết nối: " + e.getMessage());
                    lblStatus.setStyle("-fx-text-fill: #DC2626;");
                    logger.error("Lỗi nạp tiền: {}", e.getMessage());
                });
                return null;
            });

        } catch (NumberFormatException e) {
            lblAmountError.setText("❌ Số tiền không hợp lệ");
            logger.warn("Input không phải số: {}", amountText);
        }
    }

    /**
     * Các nút nạp nhanh
     */
    @FXML
    private void handleQuickDeposit100k() {
        txtDepositAmount.setText("100000");
    }

    @FXML
    private void handleQuickDeposit500k() {
        txtDepositAmount.setText("500000");
    }

    @FXML
    private void handleQuickDeposit1m() {
        txtDepositAmount.setText("1000000");
    }

    @FXML
    private void handleQuickDeposit5m() {
        txtDepositAmount.setText("5000000");
    }

    /**
     * Lớp helper để map wallet balance từ JSON
     */
    public static class WalletInfo {
        private Object walletBalance;
        private String creditCardInfo;

        public Object getWalletBalance() { return walletBalance; }
        public void setWalletBalance(Object walletBalance) { this.walletBalance = walletBalance; }

        public String getCreditCardInfo() { return creditCardInfo; }
        public void setCreditCardInfo(String creditCardInfo) { this.creditCardInfo = creditCardInfo; }
    }
}

