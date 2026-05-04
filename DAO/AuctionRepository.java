package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.Auction;
import com.server.model.BidTransaction;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuctionRepository implements IAuctionRepository {
    private static final Logger logger = LoggerFactory.getLogger(AuctionRepository.class);

    private final ItemRepository itemRepository;

    public AuctionRepository() {
        this.itemRepository = new ItemRepository();
    }

    @Override
    public Optional<Auction> findById(long auctionId) {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAuction(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi tìm phiên đấu giá theo id {}: {}", auctionId, e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public String findItemNameByItemId(long itemId) {
        return itemRepository.findItemNameByItemId(itemId);
    }

    @Override
    public List<Auction> findByStatusIn(List<Auction.AuctionStatus> statuses) {
        List<Auction> auctions = new ArrayList<>();
        if (statuses == null || statuses.isEmpty()) return auctions;

        List<String> statusList = statuses.stream().map(Enum::name).toList();
        String placeholders = String.join(",", Collections.nCopies(statusList.size(), "?"));
        String sql = "SELECT * FROM auctions WHERE status IN (" + placeholders + ")";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < statusList.size(); i++) {
                pstmt.setString(i + 1, statusList.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    auctions.add(mapResultSetToAuction(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi load phiên đấu giá: {}", e.getMessage(), e);
        }
        return auctions;
    }

    @Override
    public void save(Auction auction) {
        String sql = "UPDATE auctions SET current_highest_bid = ?, winner_id = ?, status = ?, end_time = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (auction.getCurrentHighestBid() != null) {
                pstmt.setBigDecimal(1, auction.getCurrentHighestBid());
            } else {
                pstmt.setBigDecimal(1, BigDecimal.ZERO);
            }

            if (auction.getWinnerId() != null && auction.getWinnerId() > 0) {
                pstmt.setLong(2, auction.getWinnerId());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }

            pstmt.setString(3, auction.getStatus().name());
            pstmt.setTimestamp(4, Timestamp.valueOf(auction.getEndTime()));
            pstmt.setLong(5, auction.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Lỗi update phiên đấu giá {}: {}", auction.getId(), e.getMessage(), e);
            throw new RuntimeException("Database từ chối cập nhật: " + e.getMessage());
        }
    }

    @Override
    public long create(Auction auction) {
        String sql = "INSERT INTO auctions (item_id, seller_id, start_time, end_time, step_price, current_highest_bid, winner_id, status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, auction.getItemId());
            pstmt.setLong(2, auction.getSellerId());
            pstmt.setTimestamp(3, Timestamp.valueOf(auction.getStartTime()));
            pstmt.setTimestamp(4, Timestamp.valueOf(auction.getEndTime()));
            pstmt.setBigDecimal(5, auction.getStepPrice() != null ? auction.getStepPrice() : BigDecimal.ZERO);
            pstmt.setBigDecimal(6, auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO);
            pstmt.setNull(7, Types.INTEGER);
            pstmt.setString(8, auction.getStatus().name());

            int rows = pstmt.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("Insert thất bại, không có dòng nào được tạo trong DB.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
        } catch (SQLException e) {
            logger.error("LỖI SQL KHI TẠO AUCTION: {}", e.getMessage(), e);
            throw new RuntimeException("Database từ chối lưu: " + e.getMessage());
        }
        return -1;
    }

    @Override
    public List<BidTransaction> findBidHistoryByAuction(long auctionId) {
        List<BidTransaction> bidHistory = new ArrayList<>();
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY timestamp DESC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, auctionId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    BidTransaction bid = new BidTransaction(
                            rs.getLong("auction_id"),
                            rs.getLong("bidder_id"),
                            rs.getBigDecimal("bid_amount")
                    );
                    bid.setId(rs.getLong("id"));

                    Timestamp ts = rs.getTimestamp("timestamp");
                    if (ts != null) {
                        bid.setTimestamp(ts.toLocalDateTime());
                    }

                    bid.setAutoBid(rs.getBoolean("is_auto_bid"));
                    bidHistory.add(bid);
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy lịch sử bid cho auction {}: {}", auctionId, e.getMessage(), e);
        }
        return bidHistory;
    }

    /**
     * [MỚI] Lấy danh sách các phiên đấu giá đã kết thúc mà bidderId là người thắng.
     * Query vào DB (không dùng cache) vì đây là dữ liệu đã kết thúc.
     */
    @Override
    public List<Auction> findWonAuctionsByBidderId(long bidderId) {
        List<Auction> wonAuctions = new ArrayList<>();
        // Tìm các auction đã FINISHED mà winner_id = bidderId
        String sql = "SELECT * FROM auctions WHERE winner_id = ? AND status = 'FINISHED' ORDER BY updated_at DESC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, bidderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    wonAuctions.add(mapResultSetToAuction(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy won auctions cho bidder {}: {}", bidderId, e.getMessage(), e);
        }
        return wonAuctions;
    }

    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        Auction auction = new Auction();
        auction.setId(rs.getLong("id"));
        auction.setItemId(rs.getLong("item_id"));
        auction.setSellerId(rs.getLong("seller_id"));

        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) auction.setStartTime(startTime.toLocalDateTime());

        Timestamp endTime = rs.getTimestamp("end_time");
        if (endTime != null) auction.setEndTime(endTime.toLocalDateTime());

        auction.setStepPrice(rs.getBigDecimal("step_price"));
        auction.setCurrentHighestBid(rs.getBigDecimal("current_highest_bid"));

        long winnerId = rs.getLong("winner_id");
        auction.setWinnerId(rs.wasNull() ? null : winnerId);

        auction.setStatus(Auction.AuctionStatus.valueOf(rs.getString("status")));
        return auction;
    }
}
