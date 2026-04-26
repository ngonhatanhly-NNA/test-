package com.server.controller;

import com.google.gson.Gson;
import com.server.model.Role;
import com.server.service.UserService;
import com.shared.dto.*;
import com.shared.network.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private UserService userService; // Giả lập UserService

    private UserController userController; // Controller cần test
    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() {
        // Khởi tạo UserController với UserService giả trước mỗi test
        userController = new UserController(userService);
    }

    @Test
    void handleGetUserProfile_whenUserExists_shouldReturnSuccessResponse() {
        // Arrange
        String username = "testuser";
        UserProfileResponseDTO userProfile = new UserProfileResponseDTO(1, username, "test@example.com", "Test User", "123456789", "Address", "BIDDER", 100.0);
        Response successResponse = new Response("SUCCESS", "Profile loaded", userProfile);

        // Dạy cho UserService giả: khi gọi getUserProfile, trả về successResponse
        when(userService.getUserProfile(username)).thenReturn(successResponse);

        // Act
        String resultJson = userController.handleGetUserProfile(username);

        // Assert
        // Kiểm tra xem userService có được gọi đúng không
        verify(userService).getUserProfile(username);

        // Kiểm tra xem kết quả JSON trả về có đúng không
        assertEquals(gson.toJson(successResponse), resultJson);
    }

    @Test
    void handleGetUserProfile_whenUserDoesNotExist_shouldReturnFailResponse() {
        // Arrange
        String username = "nonexistent";
        Response failResponse = new Response("FAIL", "User not found", null);

        // Dạy cho UserService giả: khi gọi getUserProfile, trả về failResponse
        when(userService.getUserProfile(username)).thenReturn(failResponse);

        // Act
        String resultJson = userController.handleGetUserProfile(username);

        // Assert
        verify(userService).getUserProfile(username);
        assertEquals(gson.toJson(failResponse), resultJson);
    }

    @Test
    void handleUpdateProfile_whenValidBidderUpdate_shouldReturnSuccessResponse() {
        // Arrange
        String username = "bidder1";
        // Sửa lỗi: Tham số cuối cùng là String (creditCardInfo), không phải BigDecimal.
        BidderProfileUpdateDTO updateDto = new BidderProfileUpdateDTO("new@email.com", "New Name", "987654321", "New Address", "1234-5678-9012-3456");
        String jsonBody = gson.toJson(updateDto);
        Response successResponse = new Response("SUCCESS", "Profile updated", null);

        // Dạy cho UserService giả
        when(userService.getUserRole(username)).thenReturn(Role.BIDDER);
        when(userService.updateProfile(eq(username), any(BidderProfileUpdateDTO.class))).thenReturn(successResponse);

        // Act
        String resultJson = userController.handleUpdateProfile(username, jsonBody);

        // Assert
        verify(userService).getUserRole(username);
        verify(userService).updateProfile(eq(username), any(BidderProfileUpdateDTO.class));
        assertEquals(gson.toJson(successResponse), resultJson);
    }

    @Test
    void handleUpdateProfile_whenUserRoleNotFound_shouldReturnFailResponse() {
        // Arrange
        String username = "unknown";
        String jsonBody = "{}"; // Nội dung JSON không quan trọng trong trường hợp này
        Response failResponse = new Response("FAIL", "Người dùng không tồn tại", null);

        // Dạy cho UserService giả: không tìm thấy vai trò người dùng
        when(userService.getUserRole(username)).thenReturn(null);

        // Act
        String resultJson = userController.handleUpdateProfile(username, jsonBody);

        // Assert
        verify(userService).getUserRole(username);
        verify(userService, never()).updateProfile(any(), any()); // Đảm bảo updateProfile không được gọi
        assertEquals(gson.toJson(failResponse), resultJson);
    }

    @Test
    void handleChangePassword_whenValidRequest_shouldReturnSuccessResponse() {
        // Arrange
        String username = "testuser";
        Map<String, String> passwords = new HashMap<>();
        passwords.put("oldPass", "oldPassword");
        passwords.put("newPass", "newPassword");
        String jsonBody = gson.toJson(passwords);
        Response successResponse = new Response("SUCCESS", "Password changed", null);

        // Dạy cho UserService giả
        when(userService.changePassword(username, "oldPassword", "newPassword")).thenReturn(successResponse);

        // Act
        String resultJson = userController.handleChangePassword(username, jsonBody);

        // Assert
        verify(userService).changePassword(username, "oldPassword", "newPassword");
        assertEquals(gson.toJson(successResponse), resultJson);
    }

    @Test
    void handleChangePassword_whenMissingPasswordInfo_shouldReturnFailResponse() {
        // Arrange
        String username = "testuser";
        Map<String, String> passwords = new HashMap<>();
        passwords.put("oldPass", "oldPassword"); // Thiếu newPass
        String jsonBody = gson.toJson(passwords);
        Response failResponse = new Response("FAIL", "Thiếu thông tin mật khẩu", null);

        // Act
        String resultJson = userController.handleChangePassword(username, jsonBody);

        // Assert
        verify(userService, never()).changePassword(any(), any(), any()); // Đảm bảo changePassword không được gọi
        assertEquals(gson.toJson(failResponse), resultJson);
    }
}
