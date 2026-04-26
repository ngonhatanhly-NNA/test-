package com.server.service;

import com.server.DAO.ItemRepository;
import com.server.model.Item;
import com.shared.dto.CreateItemRequestDTO;
import com.shared.dto.ItemResponseDTO;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public void createNewItem(CreateItemRequestDTO dto) {
        try {
            Item newItem = com.server.model.ItemFactory.createItem(
                    dto.getType(),
                    0, // ID is 0 because it's auto-incremented by the DB
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
}
