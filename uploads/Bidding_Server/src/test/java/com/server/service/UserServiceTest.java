package com.server.service;

import com.server.DAO.UserRepository;
import com.server.model.Bidder;
import com.server.model.Role;
import com.server.model.Status;
import com.shared.network.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private Bidder testBidder;

    @BeforeEach
    void setUp() {
        testBidder = new Bidder(
                1L, "bidder1", "pass", "bidder@test.com", "Test Bidder",
                "123456", "Address", Status.ACTIVE, Role.BIDDER, BigDecimal.ZERO, "1234"
        );
    }

    @Test
    void requestSellerUpgrade_whenUserExistsAndIsBidder_shouldReturnSuccess() {
        // Arrange
        when(userRepository.getUserByUsername("bidder1")).thenReturn(testBidder);
        when(userRepository.updateUserRole("bidder1", Role.SELLER)).thenReturn(true);

        // Act
        Response response = userService.requestSellerUpgrade("bidder1");

        // Assert
        assertEquals("SUCCESS", response.getStatus());
        verify(userRepository).updateUserRole("bidder1", Role.SELLER);
    }

    @Test
    void requestSellerUpgrade_whenUserNotFound_shouldReturnFail() {
        // Arrange
        when(userRepository.getUserByUsername("unknown")).thenReturn(null);

        // Act
        Response response = userService.requestSellerUpgrade("unknown");

        // Assert
        assertEquals("FAIL", response.getStatus());
        assertEquals("Người dùng không tồn tại.", response.getMessage());
        verify(userRepository, never()).updateUserRole(anyString(), any());
    }

    @Test
    void requestSellerUpgrade_whenUserNotBidder_shouldReturnFail() {
        // Arrange
        testBidder.setRole(Role.ADMIN); // Change role to something else
        when(userRepository.getUserByUsername("admin1")).thenReturn(testBidder);

        // Act
        Response response = userService.requestSellerUpgrade("admin1");

        // Assert
        assertEquals("FAIL", response.getStatus());
        assertEquals("Chỉ có Người mua (Bidder) mới có thể yêu cầu trở thành Người bán.", response.getMessage());
        verify(userRepository, never()).updateUserRole(anyString(), any());
    }
}