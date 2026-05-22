package com.client.controller.dashboard;

import com.client.network.AuctionNetwork;
import com.client.session.ClientSession;
import com.client.util.ToastUtil;
import com.google.gson.Gson;
import com.shared.dto.CreateAuctionDTO;
import com.shared.dto.AuctionDetailDTO;
import com.shared.network.Response;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.CompletableFuture;

public class CreateAuctionController {

    private static final Logger logger = LoggerFactory.getLogger(CreateAuctionController.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final Gson gson = new Gson();

    @FXML private DatePicker dpStartDate;
    @FXML private TextField txtStartTime;
    @FXML private DatePicker dpEndDate;
    @FXML private TextField txtEndTime;
    @FXML private TextField txtStepPrice;

    private long currentItemId;

    // [THÊM VÀO] Hàm initialize chạy ngay khi FXML được load
    @FXML
    public void initialize() {
        // Set dòng chữ mờ mờ hướng dẫn người dùng nhập đúng định dạng
        txtStartTime.setPromptText("HH:mm");
        txtEndTime.setPromptText("HH:mm");
    }

    public void initData(long itemId) {
        this.currentItemId = itemId;
        logger.info("Mở Popup tạo Auction cho Item ID: {}", this.currentItemId);
    }

    @FXML
    void handleCancel(ActionEvent event) {
        Stage stage = (Stage) dpStartDate.getScene().getWindow();
        stage.close();
    }

    @FXML
    void handleStartAuction(ActionEvent event) {
        try {
            // 1. Gộp Date và Time thành ISO Format
            LocalDateTime startDateTime = combineDateTime(dpStartDate.getValue(), txtStartTime.getText());
            LocalDateTime endDateTime = combineDateTime(dpEndDate.getValue(), txtEndTime.getText());

            if (endDateTime.isBefore(startDateTime)) {
                // LOG CHO DEV (Tiếng Việt)
                logger.error("Thời gian kết thúc không thể trước thời gian bắt đầu!");
                // UI CHO USER (Tiếng Anh)
                ToastUtil.showError("End time cannot be before start time!");
                return;
            }

            String startIso = startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String endIso = endDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // 2. Lấy Step Price
            String priceStr = txtStepPrice.getText().trim();
            if (priceStr.isEmpty()) throw new NumberFormatException("Step price is empty");
            BigDecimal stepPrice = new BigDecimal(priceStr);

            // 3. Lấy Seller ID cực kỳ an toàn từ ClientSession
            long sellerId = ClientSession.getUserId();

            // 4. Tạo cái hộp DTO
            CreateAuctionDTO dto = new CreateAuctionDTO(
                    this.currentItemId,
                    sellerId,
                    startIso,
                    endIso,
                    stepPrice
            );

            logger.info("Chuẩn bị gửi dữ liệu Auction. DTO data: Start={}, End={}, Step={}", startIso, endIso, stepPrice);

            // 5. Bắn API qua Network (Chạy ngầm để không đơ màn hình)
            CompletableFuture.runAsync(() -> {
                try {
                    // Nhận trực tiếp đối tượng Response từ Network
                    Response response = AuctionNetwork.createAuction(dto);

                    // Quay lại luồng UI chính để cập nhật giao diện
                    Platform.runLater(() -> {
                        if ("SUCCESS".equals(response.getStatus())) {
                            AuctionDetailDTO createdAuction = parseCreatedAuction(response);
                            if (createdAuction != null) {
                                ViewLiveAuctions liveView = ViewLiveAuctions.getExistingInstance();
                                if (liveView != null) {
                                    liveView.addOrUpdateAuctionRealtime(createdAuction);
                                }
                                SellerDashboardController sellerView = SellerDashboardController.getExistingInstance();
                                if (sellerView != null) {
                                    sellerView.addOrUpdateAuctionRealtime(createdAuction);
                                }
                            }

                            // LOG CHO DEV (Tiếng Việt)
                            logger.info("Đưa sản phẩm lên Sàn Đấu Giá THÀNH CÔNG! Đóng popup.");
                            // UI CHO USER (Tiếng Anh)
                            ToastUtil.showSuccess("Auction created successfully!");
                            handleCancel(null);
                        } else {
                            // Lấy thông báo lỗi từ Server (nếu có)
                            logger.error("Lỗi Server khi tạo Auction: {}", response.getMessage());
                            // UI CHO USER (Lấy thông điệp từ server nếu có, không thì hiện mặc định)
                            ToastUtil.showError(response.getMessage() != null ? response.getMessage() : "Failed to create auction!");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        logger.error("Lỗi kết nối mạng: {}", e.getMessage());
                        ToastUtil.showError("Connection failed! Please try again.");
                    });
                }
            });

            // ================= CÁC KHỐI CATCH BẮT LỖI GIAO DIỆN =================
        } catch (DateTimeParseException | NullPointerException e) {
            // LOG CHO DEV
            logger.error("Lỗi định dạng ngày/giờ. Vui lòng nhập giờ dạng HH:mm (VD: 14:30)");
            // UI CHO USER
            ToastUtil.showError("Invalid date or time! Please use HH:mm format.");

        } catch (NumberFormatException e) {
            // LOG CHO DEV
            logger.error("Lỗi định dạng Step Price: Yêu cầu nhập số.");
            // UI CHO USER
            ToastUtil.showError("Invalid step price! Must be a valid number.");
        }
    }

    private LocalDateTime combineDateTime(LocalDate date, String timeStr) {
        if (date == null) throw new NullPointerException("Chưa chọn ngày");
        LocalTime time = LocalTime.parse(timeStr.trim(), TIME_FORMATTER);
        return LocalDateTime.of(date, time);
    }

    private AuctionDetailDTO parseCreatedAuction(Response response) {
        if (response == null || response.getData() == null) {
            return null;
        }
        try {
            return gson.fromJson(gson.toJson(response.getData()), AuctionDetailDTO.class);
        } catch (Exception e) {
            logger.warn("Không parse được auction vừa tạo: {}", e.getMessage());
            return null;
        }
    }
}