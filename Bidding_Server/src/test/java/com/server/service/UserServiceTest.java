package com.server.service;

import com.server.DAO.UserRepository;
import com.server.model.Admin;
import com.server.model.Bidder;
import com.server.model.Role;
import com.server.model.Seller;
import com.server.model.Status;
import com.server.model.User;
import com.shared.dto.BidderProfileUpdateDTO;
import com.shared.network.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private Bidder testBidder;

    @BeforeEach
    void setUp() {
        testBidder = new Bidder(
                1L, "bidder1", "pass", "bidder@test.com", "Test Bidder",
                "123456", "Address", Status.ACTIVE, Role.BIDDER, new BigDecimal("100"), "1234"
        );
    }

    @Test
    @DisplayName("Lấy Role người dùng thành công")
    void getUserRole_shouldReturnRole_whenUserExists() {
        when(userRepository.getUserByUsername("bidder1")).thenReturn(testBidder);
        Role role = userService.getUserRole("bidder1");
        assertEquals(Role.BIDDER, role);
    }

    @Test
    @DisplayName("Lấy Role người dùng trả về null khi không tìm thấy")
    void getUserRole_shouldReturnNull_whenUserNotFound() {
        when(userRepository.getUserByUsername("unknown")).thenReturn(null);
        Role role = userService.getUserRole("unknown");
        assertNull(role);
    }

    @Test
    @DisplayName("Lấy profile người dùng thành công")
    void getUserProfile_shouldReturnSuccess_whenUserExists() {
        when(userRepository.getUserByUsername("bidder1")).thenReturn(testBidder);
        Response response = userService.getUserProfile("bidder1");
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getData());
    }
    
    @Test
    @DisplayName("Lấy profile Admin thành công")
    void getUserProfile_shouldReturnSuccess_forAdmin() {
        Admin admin = new Admin(1L, "admin", "hash", "email", "Admin Name", "123", "Address", Status.ACTIVE, "SUPER");
        when(userRepository.getUserByUsername("admin")).thenReturn(admin);
        Response response = userService.getUserProfile("admin");
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getData());
    }

    @Test
    @DisplayName("Lấy profile Seller thành công")
    void getUserProfile_shouldReturnSuccess_forSeller() {
        Seller seller = new Seller(testBidder, "Shop", "123");
        when(userRepository.getUserByUsername("seller")).thenReturn(seller);
        Response response = userService.getUserProfile("seller");
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getData());
    }

    @Test
    @DisplayName("Lấy profile thất bại khi user không tồn tại")
    void getUserProfile_shouldReturnFail_whenUserNotFound() {
        when(userRepository.getUserByUsername("unknown")).thenReturn(null);
        Response response = userService.getUserProfile("unknown");
        assertEquals("FAIL", response.getStatus());
    }

    @Test
    @DisplayName("Lấy profile thất bại do exception")
    void getUserProfile_shouldReturnError_onException() {
        when(userRepository.getUserByUsername("bidder1")).thenThrow(new RuntimeException("DB Error"));
        Response response = userService.getUserProfile("bidder1");
        assertEquals("ERROR", response.getStatus());
    }

    @Test
    @DisplayName("Lấy tất cả users thành công")
    void getAllUsers_shouldReturnSuccess() {
        Admin admin = new Admin(2L, "admin", "hash", "email", "Admin Name", "123", "Address", Status.ACTIVE, "SUPER");
        when(userRepository.getAllUsers()).thenReturn(Arrays.asList(testBidder, admin));
        Response response = userService.getAllUsers();
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getData());
    }
    
    @Test
    @DisplayName("Lấy tất cả users trả về fail khi danh sách trống")
    void getAllUsers_shouldReturnFail_whenListEmpty() {
        when(userRepository.getAllUsers()).thenReturn(Collections.emptyList());
        Response response = userService.getAllUsers();
        assertEquals("FAIL", response.getStatus());
    }
    
    @Test
    @DisplayName("Lấy tất cả users trả về error khi có lỗi DB")
    void getAllUsers_shouldReturnError_onException() {
        when(userRepository.getAllUsers()).thenThrow(new RuntimeException("DB Error"));
        Response response = userService.getAllUsers();
        assertEquals("ERROR", response.getStatus());
    }

    @Test
    @DisplayName("Cập nhật profile thành công")
    void updateProfile_shouldReturnSuccess() {
        when(userRepository.getUserByUsername("bidder1")).thenReturn(testBidder);
        when(userRepository.updateFullProfile(any(), any(), anyLong())).thenReturn(true);
        
        BidderProfileUpdateDTO updateDTO = new BidderProfileUpdateDTO();
        Response response = userService.updateProfile("bidder1", updateDTO);
        
        assertEquals("SUCCESS", response.getStatus());
    }
    
    @Test
    @DisplayName("Cập nhật profile thất bại do lỗi DB")
    void updateProfile_shouldReturnFail_onDbError() {
        when(userRepository.getUserByUsername("bidder1")).thenReturn(testBidder);
        when(userRepository.updateFullProfile(any(), any(), anyLong())).thenReturn(false);
        
        BidderProfileUpdateDTO updateDTO = new BidderProfileUpdateDTO();
        Response response = userService.updateProfile("bidder1", updateDTO);
        
        assertEquals("FAIL", response.getStatus());
    }

    @Test
    @DisplayName("Cập nhật profile thất bại khi user không tồn tại")
    void updateProfile_shouldReturnFail_whenUserNotFound() {
        when(userRepository.getUserByUsername("unknown")).thenReturn(null);
        Response response = userService.updateProfile("unknown", new BidderProfileUpdateDTO());
        assertEquals("FAIL", response.getStatus());
    }

    @Test
    @DisplayName("Đổi mật khẩu thành công")
    void changePassword_shouldReturnSuccess() {
        String hashed = BCrypt.hashpw("oldpass", BCrypt.gensalt());
        Bidder user = new Bidder("user", hashed, "email", "name");
        when(userRepository.getUserByUsername("user")).thenReturn(user);
        when(userRepository.updatePassword(eq("user"), anyString())).thenReturn(true);

        Response response = userService.changePassword("user", "oldpass", "newpass");
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    @DisplayName("Đổi mật khẩu thất bại do mật khẩu cũ sai")
    void changePassword_shouldReturnFail_whenOldPasswordIncorrect() {
        String hashed = BCrypt.hashpw("oldpass", BCrypt.gensalt());
        Bidder user = new Bidder("user", hashed, "email", "name");
        when(userRepository.getUserByUsername("user")).thenReturn(user);

        Response response = userService.changePassword("user", "wrong", "newpass");
        assertEquals("FAIL", response.getStatus());
    }
    
    @Test
    @DisplayName("Đổi mật khẩu thất bại do lỗi DB")
    void changePassword_shouldReturnFail_onDbError() {
        String hashed = BCrypt.hashpw("oldpass", BCrypt.gensalt());
        Bidder user = new Bidder("user", hashed, "email", "name");
        when(userRepository.getUserByUsername("user")).thenReturn(user);
        when(userRepository.updatePassword(eq("user"), anyString())).thenReturn(false);

        Response response = userService.changePassword("user", "oldpass", "newpass");
        assertEquals("FAIL", response.getStatus());
    }
    
    @Test
    @DisplayName("Cập nhật avatar thành công")
    void updateAvatarUrl_shouldReturnSuccess() {
        when(userRepository.updateAvatarUrl("testuser", "url")).thenReturn(true);
        Response response = userService.updateAvatarUrl("testuser", "url");
        assertEquals("SUCCESS", response.getStatus());
    }
    
    @Test
    @DisplayName("Cập nhật avatar thất bại")
    void updateAvatarUrl_shouldReturnFail() {
        when(userRepository.updateAvatarUrl("testuser", "url")).thenReturn(false);
        Response response = userService.updateAvatarUrl("testuser", "url");
        assertEquals("FAIL", response.getStatus());
    }

    @Test
    @DisplayName("Yêu cầu nâng cấp seller thành công")
    void requestSellerUpgrade_shouldReturnSuccess() {
        when(userRepository.getUserByUsername("bidder1")).thenReturn(testBidder);
        when(userRepository.updateUserRole("bidder1", Role.SELLER)).thenReturn(true);
        
        Response response = userService.requestSellerUpgrade("bidder1");
        
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    @DisplayName("Yêu cầu nâng cấp seller thất bại do không phải Bidder")
    void requestSellerUpgrade_shouldReturnFail_whenNotBidder() {
        Admin admin = new Admin();
        admin.setRole(Role.ADMIN);
        when(userRepository.getUserByUsername("admin")).thenReturn(admin);
        
        Response response = userService.requestSellerUpgrade("admin");
        
        assertEquals("FAIL", response.getStatus());
    }

    @Test
    @DisplayName("Yêu cầu nâng cấp seller thất bại khi user không tồn tại")
    void requestSellerUpgrade_shouldReturnFail_whenUserNotFound() {
        when(userRepository.getUserByUsername("unknown")).thenReturn(null);
        Response response = userService.requestSellerUpgrade("unknown");
        assertEquals("FAIL", response.getStatus());
    }

    @Test
    @DisplayName("Yêu cầu nâng cấp seller thất bại do lỗi DB")
    void requestSellerUpgrade_shouldReturnError_whenUpdateFails() {
        when(userRepository.getUserByUsername("bidder1")).thenReturn(testBidder);
        when(userRepository.updateUserRole("bidder1", Role.SELLER)).thenReturn(false);
        Response response = userService.requestSellerUpgrade("bidder1");
        assertEquals("ERROR", response.getStatus());
    }
}