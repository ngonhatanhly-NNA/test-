package com.server.service;

import com.server.DAO.UserRepository;
import com.server.exception.*;
import com.server.model.Bidder;
import com.server.model.Status;
import com.shared.dto.LoginRequestDTO;
import com.shared.dto.RegisterRequestDTO;
import com.shared.dto.UserProfileResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
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
        // Setup test data
        loginRequest = new LoginRequestDTO("testuser", "password123");
        registerRequest = new RegisterRequestDTO("testuser", "password123", "test@example.com", "Test User");

        // Hash password with BCrypt so login logic matches
        String hashedPassword = BCrypt.hashpw("password123", BCrypt.gensalt());
        // Sử dụng Constructor chính xác
        testBidder = new Bidder("testuser", hashedPassword, "test@example.com", "Test User");
        testBidder.setId(1L); // Cần có ID cho UserProfileResponseDTO
        testBidder.updateStatus(Status.ACTIVE);
    }

    // ==================== REGISTER TESTS ====================

    @Test
    @DisplayName("Register: Đăng ký thành công với dữ liệu hợp lệ")
    void testRegisterSuccess() {
        when(userRepository.getUserByUsername("testuser")).thenReturn(null);
        when(userRepository.saveUser(any(Bidder.class))).thenReturn(true);

        assertDoesNotThrow(() -> authService.register(registerRequest));
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
    @DisplayName("Register: Throw DuplicateUserException khi username đã tồn tại")
    void testRegisterWithDuplicateUsername() {
        when(userRepository.getUserByUsername("testuser")).thenReturn(testBidder);
        assertThrows(DuplicateUserException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).saveUser(any());
    }

    @Test
    @DisplayName("Register: Throw AppException khi saveUser trả về false")
    void testRegisterWhenSaveFails() {
        when(userRepository.getUserByUsername("testuser")).thenReturn(null);
        when(userRepository.saveUser(any(Bidder.class))).thenReturn(false);
        assertThrows(AppException.class, () -> authService.register(registerRequest));
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("Login: Đăng nhập thành công với credentials đúng")
    void testLoginSuccess() {
        // Đảm bảo mật khẩu lưu trữ trong Mock User khớp với password gửi lên
        String storedHash = BCrypt.hashpw("password123", BCrypt.gensalt());
        Bidder loginBidder = new Bidder("testuser", storedHash, "test@example.com", "Test User");
        loginBidder.setId(1L);
        loginBidder.updateStatus(Status.ACTIVE);

        when(userRepository.getUserByUsername("testuser")).thenReturn(loginBidder);
        UserProfileResponseDTO result = authService.login(loginRequest);

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
        when(userRepository.getUserByUsername("testuser")).thenReturn(null);
        assertThrows(UserNotFoundException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Login: Throw InvalidCredentialException khi password sai")
    void testLoginWithWrongPassword() {
        when(userRepository.getUserByUsername("testuser")).thenReturn(testBidder);
        loginRequest = new LoginRequestDTO("testuser", "wrongpassword");
        assertThrows(InvalidCredentialException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Login: Trả về profile với wallet balance từ Bidder")
    void testLoginReturnsCorrectWalletBalance() {
        String hashedPassword = BCrypt.hashpw("pass", BCrypt.gensalt());
        Bidder bidderWithWallet = new Bidder("user", hashedPassword, "email@test.com", "User");
        bidderWithWallet.setId(2L); // ID is needed
        bidderWithWallet.updateStatus(Status.ACTIVE);
        bidderWithWallet.setWalletBalance(new BigDecimal("5000.50"));

        when(userRepository.getUserByUsername("user")).thenReturn(bidderWithWallet);
        loginRequest = new LoginRequestDTO("user", "pass");

        UserProfileResponseDTO result = authService.login(loginRequest);
        assertEquals(5000.50, result.getWalletBalance());
    }
}
