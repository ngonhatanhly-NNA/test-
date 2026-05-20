package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.*;
import com.shared.dto.WalletTransactionDTO;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp thực thi các lệnh liên quan đến Bidder trong Cơ sở dữ liệu (MySQL).
 * Đây là nơi biến các yêu cầu trong Java thành lệnh SQL thật sự.
 */
public class BidderRepository implements IBidderRepository {
    private static final Logger logger = LoggerFactory.getLogger(BidderRepository.class);

    // --- VIỆC 1: CẬP NHẬT SỐ DƯ (Nạp tiền hoặc Trừ tiền) ---
    @Override
    public boolean updateBalance(long bidderId, BigDecimal newBalance) {
        // Viết lệnh SQL: Tìm đúng ngườii có user_id này và đè số dư mới lên
        String sql = "UPDATE bidders SET walletBalance = ? WHERE user_id = ?";

        // Mở "đường ống" kết nối tới Database (dùng Try-with-resources để tự đóng sau khi xong)
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Truyền dữ liệu vào các dấu hỏi chấm (?) để tránh lỗi bảo mật SQL Injection
            pstmt.setBigDecimal(1, newBalance); // Dấu hỏi số 1: Số dư mới
            pstmt.setLong(2, bidderId);        // Dấu hỏi số 2: ID ngườii cần cập nhật

            // Chốt lệnh: executeUpdate trả về số dòng bị thay đổi. Nếu > 0 tức là đã lưu thành công.
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            logger.error("Lỗi DB: Không thể cập nhật tiền cho ID {}: {}", bidderId, e.getMessage(), e);
            return false;
        }
    }

    // --- THÊM MỚI: CẬP NHẬT CẢ VÍ CHÍNH VÀ VÍ ĐÓNG BĂNG CÙNG LÚC ---
    @Override
    public boolean updateWalletBalances(long bidderId, BigDecimal mainBalance, BigDecimal reservedAmount) {
        String sql = "UPDATE bidders SET walletBalance = ?, reserved_amount = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, mainBalance);
            pstmt.setBigDecimal(2, reservedAmount);
            pstmt.setLong(3, bidderId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi DB: Không thể cập nhật ví cho ID {}: {}", bidderId, e.getMessage(), e);
            return false;
        }
    }

    // --- VIỆC 2: TÌM BIDDER THEO USERNAME (Dùng khi Đăng nhập) ---
    @Override
    public Bidder getBidderByUsername(String username) {
        // Vì dữ liệu nằm ở 2 bảng (users và bidders) nên dùng JOIN để gộp lại lấy 1 lần cho nhanh
        String sql = "SELECT u.*, b.walletBalance, b.reserved_amount, b.creditCardInfo " +
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
                        rs.getBigDecimal("reserved_amount"), // Lấy số tiền đóng băng
                        rs.getString("creditCardInfo")
                );
            }
        } catch (SQLException e) {
            logger.error("Lỗi khi tìm Bidder theo username '{}': {}", username, e.getMessage(), e);
        }
        return null; // Không tìm thấy thì trả về rỗng
    }

    // --- VIỆC 3: TÌM BIDDER THEO ID (Dùng để kiểm tra số dư mới nhất) ---
    @Override
    public Bidder getBidderById(long id) {
        // Tương tự như tìm theo Username, nhưng dùng ID làm chìa khóa tìm kiếm
        String sql = "SELECT u.*, b.walletBalance, b.reserved_amount, b.creditCardInfo " +
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
                        rs.getBigDecimal("reserved_amount"), rs.getString("creditCardInfo")
                );
            }
        } catch (SQLException e) {
            logger.error("Lỗi DB: Không tìm thấy Bidder ID {}: {}", id, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Ghi một giao dịch ví vào bảng wallet_transactions.
     * Được gọi sau khi nạp tiền thành công hoặc trừ tiền khi thắng đấu giá.
     *
     * @param userId      ID người dùng
     * @param amount      Số tiền giao dịch (luôn dương)
     * @param type        "DEPOSIT" hoặc "DEDUCT"
     * @param description Mô tả giao dịch
     * @return true nếu ghi thành công
     */
    @Override
    public boolean logTransaction(long userId, BigDecimal amount, String type, String description) {
        String sql = "INSERT INTO wallet_transactions (user_id, amount, type, description, created_at) " +
                "VALUES (?, ?, ?, ?, NOW())";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setBigDecimal(2, amount);
            pstmt.setString(3, type);
            pstmt.setString(4, description);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi DB: Không thể ghi giao dịch ví cho user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Lấy lịch sử giao dịch ví của người dùng, sắp xếp mới nhất trước.
     *
     * @param userId ID người dùng
     * @return danh sách các giao dịch
     */
    @Override
    public List<WalletTransactionDTO> getWalletTransactions(long userId) {
        List<WalletTransactionDTO> list = new ArrayList<>();
        String sql = "SELECT id, amount, type, description, created_at " +
                "FROM wallet_transactions " +
                "WHERE user_id = ? " +
                "ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    BigDecimal amount = rs.getBigDecimal("amount");
                    String type = rs.getString("type");
                    String description = rs.getString("description");
                    LocalDateTime createdAt = rs.getTimestamp("created_at") != null
                            ? rs.getTimestamp("created_at").toLocalDateTime()
                            : LocalDateTime.now();
                    list.add(new WalletTransactionDTO(id, amount, type, description, createdAt));
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi DB: Không thể lấy lịch sử giao dịch ví cho user {}: {}", userId, e.getMessage(), e);
        }
        return list;
    }
}