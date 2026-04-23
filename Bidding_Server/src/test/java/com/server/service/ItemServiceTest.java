package com.server.service;

import com.server.DAO.ItemRepository;
import com.server.model.Item;
import com.server.model.ItemFactory;
import com.shared.dto.CreateItemRequestDTO;
import com.shared.dto.ItemResponseDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Kiểm thử Dịch vụ Vật phẩm (ItemService)")
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private AutoCloseable closeable;
    private Item sampleItem;
    private CreateItemRequestDTO createItemRequest;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);

        // Sample Item for getAllItemsDTO
        sampleItem = ItemFactory.createItem(
                "ELECTRONICS", 1, "Laptop", "A powerful laptop",
                new BigDecimal("1200.00"), "NEW",
                Collections.singletonList("url.com/image.jpg"), new HashMap<>()
        );

        // Sample DTO for createNewItem using Builder pattern
        Map<String, Object> extraProps = new HashMap<>();
        extraProps.put("brand", "TestBrand");
        createItemRequest = new CreateItemRequestDTO.Builder()
                .name("Test Car")
                .description("A test car")
                .startingPrice(new BigDecimal("25000.00"))
                .condition("USED")
                .imageUrls(new ArrayList<>())
                .type("VEHICLE")
                .extraProps(extraProps)
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Nested
    @DisplayName("Logic Lấy Tất cả Vật phẩm (getAllItemsDTO)")
    class GetAllItemsDTOTests {

        @Test
        @DisplayName("Nên trả về danh sách DTO khi có vật phẩm")
        void getAllItemsDTO_shouldReturnDTOList_whenItemsExist() {
            when(itemRepository.getAllItems()).thenReturn(Collections.singletonList(sampleItem));

            List<ItemResponseDTO> result = itemService.getAllItemsDTO();

            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            ItemResponseDTO dto = result.getFirst();
            assertEquals("Laptop", dto.getName());
            assertEquals("ELECTRONICS", dto.getType());
            verify(itemRepository, times(1)).getAllItems();
        }

        @Test
        @DisplayName("Nên trả về danh sách rỗng khi không có vật phẩm")
        void getAllItemsDTO_shouldReturnEmptyList_whenNoItemsExist() {
            when(itemRepository.getAllItems()).thenReturn(Collections.emptyList());

            List<ItemResponseDTO> result = itemService.getAllItemsDTO();

            assertTrue(result.isEmpty());
            verify(itemRepository, times(1)).getAllItems();
        }
    }

    @Nested
    @DisplayName("Logic Tạo Vật phẩm Mới (createNewItem)")
    class CreateNewItemTests {

        @Test
        @DisplayName("Nên trả về true khi tạo vật phẩm thành công")
        void createNewItem_shouldReturnTrue_onSuccessfulCreation() {
            when(itemRepository.saveItem(any(Item.class))).thenReturn(true);

            boolean result = itemService.createNewItem(createItemRequest);

            assertTrue(result);
            verify(itemRepository, times(1)).saveItem(any(Item.class));
        }

        @Test
        @DisplayName("Nên trả về false khi lưu vật phẩm thất bại")
        void createNewItem_shouldReturnFalse_whenSaveFails() {
            when(itemRepository.saveItem(any(Item.class))).thenReturn(false);

            boolean result = itemService.createNewItem(createItemRequest);

            assertFalse(result);
            verify(itemRepository, times(1)).saveItem(any(Item.class));
        }

        @Test
        @DisplayName("Nên trả về false khi loại vật phẩm không hợp lệ")
        void createNewItem_shouldReturnFalse_forInvalidItemType() {
            CreateItemRequestDTO invalidRequest = new CreateItemRequestDTO.Builder()
                    .name("Test")
                    .description("Desc")
                    .startingPrice(BigDecimal.ONE)
                    .condition("NEW")
                    .imageUrls(new ArrayList<>())
                    .type("INVALID_TYPE")
                    .extraProps(new HashMap<>())
                    .build();

            // Factory throws IllegalArgumentException, which is caught in the service
            boolean result = itemService.createNewItem(invalidRequest);

            assertFalse(result);
            // saveItem should not be called if factory fails
            verify(itemRepository, never()).saveItem(any(Item.class));
        }
    }
}