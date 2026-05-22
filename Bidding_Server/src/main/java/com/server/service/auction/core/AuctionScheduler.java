package com.server.service.auction.core;

import com.server.service.auction.processor.AutoBidProcessor;
import com.shared.dto.AuctionUpdateDTO;
import com.server.DAO.AuctionRepository;
import com.server.DAO.IBidderRepository;
import com.server.DAO.ItemRepository;
import com.server.DAO.UserRepository;
import com.server.model.Auction;
import com.server.model.Bidder;
import com.server.model.User;
import com.server.model.BidTransaction;
import com.server.websocket.AuctionEventListener;
import com.shared.dto.AuctionWinnerDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;

public class AuctionScheduler {
    private static final Logger logger = LoggerFactory.getLogger(AuctionScheduler.class);

    private final AuctionSharedState sharedState;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final IBidderRepository bidderRepository;
    private final ItemRepository itemRepository;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private AuctionEventListener eventListener;
    private final AuctionQueryService queryService;
    private final AutoBidProcessor autoBidProcessor;

    public AuctionScheduler(AuctionSharedState sharedState, AuctionRepository auctionRepository,
                            UserRepository userRepository, AuctionQueryService queryService,
                            IBidderRepository bidderRepository, AutoBidProcessor autoBidProcessor) {
        this.sharedState = sharedState;
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
        this.queryService = queryService;
        this.bidderRepository = bidderRepository;
        this.itemRepository = new ItemRepository();
        this.autoBidProcessor = autoBidProcessor;
    }

    public void setEventListener(AuctionEventListener listener) {
        this.eventListener = listener;
    }

    public void init() {
        try {
            List<Auction> auctions = auctionRepository.findByStatusIn(
                    List.of(Auction.AuctionStatus.SCHEDULED, Auction.AuctionStatus.OPEN, Auction.AuctionStatus.RUNNING));

            for (Auction auction : auctions) {
                sharedState.cacheAuction(auction);
                if (auction.getStatus() == Auction.AuctionStatus.SCHEDULED) {
                    scheduleAuctionStart(auction);
                } else if (auction.getStatus() == Auction.AuctionStatus.OPEN || auction.getStatus() == Auction.AuctionStatus.RUNNING) {
                    scheduleAuctionEnd(auction);
                }
            }
            logger.info("Initiatate {} auction successfully", auctions.size());
        } catch (Exception e) {
            logger.error("Error in AuctionScheduler", e);
        }
    }

    public void cacheAndScheduleAuction(Auction auction) {
        sharedState.cacheAuction(auction);
        scheduleAuctionEnd(auction);
    }

    public void scheduleAuctionStart(Auction auction) {
        long delay = Duration.between(LocalDateTime.now(), auction.getStartTime()).toMillis();
        if (delay > 0) {
            ScheduledFuture<?> newTask = scheduler.schedule(
                    () -> startAuction(auction.getId()),
                    delay, TimeUnit.MILLISECONDS
            );
            sharedState.scheduledTasks.put(auction.getId(), newTask);
        } else {
            startAuction(auction.getId());
        }
    }

    public void scheduleAuctionEnd(Auction auction) {
        long delay = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();
        if (delay > 0) {
            ScheduledFuture<?> existingTask = sharedState.scheduledTasks.get(auction.getId());
            if (existingTask != null) {
                existingTask.cancel(false);
            }

            ScheduledFuture<?> newTask = scheduler.schedule(
                    () -> finishAuction(auction.getId()),
                    delay, TimeUnit.MILLISECONDS
            );
            sharedState.scheduledTasks.put(auction.getId(), newTask);
        } else {
            finishAuction(auction.getId());
        }
    }

