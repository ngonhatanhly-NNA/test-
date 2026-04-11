package com.server.service;

import com.server.DAO.ItemRepository;
import com.server.model.Item;
import com.google.gson.Gson;
import com.shared.dto.ItemResponseDTO; // Nhớ import cái hộp DTO của em

import java.util.List;
import java.util.stream.Collectors;

public class ItemService {

    private ItemRepository itemRepo;
    // ====================================================================
    // CODE MỚI (CHUẨN SOLID DÀNH CHO LUỒNG DTO)
    // ====================================================================

    // Constructor kiểu Dependency Injection: Không tự dùng chữ 'new' nữa,
    // ai xài Service này thì tự truyền Repo vào.
    public ItemService(ItemRepository itemRepo) {
        this.itemRepo = itemRepo;
    }

    // Hàm trả về List các hộp DTO (ItemResponseDTO) thay vì chuỗi String cứng nhắc
    public List<ItemResponseDTO> getAllItemsDTO() {
        // 1. Nhờ thủ kho lấy danh sách gốc từ DB
        List<Item> rawItems = itemRepo.getAllItems();

        // 2. Dùng băng chuyền (Stream) gọt bớt dữ liệu thừa, đóng gói vào DTO
        return rawItems.stream().map(item -> {
            // Tự động trích xuất loại "VEHICLE", "ART", "ELECTRONICS" từ tên Class
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
}