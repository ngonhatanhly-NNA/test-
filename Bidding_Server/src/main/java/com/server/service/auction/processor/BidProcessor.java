package com.server.service.auction.processor;

import com.server.model.Auction;
import com.server.model.BidTransaction;
import com.shared.dto.BidRequestDTO;
import java.util.Queue;

/**
 * Strategy Pattern: Xử lý bid sau khi validation thành công
 */
public interface BidProcessor {
    /**
     * Xử lý bid và trả về thông tin cập nhật
     */
    void process(BidRequestDTO request, Auction auction, Queue<BidTransaction> bidQueue);
}

