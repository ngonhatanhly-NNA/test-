package com.server.service;

import com.server.DAO.UserRepository;
import com.server.model.Bidder;
import com.shared.network.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("Kiểm thử Dịch vụ Người dùng (UserService)")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private AutoCloseable closeable;

    private Bidder existingBidder;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);

        existingBidder = new Bidder("testuser", "password123", "test@example.com", "Test User");
        existingBidder.setId(1L);
        existingBidder.setWalletBalance(new BigDecimal("100.00"));
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Nested
    @DisplayName("Logic Lấy Hồ sơ Người dùng")
    class GetUserProfileTests {

        @Test
        @DisplayName("Nên trả về hồ sơ người dùng khi tìm thấy theo tên người dùng")
        @SuppressWarnings("unchecked")
        void getUserProfile_returnsProfile_whenUserFoundByUsername() {
            when(userRepository.getUserByUsername(anyString())).thenReturn(existingBidder);

            Response response = userService.getUserProfile("testuser");

            assertNotNull(response);
            assertEquals("SUCCESS", response.getStatus());
            assertNotNull(response.getData());

            Map<String, Object> userProfile = (Map<String, Object>) response.getData();
            assertEquals(existingBidder.getId(), userProfile.get("id"));
            assertEquals(existingBidder.getUsername(), userProfile.get("username"));
            verify(userRepository, times(1)).getUserByUsername("testuser");
        }

        @Test
        @DisplayName("Nên trả về lỗi khi không tìm thấy người dùng theo tên người dùng")
        void getUserProfile_returnsFail_whenUserNotFoundByUsername() {
            when(userRepository.getUserByUsername(anyString())).thenReturn(null);

            Response response = userService.getUserProfile("nonexistent");

            assertNotNull(response);
            assertEquals("FAIL", response.getStatus());
            assertNull(response.getData());
            verify(userRepository, times(1)).getUserByUsername("nonexistent");
        }
    }
}