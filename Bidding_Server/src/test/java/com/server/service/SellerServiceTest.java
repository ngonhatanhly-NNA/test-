package com.server.service;

import com.server.DAO.ISellerRepository;
import com.server.model.Item;
import com.server.model.Role;
import com.server.model.Seller;
import com.server.model.Status;
import com.shared.dto.ItemResponseDTO;
import com.shared.network.Response;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerServiceTest {

    @Mock
    private ISellerRepository sellerRepository;

    @InjectMocks
    private SellerService sellerService;

    private Seller testSeller;
    private Item testItem;

    @BeforeEach
    void setUp() {
        // Sử dụng đúng Constructor của Seller lấy từ model
        testSeller = new Seller(
                1L, "seller1", "pass", "seller@test.com", "Test Seller",
                "123456", "Address", Status.ACTIVE, Role.SELLER, BigDecimal.ZERO, "1234",
                "Shop 1", "1234567890", 4.5, 10, true
        );

        // Sử dụng đúng Constructor của Electronics (ép kiểu int cho ID)
        testItem = new com.server.model.Electronics(
                1, "Laptop", "Desc", new BigDecimal("1000"), "NEW", new ArrayList<>(), "Dell", "XPS", 12
        );
    }

    @Test
    void getSellerDetails_whenSellerExists_shouldReturnSuccess() {
        // Arrange
        when(sellerRepository.findSellerByUserId(1L)).thenReturn(testSeller);

        // Act
        Response response = sellerService.getSellerDetails(1L);

        // Assert
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getData());
        SellerService.SellerInfo info = (SellerService.SellerInfo) response.getData();
        assertEquals("Shop 1", info.shopName);
        assertEquals(4.5, info.rating);
    }

    @Test
    void getSellerDetails_whenSellerNotFound_shouldReturnFail() {
        // Arrange
        when(sellerRepository.findSellerByUserId(99L)).thenReturn(null);

        // Act
        Response response = sellerService.getSellerDetails(99L);

        // Assert
        assertEquals("FAIL", response.getStatus());
        assertNull(response.getData());
    }

    @Test
    void getSellerItems_shouldReturnDTOList() {
        // Arrange
        when(sellerRepository.getItemsBySellerId(1L)).thenReturn(Collections.singletonList(testItem));

        // Act
        Response response = sellerService.getSellerItems(1L);

        // Assert
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getData());
        @SuppressWarnings("unchecked")
        List<ItemResponseDTO> items = (List<ItemResponseDTO>) response.getData();
        assertEquals(1, items.size());
        assertEquals("Laptop", items.getFirst().getName());
        assertEquals("ELECTRONICS", items.getFirst().getType());
    }

    @Test
    void getSellerStatistics_whenStatsExist_shouldReturnSuccess() {
        // Arrange
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalItems", 5);
        when(sellerRepository.getSellerStatistics(1L)).thenReturn(stats);

        // Act
        Response response = sellerService.getSellerStatistics(1L);

        // Assert
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(stats, response.getData());
    }

    @Test
    void getSellerStatistics_whenStatsNull_shouldReturnFail() {
        // Arrange
        when(sellerRepository.getSellerStatistics(1L)).thenReturn(null);

        // Act
        Response response = sellerService.getSellerStatistics(1L);

        // Assert
        assertEquals("FAIL", response.getStatus());
    }

    @Test
    void updateShopInfo_whenNameEmpty_shouldReturnFail() {
        // Act
        Response response = sellerService.updateShopInfo(1L, "", "123");

        // Assert
        assertEquals("FAIL", response.getStatus());
        verify(sellerRepository, never()).updateShopDetails(anyLong(), anyString(), anyString());
    }

    @Test
    void updateShopInfo_whenRepoSucceeds_shouldReturnSuccess() {
        // Arrange
        when(sellerRepository.updateShopDetails(1L, "New Shop", "123")).thenReturn(true);

        // Act
        Response response = sellerService.updateShopInfo(1L, "New Shop", "123");

        // Assert
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void addReview_whenSellerExists_shouldUpdateRating() {
        // Arrange
        when(sellerRepository.findSellerByUserId(1L)).thenReturn(testSeller);
        when(sellerRepository.updateRating(eq(1L), anyDouble(), anyInt())).thenReturn(true);

        // Act
        Response response = sellerService.addReview(1L, 5.0);

        // Assert
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(11, testSeller.getTotalReviews()); // 10 + 1
        // (10 * 4.5 + 5.0) / 11 = 4.5454...
        verify(sellerRepository).updateRating(eq(1L), anyDouble(), eq(11));
    }

    @Test
    void addReview_whenSellerNotFound_shouldReturnFail() {
        // Arrange
        when(sellerRepository.findSellerByUserId(99L)).thenReturn(null);

        // Act
        Response response = sellerService.addReview(99L, 5.0);

        // Assert
        assertEquals("FAIL", response.getStatus());
    }
}
