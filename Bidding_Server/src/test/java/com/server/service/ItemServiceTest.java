package com.server.service;

import com.server.DAO.ItemRepository;
import com.server.exception.ItemException;
import com.server.model.Art;
import com.server.model.Electronics;
import com.server.model.Item;
import com.server.model.Vehicle;
import com.shared.dto.CreateItemRequestDTO;
import com.shared.dto.ItemResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemService Tests")
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private CreateItemRequestDTO sampleDTO;
    private Item sampleItem;

    @BeforeEach
    void setUp() {
        Map<String, Object> extraProps = new HashMap<>();
        extraProps.put("brand", "Dell");
        extraProps.put("model", "XPS");
        
        sampleDTO = new CreateItemRequestDTO(
                "Laptop", "Description", new BigDecimal("1000"),
                "NEW", new ArrayList<>(), "ELECTRONICS",
                1, extraProps
        );

        sampleItem = new Electronics.Builder()
                .id(1)
                .sellerId(1)
                .name("Laptop")
                .startingPrice(new BigDecimal("1000"))
                .build();
    }

    @Test
    @DisplayName("Lấy tất cả items DTO thành công")
    void getAllItemsDTO_shouldReturnListOfDTOs() {
        when(itemRepository.getAllItems()).thenReturn(Collections.singletonList(sampleItem));

        List<ItemResponseDTO> result = itemService.getAllItemsDTO();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("Laptop", result.get(0).getName());
    }
    
    @Test
    @DisplayName("Lấy items DTO theo seller ID thành công")
    void getItemsBySellerIdDTO_shouldReturnListOfDTOs() {
        when(itemRepository.getItemsBySellerId(1)).thenReturn(Collections.singletonList(sampleItem));

        List<ItemResponseDTO> result = itemService.getItemsBySellerIdDTO(1);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("Laptop", result.get(0).getName());
    }

    @Test
    @DisplayName("Tạo item thành công")
    void createNewItem_shouldReturnDTO() {
        when(itemRepository.saveItem(any(Item.class))).thenReturn(1L);

        ItemResponseDTO response = itemService.createNewItem(sampleDTO);

        assertNotNull(response);
        assertEquals("Laptop", response.getName());
        verify(itemRepository, times(1)).saveItem(any(Item.class));
    }

    @Test
    @DisplayName("Tạo item thất bại khi DTO null")
    void createNewItem_shouldThrowException_whenDTONull() {
        ItemException exception = assertThrows(ItemException.class, () -> itemService.createNewItem(null));
        assertEquals(ItemException.ErrorCode.INVALID_ITEM_DATA.getCode(), exception.getErrorCode());
    }
    
    @Test
    @DisplayName("Tạo item thất bại do ItemException")
    void createNewItem_shouldThrowException_onItemException() {
        when(itemRepository.saveItem(any(Item.class))).thenThrow(new ItemException(ItemException.ErrorCode.ITEM_SAVE_FAILED, "Lỗi lưu DB"));
        
        ItemException exception = assertThrows(ItemException.class, () -> itemService.createNewItem(sampleDTO));
        assertEquals(ItemException.ErrorCode.ITEM_SAVE_FAILED.getCode(), exception.getErrorCode());
    }
    
    @Test
    @DisplayName("Tạo item thất bại do Exception chung")
    void createNewItem_shouldThrowException_onGenericException() {
        when(itemRepository.saveItem(any(Item.class))).thenThrow(new RuntimeException("Lỗi máy chủ"));
        
        ItemException exception = assertThrows(ItemException.class, () -> itemService.createNewItem(sampleDTO));
        assertEquals(ItemException.ErrorCode.FACTORY_CREATE_FAILED.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("Lấy item theo ID thành công")
    void getItemById_shouldReturnItem() {
        when(itemRepository.findItemById(1L)).thenReturn(sampleItem);

        Item item = itemService.getItemById(1L);

        assertNotNull(item);
        assertEquals("Laptop", item.getName());
    }
    
    @Test
    @DisplayName("Lấy item theo ID trả về null khi ID không hợp lệ")
    void getItemById_shouldReturnNull_whenIdInvalid() {
        Item item = itemService.getItemById(-1L);
        assertNull(item);
        
        Item itemZero = itemService.getItemById(0L);
        assertNull(itemZero);
    }
    
    @Test
    @DisplayName("Trích xuất thông số kỹ thuật chính xác cho Electronics")
    void extractItemSpecifics_shouldWorkForElectronics() {
        Electronics electronics = new Electronics.Builder()
                .brand("Apple")
                .model("MacBook")
                .warrantyMonths(12)
                .build();
                
        Map<String, String> specifics = itemService.extractItemSpecifics(electronics);
        
        assertEquals("Apple", specifics.get("Brand"));
        assertEquals("MacBook", specifics.get("Model"));
        assertEquals("12 months", specifics.get("Warranty"));
    }
    
    @Test
    @DisplayName("Trích xuất thông số kỹ thuật chính xác cho Vehicle")
    void extractItemSpecifics_shouldWorkForVehicle() {
        Vehicle vehicle = new Vehicle.Builder()
                .manufactureYear(2020)
                .mileage(50000)
                .vinNumber("VIN123")
                .build();
                
        Map<String, String> specifics = itemService.extractItemSpecifics(vehicle);
        
        assertEquals("2020", specifics.get("Manufacture Year"));
        assertEquals("50000 km", specifics.get("Mileage"));
        assertEquals("VIN123", specifics.get("VIN"));
    }
    
    @Test
    @DisplayName("Trích xuất thông số kỹ thuật chính xác cho Art")
    void extractItemSpecifics_shouldWorkForArt() {
        Art art = new Art.Builder()
                .artistName("Van Gogh")
                .material("Oil")
                .hasCertificateOfAuthenticity(true)
                .build();
                
        Map<String, String> specifics = itemService.extractItemSpecifics(art);
        
        assertEquals("Van Gogh", specifics.get("Artist"));
        assertEquals("Oil", specifics.get("Material"));
        assertEquals("Yes", specifics.get("Authenticity"));
    }

    @Test
    @DisplayName("Trích xuất loại sản phẩm chính xác")
    void extractItemType_shouldWorkForAllTypes() {
        assertEquals("ELECTRONICS", itemService.extractItemType(new Electronics.Builder().build()));
        assertEquals("VEHICLE", itemService.extractItemType(new Vehicle.Builder().build()));
        assertEquals("ART", itemService.extractItemType(new Art.Builder().build()));
        assertEquals("GENERAL", itemService.extractItemType(new Item() {
            @Override
            public void printInfo() {}
        }));
    }
}