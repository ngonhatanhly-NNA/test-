package com.server.service;

import com.server.DAO.ItemRepository;
import com.server.exception.ItemException; // Quan trọng: Phải import ItemException
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

        // Sample Item cho getAllItemsDTO
        sampleItem = ItemFactory.createItem(
                "ELECTRONICS", 1, "Laptop", "A powerful laptop",
                new BigDecimal("1200.00"), "NEW",
                Collections.singletonList("url.com/image.jpg"), new HashMap<>()
        );

        // Sample DTO cho createNewItem
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
    @DisplayName("Logic Tạo Vật phẩm Mới (createNewItem) - Mô hình Exception")
    class CreateNewItemTests {

        @Test
        @DisplayName("Nên chạy thành công (không ném lỗi) khi tạo vật phẩm hợp lệ")
        void createNewItem_shouldNotThrow_onSuccessfulCreation() {
            // Với hàm void, Mockito dùng doNothing() để giả lập việc chạy thành công
            doNothing().when(itemRepository).saveItem(any(Item.class));

            // Chạy thử hàm service, dùng assertDoesNotThrow để đảm bảo không có quả mìn nào nổ
            assertDoesNotThrow(() -> itemService.createNewItem(createItemRequest));

            // Xác minh hàm saveItem dưới Repo đã được gọi đúng 1 lần
            verify(itemRepository, times(1)).saveItem(any(Item.class));
        }

        @Test
        @DisplayName("Nên ném ItemException khi Repo lưu thất bại")
        void createNewItem_shouldThrowItemException_whenSaveFails() {
            // Giả lập việc Repo bị lỗi và ném ra một quả mìn ItemException
            doThrow(new ItemException(ItemException.ErrorCode.ITEM_SAVE_FAILED, "Database error"))
                    .when(itemRepository).saveItem(any(Item.class));

            // Bắt quả mìn đó bằng assertThrows khi gọi hàm Service
            ItemException exception = assertThrows(ItemException.class,
                    () -> itemService.createNewItem(createItemRequest));

            // Kiểm tra xem quả mìn có đúng mã lỗi ta mong muốn không
            assertEquals(ItemException.ErrorCode.ITEM_SAVE_FAILED.getCode(), exception.getErrorCode());
            verify(itemRepository, times(1)).saveItem(any(Item.class));
        }

        @Test
        @DisplayName("Nên ném Exception khi đúc Item bị lỗi (Sai Type)")
        void createNewItem_shouldThrowException_forInvalidItemType() {
            CreateItemRequestDTO invalidRequest = new CreateItemRequestDTO.Builder()
                    .name("Test")
                    .description("Desc")
                    .startingPrice(BigDecimal.ONE)
                    .condition("NEW")
                    .imageUrls(new ArrayList<>())
                    .type("INVALID_TYPE") // Loại không tồn tại
                    .extraProps(new HashMap<>())
                    .build();

            // Factory sẽ văng lỗi IllegalArgumentException, Service sẽ tóm lấy và bọc thành ItemException
            ItemException exception = assertThrows(ItemException.class,
                    () -> itemService.createNewItem(invalidRequest));

            assertEquals(ItemException.ErrorCode.FACTORY_CREATE_FAILED.getCode(), exception.getErrorCode());
            // Hàm saveItem không bao giờ được gọi tới
            verify(itemRepository, never()).saveItem(any(Item.class));
        }
    }
}