package com.client.util;

import java.util.function.Consumer;

/**
 * Ô tìm kiếm trên thanh header (Dashboard) đồng bộ với bộ lọc trên màn Explore.
 */
public final class DashboardSearchBridge {

    private static Consumer<String> onSearch;

    private DashboardSearchBridge() {
    }

    public static void setOnSearch(Consumer<String> listener) {
        onSearch = listener;
    }

    public static void publish(String query) {
        if (onSearch != null) {
            onSearch.accept(query != null ? query : "");
        }
    }
}
