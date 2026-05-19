package com.client.util;

/**
 * Cho phép màn con yêu cầu chuyển sang tab Live Auctions
 * mà không phụ thuộc trực tiếp vào DashboardController.
 */
public final class DashboardNavigation {

    private static Runnable openLiveAuctions;
    private static java.util.function.Consumer<Long> navigateToAuctionDetail;

    private DashboardNavigation() {
    }

    public static void setOpenLiveAuctions(Runnable action) {
        openLiveAuctions = action;
    }

    public static void openLiveAuctions() {
        if (openLiveAuctions != null) {
            openLiveAuctions.run();
        }
    }

    public static void setNavigateToAuctionDetail(java.util.function.Consumer<Long> action) {
        navigateToAuctionDetail = action;
    }

    public static void navigateToAuctionDetail(long auctionId) {
        if (navigateToAuctionDetail != null) {
            navigateToAuctionDetail.accept(auctionId);
        }
    }
}
