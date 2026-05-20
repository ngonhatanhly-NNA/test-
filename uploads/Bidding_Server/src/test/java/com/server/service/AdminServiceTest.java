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

        // Dùng Factory để tạo Item cho Test
        Map<String, Object> elecProps = Map.of("brand", "Dell", "model", "XPS", "warrantyMonths", 12);
        sampleItems.add(ItemFactory.createItem(
                "ELECTRONICS",           // 1. type
                1,                       // 2. id
                10,                      // 3. sellerId
                "Laptop",                // 4. name
                "Mới",                   // 5. description
                new BigDecimal("20000"), // 6. startPrice (ĐÃ ĐƯA VỀ ĐÚNG VỊ TRÍ)
                "Mới",                   // 7. condition (ĐÃ ĐƯA VỀ ĐÚNG VỊ TRÍ)
                Collections.emptyList(), // 8. imageUrls (ĐÃ ĐƯA VỀ ĐÚNG VỊ TRÍ)
                elecProps                // 9. extraProps
        ));

        Map<String, Object> artProps = Map.of("artistName", "Van Gogh", "material", "Sơn dầu", "hasCertificateOfAuthenticity", false);
        sampleItems.add(ItemFactory.createItem(
                "ART",                   // 1. type
                2,                       // 2. id
                10,                      // 3. sellerId
                "Tranh",                 // 4. name
                "Cũ",                    // 5. description
                new BigDecimal("5000"),  // 6. startPrice (ĐÃ ĐƯA VỀ ĐÚNG VỊ TRÍ)
                "Cũ",                    // 7. condition (ĐÃ ĐƯA VỀ ĐÚNG VỊ TRÍ)
                Collections.emptyList(), // 8. imageUrls (ĐÃ ĐƯA VỀ ĐÚNG VỊ TRÍ)
                artProps                 // 9. extraProps
        ));
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