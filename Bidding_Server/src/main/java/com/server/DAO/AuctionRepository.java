package com.server.DAO;

import com.server.config.DBConnection;
import com.server.model.Auction;
import com.server.model.BidTransaction;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

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
        // This now works without try-catch because ItemRepository handles its own errors
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
        // ... implementation
    }

    @Override
    public long create(Auction auction) {
        // ... implementation
        return -1;
    }

    @Override
    public List<BidTransaction> findBidHistoryByAuction(long auctionId) {
        // ... implementation
        return new ArrayList<>();
    }

    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        // This method can still throw SQLException, and it will be caught by the callers
        Auction auction = new Auction();
        // ... mapping logic
        return auction;
    }
}
