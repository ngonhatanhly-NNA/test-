package com.client.util;

import java.util.function.Consumer;

/**
 * Ô tìm kiếm trên thanh header (Dashboard) đồng bộ với bộ lọc trên màn Explore.
 */
public final class DashboardSearchBridge {
    private static Consumer<String> onSearchListener;
    private static String lastQuery = ""; // Lưu lại từ khóa
    private static boolean chkElectronics, chkArt, chkVehicle;

    private DashboardSearchBridge() {
    }

    public static void setOnSearch(Consumer<String> listener) {
        onSearchListener = listener;
        if (onSearchListener != null && !lastQuery.isEmpty()) {
            onSearchListener.accept(lastQuery);
        }
    }

    public static void publish(String query) {
        lastQuery = (query != null) ? query : "";
        if (onSearchListener != null) {
            onSearchListener.accept(lastQuery);
        }
    }

    public static String getLastQuery() {
        return lastQuery;
    }
	
    public static void resetForTest() {
        onSearchListener = null;
        lastQuery = "";
    }
}