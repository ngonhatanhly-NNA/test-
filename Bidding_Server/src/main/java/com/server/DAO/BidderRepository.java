package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.*;
import java.math.BigDecimal;
import java.sql.*;

/**
 * Lớp thực thi các lệnh liên quan đến Bidder trong Cơ sở dữ liệu (MySQL).
 * Đây là nơi biến các yêu cầu trong Java thành lệnh SQL thật sự.
 */
public class BidderRepository implements IBidderRepository {

    // --- VIỆC 1: CẬP NHẬT SỐ DƯ (Nạp tiền hoặc Trừ tiền) ---
    @Override
    public boolean updateBalance(long bidderId, BigDecimal newBalance) {
        // Viết lệnh SQL: Tìm đúng người có user_id này và đè số dư mới lên
        String sql = "UPDATE bidders SET walletBalance = ? WHERE user_id = ?";

        // Mở "đường ống" kết nối tới Database (dùng Try-with-resources để tự đóng sau khi xong)
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Truyền dữ liệu vào các dấu hỏi chấm (?) để tránh lỗi bảo mật SQL Injection
            pstmt.setBigDecimal(1, newBalance); // Dấu hỏi số 1: Số dư mới
            pstmt.setLong(2, bidderId);        // Dấu hỏi số 2: ID người cần cập nhật

            // Chốt lệnh: executeUpdate trả về số dòng bị thay đổi. Nếu > 0 tức là đã lưu thành công.
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi DB: Không thể cập nhật tiền cho ID " + bidderId);
            e.printStackTrace();
            return false;
        }
    }

    // --- VIỆC 2: TÌM BIDDER THEO USERNAME (Dùng khi Đăng nhập) ---
    @Override
    public Bidder getBidderByUsername(String username) {
        // Vì dữ liệu nằm ở 2 bảng (users và bidders) nên dùng JOIN để gộp lại lấy 1 lần cho nhanh
        String sql = "SELECT u.*, b.walletBalance, b.creditCardInfo " +
                "FROM users u JOIN bidders b ON u.id = b.user_id " +
                "WHERE u.username = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username); // Điền username vào lệnh tìm kiếm
            ResultSet rs = pstmt.executeQuery(); // Thực hiện lệnh "Đọc" dữ liệu

            // Nếu tìm thấy kết quả (rs.next() là đúng)
            if (rs.next()) {
                // "Đổ" dữ liệu từ các cột trong Database vào lại Object Bidder của Java
                return new Bidder(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("passwordHash"),
                        rs.getString("email"),
                        rs.getString("fullName"),
                        rs.getString("phoneNumber"),
                        rs.getString("address"),
                        // Chuyển chữ (String) từ DB thành Enum trong Java
                        Status.valueOf(rs.getString("status").toUpperCase()),
                        Role.valueOf(rs.getString("role").toUpperCase()),
                        rs.getBigDecimal("walletBalance"), // Lấy số dư kiểu tiền tệ
                        rs.getString("creditCardInfo")
                );
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi tìm Bidder theo username: " + username);
            e.printStackTrace();
        }
        return null; // Không tìm thấy thì trả về rỗng
    }

    // --- VIỆC 3: TÌM BIDDER THEO ID (Dùng để kiểm tra số dư mới nhất) ---
    @Override
    public Bidder getBidderById(long id) {
        // Tương tự như tìm theo Username, nhưng dùng ID làm chìa khóa tìm kiếm
        String sql = "SELECT u.*, b.walletBalance, b.creditCardInfo " +
                "FROM users u JOIN bidders b ON u.id = b.user_id " +
                "WHERE u.id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Khởi tạo lại Object Bidder với dữ liệu tươi mới nhất từ Database
                return new Bidder(
                        rs.getLong("id"), rs.getString("username"), rs.getString("passwordHash"),
                        rs.getString("email"), rs.getString("fullName"), rs.getString("phoneNumber"),
                        rs.getString("address"), Status.valueOf(rs.getString("status")),
                        Role.valueOf(rs.getString("role")), rs.getBigDecimal("walletBalance"),
                        rs.getString("creditCardInfo")
                );
            }
        } catch (SQLException e) {
            System.err.println("Lỗi DB: Không tìm thấy Bidder ID " + id);
            e.printStackTrace();
        }
        return null;
    }
}