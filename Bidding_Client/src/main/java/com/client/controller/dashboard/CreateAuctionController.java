package com.client.controller.dashboard;

import com.client.network.AuctionNetwork;
import com.client.session.ClientSession;
import com.shared.dto.CreateAuctionDTO;
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

    @FXML private DatePicker dpStartDate;
    @FXML private TextField txtStartTime;
    @FXML private DatePicker dpEndDate;
    @FXML private TextField txtEndTime;
    @FXML private TextField txtStepPrice;

    private long currentItemId;

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
                logger.error("Thời gian kết thúc không thể trước thời gian bắt đầu!");
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
                            logger.info("Đưa sản phẩm lên Sàn Đấu Giá THÀNH CÔNG! Đóng popup.");
                            handleCancel(null);
                        } else {
                            // Lấy thông báo lỗi từ Server (nếu có)
                            logger.error("Lỗi Server khi tạo Auction: {}", response.getMessage());
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> logger.error("Lỗi kết nối mạng: {}", e.getMessage()));
                }
            });

        } catch (DateTimeParseException | NullPointerException e) {
            logger.error("Lỗi định dạng ngày/giờ. Vui lòng nhập giờ dạng HH:mm (VD: 14:30)");
        } catch (NumberFormatException e) {
            logger.error("Lỗi định dạng Step Price: Yêu cầu nhập số.");
        }
    }

    private LocalDateTime combineDateTime(LocalDate date, String timeStr) {
        if (date == null) throw new NullPointerException("Chưa chọn ngày");
        LocalTime time = LocalTime.parse(timeStr.trim());
        return LocalDateTime.of(date, time);
    }
}