package com.server.controller;

import com.google.gson.Gson;
import com.server.model.Role;
import com.server.security.JwtUtil;
import com.server.service.AuthService;
import com.shared.dto.LoginRequestDTO;
import com.shared.dto.RegisterRequestDTO;
import com.shared.dto.UserProfileResponseDTO;
import com.shared.dto.LoginResponseDTO;
import com.shared.network.Response;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private Context ctx;

    private AuthController authController;
    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService, jwtUtil);
    }

    @Test
    void processRegisterRest_whenValidRequest_shouldCallServiceAndReturnSuccess() {
        // Arrange
        RegisterRequestDTO registerRequest = new RegisterRequestDTO("newuser", "password123", "newuser@example.com", "New User");
        String requestJson = gson.toJson(registerRequest);

        when(ctx.body()).thenReturn(requestJson);
        doNothing().when(authService).register(any(RegisterRequestDTO.class));

        // Act
        authController.processRegisterRest(ctx);

        // Assert
        verify(authService).register(any(RegisterRequestDTO.class));
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).json(responseCaptor.capture());
        assertEquals("SUCCESS", responseCaptor.getValue().getStatus());
        assertEquals("Đăng ký thành công!", responseCaptor.getValue().getMessage());
    }

    @Test
    void processRegisterRest_whenServiceThrowsException_shouldSet400StatusAndReturnError() {
        // Arrange
        RegisterRequestDTO registerRequest = new RegisterRequestDTO("existinguser", "password123", "existing@example.com", "Existing");
        String requestJson = gson.toJson(registerRequest);

        when(ctx.body()).thenReturn(requestJson);
        doThrow(new RuntimeException("Username đã tồn tại")).when(authService).register(any(RegisterRequestDTO.class));
        when(ctx.status(400)).thenReturn(ctx);

        // Act
        authController.processRegisterRest(ctx);

        // Assert
        verify(ctx).status(400);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).json(responseCaptor.capture());
        assertEquals("ERROR", responseCaptor.getValue().getStatus());
        assertEquals("Username đã tồn tại", responseCaptor.getValue().getMessage());
    }

    @Test
    void processLoginRest_whenValidCredentials_shouldReturnTokenAndProfile() throws Exception {
        // Arrange
        LoginRequestDTO loginRequest = new LoginRequestDTO("testuser", "password123");
        String requestJson = gson.toJson(loginRequest);

        UserProfileResponseDTO mockProfile = new UserProfileResponseDTO(
                1L, "testuser", "test@test.com", "Test User", "123", "Addr", "BIDDER", 100.0
        );

        String mockToken = "mock.jwt.token";

        when(ctx.body()).thenReturn(requestJson);
        when(authService.login(any(LoginRequestDTO.class))).thenReturn(mockProfile);
        when(jwtUtil.generateToken(1L, "testuser", Role.BIDDER)).thenReturn(mockToken);

        // Act
        authController.processLoginRest(ctx);

        // Assert
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).json(responseCaptor.capture());
        assertEquals("SUCCESS", responseCaptor.getValue().getStatus());

        LoginResponseDTO responseData = (LoginResponseDTO) responseCaptor.getValue().getData();
        assertEquals(mockToken, responseData.getToken());
        assertEquals("testuser", responseData.getProfile().getUsername());
    }

    @Test
    void getUserProfile_whenAuthenticated_shouldReturnProfile() throws Exception {
        // Arrange
        String username = "testuser";
        UserProfileResponseDTO mockProfile = new UserProfileResponseDTO(
                1L, "testuser", "test@test.com", "Test User", "123", "Addr", "BIDDER", 100.0
        );

        when(ctx.attribute("auth.username")).thenReturn(username);
        when(ctx.queryParam("username")).thenReturn(null);
        when(authService.getUserProfile(username)).thenReturn(mockProfile);

        // Act
        authController.getUserProfile(ctx);

        // Assert
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).json(responseCaptor.capture());
        assertEquals("SUCCESS", responseCaptor.getValue().getStatus());
        assertEquals(mockProfile, responseCaptor.getValue().getData());
    }

    @Test
    void getUserProfile_whenNotAuthenticated_shouldReturn401() throws Exception {
        // Arrange
        when(ctx.attribute("auth.username")).thenReturn(null);
        when(ctx.status(401)).thenReturn(ctx);

        // Act
        authController.getUserProfile(ctx);

        // Assert
        verify(ctx).status(401);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).json(responseCaptor.capture());
        assertEquals("FAIL", responseCaptor.getValue().getStatus());
    }
}
