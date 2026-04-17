package com.client.controller.dashboard;

import com.client.network.ItemNetwork;
import com.shared.dto.CreateItemRequestDTO;
import com.shared.network.Response;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.HashMap;

public class CreateItemController {

    @FXML private TextField txtItemName;
    @FXML private ComboBox<String> cbCategory; // Lưu ý: Trong UI đặt là cbCategory, nhưng data map sang 'type'
    @FXML private TextField txtStartPrice;
    @FXML private DatePicker dpEndDate;
    @FXML private TextArea txtDescription;

    private final ItemNetwork itemNetwork = new ItemNetwork();

    @FXML
    public void initialize() {
        cbCategory.getItems().addAll("ELECTRONICS", "VEHICLE", "ART");
    }

    @FXML
    void handleCancel(ActionEvent event) {
        Stage stage = (Stage) txtItemName.getScene().getWindow();
        stage.close();
    }

    @FXML
    void handleSaveItem(ActionEvent event) {
        try {
            // 1. Thu thập dữ liệu
            String name = txtItemName.getText();
            String type = cbCategory.getValue(); // Nối với thuộc tính 'type' của DTO
            String desc = txtDescription.getText();

            // Xử lý giá tiền cẩn thận
            String priceStr = txtStartPrice.getText().trim();
            if (priceStr.isEmpty()) throw new NumberFormatException();
            BigDecimal price = new BigDecimal(priceStr);

            // Tạm thời chưa có giao diện nhập thuộc tính riêng, ta tạo Map rỗng
            HashMap<String, Object> extraProperties = new HashMap<>();

            // 2. TẠO HỘP PIZZA VỚI BUILDER PATTERN! (Rất ngầu)
            CreateItemRequestDTO requestDTO = new CreateItemRequestDTO.Builder()
                    .name(name)
                    .type(type)
                    .startingPrice(price)
                    .description(desc)
                    .condition("NEW") // Tạm fix cứng vì UI chưa có trường nhập Condition
                    .extraProps(extraProperties)
                    .build();

            System.out.println("Đang gửi yêu cầu lên Server...");

            // 3. Giao cho Shipper (Đã có Cookie) gửi đi
            itemNetwork.createItem(requestDTO).thenAccept(response -> {
                Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        System.out.println("Tạo sản phẩm THÀNH CÔNG! Check DB ngay!");
                        handleCancel(null); // Đóng popup
                    } else {
                        System.out.println("Lỗi Server: " + response.getMessage());
                    }
                });
            });

        } catch (NumberFormatException e) {
            System.out.println("Lỗi: Giá tiền phải là một con số hợp lệ!");
        }
    }
}