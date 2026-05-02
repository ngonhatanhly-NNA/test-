package com.server.controller;

import com.server.service.SellerService;
import com.shared.network.Response;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SellerControllerTest {

    @Mock
    private SellerService sellerService;

    @Mock
    private Context ctx;

    private SellerController sellerController;

    @BeforeEach
    void setUp() {
        sellerController = new SellerController(sellerService);
    }

    @Test
    void getSellerById_whenValidId_shouldReturnSellerInfo() {
        // Arrange
        long sellerId = 100L;
        Response mockResponse = new Response("SUCCESS", "Lấy thông tin thành công", null);

        when(ctx.pathParam("sellerId")).thenReturn(String.valueOf(sellerId));
        when(sellerService.getSellerDetails(sellerId)).thenReturn(mockResponse);

        // Act
        sellerController.getSellerById(ctx);

        // Assert
        verify(sellerService).getSellerDetails(sellerId);
        verify(ctx).json(mockResponse);
    }

    @Test
    void getSellerById_whenInvalidId_shouldReturn400Error() {
        // Arrange
        when(ctx.pathParam("sellerId")).thenReturn("abc");
        when(ctx.status(400)).thenReturn(ctx);

        // Act
        sellerController.getSellerById(ctx);

        // Assert
        verify(ctx).status(400);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).json(responseCaptor.capture());
        assertEquals("ERROR", responseCaptor.getValue().getStatus());
        assertEquals("Seller ID không hợp lệ", responseCaptor.getValue().getMessage());
        verify(sellerService, never()).getSellerDetails(anyLong());
    }

    @Test
    void getSellerItems_whenValidId_shouldReturnItems() {
        // Arrange
        long sellerId = 100L;
        Response mockResponse = new Response("SUCCESS", "Lấy items thành công", Collections.emptyList());

        when(ctx.pathParam("sellerId")).thenReturn(String.valueOf(sellerId));
        when(sellerService.getSellerItems(sellerId)).thenReturn(mockResponse);

        // Act
        sellerController.getSellerItems(ctx);

        // Assert
        verify(sellerService).getSellerItems(sellerId);
        verify(ctx).json(mockResponse);
    }
}
