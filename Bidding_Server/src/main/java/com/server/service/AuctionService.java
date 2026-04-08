package com.server.service;

import com.server.model.*;
import com.server.DAO.*;
import com.server.websocket.*;
import com.shared.dto.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionService {

    // Kết nối với bên DAO
    private final AuctionRepository auctionRepository;
    private final BidTransactionRepository bidRepository;

    // ConcurrentHashMap an toàn hơn Normal do xử lí concurrent
    private final ConcurrentHashMap<Long, Auction> auctionCache = new ConcurrentHashMap<>();
    // Mỗi phiên đấu giá có 1 ở riêng tránh xung đột v người A vào phòng A, k ảnh hưởng B vào phòng B
    private final ConcurrentHashMap<Long, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    // Dùng để theo dõi và hủy các lịch đóng phiên đấu giá (cần cho anti-sniping)
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    // Hàng đợi xếp hàng các lượt đặt giá để chờ lưu vào DB
    private final LinkedBlockingQueue<BidTransaction> bidQueue = new LinkedBlockingQueue<>();
    // Tjw đóng các phiên dấu giá
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    // Loa phát thanh trực tếp dùng Ưebsocket
    private AuctionEventListener eventListener;

    public AuctionService(AuctionRepository auctionRepo, BidTransactionRepository bidRepo) {
        this.auctionRepository = auctionRepo;
        this.bidRepository = bidRepo;
        setEventListener(new Broadcaster()); //  Cắm điện đến loa phát thanh trong buổi đấu giá
        init(); // TÌm các phiên đang hoạt động luôn
    }

    // Timf cas phien da gia dang hoat dong
    private void init() {
        List<Auction> activeAuctions = auctionRepository.findByStatusIn(
                List.of(Auction.AuctionStatus.OPEN, Auction.AuctionStatus.RUNNING));

        activeAuctions.forEach(this::cacheAndScheduleAuction);
        startBidQueueProcessor();
    }

    // Lấy từ SQL lên RAM và đặt lichj
    private void cacheAndScheduleAuction(Auction auction) {
        auctionCache.put(auction.getId(), auction);
        auctionLocks.putIfAbsent(auction.getId(), new ReentrantLock());
        scheduleAuctionEnd(auction);
    }

    // Khóa ở khóa phòng đấu gi này
    public AuctionUpdateDTO placeBid(BidRequestDTO request) {
        ReentrantLock lock = auctionLocks.get(request.getAuctionId());
        if (lock == null) throw new RuntimeException("Không tìm thấy phiên đấu giá (Auction không có sẵn trong bộ nhớ)");

        lock.lock();
        try {
            Auction auction = auctionCache.get(request.getAuctionId());
            // Đã fix lỗi Logic: Cho phép đặt giá khi đang OPEN
            if (auction == null || (auction.getStatus() != Auction.AuctionStatus.RUNNING && auction.getStatus() != Auction.AuctionStatus.OPEN)) {
                throw new RuntimeException("Phiên đấu giá chưa bắt đầu hoặc đã kết thúc!");
            }

            // Kiểm tra giá hợp lệ
            BigDecimal currentBid = auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO;
            BigDecimal minBid = currentBid.add(auction.getStepPrice());
            if (request.getBidAmount().compareTo(minBid) < 0) {
                throw new RuntimeException("Giá đặt quá thấp! Giá tối thiểu hiện tại là: " + minBid);
            }

            // Chuyển status nếu đây là bid đầu tiên
            if (auction.getStatus() == Auction.AuctionStatus.OPEN) {
                auction.setStatus(Auction.AuctionStatus.RUNNING);
            }

            // Cập nhật RAM
            auction.setCurrentHighestBid(request.getBidAmount());
            auction.setWinnerId(request.getBidderId());

            // Anti-sniping , chống giây cuối đặt giá cao hơn
            LocalDateTime now = LocalDateTime.now();
            long secondsRemaining = Duration.between(now, auction.getEndTime()).getSeconds();

            if (secondsRemaining > 0 && secondsRemaining < 30) {
                // Nếu còn dưới 30s thì ccongjt hêm 60s vào giờ
                auction.setEndTime(now.plusSeconds(60));
                scheduleAuctionEnd(auction); // Hàm này giờ sẽ an toàn hủy task cũ
            } else if (secondsRemaining <= 0) {
                throw new RuntimeException("Phiên đấu giá đã kết thúc!");
            }

            // Queue bid transaction, gói thông tin lại và chuyển bị chờ đẩy vào SQL
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
        return new AuctionUpdateDTO(auction.getId(), auction.getCurrentHighestBid(), bidderName, Math.max(0, remaining));
    }

    // Đặt lihcj, hẹn giờ kết thúc đấu giá
    private void scheduleAuctionEnd(Auction auction) {
        long delay = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();
        if (delay > 0) {
            // HỦY TASK CŨ (nếu có) để tránh xung đột
            ScheduledFuture<?> existingTask = scheduledTasks.get(auction.getId());
            if (existingTask != null) {
                existingTask.cancel(false);
            }

            // LÊN LỊCH TASK MỚI
            ScheduledFuture<?> newTask = scheduler.schedule(() -> finishAuction(auction.getId()), delay, TimeUnit.MILLISECONDS);
            scheduledTasks.put(auction.getId(), newTask);
        } else {
            finishAuction(auction.getId());
        }
    }

    ///  tự động lấy khóa ra, khóa trong lúc đóng tránh bấm đặt gia 0.01 s cyoois
    private void finishAuction(long auctionId) {
        ReentrantLock lock = auctionLocks.get(auctionId);
        if (lock != null) {
            lock.lock();
            try {
                Auction auction = auctionCache.get(auctionId);
                if (auction != null && (auction.getStatus() == Auction.AuctionStatus.RUNNING || auction.getStatus() == Auction.AuctionStatus.OPEN)) {
                    auction.setStatus(Auction.AuctionStatus.FINISHED);
                    auctionRepository.save(auction);

                    // Dọn dẹp cache, nhẹ Server , lư vào DB
                    auctionCache.remove(auctionId);
                    auctionLocks.remove(auctionId);
                    scheduledTasks.remove(auctionId);

                    System.out.println("Đã đóng phiên đấu giá: " + auctionId);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private void startBidQueueProcessor() {
        new Thread(() -> { // tạo thread chạy vô tận, làm việc tới khi tắt server
            while (true) {
                try {
                    BidTransaction bid = bidQueue.take();
                    try {
                        // Đã fix: Bọc try-catch để ngăn Thread bị "chết ngầm" do lỗi DB
                        bidRepository.save(bid);
                    } catch (Exception dbError) {
                        System.err.println("Lỗi lưu DB cho BidTransaction (Auction " + bid.getAuctionId() + "): " + dbError.getMessage());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Hàng đợi Bid bị gián đoạn, dừng processor.");
                    break;
                }
            }
        }, "bid-processor").start();
    }

    public void setEventListener(AuctionEventListener listener) {
        this.eventListener = listener;
    }


    // Lấy danh sách đang chạy để hiển thị lên UI Client
    public List<AuctionDetailDTO> getActiveAuctions() {
        return auctionCache.values().stream() // các phòng đấu giá đặt lên băng chuyền, lọc ra cái đang ở
                .filter(a -> a.getStatus() == Auction.AuctionStatus.RUNNING || a.getStatus() == Auction.AuctionStatus.OPEN)
                .map(a -> {
                    long remaining = java.time.Duration.between(LocalDateTime.now(), a.getEndTime()).toMillis();
                    String bidderName = a.getWinnerId() != null ? "User_" + a.getWinnerId() : "No bids";
                    return new AuctionDetailDTO(
                            a.getId(), a.getItemId(), "Item #" + a.getItemId(), 
                            a.getCurrentHighestBid(), bidderName, Math.max(0, remaining), a.getStepPrice()
                    );
                })
                .toList();
	}
}