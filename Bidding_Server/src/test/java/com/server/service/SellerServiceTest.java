package com.server.service;

import com.server.DAO.AuctionRepository;
import com.server.DAO.ISellerRepository;
import com.server.DAO.ItemRepository;
import com.server.model.Auction;
import com.server.model.Electronics;
import com.server.model.Item;
import com.server.model.Seller;
import com.shared.network.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerService Tests")
class SellerServiceTest {

    @Mock
    private ISellerRepository sellerRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private AuctionRepository auctionRepository;

    private SellerService sellerService;

    private Seller testSeller;

    @BeforeEach
    void setUp() throws Exception {
        sellerService = new SellerService(sellerRepository);

        // Inject mocks via reflection because the fields are initialized in constructor
        Field itemRepoField = SellerService.class.getDeclaredField("itemRepo");
        itemRepoField.setAccessible(true);
        itemRepoField.set(sellerService, itemRepository);

        Field auctionRepoField = SellerService.class.getDeclaredField("auctionRepo");
        auctionRepoField.setAccessible(true);
        auctionRepoField.set(sellerService, auctionRepository);

        testSeller = new Seller();
        testSeller.setId(1L);
        testSeller.setShopName("My Shop");
    }

    @Test
    @DisplayName("Lấy seller details thành công")
    void getSellerDetails_shouldReturnSuccess() {
        when(sellerRepository.findSellerByUserId(1L)).thenReturn(testSeller);

        Response response = sellerService.getSellerDetails(1L);

        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getData());
    }

    @Test
    @DisplayName("Lấy seller details thất bại do không tìm thấy")
    void getSellerDetails_shouldReturnFail_whenNotFound() {
        when(sellerRepository.findSellerByUserId(1L)).thenReturn(null);

        Response response = sellerService.getSellerDetails(1L);

        assertEquals("FAIL", response.getStatus());
    }

    @Test
    @DisplayName("Lấy seller items thành công")
    void getSellerItems_shouldReturnSuccess() {
        Item item = new Electronics.Builder().imageUrls(new ArrayList<>()).build();
        when(sellerRepository.getItemsBySellerId(1L)).thenReturn(Collections.singletonList(item));

        Response response = sellerService.getSellerItems(1L);

        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getData());
    }

    @Test
    @DisplayName("Cập nhật shop info thành công")
    void updateShopInfo_shouldReturnSuccess() {
        when(sellerRepository.updateShopDetails(1L, "New Shop", "123")).thenReturn(true);

        Response response = sellerService.updateShopInfo(1L, "New Shop", "123");

        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    @DisplayName("Cập nhật shop info thất bại do tên rỗng")
    void updateShopInfo_shouldReturnFail_whenNameEmpty() {
        Response response = sellerService.updateShopInfo(1L, "", "123");
        assertEquals("FAIL", response.getStatus());
        
        response = sellerService.updateShopInfo(1L, null, "123");
        assertEquals("FAIL", response.getStatus());
    }
    
    @Test
    @DisplayName("Cập nhật shop info thất bại do DB")
    void updateShopInfo_shouldReturnFail_whenDBFails() {
        when(sellerRepository.updateShopDetails(1L, "New Shop", "123")).thenReturn(false);

        Response response = sellerService.updateShopInfo(1L, "New Shop", "123");

        assertEquals("ERROR", response.getStatus());
    }

    @Test
    @DisplayName("Thêm review thành công")
    void addReview_shouldReturnSuccess() {
        when(sellerRepository.findSellerByUserId(1L)).thenReturn(testSeller);
        when(sellerRepository.updateRating(anyLong(), anyDouble(), anyInt())).thenReturn(true);

        Response response = sellerService.addReview(1L, 5.0);

        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    @DisplayName("Thêm review thất bại do không tìm thấy seller")
    void addReview_shouldReturnFail_whenSellerNotFound() {
        when(sellerRepository.findSellerByUserId(1L)).thenReturn(null);

        Response response = sellerService.addReview(1L, 5.0);

        assertEquals("FAIL", response.getStatus());
    }

    @Test
    @DisplayName("Thêm review thất bại do lỗi DB")
    void addReview_shouldReturnError_onDbError() {
        when(sellerRepository.findSellerByUserId(1L)).thenReturn(testSeller);
        when(sellerRepository.updateRating(anyLong(), anyDouble(), anyInt())).thenReturn(false);

        Response response = sellerService.addReview(1L, 5.0);

        assertEquals("ERROR", response.getStatus());
    }

    @Test
    @DisplayName("Lấy seller statistics thành công")
    void getSellerStatistics_shouldReturnSuccess() {
        Map<String, Object> mockStats = new HashMap<>();
        when(sellerRepository.getSellerStatistics(1L)).thenReturn(mockStats);

        Response response = sellerService.getSellerStatistics(1L);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(mockStats, response.getData());
    }
    
    @Test
    @DisplayName("Lấy seller statistics thất bại (null)")
    void getSellerStatistics_shouldReturnFail_whenNull() {
        when(sellerRepository.getSellerStatistics(1L)).thenReturn(null);

        Response response = sellerService.getSellerStatistics(1L);

        assertEquals("FAIL", response.getStatus());
    }

    @Test
    @DisplayName("Lấy transfer history thành công")
    void getTransferHistory_shouldReturnSuccess() {
        when(sellerRepository.getTransferHistory(1L)).thenReturn(Collections.emptyList());

        Response response = sellerService.getTransferHistory(1L);

        assertEquals("SUCCESS", response.getStatus());
    }
    
    @Test
    @DisplayName("Xóa item thành công (chưa có auction)")
    void deleteItem_shouldReturnSuccess() {
        Item item = new Electronics.Builder().id(1).build();
        when(sellerRepository.getItemsBySellerId(1L)).thenReturn(Collections.singletonList(item));
        when(auctionRepository.findByItemId(1L)).thenReturn(Collections.emptyList());
        when(itemRepository.delete(1L)).thenReturn(true);
        
        Response response = sellerService.deleteItem(1L, 1L);
        
        assertEquals("SUCCESS", response.getStatus());
    }
    
    @Test
    @DisplayName("Xóa item thất bại (không phải của seller)")
    void deleteItem_shouldReturnError_whenNotOwned() {
        Item item = new Electronics.Builder().id(2).build();
        when(sellerRepository.getItemsBySellerId(1L)).thenReturn(Collections.singletonList(item));
        
        Response response = sellerService.deleteItem(1L, 1L);
        
        assertEquals("ERROR", response.getStatus());
    }

    @Test
    @DisplayName("Xóa item thất bại (đang có auction)")
    void deleteItem_shouldReturnError_whenAuctionActive() {
        Item item = new Electronics.Builder().id(1).build();
        Auction auction = new Auction();
        auction.setStatus(Auction.AuctionStatus.RUNNING);
        when(sellerRepository.getItemsBySellerId(1L)).thenReturn(Collections.singletonList(item));
        when(auctionRepository.findByItemId(1L)).thenReturn(Collections.singletonList(auction));
        
        Response response = sellerService.deleteItem(1L, 1L);
        
        assertEquals("ERROR", response.getStatus());
    }

    @Test
    @DisplayName("Xóa item thất bại do lỗi DB")
    void deleteItem_shouldReturnError_onDbError() {
        Item item = new Electronics.Builder().id(1).build();
        when(sellerRepository.getItemsBySellerId(1L)).thenReturn(Collections.singletonList(item));
        when(auctionRepository.findByItemId(1L)).thenReturn(Collections.emptyList());
        when(itemRepository.delete(1L)).thenReturn(false);
        
        Response response = sellerService.deleteItem(1L, 1L);

        assertEquals("ERROR", response.getStatus());
    }
}