package com.server.controller;

import com.server.service.AuthService;
import com.google.gson.Gson;
import com.shared.dto.LoginRequestDTO;
import com.shared.dto.RegisterRequestDTO;
import com.shared.dto.UserProfileResponseDTO;
import com.shared.network.Response;
import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho AuthController sử dụng JUnit 5 và Mockito
 * Kiểm tra xử lý HTTP requests cho đăng ký và đăng nhập
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController - Xử lý HTTP requests cho xác thực")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private Context context;

    @InjectMocks
    private AuthController authController;

    private Gson gson = new Gson();
    private RegisterRequestDTO registerRequest;
    private LoginRequestDTO loginRequest;
    private UserProfileResponseDTO userProfile;

    @BeforeEach
    void setUp() {
        // Setup test data - Sử dụng constructors thật từ DTOs
        registerRequest = new RegisterRequestDTO("testuser", "password123", "test@example.com", "Test User");
        loginRequest = new LoginRequestDTO("testuser", "password123");

        // Setup user profile response
        userProfile = new UserProfileResponseDTO(
                1L,                    // id (long, không phải int)
                "testuser",            // username
                "test@example.com",    // email
                "Test User",           // fullName
                "0987654321",          // phoneNumber
                "123 Test Street",     // address
                "BIDDER",              // role
                1000.0                 // walletBalance
        );
    }

    // ==================== REGISTER TESTS ====================

    @Test
    @DisplayName("Register: Xử lý register request thành công")
    void testProcessRegisterSuccess() {
        // Setup
        when(context.body()).thenReturn(gson.toJson(registerRequest));
        doNothing().when(authService).register(any(RegisterRequestDTO.class));

        // Execute
        assertDoesNotThrow(() -> authController.processRegisterRest(context));

        // Verify
        verify(authService, times(1)).register(any(RegisterRequestDTO.class));
        verify(context, times(1)).json(any(Response.class));
    }

    @Test
    @DisplayName("Register: Response có status SUCCESS")
    void testProcessRegisterResponseStatus() {
        // Setup
        when(context.body()).thenReturn(gson.toJson(registerRequest));
        doNothing().when(authService).register(any(RegisterRequestDTO.class));

        // Execute
        authController.processRegisterRest(context);

        // Verify: Capture response và kiểm tra status
        verify(context).json(argThat(response ->
                response instanceof Response &&
                "SUCCESS".equals(((Response) response).getStatus())
        ));
    }

    @Test
    @DisplayName("Register: Xử lý AuthValidationException từ Service")
    void testProcessRegisterWithValidationException() {
        // Setup
        when(context.body()).thenReturn(gson.toJson(registerRequest));
        doThrow(new com.server.exception.AuthValidationException("Dữ liệu không hợp lệ"))
                .when(authService).register(any(RegisterRequestDTO.class));

        // Execute & Assert: Exception propagate từ Service
        assertThrows(com.server.exception.AuthValidationException.class,
                () -> authController.processRegisterRest(context));
    }

    @Test
    @DisplayName("Register: Xử lý DuplicateUserException từ Service")
    void testProcessRegisterWithDuplicateException() {
        // Setup
        when(context.body()).thenReturn(gson.toJson(registerRequest));
        doThrow(new com.server.exception.DuplicateUserException(
                com.server.exception.DuplicateUserException.ErrorCode.USERNAME_EXISTED, "testuser"
        )).when(authService).register(any(RegisterRequestDTO.class));

        // Execute & Assert
        assertThrows(com.server.exception.DuplicateUserException.class,
                () -> authController.processRegisterRest(context));
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("Login: Xử lý login request thành công")
    void testProcessLoginSuccess() throws Exception {
        // Setup
        when(context.body()).thenReturn(gson.toJson(loginRequest));
        when(authService.login(any(LoginRequestDTO.class))).thenReturn(userProfile);

        // Execute
        authController.processLoginRest(context);

        // Verify
        verify(authService, times(1)).login(any(LoginRequestDTO.class));
        verify(context, times(1)).sessionAttribute("username", "testuser");
        verify(context, times(1)).json(any(Response.class));
    }

    @Test
    @DisplayName("Login: Response có status SUCCESS và chứa user profile")
    void testProcessLoginResponseContainsProfile() throws Exception {
        // Setup
        when(context.body()).thenReturn(gson.toJson(loginRequest));
        when(authService.login(any(LoginRequestDTO.class))).thenReturn(userProfile);

        // Execute
        authController.processLoginRest(context);

        // Verify: Response chứa user profile data
        verify(context).json(argThat(response ->
                response instanceof Response &&
                "SUCCESS".equals(((Response) response).getStatus()) &&
                ((Response) response).getData() instanceof UserProfileResponseDTO
        ));
    }

    @Test
    @DisplayName("Login: Session attribute username được set đúng")
    void testProcessLoginSessionAttribute() throws Exception {
        // Setup
        when(context.body()).thenReturn(gson.toJson(loginRequest));
        when(authService.login(any(LoginRequestDTO.class))).thenReturn(userProfile);

        // Execute
        authController.processLoginRest(context);

        // Verify
        verify(context).sessionAttribute("username", "testuser");
    }

    @Test
    @DisplayName("Login: Xử lý UserNotFoundException từ Service")
    void testProcessLoginWithUserNotFound() throws Exception {
        // Setup
        when(context.body()).thenReturn(gson.toJson(loginRequest));
        doThrow(new com.server.exception.UserNotFoundException("testuser"))
                .when(authService).login(any(LoginRequestDTO.class));

        // Execute & Assert
        assertThrows(com.server.exception.UserNotFoundException.class,
                () -> authController.processLoginRest(context));

        // Verify: sessionAttribute không được gọi
        verify(context, never()).sessionAttribute(anyString(), anyString());
    }

    @Test
    @DisplayName("Login: Xử lý InvalidCredentialException từ Service")
    void testProcessLoginWithInvalidCredentials() throws Exception {
        // Setup
        when(context.body()).thenReturn(gson.toJson(loginRequest));
        doThrow(new com.server.exception.InvalidCredentialException())
                .when(authService).login(any(LoginRequestDTO.class));

        // Execute & Assert
        assertThrows(com.server.exception.InvalidCredentialException.class,
                () -> authController.processLoginRest(context));
    }

    @Test
    @DisplayName("Login: Xử lý AuthValidationException từ Service")
    void testProcessLoginWithValidationException() throws Exception {
        // Setup
        when(context.body()).thenReturn(gson.toJson(loginRequest));
        doThrow(new com.server.exception.AuthValidationException("Username hoặc password rỗng"))
                .when(authService).login(any(LoginRequestDTO.class));

        // Execute & Assert
        assertThrows(com.server.exception.AuthValidationException.class,
                () -> authController.processLoginRest(context));
    }
}


