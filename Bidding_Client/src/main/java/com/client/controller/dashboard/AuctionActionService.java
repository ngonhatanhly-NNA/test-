package com.client.controller.dashboard;

import com.client.network.AuctionNetwork;
import com.client.session.ClientSession;
import com.shared.dto.AuctionDetailDTO;
import com.shared.network.Response;
import javafx.application.Platform;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AuctionActionService {

    private final ExecutorService ioPool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "auction-http");
        t.setDaemon(true);
        return t;
    });

    public void fetchActiveAuctions(Consumer<List<AuctionDetailDTO>> onSuccess, Consumer<Throwable> onError) {
        ioPool.execute(() -> {
            try {
                List<AuctionDetailDTO> list = AuctionNetwork.getActiveAuctions();
                Platform.runLater(() -> onSuccess.accept(list));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept(e));
            }
        });
    }

    public void placeBid(long currentAuctionId, String bidAmountStr, boolean isAutoBid, String maxAutoBidStr, String customStepStr, Consumer<Response> onResult, Consumer<String> onError) {
        ioPool.execute(() -> {
            try {
                String rawDetail = AuctionNetwork.getAuctionDetail(currentAuctionId);
                Response res = AuctionNetwork.parseResponse(rawDetail);
                AuctionDetailDTO detail = AuctionNetwork.parseAuctionDetail(res);

                Platform.runLater(() -> {
                    try {
                        BigDecimal addedAmount = new BigDecimal(bidAmountStr.trim());
                        BigDecimal stepPrice = (detail != null && detail.getStepPrice() != null) ? detail.getStepPrice() : BigDecimal.ZERO;
                        
                        if (addedAmount.compareTo(stepPrice) < 0) {
                            onError.accept("Adding bid must be greater than: " + AuctionUIHelper.formatMoney(stepPrice) + " đ");
                            return;
                        }

                        long bidderId = ClientSession.getUserId();
                        String raw2;

                        if (isAutoBid) {
                            BigDecimal maxAutoBid = new BigDecimal(maxAutoBidStr.trim());
                            BigDecimal customStep = (customStepStr != null && !customStepStr.trim().isEmpty()) ? new BigDecimal(customStepStr.trim()) : null;
                            raw2 = AuctionNetwork.placeBidWithAutoBid(currentAuctionId, bidderId, addedAmount, maxAutoBid, customStep);
                        } else {
                            raw2 = AuctionNetwork.placeBid(currentAuctionId, bidderId, addedAmount);
                        }
                        
                        Response res2 = AuctionNetwork.parseResponse(raw2);
                        onResult.accept(res2);

                    } catch (NumberFormatException e) {
                        onError.accept("Please enter correct number");
                    } catch (Exception e) {
                        onError.accept("Error in bidding: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept("Error in checking auction: " + e.getMessage()));
            }
        });
    }

    public void cancelAutoBid(long currentAuctionId, Consumer<Response> onResult, Consumer<String> onError) {
        ioPool.execute(() -> {
            try {
                String raw = AuctionNetwork.cancelAutoBid(currentAuctionId, ClientSession.getUserId());
                Response res = AuctionNetwork.parseResponse(raw);
                Platform.runLater(() -> onResult.accept(res));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept("Error in quit auto-bid: " + e.getMessage()));
            }
        });
    }

    public void updateAutoBid(long currentAuctionId, String newMaxBidStr, String customStepStr, Consumer<Response> onResult, Consumer<String> onError) {
        ioPool.execute(() -> {
            try {
                BigDecimal newMaxBid = new BigDecimal(newMaxBidStr.trim());
                BigDecimal customStep = (customStepStr != null && !customStepStr.trim().isEmpty()) ? new BigDecimal(customStepStr.trim()) : null;
                
                String raw = AuctionNetwork.updateAutoBid(currentAuctionId, ClientSession.getUserId(), newMaxBid, customStep);
                Response res = AuctionNetwork.parseResponse(raw);
                Platform.runLater(() -> onResult.accept(res));
            } catch (Exception e) {
                Platform.runLater(() -> onError.accept("Lỗi cập nhật auto-bid: " + e.getMessage()));
            }
        });
    }

    public ExecutorService getIoPool() {
        return ioPool;
    }
}