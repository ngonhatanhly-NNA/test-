package com.server.service.auction.core;

import com.server.model.BidTransaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BidPersistenceBatch {
    private final long auctionId;
    private final List<BidTransaction> bids;

    public BidPersistenceBatch(long auctionId, List<BidTransaction> bids) {
        this.auctionId = auctionId;
        this.bids = Collections.unmodifiableList(new ArrayList<>(bids));
    }

    public long getAuctionId() {
        return auctionId;
    }

    public List<BidTransaction> getBids() {
        return bids;
    }
}
