package com.server.service;

import com.google.gson.Gson;
import com.server.DAO.UserRepository;
import com.server.DAO.ItemRepository;
import com.server.DAO.AdminRepository;
import com.server.model.*;
import com.shared.network.Response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminService {
    private final UserRepository userRepo = new UserRepository();
    private final ItemRepository itemRepo = new ItemRepository();
    private final AdminRepository adminRepo = new AdminRepository();
    private final Gson gson = new Gson();

    // ========================================================
    //  1. BANG DIEU KHIEN
    // ========================================================
    public String getDashboard() {
        List<Item> allItems = itemRepo.getAllItems(); // Lay tu DB that

        Map<String, Object> dashboard = Map.of(
                "thoiGian", java.time.LocalDateTime.now().toString(),
                "tongSanPham", allItems.size(),
                "dienTu", (int) allItems.stream().filter(item -> item instanceof Electronics).count(),
                "ngheThuat", (int) allItems.stream().filter(item -> item instanceof Art).count(),
                "giaTrungBinh", String.format("%.0f", allItems.stream()
                        .mapToDouble(item -> item.getStartingPrice().doubleValue()).average().orElse(0.0)),
                "sanPhamDatNhat", allItems.stream()
                        .max((i1, i2) -> i1.getStartingPrice().compareTo(i2.getStartingPrice()))
                        .map(i -> i.getName() + " (" + i.getStartingPrice() + ")")
                        .orElse("Chua co san pham"),
                "sanPhamGanDay", allItems.stream().limit(5)
                        .map(i -> Map.of("ten", i.getName(), "gia", i.getStartingPrice()))
                        .collect(Collectors.toList())
        );

        return gson.toJson(new Response("THANH_CONG", "Bang dieu khien Admin", dashboard));
    }

    // ========================================================
    // 2. QUAN LY NGUOI DUNG
    // ========================================================

    public String timKiemNguoiDung(String username) {
        User user = userRepo.getUserByUsername(username); // Lay tu DB that
        if (user == null) {
            return gson.toJson(new Response("LOI", "Khong tim thay nguoi dung: " + username, null));
        }

        BigDecimal balance = BigDecimal.ZERO;
        if (user instanceof Bidder) {
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
        return gson.toJson(new Response("THANH_CONG", "Thong tin nguoi dung", userData));
    }

    public String camTaiKhoan(String username) {
        User user = userRepo.getUserByUsername(username);
        if (!(user instanceof Bidder)) {
            return gson.toJson(new Response("LOI", "Nguoi dung khong ton tai hoac khong phai Bidder!", null));
        }

        Bidder bidder = (Bidder) user;
        adminRepo.updateUserStatus(bidder.getId(), Status.BANNED); // Cap nhat DB

        System.out.println(" [ADMIN CAM] Nguoi dung: " + username + " (ID: " + bidder.getId() + ")");
        return gson.toJson(new Response("THANH_CONG", "Da cam tai khoan: " + username, Map.of("maNguoiDung", bidder.getId())));
    }

    public String boCamTaiKhoan(String username) {
        User user = userRepo.getUserByUsername(username);
        if (!(user instanceof Bidder)) {
            return gson.toJson(new Response("LOI", "Nguoi dung khong ton tai!", null));
        }

        Bidder bidder = (Bidder) user;
        adminRepo.updateUserStatus(bidder.getId(), Status.ACTIVE); // Cap nhat DB

        System.out.println(" [ADMIN BO CAM] Nguoi dung: " + username + " (ID: " + bidder.getId() + ")");
        return gson.toJson(new Response("THANH_CONG", "Da bo cam tai khoan: " + username, Map.of("maNguoiDung", bidder.getId())));
    }

    // ========================================================
    //  3. QUAN LY SAN PHAM
    // ========================================================

    public String phanTichSanPham() {
        List<Item> items = itemRepo.getAllItems();

        Map<String, Object> analytics = Map.of(
                "tongSanPham", items.size(),
                "phanLoai", Map.of(
                        "DienTu", (int) items.stream().filter(item -> item instanceof Electronics).count(),
                        "NgheThuat", (int) items.stream().filter(item -> item instanceof Art).count()
                ),
                "thongKeGia", Map.of(
                        "giaThapNhat", items.stream().mapToDouble(i -> i.getStartingPrice().doubleValue()).min().orElse(0),
                        "giaCaoNhat", items.stream().mapToDouble(i -> i.getStartingPrice().doubleValue()).max().orElse(0),
                        "giaTrungBinh", String.format("%.0f", items.stream().mapToDouble(i -> i.getStartingPrice().doubleValue()).average().orElse(0))
                ),
                "tinhTrang", items.stream()
                        .collect(Collectors.groupingBy(Item::getCondition, Collectors.counting()))
        );

        return gson.toJson(new Response("THANH_CONG", "Phan tich san pham", analytics));
    }

    public String pheDuyetNguoiBan(String bidderUsername) {
        User user = userRepo.getUserByUsername(bidderUsername);
        if (!(user instanceof Bidder)) {
            return gson.toJson(new Response("LOI", "Nguoi dau gia khong ton tai!", null));
        }

        Bidder bidder = (Bidder) user;
        Seller seller = new Seller(bidder, bidderUsername + "_Shop", "BANK_" + bidder.getId());

        // Goi AdminRepo de thuc hien nang cap trong DB
        boolean result = adminRepo.promoteToSeller(seller);

        if (result) {
            return gson.toJson(new Response("THANH_CONG", "Phe duyet thanh cong", Map.of("tenCuaHang", seller.getShopName())));
        } else {
            return gson.toJson(new Response("LOI", "Loi khi cap nhat DB", null));
        }
    }

    // ========================================================
    //  5. TAI CHINH
    // ========================================================

    public String uocTinhDoanhThu() {
        List<Item> items = itemRepo.getAllItems();
        BigDecimal tongGiaTri = items.stream()
                .map(Item::getStartingPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal phiNenTang = tongGiaTri.multiply(new BigDecimal("0.08"));

        Map<String, Object> doanhThu = Map.of(
                "tongGiaTriSanPham", tongGiaTri.toString(),
                "phiNenTang_8%", phiNenTang.toString(),
                "soLuongSanPham", items.size(),
                "sanPhamGiaCao", items.stream()
                        .filter(item -> item.getStartingPrice().compareTo(new BigDecimal("10000000")) > 0)
                        .map(Item::getName)
                        .collect(Collectors.toList())
        );

        return gson.toJson(new Response("THANH_CONG", "Uoc tinh doanh thu", doanhThu));
    }
}