    private void startAuction(long auctionId) {
        ReentrantLock lock = sharedState.auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock());
        lock.lock();
        try {
            Auction auction = auctionRepository.findById(auctionId).orElse(null);
            if (auction != null && auction.getStatus() == Auction.AuctionStatus.SCHEDULED) {
                auction.setStatus(Auction.AuctionStatus.OPEN);
                auctionRepository.save(auction);
                cacheAndScheduleAuction(auction);
                logger.info("Auction {} started", auctionId);

                if (eventListener != null) {
                    eventListener.onAuctionCreated(queryService.toDetailDto(auction));
                }

                List<BidTransaction> initialAutoBids = new ArrayList<>();
                autoBidProcessor.process(auction, initialAutoBids);

                if (!initialAutoBids.isEmpty()) {
                    sharedState.bidQueue.offer(new BidPersistenceBatch(auction.getId(), initialAutoBids));
                }

                if (eventListener != null && auction.getWinnerId() != null) {
                    String bidderName = sharedState.resolveUserDisplayName(auction.getWinnerId(), "User");
                    long remaining = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();

                    AuctionUpdateDTO update = new AuctionUpdateDTO(
                            auction.getId(),
                            auction.getCurrentHighestBid(),
                            bidderName,
                            Math.max(0, remaining)
                    );
                    eventListener.onAuctionUpdate(update);
                }
            }
        } catch (Exception e) {
            logger.error("Error in starting auction {}: {}", auctionId, e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private void finishAuction(long auctionId) {
        ReentrantLock lock = sharedState.auctionLocks.get(auctionId);
        if (lock != null) {
            lock.lock();
            try {
                Auction auction = sharedState.auctionCache.get(auctionId);
                if (auction != null &&
                        (auction.getStatus() == Auction.AuctionStatus.RUNNING ||
                                auction.getStatus() == Auction.AuctionStatus.OPEN)) {

                    auction.setStatus(Auction.AuctionStatus.FINISHED);
                    auctionRepository.save(auction);
                    logger.info("Auction {} has finished. Winner: {}, Bid: {}",
                            auctionId,
                            auction.getWinnerId() != null ? auction.getWinnerId() : "None",
                            auction.getCurrentHighestBid());

                    if (auction.getWinnerId() != null && auction.getCurrentHighestBid() != null) {
                        deductWinnerBalance(auction.getWinnerId(), auction.getCurrentHighestBid(), auctionId);
                        creditSellerBalance(auction.getSellerId(), auction.getCurrentHighestBid(), auctionId, auction.getItemId());
                    }

                    if (auction.getWinnerId() != null && eventListener != null) {
                        try {
                            User winner = userRepository.getUserById(auction.getWinnerId());
                            String winnerName = (winner != null && winner.getFullName() != null) ? winner.getFullName() : "Unknown";
                            String itemName = "Name: #" + auction.getItemId();
                            AuctionWinnerDTO winnerDTO = new AuctionWinnerDTO(
                                    auction.getId(), itemName, auction.getWinnerId(), winnerName, auction.getCurrentHighestBid()
                            );
                            eventListener.onAuctionWon(winnerDTO);
                        } catch (Exception e) {
                            logger.error("Error loading auction winner: {}", e.getMessage(), e);
                        }
                    }

                    sharedState.auctionCache.remove(auctionId);
                    sharedState.auctionLocks.remove(auctionId);
                    sharedState.scheduledTasks.remove(auctionId);

                    if (eventListener != null) {
                        eventListener.onAuctionFinished(auctionId);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private void deductWinnerBalance(long winnerId, BigDecimal finalAmount, long auctionId) {
        try {
            Bidder winner = bidderRepository.getBidderById(winnerId);
            if (winner == null) {
                logger.error("Auction {}: Không tìm thấy Bidder ID {} để trừ tiền!", auctionId, winnerId);
                return;
            }

            boolean deducted = winner.deductFunds(finalAmount);
            if (!deducted) {
                logger.error("Auction {}: Không đủ số dư để trừ tiền Bidder ID {}. Cần: {}, Có: {}",
                        auctionId, winnerId, finalAmount, winner.getWalletBalance());
                return;
            }

            boolean saved = bidderRepository.updateBalance(winnerId, winner.getWalletBalance());
            if (saved) {
                logger.info("Auction {}: Đã trừ {} từ ví Bidder ID {}. Số dư còn lại: {}",
                        auctionId, finalAmount, winnerId, winner.getWalletBalance());

                Auction auction = auctionRepository.findById(auctionId).orElse(null);
                String itemName = "Phiên đấu giá #" + auctionId;
                if (auction != null) {
                    try {
                        String name = itemRepository.findItemNameByItemId(auction.getItemId());
                        if (name != null && !name.isBlank()) {
                            itemName = name;
                        }
                    } catch (Exception ex) {
                        logger.warn("Auction {}: Không lấy được tên sản phẩm: {}", auctionId, ex.getMessage());
                    }
                }

                String description = String.format(
                        "Thanh toán đấu giá thành công: %s (-%,.0f VNĐ)", itemName, finalAmount.doubleValue());
                boolean logged = bidderRepository.logTransaction(winnerId, finalAmount, "DEDUCT", description);
                if (logged) {
                    logger.info("Auction {}: Đã ghi lịch sử giao dịch trừ tiền cho Bidder ID {}", auctionId, winnerId);
                } else {
                    logger.error("Auction {}: Ghi lịch sử giao dịch thất bại cho Bidder ID {}", auctionId, winnerId);
                }
            } else {
                logger.error("Auction {}: Trừ tiền thành công trên RAM nhưng LỖI KHI LƯU DB cho Bidder ID {}!",
                        auctionId, winnerId);
            }
        } catch (Exception e) {
            logger.error("Auction {}: Lỗi khi trừ tiền Bidder ID {}: {}", auctionId, winnerId, e.getMessage(), e);
        }
    }

    private void creditSellerBalance(long sellerId, BigDecimal finalAmount, long auctionId, long itemId) {
        try {
            Bidder seller = bidderRepository.getBidderById(sellerId);
            if (seller == null) {
                logger.error("Auction {}: Không tìm thấy Seller ID {} để cộng tiền!", auctionId, sellerId);
                return;
            }

            BigDecimal newBalance = seller.getWalletBalance().add(finalAmount);
            boolean saved = bidderRepository.updateBalance(sellerId, newBalance);
            if (saved) {
                logger.info("Auction {}: Đã cộng {} vào ví Seller ID {}. Số dư mới: {}",
                        auctionId, finalAmount, sellerId, newBalance);

                String itemName = "Phiên đấu giá #" + auctionId;
                try {
                    String name = itemRepository.findItemNameByItemId(itemId);
                    if (name != null && !name.isBlank()) {
                        itemName = name;
                    }
                } catch (Exception ex) {
                    logger.warn("Auction {}: Không lấy được tên sản phẩm: {}", auctionId, ex.getMessage());
                }

                String description = String.format(
                        "Thu tiền đấu giá thành công: %s (+%,.0f VNĐ)", itemName, finalAmount.doubleValue());
                boolean logged = bidderRepository.logTransaction(sellerId, finalAmount, "DEPOSIT", description);
                if (logged) {
                    logger.info("Auction {}: Đã ghi lịch sử giao dịch cộng tiền cho Seller ID {}", auctionId, sellerId);
                } else {
                    logger.error("Auction {}: Ghi lịch sử giao dịch thất bại cho Seller ID {}", auctionId, sellerId);
                }
            } else {
                logger.error("Auction {}: Lỗi khi lưu DB khi cộng tiền cho Seller ID {}!", auctionId, sellerId);
            }
        } catch (Exception e) {
            logger.error("Auction {}: Lỗi khi cộng tiền Seller ID {}: {}", auctionId, sellerId, e.getMessage(), e);
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}