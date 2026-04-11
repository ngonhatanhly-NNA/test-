package com.shared.dto;

import java.math.BigDecimal;
import java.util.List;

public class ItemResponseDTO {
    // Để hiển thị lên màn hình, thường ta chỉ cần ID, Tên, Giá, Ảnh và Loại để biết đường vẽ icon.
    private long id;
    private String name;
    private String description;
    private BigDecimal startingPrice;
    private String type; // "ELECTRONICS", "ART", "VEHICLE"
    private List<String> imageUrls; // Lấy cái ảnh đầu tiên ra làm avatar

    public ItemResponseDTO() {}

    public ItemResponseDTO(long id, String name, String description, BigDecimal startingPrice, String type, List<String> imageUrls) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.type = type;
        this.imageUrls = imageUrls;
    }

    // --- GETTERS (Để Client lôi ra đắp lên màn hình UI) ---
    public long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getStartingPrice() { return startingPrice; }
    public String getType() { return type; }
    public List<String> getImageUrls() { return imageUrls; }

    // Lấy ảnh đầu tiên làm Thumbnail cho đẹp
    public String getThumbnailUrl() {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            return imageUrls.get(0);
        }
        return "default-item.png"; // Ảnh mặc định nếu không có
    }

    // --- SETTERS ---
    public void setId(long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStartingPrice(BigDecimal startingPrice) { this.startingPrice = startingPrice; }
    public void setType(String type) { this.type = type; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
}