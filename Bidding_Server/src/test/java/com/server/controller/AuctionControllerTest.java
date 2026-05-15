package com.server.controller;

import com.google.gson.Gson;
import com.server.exception.AuctionException;
import com.server.service.AuctionService;
import com.shared.dto.AuctionDetailDTO;
import com.shared.dto.AuctionUpdateDTO;
import com.shared.dto.BidRequestDTO;
import com.shared.network.Response;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Kích hoạt Mockito
public class AuctionControllerTest {

    @Mock
    private AuctionService auctionService; // Service giả

    @Mock
    private Context ctx; // Context giả của Javalin

    private AuctionController auctionController; // Controller thật cần test
    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() {
        // Khởi tạo controller với service giả trước mỗi test
        auctionController = new AuctionController(auctionService);
    }

    @Test
    void placeBid_whenValidRequest_shouldCallServiceAndReturnSuccess() throws AuctionException {
        // Arrange (Chuẩn bị)
        BidRequestDTO bidRequest = new BidRequestDTO(1L, 101L, new BigDecimal("1500.00"), false, null);
        String requestJson = gson.toJson(bidRequest);

        AuctionUpdateDTO auctionUpdate = new AuctionUpdateDTO(1L, new BigDecimal("1500.00"), "User 101", 3600000L);

        // Dạy cho Context giả: "Khi ai đó hỏi body, hãy trả về chuỗi JSON này"
        when(ctx.body()).thenReturn(requestJson);

        // Dạy cho Service giả: "Khi ai đó gọi placeBid, hãy trả về đối tượng này"
        when(auctionService.placeBid(any(BidRequestDTO.class))).thenReturn(auctionUpdate);

        // Act (Hành động)
        auctionController.placeBid(ctx);

        // Assert (Kiểm tra)
        // 1. Kiểm tra xem service có được gọi với đúng dữ liệu không
        ArgumentCaptor<BidRequestDTO> bidRequestCaptor = ArgumentCaptor.forClass(BidRequestDTO.class);
        verify(auctionService).placeBid(bidRequestCaptor.capture());
        assertEquals(1L, bidRequestCaptor.getValue().getAuctionId());

        // 2. Kiểm tra xem controller có trả về đúng JSON thành công không
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).json(responseCaptor.capture());
        assertEquals("SUCCESS", responseCaptor.getValue().getStatus());
        assertEquals(auctionUpdate, responseCaptor.getValue().getData());
    }

    @Test
    void placeBid_whenServiceThrowsException_shouldSetCorrectStatusAndErrorResponse() throws AuctionException {
        // Arrange
        BidRequestDTO bidRequest = new BidRequestDTO(1L, 101L, new BigDecimal("900.00"), false, null);
        String requestJson = gson.toJson(bidRequest);

        // Dạy cho Context giả
        when(ctx.body()).thenReturn(requestJson);

        // Dạy cho Context trả về chính nó khi gọi status()
        when(ctx.status(anyInt())).thenReturn(ctx);

        // Dạy cho Service giả: "Khi ai đó gọi placeBid, hãy ném ra lỗi này"
        AuctionException exception = new AuctionException(AuctionException.ErrorCode.BID_AMOUNT_TOO_LOW);
        when(auctionService.placeBid(any(BidRequestDTO.class))).thenThrow(exception);

        // Act
        auctionController.placeBid(ctx);

        // Assert
        // 1. Kiểm tra xem controller có set đúng mã lỗi HTTP không (400 Bad Request)
        verify(ctx).status(400);

        // 2. Kiểm tra xem controller có trả về đúng JSON lỗi không
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).json(responseCaptor.capture());
        assertEquals("ERROR", responseCaptor.getValue().getStatus());
        assertEquals(exception.getMessage(), responseCaptor.getValue().getMessage());
    }

    @Test
    void getAuctionDetail_whenAuctionExists_shouldReturnDetail() throws AuctionException {
        // Arrange
        long auctionId = 1L;
        AuctionDetailDTO auctionDetail = new AuctionDetailDTO(
                auctionId, 202L, "Vintage Watch", "Description", new BigDecimal("1200.00"), "User 100", 1800000L, new BigDecimal("50.00"), "ELECTRONICS", new HashMap<>(), Collections.emptyList(), "SellerName"
        );

        // Dạy cho Context giả
        when(ctx.pathParam("auctionId")).thenReturn(String.valueOf(auctionId));

        // Dạy cho Service giả
        when(auctionService.getAuctionDetail(auctionId)).thenReturn(auctionDetail);

        // Act
        auctionController.getAuctionDetail(ctx);

        // Assert
        verify(auctionService).getAuctionDetail(auctionId);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).json(responseCaptor.capture());
        assertEquals("SUCCESS", responseCaptor.getValue().getStatus());
        assertEquals(auctionDetail, responseCaptor.getValue().getData());
    }

    @Test
    void getAuctionDetail_whenAuctionNotFound_shouldSet404Status() throws AuctionException {
        // Arrange
        long nonExistentAuctionId = 999L;

        // Dạy cho Context giả
        when(ctx.pathParam("auctionId")).thenReturn(String.valueOf(nonExistentAuctionId));

        // Dạy cho Context trả về chính nó khi gọi status()
        when(ctx.status(anyInt())).thenReturn(ctx);

        // Dạy cho Service giả
        when(auctionService.getAuctionDetail(nonExistentAuctionId))
                .thenThrow(new AuctionException(AuctionException.ErrorCode.AUCTION_NOT_FOUND));

        // Act
        auctionController.getAuctionDetail(ctx);

        // Assert
        verify(ctx).status(404); // Mong đợi HTTP 404 Not Found
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).json(responseCaptor.capture());
        assertEquals("ERROR", responseCaptor.getValue().getStatus());
    }
}
