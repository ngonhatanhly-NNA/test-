package com.server.service.auction.processor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Queue;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.server.DAO.AuctionRepository;
import com.server.DAO.AutoBidRepository;
import com.server.DAO.BidderRepository;
import com.server.model.Auction;
import com.server.model.AutoBidTracker;
import com.server.model.BidTransaction;
import com.server.model.Bidder;
import com.server.model.Wallet; // Sử dụng Wallet model của team

/**
 * Lớp này hoạt động như một con Bot thông minh. Nó sẽ tự động quét danh sách những người
 * đã bật Auto-bid, tính toán giá mới và "đánh nhau" tranh top thay cho người thật.
 */
public class AutoBidProcessor {
    private final AutoBidRepository autoBidRepository;

    // [THÊM MỚI] Cần AuctionRepository để lưu cái giá cuối cùng mà Bot vừa chốt xuống DB
    private final AuctionRepository auctionRepository;

    // [THÊM MỚI] Cần BidderRepository để thao tác móc ví trừ tiền của Bot
    private final BidderRepository bidderRepository;

    private static final Logger logger = LoggerFactory.getLogger(AutoBidProcessor.class);

    // Constructor đã được nâng cấp để nhận thêm đồ chơi (Dependency Injection)
    public AutoBidProcessor(AutoBidRepository autoBidRepository, AuctionRepository auctionRepository, BidderRepository bidderRepository) {
        this.autoBidRepository = autoBidRepository;
        this.auctionRepository = auctionRepository;
        this.bidderRepository = bidderRepository;
    }

    /**
     * Hàm tính tiền nhẩm của Bot: Giá mới = Giá đỉnh hiện tại + Bước giá
     */
    private BigDecimal calculateNextBidAmount(BigDecimal currentBid, BigDecimal stepPrice) {
        if (currentBid == null || currentBid.compareTo(BigDecimal.ZERO) <= 0) {
            return stepPrice;
        }
        return currentBid.add(stepPrice);
    }

