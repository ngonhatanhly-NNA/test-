package com.server.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.server.DAO.ItemRepository;
import com.server.model.Art;
import com.server.model.Electronics;
import com.server.model.Item;
import com.server.model.Vehicle;
import com.shared.dto.CreateItemRequestDTO;
import com.shared.dto.ItemResponseDTO;

public class ItemService {

    private final ItemRepository itemRepo;
    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);

    public ItemService(ItemRepository itemRepo) {
        this.itemRepo = itemRepo;
    }

    public List<ItemResponseDTO> getAllItemsDTO() {
        List<Item> rawItems = itemRepo.getAllItems();

        return rawItems.stream().map(item -> {
            String type = item.getClass().getSimpleName().toUpperCase();
            return new ItemResponseDTO(
                    item.getId(),
                    item.getName(),
                    item.getDescription(),
                    item.getStartingPrice(),
                    type,
                    item.getImageUrls()
            );
        }).collect(Collectors.toList());
    }

    public Item getItemById(long itemId) {
        return itemRepo.findItemById(itemId);
    }
    public List<ItemResponseDTO> getItemsBySellerIdDTO(int sellerId) {
        logger.info("ItemService.getItemsBySellerIdDTO: Fetching items for seller ID: {}", sellerId);
        List<Item> rawItems = itemRepo.getItemsBySellerId(sellerId);
        logger.info("ItemService.getItemsBySellerIdDTO: Found {} raw items", rawItems.size());

        return rawItems.stream().map(item -> {
            String type = item.getClass().getSimpleName().toUpperCase();
            logger.debug("ItemService: Converting item {} (seller_id: {}, type: {})", item.getId(), item.getSellerId(), type);
            return new ItemResponseDTO(
                    item.getId(),
                    item.getName(),
                    item.getDescription(),
                    item.getStartingPrice(),
                    type,
                    item.getImageUrls()
            );
        }).collect(Collectors.toList());
    }

    public void createNewItem(CreateItemRequestDTO dto) {
        try {
            Item newItem = com.server.model.ItemFactory.createItem(
                    dto.getType(),
                    0, // ID is 0 because it's auto-incremented by the DB
                    dto.getSellerId() != null ? dto.getSellerId() : 0,
                    dto.getName(),
                    dto.getDescription(),
                    dto.getStartingPrice(),
                    dto.getCondition(),
                    dto.getImageUrls(),
                    dto.getExtraProps()
            );
            
            itemRepo.saveItem(newItem);

        } catch (Exception e) {
            logger.error("Lỗi khi tạo sản phẩm từ DTO: " + e.getMessage(), e);
        }
    }

    public Map<String, String> extractItemSpecifics(Item item) {
        Map<String, String> specifics = new HashMap<>();

        // Tự động ép kiểu ngay trong câu lệnh if (Pattern Matching)
        if (item instanceof Vehicle v) {
            specifics.put("Năm sản xuất", String.valueOf(v.getManufactureYear()));
            specifics.put("Số KM", v.getMileage() + " km");
            specifics.put("Số VIN", v.getVinNumber());
        }
        else if (item instanceof Art a) {
            specifics.put("Tác giả", a.getArtistName());
            specifics.put("Chất liệu", a.getMaterial());
            specifics.put("Chứng nhận Auth", a.isHasCertificateOfAuthenticity() ? "Có" : "Không");
        }
        else if (item instanceof Electronics e) {
            specifics.put("Hãng", e.getBrand());
            specifics.put("Dòng máy", e.getModel());
            specifics.put("Bảo hành", e.getWarrantyMonths() + " tháng");
        }

        return specifics;
    }

    public String extractItemType(Item item) {
        if (item instanceof Vehicle) return "VEHICLE";
        if (item instanceof Art) return "ART";
        if (item instanceof Electronics) return "ELECTRONICS";
        return "GENERAL";
    }
}
