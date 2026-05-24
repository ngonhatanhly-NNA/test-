package com.server.controller;

import com.server.service.AuctionService;
import com.shared.dto.AuctionDetailDTO;
import com.shared.network.Response;
import io.javalin.http.Context;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionController Tests")
public class AuctionControllerTest {

    @Mock
    private AuctionService auctionService;

    @Mock
    private Context ctx;

    @InjectMocks
    private AuctionController auctionController;

    @Test
    @DisplayName("getActiveAuctionsBySeller should return auctions for a valid seller")
    void getActiveAuctionsBySeller_whenValidSeller_shouldReturnAuctions() {
        // Arrange
        long sellerId = 1L;
        List<AuctionDetailDTO> expectedAuctions = Collections.singletonList(new AuctionDetailDTO());
        when(ctx.pathParam("sellerId")).thenReturn(String.valueOf(sellerId));
        when(auctionService.getActiveAuctionsBySeller(sellerId)).thenReturn(expectedAuctions);

        // Act
        auctionController.getActiveAuctionsBySeller(ctx);

        // Assert
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).json(responseCaptor.capture());
        Response response = responseCaptor.getValue();

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(expectedAuctions, response.getData());
    }

    @Test
    @DisplayName("getActiveAuctionsBySeller should return 400 for invalid seller ID")
    void getActiveAuctionsBySeller_whenInvalidSellerId_shouldReturn400() {
        // Arrange
        when(ctx.pathParam("sellerId")).thenReturn("abc");
        when(ctx.status(anyInt())).thenReturn(ctx); // Stubbing moved here

        // Act
        auctionController.getActiveAuctionsBySeller(ctx);

        // Assert
        verify(ctx).status(400);
        verify(ctx).json(any(Response.class));
    }

    @Test
    @DisplayName("getWonAuctionsByBidder should return won auctions for a valid bidder")
    void getWonAuctionsByBidder_whenValidBidder_shouldReturnWonAuctions() {
        // Arrange
        long bidderId = 2L;
        List<AuctionDetailDTO> expectedAuctions = Collections.singletonList(new AuctionDetailDTO());
        when(ctx.pathParam("bidderId")).thenReturn(String.valueOf(bidderId));
        when(auctionService.getWonAuctionsByBidder(bidderId)).thenReturn(expectedAuctions);

        // Act
        auctionController.getWonAuctionsByBidder(ctx);

        // Assert
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).json(responseCaptor.capture());
        Response response = responseCaptor.getValue();

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(expectedAuctions, response.getData());
    }

    @Test
    @DisplayName("getAuctionStatusByItemId should return status for a valid item")
    void getAuctionStatusByItemId_whenValidItem_shouldReturnStatus() {
        // Arrange
        long itemId = 3L;
        String expectedStatus = "ACTIVE";
        when(ctx.pathParam("itemId")).thenReturn(String.valueOf(itemId));
        when(auctionService.getAuctionStatusByItemId(itemId)).thenReturn(expectedStatus);

        // Act
        auctionController.getAuctionStatusByItemId(ctx);

        // Assert
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).json(responseCaptor.capture());
        Response response = responseCaptor.getValue();

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(expectedStatus, response.getData());
    }
}