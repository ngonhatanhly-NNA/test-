package com.server.service;

import com.server.DAO.ISellerRepository;
import com.server.model.Role;
import com.server.model.Seller;
import com.server.model.Status;
import com.shared.network.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("Kiểm thử Dịch vụ Người bán (SellerService)")
class SellerServiceTest {

    @Mock
    private ISellerRepository sellerRepository;

    @InjectMocks
    private SellerService sellerService;

    private AutoCloseable closeable;
    private Seller existingSeller;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        existingSeller = new Seller(
                1L, "sellerUser", "password", "seller@example.com", "Seller Name",
                "123456789", "123 Main St", Status.ACTIVE, Role.SELLER,
                new BigDecimal("1000.00"), "1234-5678-9012-3456",
                "My Shop", "123456", 4.5, 10, true
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Nested
    @DisplayName("Logic Cập nhật Thông tin Cửa hàng")
    class UpdateShopInfoTests {

        @Test
        @DisplayName("Nên cập nhật thành công với dữ liệu hợp lệ")
        void updateShopInfo_succeeds_forValidData() {
            when(sellerRepository.updateShopDetails(anyLong(), anyString(), anyString())).thenReturn(true);

            Response response = sellerService.updateShopInfo(1L, "New Shop Name", "654321");

            assertEquals("SUCCESS", response.getStatus());
            verify(sellerRepository, times(1)).updateShopDetails(1L, "New Shop Name", "654321");
        }

        @Test
        @DisplayName("Nên trả về lỗi khi tên cửa hàng trống")
        void updateShopInfo_fails_forBlankShopName() {
            Response response = sellerService.updateShopInfo(1L, "  ", "654321");

            assertEquals("FAIL", response.getStatus());
            verify(sellerRepository, never()).updateShopDetails(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("Nên trả về lỗi khi repository thất bại")
        void updateShopInfo_fails_whenRepositoryFails() {
            when(sellerRepository.updateShopDetails(anyLong(), anyString(), anyString())).thenReturn(false);

            Response response = sellerService.updateShopInfo(1L, "New Shop Name", "654321");

            assertEquals("ERROR", response.getStatus());
            verify(sellerRepository, times(1)).updateShopDetails(1L, "New Shop Name", "654321");
        }
    }

    @Nested
    @DisplayName("Logic Thêm Đánh giá")
    class AddReviewTests {

        @Test
        @DisplayName("Nên thêm đánh giá và cập nhật rating thành công")
        void addReview_succeeds_andUpdatesRating() {
            when(sellerRepository.findSellerByUserId(anyLong())).thenReturn(existingSeller);
            when(sellerRepository.updateRating(anyLong(), anyDouble(), anyInt())).thenReturn(true);

            Response response = sellerService.addReview(1L, 3.5);

            assertEquals("SUCCESS", response.getStatus());
            // Verify rating is calculated correctly: (4.5 * 10 + 3.5) / 11 = 4.409...
            verify(sellerRepository, times(1)).updateRating(eq(1L), doubleThat(d -> Math.abs(d - 4.409) < 0.001), eq(11));
        }

        @Test
        @DisplayName("Nên trả về lỗi khi không tìm thấy người bán")
        void addReview_fails_whenSellerNotFound() {
            when(sellerRepository.findSellerByUserId(anyLong())).thenReturn(null);

            Response response = sellerService.addReview(1L, 5.0);

            assertEquals("FAIL", response.getStatus());
            verify(sellerRepository, never()).updateRating(anyLong(), anyDouble(), anyInt());
        }
    }

    @Nested
    @DisplayName("Logic Lấy Chi tiết Người bán")
    class GetSellerDetailsTests {

        @Test
        @DisplayName("Nên trả về thông tin người bán khi tìm thấy")
        void getSellerDetails_succeeds_whenSellerFound() {
            when(sellerRepository.findSellerByUserId(anyLong())).thenReturn(existingSeller);

            Response response = sellerService.getSellerDetails(1L);

            assertEquals("SUCCESS", response.getStatus());
            assertNotNull(response.getData());
            assertInstanceOf(SellerService.SellerInfo.class, response.getData());

            SellerService.SellerInfo info = (SellerService.SellerInfo) response.getData();
            assertEquals("My Shop", info.shopName);
            assertEquals("Seller Name", info.ownerName);
        }

        @Test
        @DisplayName("Nên trả về lỗi khi không tìm thấy người bán")
        void getSellerDetails_fails_whenSellerNotFound() {
            when(sellerRepository.findSellerByUserId(anyLong())).thenReturn(null);

            Response response = sellerService.getSellerDetails(1L);

            assertEquals("FAIL", response.getStatus());
            assertNull(response.getData());
        }
    }
}