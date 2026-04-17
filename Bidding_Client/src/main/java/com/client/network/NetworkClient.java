package com.client.network;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;

/**
 * Class này là "Tổng đài Shipper" dùng chung cho toàn bộ ứng dụng JavaFX.
 * Nó giữ duy nhất MỘT cái ví (CookieManager) để nhớ trạng thái Đăng nhập.
 */
public class NetworkClient {

    // Tạo 1 cái ví dùng chung
    private static final CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

    // Tạo 1 thằng Shipper duy nhất, giao cái ví cho nó giữ
    private static final HttpClient SHARED_CLIENT = HttpClient.newBuilder()
            .cookieHandler(cookieManager)
            .build();

    // Các class khác (AuthNetwork, ItemNetwork) muốn gửi thư thì gọi hàm này để lấy thằng Shipper xịn
    public static HttpClient getInstance() {
        return SHARED_CLIENT;
    }

    // Hàm này dùng để vứt cái thẻ đi khi người dùng bấm Đăng Xuất
    public static void clearCookies() {
        cookieManager.getCookieStore().removeAll();
    }
}