    /**
     * Sân chơi đẫm máu của các Bot Auto-bid
     */
    public void process(Auction auction, Queue<BidTransaction> bidQueue) {
        // 1. Kéo tất cả "đấu sĩ" (Bot) đang rảnh rỗi vào võ đài
        List<AutoBidTracker> activeBids = autoBidRepository.findAllActiveByAuction(auction.getId());
        List<AutoBidTracker> deactivatedBids = new ArrayList<>();

        boolean priceChanged = true; // Cờ hiệu: Chừng nào còn đứa nâng giá thì võ đài còn xoay vòng

        // ================= VÒNG LẶP ĐẤU TRƯỜNG =================
        while (priceChanged) {
            priceChanged = false; // Bắt đầu hiệp đấu, giả định chưa ai ra đòn

            for (AutoBidTracker autoBid : activeBids) {
                // Đang top 1 rồi thì ngồi im, không tự đấu với chính mình
                if (auction.getWinnerId() != null && autoBid.getBidderId() == auction.getWinnerId()) {
                    continue;
                }

                // Nhìn xem giá đỉnh đang là bao nhiêu
                BigDecimal currentBid = auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO;

                // Nhìn xem bước giá là bao nhiêu để cộng thêm
                BigDecimal auctionStep = auction.getStepPrice();
                if (auctionStep == null || auctionStep.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                // Nếu Bot này được chủ nạp cho bước giá VIP (Custom Step) thì ưu tiên dùng
                BigDecimal customStep = autoBid.getCustomStepPrice();
                BigDecimal actualStepPrice = auctionStep;
                if (customStep != null && customStep.compareTo(auctionStep) > 0) {
                    actualStepPrice = customStep;
                }

                // Bot tính nhẩm: Giá tối thiểu để cướp top là bao nhiêu?
                BigDecimal minBid = calculateNextBidAmount(currentBid, actualStepPrice);

                // TRỌNG TÀI CHECK VÍ: Móc ví ra xem tiền thật còn đủ không?
                Bidder botOwner = bidderRepository.getBidderById(autoBid.getBidderId());
                BigDecimal actualWalletBalance = (botOwner != null && botOwner.getWalletBalance() != null)
                        ? botOwner.getWalletBalance()
                        : BigDecimal.ZERO;

                // Điều kiện kép: Giá đặt phải nằm trong giới hạn Max (Cài đặt) VÀ Tiền ví phải đủ trả (Thực tế)
                if (autoBid.getMaxBidAmount().compareTo(minBid) >= 0 && actualWalletBalance.compareTo(minBid) >= 0) {

                    // Chốt số tiền sẽ đặt
                    BigDecimal autoBidAmount = calculateAutoBidAmount(currentBid, minBid, autoBid.getMaxBidAmount());

                    // Nếu giá này to hơn giá hiện tại, bắt đầu lật đổ Top 1
                    if (autoBidAmount.compareTo(currentBid) > 0) {

                        // [THÊM LOGIC TIỀN TỆ] Hoàn trả tiền cọc cho đứa Top 1 cũ (bị văng khỏi ngai vàng)
                        long oldWinnerId = auction.getWinnerId() != null ? auction.getWinnerId() : 0;
                        if (oldWinnerId > 0 && oldWinnerId != autoBid.getBidderId()) {
                            Bidder oldWinner = bidderRepository.getBidderById(oldWinnerId);
                            if (oldWinner != null) {
                                BigDecimal oldMain = oldWinner.getWalletBalance();
                                // Trả lại số tiền currentBid (là tiền cọc của nó) về ví chính
                                bidderRepository.updateWalletBalances(oldWinner.getId(), oldMain.add(currentBid), BigDecimal.ZERO);
                                logger.info("Auto-bid: Đã hoàn trả cọc {} cho kẻ thua cuộc {}", currentBid, oldWinner.getUsername());
                            }
                        }

                        // [THÊM LOGIC TIỀN TỆ] Trừ tiền ví chính của đứa Top 1 mới (Đóng băng tiền cọc)
                        BigDecimal main = botOwner.getWalletBalance();
                        // Tiền thực bị trừ đi autoBidAmount, và autoBidAmount đó chuyển sang ví Tạm
                        bidderRepository.updateWalletBalances(botOwner.getId(), main.subtract(autoBidAmount), autoBidAmount);
                        logger.info("Auto-bid: Đã đóng băng cọc {} của kẻ chiến thắng mới {}", autoBidAmount, botOwner.getUsername());

                        // CẬP NHẬT Trạng thái phiên đấu giá
                        auction.setCurrentHighestBid(autoBidAmount);
                        auction.setWinnerId(autoBid.getBidderId());

                        // [QUAN TRỌNG] Lưu ngay kẻo lỡ - Ghi đè trạng thái Auction xuống Database
                        try {
                            auctionRepository.save(auction);
                        } catch (Exception e) {
                            logger.error("Lỗi chí mạng: Không lưu được giá Auto-bid xuống DB: {}", e.getMessage());
                            throw new RuntimeException("DB từ chối lưu Auto-bid: " + e.getMessage());
                        }

                        // Viết "Sổ thiên tào": Đẩy lịch sử giao dịch vào hàng đợi để thread khác lưu từ từ
                        BidTransaction autoBidTx = new BidTransaction(auction.getId(), autoBid.getBidderId(), autoBidAmount);
                        autoBidTx.setAutoBid(true);
                        bidQueue.offer(autoBidTx);

                        logger.info("Bot {} đã cướp cờ thành công với giá: {}", autoBid.getBidderId(), autoBidAmount);

                        // Có biến! Báo hiệu cho các Bot khác biết giá đã đổi để chúng nó tính lại từ đầu
                        priceChanged = true;
                    }
                } else {
                    // Bot hết giới hạn (Limit) HOẶC hết tiền thật trong ví -> CÚT KHỎI VÕ ĐÀI!
                    autoBid.setActive(false);
                    deactivatedBids.add(autoBid);
                    logger.info("Bot {} bị loại do hết MaxLimit hoặc cạn tiền trong ví.", autoBid.getBidderId());
                }
            }

            // Dọn rác: Đuổi những thằng Bot cạn tiền khỏi mảng để vòng while đỡ phải lặp lại
            activeBids.removeIf(bid -> !bid.isActive());
        }

        // ================= KẾT THÚC ĐẤU TRƯỜNG =================
        // Gạch tên mấy đứa nghèo khỏi danh sách đăng ký Auto-bid trong Database
        for (AutoBidTracker deactivated : deactivatedBids) {
            autoBidRepository.deactivate(auction.getId(), deactivated.getBidderId());
        }
    }

    /**
     * Hàm tối ưu lợi nhuận cho người mua: Đặt giá VỪA ĐỦ ĐỂ THẮNG, không dại gì phang thẳng max tiền.
     */
    private BigDecimal calculateAutoBidAmount(BigDecimal currentBid, BigDecimal minBid, BigDecimal maxAmount) {
        if (maxAmount.compareTo(currentBid) <= 0) return currentBid;
        if (maxAmount.compareTo(minBid) < 0) return maxAmount; // Nếu tiền Max còn lại ít hơn bước giá tối thiểu, thì khô máu bằng số tiền Max
        return minBid; // Mặc định: Chỉ xuất ra số tiền nhỏ nhất đủ để cướp Top 1.
    }
}