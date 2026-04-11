package com.shared.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class CreateItemRequestDTO {
    // 1. Thông tin CHUNG (Mặt hàng nào cũng phải có)
    private String name;
    private String description;
    private BigDecimal startingPrice;
    private String condition;
    private List<String> imageUrls;

    // 2. Thông tin PHÂN LOẠI
    private String type; // Chỉ được phép là: "ELECTRONICS", "ART", hoặc "VEHICLE"

    // 3. Thông tin RIÊNG (Tùy loại mặt hàng mới có)
    // Dùng Map (kiểu Từ điển) là cách thông minh nhất để chứa các thuộc tính tự do
    // mà không cần phải tạo ra 3 cái DTO khác nhau.
    private Map<String, Object> extraProps;

    // --- Constructor rỗng (BẮT BUỘC phải có để thư viện Gson nó dịch từ JSON ra) ---
    public CreateItemRequestDTO() {}

    // --- Constructor có tham số (Dùng khi đóng gói ở Client) ---
    public CreateItemRequestDTO(String name, String description, BigDecimal startingPrice,
                                String condition, List<String> imageUrls,
                                String type, Map<String, Object> extraProps) {
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.condition = condition;
        this.imageUrls = imageUrls;
        this.type = type;
        this.extraProps = extraProps;
    }

    // --- GETTERS (Để Server mở hộp ra lấy đồ) ---
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getStartingPrice() { return startingPrice; }
    public String getCondition() { return condition; }
    public List<String> getImageUrls() { return imageUrls; }
    public String getType() { return type; }
    public Map<String, Object> getExtraProps() { return extraProps; }

    // --- SETTERS (Để an toàn, có thể bỏ qua Setters nếu em muốn DTO chỉ đọc, nhưng cứ tạo cho đầy đủ) ---
    // (Em có thể tự dùng chức năng Generate của IntelliJ: Alt + Insert -> Setter để tạo cho nhanh)
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStartingPrice(BigDecimal startingPrice) { this.startingPrice = startingPrice; }
    public void setCondition(String condition) { this.condition = condition; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public void setType(String type) { this.type = type; }
    public void setExtraProps(Map<String, Object> extraProps) { this.extraProps = extraProps; }
}