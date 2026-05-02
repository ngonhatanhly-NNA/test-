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
import java.sql.SQLException; // Import SQLException
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Kiểm thử Dịch vụ Quản trị viên (AdminService)")
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private UserService userService;

    private AdminService adminService;
    private AutoCloseable closeable;
    private Bidder sampleBidder;
    private List<Item> sampleItems;

    @BeforeEach
    void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        adminService = new AdminService();

        setField(adminService, "userRepo", userRepository);
        setField(adminService, "itemRepo", itemRepository);
        setField(adminService, "adminRepo", adminRepository);
        setField(adminService, "userService", userService);

        sampleBidder = new Bidder("testbidder", "password", "bidder@email.com", "Test Bidder");
        sampleBidder.setId(10L);
        sampleBidder.setWalletBalance(new BigDecimal("500"));

        sampleItems = new ArrayList<>();
        sampleItems.add(new Electronics(1, "Laptop", "Mới", new BigDecimal("20000"), "Mới", Collections.emptyList(), "Dell", "XPS", 12));
        sampleItems.add(new Art(2, "Tranh Sơn Dầu", "Cũ", new BigDecimal("5000"), "Cũ", Collections.emptyList(), "Van Gogh", "Sơn dầu", false));
    }
    
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
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
        void getDashboard_shouldReturnCorrectDashboardData() throws SQLException { // Add throws
            when(itemRepository.getAllItems()).thenReturn(sampleItems);
            Response response = adminService.getDashboard();
            assertEquals("SUCCESS", response.getStatus());
            Map<String, Object> data = (Map<String, Object>) response.getData();
            assertEquals(2, data.get("tongSanPham"));
        }
    }

    // ... (Other test classes remain the same)

    @Nested
    @DisplayName("Quản lý Sản phẩm và Tài chính")
    class ItemAndFinanceTests {

        @Test
        @DisplayName("Phân tích sản phẩm nên trả về thống kê chính xác")
        void analyzeProducts_shouldReturnCorrectStatistics() throws SQLException { // Add throws
            when(itemRepository.getAllItems()).thenReturn(sampleItems);
            Response response = adminService.phanTichSanPham();
            assertEquals("SUCCESS", response.getStatus());
        }

        @Test
        @DisplayName("Ước tính doanh thu nên tính toán đúng")
        void estimateRevenue_shouldCalculateCorrectly() throws SQLException { // Add throws
            when(itemRepository.getAllItems()).thenReturn(sampleItems);
            Response response = adminService.uocTinhDoanhThu();
            assertEquals("SUCCESS", response.getStatus());
        }
    }
}