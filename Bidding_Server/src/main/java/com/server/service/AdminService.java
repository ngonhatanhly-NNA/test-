package com.server.service;

import com.google.gson.Gson;
import com.server.DAO.UserRepository;
import com.server.DAO.ItemRepository;
import com.server.model.*;
import com.shared.network.Response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminService {
    private final UserRepository userRepo = new UserRepository();
    private final ItemRepository itemRepo = new ItemRepository();
    private final Gson gson = new Gson();

    // ========================================================
    //  1. BẢNG ĐIỀU KHIỂN
    // ========================================================
    public String getDashboard() {
        List<Item> allItems = itemRepo.getAllItems();

        Map<String, Object> dashboard = Map.of(
                "thoiGian", java.time.LocalDateTime.now().toString(),
                "tongSanPham", allItems.size(),
                "dienTu", (int) allItems.stream().filter(item -> item instanceof Electronics).count(),
                "ngheThuat", (int) allItems.stream().filter(item -> item instanceof Art).count(),
                // FIX: Dùng mapToDouble thay vì mapToBigDecimal (vì BigDecimal không có sẵn trong IntStream/DoubleStream)
                "giaTrungBinh", String.format("%.0f", allItems.stream()
                        .mapToDouble(item -> item.getStartingPrice().doubleValue()).average().orElse(0.0)),
                // FIX: Dùng phương thức compareTo chuẩn của BigDecimal
                "sanPhamDatNhat", allItems.stream()
                        .max((i1, i2) -> i1.getStartingPrice().compareTo(i2.getStartingPrice()))
                        .map(i -> i.getName() + " (" + i.getStartingPrice() + ")")
                        .orElse("Chưa có sản phẩm"),
                "sanPhamGanDay", allItems.stream().limit(5)
                        .map(i -> Map.of("ten", i.getName(), "gia", i.getStartingPrice()))
                        .collect(Collectors.toList())
        );

        return gson.toJson(new Response("THÀNH_CÔNG", "Bảng điều khiển Admin", dashboard));
    }

    // ========================================================
    // 2. QUẢN LÝ NGƯỜI DÙNG
    // ========================================================

    public String timKiemNguoiDung(String username) {
        // FIX: userRepo.getUserByUsername đang trả về Bidder (hoặc User), cần ép kiểu an toàn
        User user = userRepo.getUserByUsername(username);
        if (user == null) {
            return gson.toJson(new Response("LỖI", "Không tìm thấy người dùng: " + username, null));
        }

        // 1. Khai báo kiểu BigDecimal ngay từ đầu
        BigDecimal balance = BigDecimal.ZERO;
        if (user instanceof Bidder) {
            // 2. Chỉ gán lại giá trị, không khai báo lại kiểu dữ liệu
            balance = ((Bidder) user).getWalletBalance();
        }

        Map<String, Object> userData = Map.of(
                "id", user.getId(),
                "taiKhoan", user.getUsername(),
                "email", user.getEmail(),
                "vaiTro", user.getRole(),
                "soDu", balance,
                "hoTen", user.getFullName()
        );
        return gson.toJson(new Response("THÀNH_CÔNG", "Thông tin người dùng", userData));
    }

    public String camTaiKhoan(String username) {
        // FIX: userRepo trả về User, mà bạn khai báo nhận vào Bidder -> Cần Downcasting
        User user = userRepo.getUserByUsername(username);
        if (!(user instanceof Bidder)) {
            return gson.toJson(new Response("LỖI", "Người dùng không tồn tại hoặc không phải Bidder!", null));
        }

        Bidder bidder = (Bidder) user;
        System.out.println(" [ADMIN CẤM] Người dùng: " + username + " (ID: " + bidder.getId() + ")");
        return gson.toJson(new Response("THÀNH_CÔNG", "Đã cấm tài khoản: " + username, Map.of("maNguoiDung", bidder.getId())));
    }

    public String boCamTaiKhoan(String username) {
        User user = userRepo.getUserByUsername(username);
        if (!(user instanceof Bidder)) {
            return gson.toJson(new Response("LỖI", "Người dùng không tồn tại!", null));
        }

        Bidder bidder = (Bidder) user;
        System.out.println(" [ADMIN BỎ CẤM] Người dùng: " + username + " (ID: " + bidder.getId() + ")");
        return gson.toJson(new Response("THÀNH_CÔNG", "Đã bỏ cấm tài khoản: " + username, Map.of("maNguoiDung", bidder.getId())));
    }

    // ========================================================
    //  3. QUẢN LÝ SẢN PHẨM
    // ========================================================

    public String phanTichSanPham() {
        List<Item> items = itemRepo.getAllItems();

        Map<String, Object> analytics = Map.of(
                "tongSanPham", items.size(),
                "phanLoai", Map.of(
                        "DienTu", (int) items.stream().filter(item -> item instanceof Electronics).count(),
                        "NgheThuat", (int) items.stream().filter(item -> item instanceof Art).count()
                ),
                // FIX: Chuyển BigDecimal sang Double để dùng các hàm min/max/average của Stream
                "thongKeGia", Map.of(
                        "giaThapNhat", items.stream().mapToDouble(i -> i.getStartingPrice().doubleValue()).min().orElse(0),
                        "giaCaoNhat", items.stream().mapToDouble(i -> i.getStartingPrice().doubleValue()).max().orElse(0),
                        "giaTrungBinh", String.format("%.0f", items.stream().mapToDouble(i -> i.getStartingPrice().doubleValue()).average().orElse(0))
                ),
                "tinhTrang", items.stream()
                        .collect(Collectors.groupingBy(Item::getCondition, Collectors.counting()))
        );

        return gson.toJson(new Response("THÀNH_CÔNG", "Phân tích sản phẩm", analytics));
    }

    // ... (Các hàm xoaSanPham giữ nguyên nếu không lỗi)

    public String pheDuyetNguoiBan(String bidderUsername) {
        User user = userRepo.getUserByUsername(bidderUsername);
        if (!(user instanceof Bidder)) {
            return gson.toJson(new Response("LỖI", "Người đấu giá không tồn tại!", null));
        }

        Bidder bidder = (Bidder) user;
        Seller seller = new Seller(bidder, bidderUsername + "_Shop", "BANK_" + bidder.getId());
        // seller.changeRole(); // Giả sử hàm này tồn tại trong model của bạn

        return gson.toJson(new Response("THÀNH_CÔNG", "Phê duyệt thành công", Map.of("tenCuaHang", seller.getShopName())));
    }

    // ========================================================
    //  5. TÀI CHÍNH
    // ========================================================

    public String uocTinhDoanhThu() {
        List<Item> items = itemRepo.getAllItems();
        // FIX: Cộng dồn BigDecimal bằng reduce
        BigDecimal tongGiaTri = items.stream()
                .map(Item::getStartingPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal phiNenTang = tongGiaTri.multiply(new BigDecimal("0.08"));

        Map<String, Object> doanhThu = Map.of(
                "tongGiaTriSanPham", tongGiaTri.toString(),
                "phiNenTang_8%", phiNenTang.toString(),
                "soLuongSanPham", items.size(),
                // FIX: So sánh BigDecimal dùng compareTo thay vì dấu '>'
                "sanPhamGiaCao", items.stream()
                        .filter(item -> item.getStartingPrice().compareTo(new BigDecimal("10000000")) > 0)
                        .map(Item::getName)
                        .collect(Collectors.toList())
        );

        return gson.toJson(new Response("THÀNH_CÔNG", "Ước tính doanh thu", doanhThu));
    }
}