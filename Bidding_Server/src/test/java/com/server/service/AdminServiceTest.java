package com.server.service;

import com.google.gson.Gson;
import com.server.DAO.AdminRepository;
import com.server.DAO.ItemRepository;
import com.server.DAO.UserRepository;
import com.server.model.*;
import com.shared.network.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Kiểm thử Dịch vụ Quản trị viên (AdminService)")
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private AdminRepository adminRepository;

    private AdminService adminService;

    private AutoCloseable closeable;
    private final Gson gson = new Gson();
    private Bidder sampleBidder;
    private List<Item> sampleItems;

    @BeforeEach
    void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        adminService = new AdminService();

        Field userRepoField = AdminService.class.getDeclaredField("userRepo");
        userRepoField.setAccessible(true);
        userRepoField.set(adminService, userRepository);

        Field itemRepoField = AdminService.class.getDeclaredField("itemRepo");
        itemRepoField.setAccessible(true);
        itemRepoField.set(adminService, itemRepository);

        Field adminRepoField = AdminService.class.getDeclaredField("adminRepo");
        adminRepoField.setAccessible(true);
        adminRepoField.set(adminService, adminRepository);

        sampleBidder = new Bidder("testbidder", "password", "bidder@email.com", "Test Bidder");
        sampleBidder.setId(10L);
        sampleBidder.setWalletBalance(new BigDecimal("500"));

        sampleItems = new ArrayList<>();
        sampleItems.add(new Electronics(1, "Laptop", "Mới", new BigDecimal("20000"), "Mới", Collections.emptyList(), "Dell", "XPS", 12));
        sampleItems.add(new Art(2, "Tranh Sơn Dầu", "Cũ", new BigDecimal("5000"), "Cũ", Collections.emptyList(), "Van Gogh", "Sơn dầu", false));
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Nested
    @DisplayName("Bảng điều khiển (Dashboard)")
    class DashboardTests {
        @Test
        @DisplayName("Nên trả về dữ liệu dashboard chính xác")
        void getDashboard_shouldReturnCorrectDashboardData() {
            when(itemRepository.getAllItems()).thenReturn(sampleItems);

            String jsonResponse = adminService.getDashboard();
            Response response = gson.fromJson(jsonResponse, Response.class);

            assertEquals("THANH_CONG", response.getStatus());
            Map<String, Object> data = (Map<String, Object>) response.getData();
            assertEquals(2.0, data.get("tongSanPham"));
            assertEquals(1.0, data.get("dienTu"));
            assertEquals(1.0, data.get("ngheThuat"));
            assertEquals("12500", data.get("giaTrungBinh"));
        }
    }

    @Nested
    @DisplayName("Quản lý Người dùng")
    class UserManagementTests {

        @Test
        @DisplayName("Tìm kiếm người dùng nên trả về dữ liệu khi tìm thấy")
        void findUser_shouldReturnUserData_whenFound() {
            when(userRepository.getUserByUsername("testbidder")).thenReturn(sampleBidder);

            String jsonResponse = adminService.timKiemNguoiDung("testbidder");
            Response response = gson.fromJson(jsonResponse, Response.class);

            assertEquals("THANH_CONG", response.getStatus());
            Map<String, Object> data = (Map<String, Object>) response.getData();
            assertNotNull(data);
            assertEquals("testbidder", data.get("taiKhoan"));
            // Gson có thể đọc số thành các kiểu khác nhau, nên ép kiểu về double để so sánh
            assertEquals(500.0, ((Number) data.get("soDu")).doubleValue());
        }

        @Test
        @DisplayName("Tìm kiếm người dùng nên trả về lỗi khi không tìm thấy")
        void findUser_shouldReturnError_whenNotFound() {
            when(userRepository.getUserByUsername(anyString())).thenReturn(null);

            String jsonResponse = adminService.timKiemNguoiDung("unknown");
            Response response = gson.fromJson(jsonResponse, Response.class);

            assertEquals("LOI", response.getStatus());
        }

        @Test
        @DisplayName("Cấm tài khoản nên thành công đối với Bidder")
        void banAccount_shouldSucceed_forBidder() {
            when(userRepository.getUserByUsername("testbidder")).thenReturn(sampleBidder);
            doNothing().when(adminRepository).updateUserStatus(sampleBidder.getId(), Status.BANNED);

            String jsonResponse = adminService.camTaiKhoan("testbidder");
            Response response = gson.fromJson(jsonResponse, Response.class);

            assertEquals("THANH_CONG", response.getStatus());
            verify(adminRepository, times(1)).updateUserStatus(10L, Status.BANNED);
        }

        @Test
        @DisplayName("Bỏ cấm tài khoản nên thành công đối với Bidder")
        void unbanAccount_shouldSucceed_forBidder() {
            when(userRepository.getUserByUsername("testbidder")).thenReturn(sampleBidder);
            doNothing().when(adminRepository).updateUserStatus(sampleBidder.getId(), Status.ACTIVE);

            String jsonResponse = adminService.boCamTaiKhoan("testbidder");
            Response response = gson.fromJson(jsonResponse, Response.class);

            assertEquals("THANH_CONG", response.getStatus());
            verify(adminRepository, times(1)).updateUserStatus(10L, Status.ACTIVE);
        }

        @Test
        @DisplayName("Phê duyệt người bán nên thành công khi DB cập nhật thành công")
        void approveSeller_shouldSucceed_whenDbUpdateIsSuccessful() {
            when(userRepository.getUserByUsername("testbidder")).thenReturn(sampleBidder);
            when(adminRepository.promoteToSeller(any(Seller.class))).thenReturn(true);

            String jsonResponse = adminService.pheDuyetNguoiBan("testbidder");
            Response response = gson.fromJson(jsonResponse, Response.class);

            assertEquals("THANH_CONG", response.getStatus());
            Map<String, Object> data = (Map<String, Object>) response.getData();
            assertTrue(data.get("tenCuaHang").toString().endsWith("_Shop"));
            verify(adminRepository, times(1)).promoteToSeller(any(Seller.class));
        }

        @Test
        @DisplayName("Phê duyệt người bán nên thất bại khi DB cập nhật lỗi")
        void approveSeller_shouldFail_whenDbUpdateFails() {
            when(userRepository.getUserByUsername("testbidder")).thenReturn(sampleBidder);
            when(adminRepository.promoteToSeller(any(Seller.class))).thenReturn(false);

            String jsonResponse = adminService.pheDuyetNguoiBan("testbidder");
            Response response = gson.fromJson(jsonResponse, Response.class);

            assertEquals("LOI", response.getStatus());
            verify(adminRepository, times(1)).promoteToSeller(any(Seller.class));
        }
    }

    @Nested
    @DisplayName("Quản lý Sản phẩm và Tài chính")
    class ItemAndFinanceTests {

        @Test
        @DisplayName("Phân tích sản phẩm nên trả về thống kê chính xác")
        void analyzeProducts_shouldReturnCorrectStatistics() {
            when(itemRepository.getAllItems()).thenReturn(sampleItems);

            String jsonResponse = adminService.phanTichSanPham();
            Response response = gson.fromJson(jsonResponse, Response.class);

            assertEquals("THANH_CONG", response.getStatus());
            Map<String, Object> data = (Map<String, Object>) response.getData();
            assertEquals(2.0, data.get("tongSanPham"));

            Map<String, Object> phanLoai = (Map<String, Object>) data.get("phanLoai");
            assertEquals(1.0, phanLoai.get("DienTu"));
            assertEquals(1.0, phanLoai.get("NgheThuat"));
        }

        @Test
        @DisplayName("Ước tính doanh thu nên tính toán đúng")
        void estimateRevenue_shouldCalculateCorrectly() {
            when(itemRepository.getAllItems()).thenReturn(sampleItems);

            String jsonResponse = adminService.uocTinhDoanhThu();
            Response response = gson.fromJson(jsonResponse, Response.class);

            assertEquals("THANH_CONG", response.getStatus());
            Map<String, Object> data = (Map<String, Object>) response.getData();

            assertEquals("25000", data.get("tongGiaTriSanPham").toString());
            assertEquals("2000.00", data.get("phiNenTang_8%").toString());
            assertEquals(2.0, data.get("soLuongSanPham"));
        }
    }
}