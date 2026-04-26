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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminService {
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private final UserRepository userRepo;
    private final ItemRepository itemRepo;
    private final AdminRepository adminRepo;
    private final UserService userService;
    private final Gson gson = new Gson();

    public AdminService() {
        this.userRepo = new UserRepository();
        this.itemRepo = new ItemRepository();
        this.adminRepo = new AdminRepository();
        this.userService = new UserService();
    }

    public Response getDashboard() {
        List<Item> allItems = itemRepo.getAllItems();
        Map<String, Object> dashboard = Map.of(
                "thoiGian", java.time.LocalDateTime.now().toString(),
                "tongSanPham", allItems.size()
        );
        return new Response("SUCCESS", "Bang dieu khien Admin", dashboard);
    }

    public Response getAllUsers() {
        return userService.getAllUsers();
    }

    public Response phanTichSanPham() {
        List<Item> items = itemRepo.getAllItems();
        Map<String, Object> analytics = Map.of(
                "tongSanPham", items.size()
        );
        return new Response("SUCCESS", "Phan tich san pham", analytics);
    }

    public Response uocTinhDoanhThu() {
        List<Item> items = itemRepo.getAllItems();
        BigDecimal tongGiaTri = items.stream()
                .map(Item::getStartingPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal phiNenTang = tongGiaTri.multiply(new BigDecimal("0.08"));
        Map<String, Object> doanhThu = Map.of(
                "tongGiaTriSanPham", tongGiaTri.toString(),
                "phiNenTang_8%", phiNenTang.toString(),
                "soLuongSanPham", items.size()
        );
        return new Response("SUCCESS", "Uoc tinh doanh thu", doanhThu);
    }

    // Other methods that might have been deleted
    public Response timKiemNguoiDung(String username) {
        User user = userRepo.getUserByUsername(username);
        if (user == null) {
            return new Response("ERROR", "User not found", null);
        }
        return new Response("SUCCESS", "User found", user);
    }

    public Response camTaiKhoan(String username) {
        User user = userRepo.getUserByUsername(username);
        if (user == null) {
            return new Response("ERROR", "User not found", null);
        }
        userRepo.updateUserStatus(user.getId(), Status.BANNED);
        return new Response("SUCCESS", "User banned", null);
    }

    public Response boCamTaiKhoan(String username) {
        User user = userRepo.getUserByUsername(username);
        if (user == null) {
            return new Response("ERROR", "User not found", null);
        }
        userRepo.updateUserStatus(user.getId(), Status.ACTIVE);
        return new Response("SUCCESS", "User unbanned", null);
    }
}
