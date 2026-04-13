package com.client.util;

/**
 * Cho phép màn con yêu cầu chuyển sang tab Live Auctions
 * mà không phụ thuộc trực tiếp vào DashboardController.
 */
public final class DashboardNavigation {

    private static Runnable openLiveAuctions;

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
}
