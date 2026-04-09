package com.client.network;
import com.shared.dto.*;
// Đường mạng đến Controller để nối server

import com.google.gson.Gson;
import com.shared.dto.BidRequestDTO;
import java.net.URI;
import java.net.http.*;
import java.math.BigDecimal;

public class AuctionNetwork {
    private static final String SERVER_URL = "https://localhost:7070/api/auctions";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    public static final String getActiveAuctions () throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(SERVER_URL + "/active")).
                GET().build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body(); // Trả về chuỗi JSON chứa danh sách
    }

    // Gửi lệnh đặt giá từ CLient để xem đc k ở Server
    public static String placeBid (long auctionId, long bidderId, BigDecimal amount) throws Exception{
        BidRequestDTO dto = new BidRequestDTO(auctionId, bidderId, amount);
        String jsonBody = gson.toJson(dto);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(SERVER_URL + "/bid"))
                .header("Bid-Transaction", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();

        // tóm request, và tự động biên dịch byte thô thành Sirng và trả về
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    // Đặt giá với auto-bid được kích hoạt
    public static String placeBidWithAutoBid(long auctionId, long bidderId, BigDecimal amount, BigDecimal maxAutoBidAmount) throws Exception {
        BidRequestDTO dto = new BidRequestDTO(auctionId, bidderId, amount, true, maxAutoBidAmount);
        String jsonBody = gson.toJson(dto);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(SERVER_URL + "/bid"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    // Hủy auto-bid
    public static String cancelAutoBid(long auctionId, long bidderId) throws Exception {
        AutoBidCancelDTO cancelDto = new AutoBidCancelDTO(bidderId);
        String jsonBody = gson.toJson(cancelDto);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/" + auctionId + "/auto-bid/cancel"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    // Cập nhật giá tối đa auto-bid
    public static String updateAutoBid(long auctionId, long bidderId, BigDecimal maxBidAmount) throws Exception {
        AutoBidUpdateDTO updateDto = new AutoBidUpdateDTO(bidderId, maxBidAmount);
        String jsonBody = gson.toJson(updateDto);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/" + auctionId + "/auto-bid/update"))
                .header("Content-Type", "application/json")
                .method("PUT", HttpRequest.BodyPublishers.ofString(jsonBody)).build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
