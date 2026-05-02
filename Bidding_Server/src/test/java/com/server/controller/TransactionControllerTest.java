package com.server.controller;

import com.server.service.AuctionService;
import com.shared.dto.BidHistoryDTO;
import com.shared.network.Response;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionControllerTest {

    @Mock
    private AuctionService auctionService;

    @Mock
    private Context ctx;

    private TransactionController transactionController;

    @BeforeEach
    void setUp() {
        // Khởi tạo controller với service giả
        transactionController = new TransactionController(auctionService);
    }

    @Test
    void getBidHistoryByAuction_whenAuctionExists_shouldReturnHistory() {
        // Arrange
        long auctionId = 1L;
        // Sử dụng đúng constructor của BidHistoryDTO
        BidHistoryDTO historyItem = new BidHistoryDTO(101L, "User 101", new BigDecimal("120.00"), LocalDateTime.now(), false);
        List<BidHistoryDTO> historyList = Collections.singletonList(historyItem);

        // Dạy cho Context giả cách hành xử
        when(ctx.pathParam("auctionId")).thenReturn(String.valueOf(auctionId));

        // Dạy cho Service giả cách hành xử
        when(auctionService.getBidHistory(auctionId)).thenReturn(historyList);

        // Act
        // Gọi đúng phương thức getBidHistoryByAuction trong controller
        transactionController.getBidHistoryByAuction(ctx);

        // Assert
        // 1. Kiểm tra service có được gọi đúng không
        verify(auctionService).getBidHistory(auctionId);

        // 2. Kiểm tra controller có trả về đúng JSON không
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).json(responseCaptor.capture());

        Response response = responseCaptor.getValue();
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(historyList, response.getData());
    }

    @Test
    void getBidHistoryByAuction_whenInvalidAuctionId_shouldReturn400BadRequest() {
        // Arrange
        String invalidAuctionId = "abc";
        when(ctx.pathParam("auctionId")).thenReturn(invalidAuctionId);
        
        // CẦN THIẾT: Mock hành vi cho ctx.status(400) để không trả về null
        when(ctx.status(400)).thenReturn(ctx);

        // Act
        transactionController.getBidHistoryByAuction(ctx);

        // Assert
        // 1. Kiểm tra controller có set đúng mã lỗi HTTP không
        verify(ctx).status(400);

        // 2. Kiểm tra controller có trả về đúng JSON lỗi không
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).json(responseCaptor.capture());

        Response response = responseCaptor.getValue();
        assertEquals("ERROR", response.getStatus());
        assertEquals("Auction ID không hợp lệ", response.getMessage());

        // 3. Đảm bảo service không bao giờ được gọi
        verify(auctionService, never()).getBidHistory(anyLong());
    }

    @Test
    void getBidHistoryByAuction_whenServiceThrowsException_shouldReturn500ServerError() {
        // Arrange
        long auctionId = 1L;
        when(ctx.pathParam("auctionId")).thenReturn(String.valueOf(auctionId));

        // Dạy cho service giả ném ra một lỗi bất kỳ
        when(auctionService.getBidHistory(auctionId)).thenThrow(new RuntimeException("Database connection failed"));
        
        // CẦN THIẾT: Mock hành vi cho ctx.status(500) để không trả về null
        when(ctx.status(500)).thenReturn(ctx);

        // Act
        transactionController.getBidHistoryByAuction(ctx);

        // Assert
        verify(ctx).status(500);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).json(responseCaptor.capture());

        Response response = responseCaptor.getValue();
        assertEquals("ERROR", response.getStatus());
        assertEquals("Lỗi server: Database connection failed", response.getMessage());
    }
}
