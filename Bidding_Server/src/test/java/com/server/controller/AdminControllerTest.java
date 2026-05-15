package com.server.controller;

import com.google.gson.Gson;
import com.server.service.AdminService;
import com.shared.network.Response;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @Mock
    private Context ctx;

    private AdminController adminController;
    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() {
        adminController = new AdminController(adminService);
    }

    @Test
    void getAllUsers_shouldCallServiceAndReturnResponse() {
        // Arrange
        Response mockResponse = new Response("SUCCESS", "Danh sách người dùng", null);
        when(adminService.getAllUsers()).thenReturn(mockResponse);

        // Act
        adminController.getAllUsers(ctx);

        // Assert
        verify(adminService).getAllUsers();
        verify(ctx).json(mockResponse);
    }

    @Test
    void banUser_whenValidRequest_shouldCallServiceAndReturnResponse() {
        // Arrange
        String username = "testuser";
        String requestJson = gson.toJson(Map.of("username", username));
        Response mockResponse = new Response("SUCCESS", "User banned", null);

        when(ctx.body()).thenReturn(requestJson);
        when(adminService.camTaiKhoan(username)).thenReturn(mockResponse);

        // Act
        adminController.banUser(ctx);

        // Assert
        verify(adminService).camTaiKhoan(username);
        verify(ctx).json(mockResponse);
    }

    @Test
    void banUser_whenUsernameIsMissing_shouldReturnError() {
        // Arrange
        String requestJson = gson.toJson(Map.of()); // Empty map
        when(ctx.body()).thenReturn(requestJson);

        // Act
        adminController.banUser(ctx);

        // Assert
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).json(responseCaptor.capture());
        assertEquals("FAIL", responseCaptor.getValue().getStatus());
        assertEquals("Username is required.", responseCaptor.getValue().getMessage());
        verify(adminService, never()).camTaiKhoan(anyString());
    }

    @Test
    void deleteItem_whenValidId_shouldCallServiceAndReturnResponse() {
        // Arrange
        long itemId = 1L;
        Response mockResponse = new Response("SUCCESS", "Item deleted successfully", null);

        when(ctx.pathParam("itemId")).thenReturn(String.valueOf(itemId));
        when(adminService.deleteItem(itemId)).thenReturn(mockResponse);

        // Act
        adminController.deleteItem(ctx);

        // Assert
        verify(adminService).deleteItem(itemId);
        verify(ctx).json(mockResponse);
    }
}
