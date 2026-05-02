package com.server.controller;

import com.google.gson.Gson;
import com.server.service.UserService;
import com.shared.network.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private UserService userService;

    private UserController userController;
    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() {
        userController = new UserController(userService);
    }

    @Test
    void handleUpgradeToSeller_shouldCallServiceAndReturnResult() {
        // Arrange
        String username = "testuser";
        Response mockResponse = new Response("SUCCESS", "Yêu cầu nâng cấp thành công", null);
        String expectedResult = gson.toJson(mockResponse);

        when(userService.requestSellerUpgrade(username)).thenReturn(mockResponse);

        // Act
        String result = userController.handleUpgradeToSeller(username);

        // Assert
        verify(userService).requestSellerUpgrade(username);
        assertEquals(expectedResult, result);
    }
}
