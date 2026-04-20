package com.server.service;

import com.server.DAO.UserRepository;
import com.server.exception.*;
import com.server.model.Bidder;
import com.server.model.Role;
import com.server.model.Status;
import com.shared.dto.LoginRequestDTO;
import com.shared.dto.RegisterRequestDTO;
import com.shared.dto.UserProfileResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho AuthService sử dụng JUnit 5 và Mockito
 * Kiểm tra logic đăng ký, đăng nhập, và xác thực người dùng
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Xác thực và đăng ký người dùng")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private RegisterRequestDTO registerRequest;
    private LoginRequestDTO loginRequest;
    private Bidder testBidder;

    @BeforeEach
    void setUp() {
        // Setup test data - LoginRequestDTO có constructor với 2 tham số
        loginRequest = new LoginRequestDTO("testuser", "password123");

        // Setup RegisterRequestDTO - có constructor với 4 tham số
        registerRequest = new RegisterRequestDTO("testuser", "password123", "test@example.com", "Test User");

        // Setup test Bidder từ Register (simulating what AuthService creates)
        testBidder = new Bidder("testuser", "password123", "test@example.com", "Test User");
    }

    // ==================== REGISTER TESTS ====================

    @Test
    @DisplayName("Register: Đăng ký thành công với dữ liệu hợp lệ")
    void testRegisterSuccess() {
        // Setup
        when(userRepository.getUserByUsername("testuser")).thenReturn(null);
        when(userRepository.saveUser(any(Bidder.class))).thenReturn(true);

        // Execute & Assert: Không throw exception = thành công
        assertDoesNotThrow(() -> authService.register(registerRequest));

        // Verify: saveUser được gọi đúng 1 lần với Bidder object
        verify(userRepository, times(1)).getUserByUsername("testuser");
        verify(userRepository, times(1)).saveUser(any(Bidder.class));
    }

    @Test
    @DisplayName("Register: Throw AuthValidationException khi DTO null")
    void testRegisterWithNullDTO() {
        assertThrows(AuthValidationException.class, () -> authService.register(null));
        verify(userRepository, never()).saveUser(any());
    }

    @Test
    @DisplayName("Register: Throw AuthValidationException khi username null")
    void testRegisterWithNullUsername() {
        registerRequest = new RegisterRequestDTO(null, "pass", "email@test.com", "Name");

        assertThrows(AuthValidationException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).saveUser(any());
    }

    @Test
    @DisplayName("Register: Throw AuthValidationException khi username trắng")
    void testRegisterWithBlankUsername() {
        registerRequest = new RegisterRequestDTO("   ", "pass", "email@test.com", "Name");

        assertThrows(AuthValidationException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).saveUser(any());
    }

    @Test
    @DisplayName("Register: Throw AuthValidationException khi password null")
    void testRegisterWithNullPassword() {
        registerRequest = new RegisterRequestDTO("user", null, "email@test.com", "Name");

        assertThrows(AuthValidationException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).saveUser(any());
    }

    @Test
    @DisplayName("Register: Throw AuthValidationException khi email trắng")
    void testRegisterWithBlankEmail() {
        registerRequest = new RegisterRequestDTO("user", "pass", "  ", "Name");

        assertThrows(AuthValidationException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).saveUser(any());
    }

    @Test
    @DisplayName("Register: Throw AuthValidationException khi fullName null")
    void testRegisterWithNullFullName() {
        registerRequest = new RegisterRequestDTO("user", "pass", "email@test.com", null);

        assertThrows(AuthValidationException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).saveUser(any());
    }

    @Test
    @DisplayName("Register: Throw DuplicateUserException khi username đã tồn tại")
    void testRegisterWithDuplicateUsername() {
        // Setup: User đã tồn tại
        when(userRepository.getUserByUsername("testuser")).thenReturn(testBidder);

        // Execute & Assert
        assertThrows(DuplicateUserException.class, () -> authService.register(registerRequest));

        // Verify: saveUser không được gọi
        verify(userRepository, never()).saveUser(any());
    }

    @Test
    @DisplayName("Register: Throw AppException khi saveUser trả về false")
    void testRegisterWhenSaveFails() {
        // Setup
        when(userRepository.getUserByUsername("testuser")).thenReturn(null);
        when(userRepository.saveUser(any(Bidder.class))).thenReturn(false);

        // Execute & Assert
        assertThrows(AppException.class, () -> authService.register(registerRequest));
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("Login: Đăng nhập thành công với credentials đúng")
    void testLoginSuccess() {
        // Setup
        when(userRepository.getUserByUsername("testuser")).thenReturn(testBidder);

        // Execute
        UserProfileResponseDTO result = authService.login(loginRequest);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("Test User", result.getFullName());
        assertEquals("BIDDER", result.getRole());
    }

    @Test
    @DisplayName("Login: Throw AuthValidationException khi DTO null")
    void testLoginWithNullDTO() {
        assertThrows(AuthValidationException.class, () -> authService.login(null));
    }

    @Test
    @DisplayName("Login: Throw AuthValidationException khi username null")
    void testLoginWithNullUsername() {
        loginRequest = new LoginRequestDTO(null, "pass");

        assertThrows(AuthValidationException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Login: Throw AuthValidationException khi username trắng")
    void testLoginWithBlankUsername() {
        loginRequest = new LoginRequestDTO("  ", "pass");

        assertThrows(AuthValidationException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Login: Throw AuthValidationException khi password null")
    void testLoginWithNullPassword() {
        loginRequest = new LoginRequestDTO("testuser", null);

        assertThrows(AuthValidationException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Login: Throw AuthValidationException khi password trắng")
    void testLoginWithBlankPassword() {
        loginRequest = new LoginRequestDTO("testuser", "  ");

        assertThrows(AuthValidationException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Login: Throw UserNotFoundException khi user không tồn tại")
    void testLoginWithNonExistentUser() {
        // Setup
        when(userRepository.getUserByUsername("testuser")).thenReturn(null);

        // Execute & Assert
        assertThrows(UserNotFoundException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Login: Throw InvalidCredentialException khi password sai")
    void testLoginWithWrongPassword() {
        // Setup
        when(userRepository.getUserByUsername("testuser")).thenReturn(testBidder);
        loginRequest = new LoginRequestDTO("testuser", "wrongpassword");

        // Execute & Assert
        assertThrows(InvalidCredentialException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Login: Trả về profile với wallet balance từ Bidder")
    void testLoginReturnsCorrectWalletBalance() {
        // Setup
        Bidder bidderWithWallet = new Bidder("user", "pass", "email@test.com", "User");
        bidderWithWallet.setWalletBalance(new BigDecimal("5000.50"));

        when(userRepository.getUserByUsername("user")).thenReturn(bidderWithWallet);

        loginRequest = new LoginRequestDTO("user", "pass");

        // Execute
        UserProfileResponseDTO result = authService.login(loginRequest);

        // Assert
        assertEquals(5000.50, result.getWalletBalance());
    }
}
