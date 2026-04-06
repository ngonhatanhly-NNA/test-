package com.server.service;

import com.server.DAO.AuctionRepository;
import com.server.model.*;
import com.server.DAO.*;
import com.server.websocket.*;
import com.shared.dto.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
// Xuwr lis bat dong khi co hang ngan nguoi chay cung 1 luc
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final BidTransactionRepository bidRepository;

    // Cache & Locks theo spec
    private final ConcurrentHashMap<Long, Auction> auctionCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();
    private final LinkedBlockingQueue<BidTransaction> bidQueue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    // WebSocket broadcaster
    private final AuctionEventListener eventListener;

    public AuctionService(AuctionRepository auctionRepo, BidTransactionRepository bidRepo) {
        this.auctionRepository = auctionRepo;
        this.bidRepository = bidRepo;
        init();
    }

    private void init() {
        // 3. Load active auctions vào cache
        List<Auction> activeAuctions = auctionRepository.findByStatusIn(
                List.of(Auction.AuctionStatus.OPEN, Auction.AuctionStatus.RUNNING));

        activeAuctions.forEach(this::cacheAndScheduleAuction);
        startBidQueueProcessor();
    }

    private void cacheAndScheduleAuction(Auction auction) {
        auctionCache.put(auction.getId(), auction);
        auctionLocks.putIfAbsent(auction.getId(), new ReentrantLock());
        scheduleAuctionEnd(auction);
    }

    // 2. Logic đặt giá theo spec
    public AuctionUpdateDTO placeBid(BidRequestDTO request) {
        ReentrantLock lock = auctionLocks.get(request.getAuctionId());
        if (lock == null) throw new RuntimeException("Auction not found");

        lock.lock();
        try {
            Auction auction = auctionCache.get(request.getAuctionId());
            if (auction == null || auction.getStatus() != Auction.AuctionStatus.RUNNING) {
                throw new RuntimeException("Auction not running");
            }

            // Kiểm tra giá hợp lệ
            BigDecimal minBid = auction.getCurrentHighestBid().add(auction.getStepPrice());
            if (request.getBidAmount().compareTo(minBid) < 0) {
                throw new RuntimeException("Bid too low: " + minBid);
            }

            // Cập nhật RAM
            auction.setCurrentHighestBid(request.getBidAmount());
            auction.setWinnerId(request.getBidderId());

            // 5.1 Anti-sniping
            LocalDateTime now = LocalDateTime.now();
            if (Duration.between(now, auction.getEndTime()).getSeconds() < 30) {
                auction.setEndTime(now.plusSeconds(60));
                scheduleAuctionEnd(auction); // Reschedule
            }

            // Queue bid transaction
            BidTransaction transaction = new BidTransaction(
                    request.getAuctionId(), request.getBidderId(), request.getBidAmount());
            bidQueue.offer(transaction);

            // Broadcast qua WebSocket
            AuctionUpdateDTO update = createUpdateDTO(auction);
            if (eventListener != null) {
                eventListener.onAuctionUpdate(update);
            }

            return update;

        } finally {
            lock.unlock();
        }
    }

    private AuctionUpdateDTO createUpdateDTO(Auction auction) {
        String bidderName = "User_" + auction.getWinnerId();
        long remaining = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();
        return new AuctionUpdateDTO(auction.getId(), auction.getCurrentHighestBid(), bidderName, remaining);
    }

    private void scheduleAuctionEnd(Auction auction) {
        long delay = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();
        if (delay > 0) {
            scheduler.schedule(() -> finishAuction(auction.getId()), delay, TimeUnit.MILLISECONDS);
        }
    }

    private void finishAuction(long auctionId) {
        ReentrantLock lock = auctionLocks.get(auctionId);
        if (lock != null) {
            lock.lock();
            try {
                Auction auction = auctionCache.get(auctionId);
                if (auction != null && auction.getStatus() == Auction.AuctionStatus.RUNNING) {
                    auction.setStatus(Auction.AuctionStatus.FINISHED);
                    auctionRepository.save(auction); // Persist winner
                    auctionCache.remove(auctionId);
                    auctionLocks.remove(auctionId);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private void startBidQueueProcessor() {
        new Thread(() -> {
            while (true) {
                try {
                    BidTransaction bid = bidQueue.take();
                    bidRepository.save(bid);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "bid-processor").start();
    }

    // Setter cho WebSocket listener
    public void setEventListener(AuctionEventListener listener) {
        this.eventListener = listener;
    }
}