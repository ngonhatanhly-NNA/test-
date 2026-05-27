package com.server.service;

import com.server.DAO.IBidderRepository;
import com.server.model.Bidder;
import com.server.model.Role;
import com.server.model.Status;
import com.shared.network.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BidderService Tests")
class BidderServiceTest {

    @Mock
    private IBidderRepository bidderRepository;

    private BidderService bidderService;

    private Bidder testBidder;

    @BeforeEach
    void setUp() {
        bidderService = new BidderService(bidderRepository);
        testBidder = new Bidder(
                1L, "bidder1", "pass", "bidder@test.com", "Test Bidder",
                "123456", "Address", Status.ACTIVE, Role.BIDDER, new BigDecimal("100"), "1234"
        );
    }

    @Test
    @DisplayName("Nạp tiền thành công (overload 1)")
    void depositMoney_shouldReturnSuccess_overload1() {
        when(bidderRepository.updateBalance(eq(1L), any(BigDecimal.class))).thenReturn(true);
        String responseJson = bidderService.depositMoney(testBidder, 50.0);
        assertNotNull(responseJson);
        assertTrue(responseJson.contains("SUCCESS"));
        verify(bidderRepository, times(1)).updateBalance(1L, BigDecimal.valueOf(150.0));
    }

    @Test
    @DisplayName("Nạp tiền thất bại do DB lỗi (overload 1)")
    void depositMoney_shouldReturnError_onDbError_overload1() {
        when(bidderRepository.updateBalance(eq(1L), any(BigDecimal.class))).thenReturn(false);
        String responseJson = bidderService.depositMoney(testBidder, 50.0);
        assertNotNull(responseJson);
        assertTrue(responseJson.contains("ERROR"));
    }

    @Test
    @DisplayName("Nạp tiền thất bại do số tiền <= 0 (overload 1)")
    void depositMoney_shouldReturnFail_whenAmountNegative_overload1() {
        String responseJson = bidderService.depositMoney(testBidder, 0.0);
        assertNotNull(responseJson);
        assertTrue(responseJson.contains("FAIL"));
    }

    @Test
    @DisplayName("Nạp tiền thành công (overload 2)")
    void depositMoney_shouldReturnSuccess_overload2() {
        when(bidderRepository.getBidderById(1L)).thenReturn(testBidder);
        when(bidderRepository.updateBalance(eq(1L), any(BigDecimal.class))).thenReturn(true);

        String responseJson = bidderService.depositMoney(1L, 50.0);

        assertNotNull(responseJson);
        assertTrue(responseJson.contains("SUCCESS"));
        verify(bidderRepository, times(1)).updateBalance(1L, BigDecimal.valueOf(150.0));
    }

    @Test
    @DisplayName("Nạp tiền thất bại do số tiền <= 0 (overload 2)")
    void depositMoney_shouldReturnFail_whenAmountNegative_overload2() {
        String responseJson = bidderService.depositMoney(1L, -10.0);
        assertNotNull(responseJson);
        assertTrue(responseJson.contains("FAIL"));
    }

    @Test
    @DisplayName("Nạp tiền thất bại do không tìm thấy bidder (overload 2)")
    void depositMoney_shouldReturnFail_whenBidderNotFound_overload2() {
        when(bidderRepository.getBidderById(99L)).thenReturn(null);
        String responseJson = bidderService.depositMoney(99L, 50.0);
        assertNotNull(responseJson);
        assertTrue(responseJson.contains("FAIL"));
    }

    @Test
    @DisplayName("Kiểm tra canAffordBid trả về true")
    void canAffordBid_shouldReturnTrue() {
        when(bidderRepository.getBidderById(1L)).thenReturn(testBidder);
        boolean result = bidderService.canAffordBid(testBidder, 50.0);
        assertTrue(result);
    }

    @Test
    @DisplayName("Kiểm tra canAffordBid trả về false")
    void canAffordBid_shouldReturnFalse() {
        when(bidderRepository.getBidderById(1L)).thenReturn(testBidder);
        boolean result = bidderService.canAffordBid(testBidder, 150.0);
        assertFalse(result);
    }

    @Test
    @DisplayName("Thanh toán thành công (có itemName)")
    void settlePayment_shouldSucceed() {
        when(bidderRepository.updateBalance(eq(1L), any(BigDecimal.class))).thenReturn(true);
        bidderService.settlePayment(testBidder, 50.0, "Test Item");
        verify(bidderRepository, times(1)).updateBalance(1L, BigDecimal.valueOf(50.0));
        verify(bidderRepository, times(1)).logTransaction(eq(1L), any(BigDecimal.class), eq("DEDUCT"), anyString());
    }

    @Test
    @DisplayName("Thanh toán thành công (không có itemName)")
    void settlePayment_shouldSucceed_noItemName() {
        when(bidderRepository.updateBalance(eq(1L), any(BigDecimal.class))).thenReturn(true);
        bidderService.settlePayment(testBidder, 50.0);
        verify(bidderRepository, times(1)).updateBalance(1L, BigDecimal.valueOf(50.0));
    }

    @Test
    @DisplayName("Thanh toán thất bại do không đủ tiền")
    void settlePayment_shouldNotUpdate_whenInsufficientFunds() {
        bidderService.settlePayment(testBidder, 150.0);
        verify(bidderRepository, never()).updateBalance(anyLong(), any());
    }

    @Test
    @DisplayName("Lấy chi tiết bidder thành công")
    void getBidderDetails_shouldReturnBidder() {
        when(bidderRepository.getBidderById(1L)).thenReturn(testBidder);
        Bidder bidder = bidderService.getBidderDetails(1L);
        assertNotNull(bidder);
        assertEquals("bidder1", bidder.getUsername());
    }

    @Test
    @DisplayName("Lấy lịch sử giao dịch ví")
    void getWalletTransactions_shouldReturnTransactions() {
        when(bidderRepository.getWalletTransactions(1L)).thenReturn(java.util.Collections.emptyList());
        Response response = bidderService.getWalletTransactions(1L);
        assertEquals("SUCCESS", response.getStatus());
    }
}