package com.client.controller.dashboard;

import com.client.network.ItemNetwork;
import com.client.session.ClientSession;
import com.google.gson.Gson;
import com.shared.dto.CreateItemRequestDTO;
import com.shared.dto.ItemResponseDTO;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

public class CreateItemController {

    private static final Logger logger = LoggerFactory.getLogger(CreateItemController.class);

    @FXML private TextField txtItemName;
    @FXML private ComboBox<String> cbCategory;
    @FXML private TextField txtStartPrice;
    @FXML private TextArea txtDescription;

    // --- BẮT CÁC THÀNH PHẦN DYNAMIC UI TỪ FXML ---
    @FXML private VBox vboxElectronics, vboxVehicle, vboxArt;

    // Thuộc tính Electronics
    @FXML private TextField txtElecBrand, txtElecModel, txtElecWarranty;
    // Thuộc tính Vehicle
    @FXML private TextField txtVehYear, txtVehMileage, txtVehVin;
    // Thuộc tính Art
    @FXML private TextField txtArtArtist, txtArtMaterial;
    @FXML private ComboBox<String> cbArtCert;

    private final ItemNetwork itemNetwork = new ItemNetwork();
    private final Gson gson = new Gson();
    private ItemResponseDTO createdItem;

    @FXML
    public void initialize() {
        // Đổ dữ liệu vào ComboBox
        cbCategory.getItems().addAll("ELECTRONICS", "VEHICLE", "ART");
        cbArtCert.getItems().addAll("Yes", "No");

        // LẮNG NGHE SỰ KIỆN: Khi người dùng chọn Category, UI sẽ biến hình!
        cbCategory.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            hideAllDynamicFields(); // Giấu hết đi trước

            if (newVal == null) return;
            switch (newVal) {
                case "ELECTRONICS":
                    showVBox(vboxElectronics);
                    break;
                case "VEHICLE":
                    showVBox(vboxVehicle);
                    break;
                case "ART":
                    showVBox(vboxArt);
                    break;
            }
        });
    }

    // Tắt tàng hình và bung ra
    private void showVBox(VBox box) {
        box.setVisible(true);
        box.setManaged(true);
    }

    // Bật tàng hình và thu gọn lại
    private void hideAllDynamicFields() {
        vboxElectronics.setVisible(false); vboxElectronics.setManaged(false);
        vboxVehicle.setVisible(false); vboxVehicle.setManaged(false);
        vboxArt.setVisible(false); vboxArt.setManaged(false);
    }

    @FXML
    void handleCancel(ActionEvent event) {
        Stage stage = (Stage) txtItemName.getScene().getWindow();
        stage.close();
    }

    @FXML
    void handleSaveItem(ActionEvent event) {
        try {
            String name = txtItemName.getText();
            String type = cbCategory.getValue();
            String desc = txtDescription.getText();

            String priceStr = txtStartPrice.getText().trim();
            if (priceStr.isEmpty()) throw new NumberFormatException("Giá không được để trống");
            BigDecimal price = new BigDecimal(priceStr);

            // ================= ĐÓNG GÓI EXTRA PROPS =================
            HashMap<String, Object> extraProperties = new HashMap<>();

            if ("ELECTRONICS".equals(type)) {
                extraProperties.put("brand", txtElecBrand.getText());
                extraProperties.put("model", txtElecModel.getText());
                // Parse an toàn cho số
                String warranty = txtElecWarranty.getText();
                extraProperties.put("warrantyMonths", warranty.isEmpty() ? 0 : Integer.parseInt(warranty));

            } else if ("VEHICLE".equals(type)) {
                String year = txtVehYear.getText();
                String mileage = txtVehMileage.getText();
                extraProperties.put("manufactureYear", year.isEmpty() ? 0 : Integer.parseInt(year));
                extraProperties.put("mileage", mileage.isEmpty() ? 0 : Integer.parseInt(mileage));
                extraProperties.put("vinNumber", txtVehVin.getText());

            } else if ("ART".equals(type)) {
                extraProperties.put("artistName", txtArtArtist.getText());
                extraProperties.put("material", txtArtMaterial.getText());
                extraProperties.put("hasCertificateOfAuthenticity", "Yes".equals(cbArtCert.getValue()));
            }
            // =========================================================

            // Dùng Builder Pattern
            CreateItemRequestDTO requestDTO = new CreateItemRequestDTO.Builder()
                    .name(name)
                    .type(type)
                    .startingPrice(price)
                    .description(desc)
                    .condition("NEW")
                    .imageUrls(new ArrayList<>()) // Bọc chống đạn
                    .sellerId((int) ClientSession.getUserId())
                    .extraProps(extraProperties)
                    .build();

            logger.info("Đang gửi yêu cầu tạo Item lên Server...");

            itemNetwork.createItem(requestDTO).thenAccept(response -> {
                Platform.runLater(() -> {
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        if (response.getData() != null) {
                            createdItem = gson.fromJson(gson.toJson(response.getData()), ItemResponseDTO.class);
                        }
                        logger.info("Tạo sản phẩm THÀNH CÔNG! Đóng popup.");
                        handleCancel(null);
                    } else {
                        String message = response != null ? response.getMessage() : "Không nhận được phản hồi từ server";
                        logger.error("Lỗi Server: {}", message);
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Create Item Failed");
                        alert.setHeaderText(null);
                        alert.setContentText(message);
                        alert.showAndWait();
                    }
                });
            });

        } catch (NumberFormatException e) {
            logger.error("Dữ liệu số nhập vào không hợp lệ: {}", e.getMessage());
        }
    }

    public ItemResponseDTO getCreatedItem() {
        return createdItem;
    }
}