package com.server.service;

import com.server.DAO.AdminRepository;
import com.server.DAO.ItemRepository;
import com.server.DAO.UserRepository;
import com.server.model.Bidder;
import com.server.model.Electronics;
import com.server.model.User;
import com.shared.network.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminService Tests")
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private UserService userService;

    private AdminService adminService;

    @BeforeEach
    void setUp() throws Exception {
        adminService = new AdminService();

        // Inject mocks via reflection because the fields are final
        Field userRepoField = AdminService.class.getDeclaredField("userRepo");
        userRepoField.setAccessible(true);
        userRepoField.set(adminService, userRepository);

        Field itemRepoField = AdminService.class.getDeclaredField("itemRepo");
        itemRepoField.setAccessible(true);
        itemRepoField.set(adminService, itemRepository);

        Field adminRepoField = AdminService.class.getDeclaredField("adminRepo");
        adminRepoField.setAccessible(true);
        adminRepoField.set(adminService, adminRepository);

        Field userServiceField = AdminService.class.getDeclaredField("userService");
        userServiceField.setAccessible(true);
        userServiceField.set(adminService, userService);
    }

    @Test
    @DisplayName("Lấy dashboard thành công")
    void getDashboard_shouldReturnSuccess() {
        when(itemRepository.getAllItems()).thenReturn(Collections.singletonList(new Electronics.Builder().build()));
        Response response = adminService.getDashboard();
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getData());
    }

    @Test
    @DisplayName("Lấy tất cả users")
    void getAllUsers_shouldCallUserService() {
        adminService.getAllUsers();
        verify(userService, times(1)).getAllUsers();
    }

    @Test
    @DisplayName("Phân tích sản phẩm thành công")
    void phanTichSanPham_shouldReturnSuccess() {
        when(itemRepository.getAllItems()).thenReturn(Collections.singletonList(new Electronics.Builder().build()));
        Response response = adminService.phanTichSanPham();
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    @DisplayName("Xóa item thành công")
    void deleteItem_shouldReturnSuccess() {
        when(itemRepository.delete(1L)).thenReturn(true);
        Response response = adminService.deleteItem(1L);
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    @DisplayName("Xóa item thất bại")
    void deleteItem_shouldReturnError() {
        when(itemRepository.delete(1L)).thenReturn(false);
        Response response = adminService.deleteItem(1L);
        assertEquals("ERROR", response.getStatus());
    }

    @Test
    @DisplayName("Ước tính doanh thu thành công")
    void uocTinhDoanhThu_shouldReturnSuccess() {
        Electronics item = new Electronics.Builder().startingPrice(new BigDecimal("1000")).build();
        when(itemRepository.getAllItems()).thenReturn(Collections.singletonList(item));
        Response response = adminService.uocTinhDoanhThu();
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    @DisplayName("Tìm kiếm người dùng thành công")
    void timKiemNguoiDung_shouldReturnSuccess() {
        when(userRepository.getUserByUsername("test")).thenReturn(new Bidder());
        Response response = adminService.timKiemNguoiDung("test");
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    @DisplayName("Tìm kiếm người dùng thất bại")
    void timKiemNguoiDung_shouldReturnError() {
        when(userRepository.getUserByUsername("test")).thenReturn(null);
        Response response = adminService.timKiemNguoiDung("test");
        assertEquals("ERROR", response.getStatus());
    }

    @Test
    @DisplayName("Cấm tài khoản thành công")
    void camTaiKhoan_shouldReturnSuccess() {
        User user = new Bidder();
        user.setId(1L);
        when(userRepository.getUserByUsername("test")).thenReturn(user);
        
        Response response = adminService.camTaiKhoan("test");
        
        assertEquals("SUCCESS", response.getStatus());
        verify(userRepository, times(1)).updateUserStatus(eq(1L), any());
    }
    
    @Test
    @DisplayName("Cấm tài khoản thất bại do user null")
    void camTaiKhoan_shouldReturnError_whenUserNull() {
        when(userRepository.getUserByUsername("test")).thenReturn(null);
        
        Response response = adminService.camTaiKhoan("test");
        
        assertEquals("ERROR", response.getStatus());
        verify(userRepository, never()).updateUserStatus(anyLong(), any());
    }

    @Test
    @DisplayName("Bỏ cấm tài khoản thành công")
    void boCamTaiKhoan_shouldReturnSuccess() {
        User user = new Bidder();
        user.setId(1L);
        when(userRepository.getUserByUsername("test")).thenReturn(user);
        
        Response response = adminService.boCamTaiKhoan("test");
        
        assertEquals("SUCCESS", response.getStatus());
        verify(userRepository, times(1)).updateUserStatus(eq(1L), any());
    }
    
    @Test
    @DisplayName("Bỏ cấm tài khoản thất bại do user null")
    void boCamTaiKhoan_shouldReturnError_whenUserNull() {
        when(userRepository.getUserByUsername("test")).thenReturn(null);
        
        Response response = adminService.boCamTaiKhoan("test");
        
        assertEquals("ERROR", response.getStatus());
        verify(userRepository, never()).updateUserStatus(anyLong(), any());
    }
}