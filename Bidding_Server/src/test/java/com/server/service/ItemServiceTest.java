package com.server.service;

import com.server.DAO.ItemRepository;
import com.server.model.Item;
import com.server.model.ItemFactory;
import com.shared.dto.CreateItemRequestDTO;
import com.shared.dto.ItemResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho ItemService
 * Kiểm tra logic tạo, lấy, cập nhật items và quản lý thuộc tính item
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ItemService - Quản lý Sản phẩm")
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private Item testItem;
    private CreateItemRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        Map<String, Object> testSpecifics = new HashMap<>();
        testSpecifics.put("brand", "Toyota");
        testSpecifics.put("year", "2020");
        testSpecifics.put("manufactureYear", 2020);
        testSpecifics.put("mileage", 10000);
        testSpecifics.put("vinNumber", "VIN123456");

        // Sử dụng ItemFactory để tạo mock item thay vì new Item()
        testItem = ItemFactory.createItem(
                "VEHICLE", 1, 10, "Toyota Camry", "Beautiful car",
                new BigDecimal("25000"), "USED", Collections.singletonList("image1.png"), testSpecifics
        );

        validRequest = new CreateItemRequestDTO.Builder()
                .name("Toyota Camry")
                .description("Beautiful car")
                .type("VEHICLE")
                .startingPrice(new BigDecimal("25000"))
                .condition("USED")
                .imageUrls(Collections.singletonList("image1.png"))
                .sellerId(10)
                .extraProps(testSpecifics)
                .build();
    }

    @Nested
    @DisplayName("Logic Tạo Item (createNewItem)")
    class CreateItemTests {

        @Test
        @DisplayName("Nên tạo item thành công với dữ liệu hợp lệ")
        void createNewItem_succeeds_withValidData() {
            when(itemRepository.saveItem(any(Item.class))).thenReturn(1L);

            ItemResponseDTO result = itemService.createNewItem(validRequest);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("Toyota Camry", result.getName());
            assertEquals("VEHICLE", result.getType());
            verify(itemRepository, times(1)).saveItem(any(Item.class));
        }

        @Test
        @DisplayName("Nên throw exception khi dto null")
        void createNewItem_throwsException_whenDtoNull() {
            assertThrows(com.server.exception.ItemException.class, () ->
                    itemService.createNewItem(null)
            );
            verify(itemRepository, never()).saveItem(any());
        }
    }

    @Nested
    @DisplayName("Logic Lấy Item (getItemById)")
    class GetItemTests {

        @Test
        @DisplayName("Nên lấy item thành công khi found")
        void getItemById_succeeds_whenItemFound() {
            when(itemRepository.findItemById(1L)).thenReturn(testItem);

            Item result = itemService.getItemById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("Toyota Camry", result.getName());
            verify(itemRepository, times(1)).findItemById(1L);
        }

        @Test
        @DisplayName("Nên trả về null khi item không tồn tại")
        void getItemById_returnsNull_whenItemNotFound() {
            when(itemRepository.findItemById(999L)).thenReturn(null);

            Item result = itemService.getItemById(999L);

            assertNull(result);
            verify(itemRepository, times(1)).findItemById(999L);
        }

        @Test
        @DisplayName("Nên trả về null khi id <= 0")
        void getItemById_returnsNull_whenIdInvalid() {
            Item result = itemService.getItemById(-1L);
            assertNull(result);
            verify(itemRepository, never()).findItemById(anyLong());
        }
    }

    @Nested
    @DisplayName("Logic Lấy Danh sách Items")
    class GetItemsListTests {

        @Test
        @DisplayName("Nên trả về tất cả items dưới dạng DTO")
        void getAllItemsDTO_succeeds() {
            when(itemRepository.getAllItems()).thenReturn(Collections.singletonList(testItem));

            List<ItemResponseDTO> result = itemService.getAllItemsDTO();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Toyota Camry", result.getFirst().getName());
            verify(itemRepository, times(1)).getAllItems();
        }

        @Test
        @DisplayName("Nên trả về items của một seller dưới dạng DTO")
        void getItemsBySellerIdDTO_succeeds() {
            when(itemRepository.getItemsBySellerId(10)).thenReturn(Collections.singletonList(testItem));

            List<ItemResponseDTO> result = itemService.getItemsBySellerIdDTO(10);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Toyota Camry", result.getFirst().getName());
            verify(itemRepository, times(1)).getItemsBySellerId(10);
        }
    }

    @Nested
    @DisplayName("Logic Extract Specifics và Type")
    class ExtractItemTests {

        @Test
        @DisplayName("Nên trích xuất đúng specifics cho Vehicle")
        void extractItemSpecifics_forVehicle() {
            Map<String, String> specifics = itemService.extractItemSpecifics(testItem);

            assertNotNull(specifics);
            assertEquals("2020", specifics.get("Năm sản xuất"));
            assertEquals("10000 km", specifics.get("Số KM"));
            assertEquals("VIN123456", specifics.get("Số VIN"));
        }

        @Test
        @DisplayName("Nên trích xuất đúng type cho Vehicle")
        void extractItemType_forVehicle() {
            String type = itemService.extractItemType(testItem);
            assertEquals("VEHICLE", type);
        }
    }
}
