package com.client.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shared.dto.*;
import com.shared.network.Response;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;

/**
 * REST tới Bidding_Server (Javalin cổng 7070, HTTP — không dùng HTTPS trừ khi bạn cấu hình TLS).
 */
public class AuctionNetwork {

    private static final String BASE_URL = "http://localhost:7070/api/auctions";
    private static final Gson gson = new Gson();

    private static final Type AUCTION_DETAIL_LIST = new TypeToken<List<AuctionDetailDTO>>() {
    }.getType();

    public static Response parseResponse(String jsonBody) {
        return gson.fromJson(jsonBody, Response.class);
    }

    /** Danh sách phiên đang mở; nếu lỗi parse trả list rỗng. */
    public static List<AuctionDetailDTO> parseActiveAuctionList(Response response) {
        if (response == null || response.getData() == null || !"SUCCESS".equals(response.getStatus())) {
            return Collections.emptyList();
        }
        try {
            return gson.fromJson(gson.toJson(response.getData()), AUCTION_DETAIL_LIST);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static String getActiveAuctions() throws Exception {
        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(BASE_URL + "/active"))
                .GET()
                .build();
        HttpResponse<String> response = NetworkClient.getInstance().send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public static String getAuctionDetail(long auctionId) throws Exception {
        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(BASE_URL + "/" + auctionId))
                .GET()
                .build();
        HttpResponse<String> response = NetworkClient.getInstance().send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public static AuctionDetailDTO parseAuctionDetail(Response response) {
        if (response == null || response.getData() == null || !"SUCCESS".equals(response.getStatus())) {
            return null;
        }
        try {
            return gson.fromJson(gson.toJson(response.getData()), AuctionDetailDTO.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String placeBid(long auctionId, long bidderId, BigDecimal amount) throws Exception {
        BidRequestDTO dto = new BidRequestDTO(auctionId, bidderId, amount);
        return postJson(BASE_URL + "/bid", gson.toJson(dto));
    }

    public static String placeBidWithAutoBid(long auctionId, long bidderId, BigDecimal amount, BigDecimal maxAutoBidAmount) throws Exception {
        BidRequestDTO dto = new BidRequestDTO(auctionId, bidderId, amount, true, maxAutoBidAmount);
        return postJson(BASE_URL + "/bid", gson.toJson(dto));
    }

    public static String cancelAutoBid(long auctionId, long bidderId) throws Exception {
        AutoBidCancelDTO cancelDto = new AutoBidCancelDTO(auctionId, bidderId);
        return postJson(BASE_URL + "/" + auctionId + "/auto-bid/cancel", gson.toJson(cancelDto));
    }

    public static String updateAutoBid(long auctionId, long bidderId, BigDecimal maxBidAmount) throws Exception {
        AutoBidUpdateDTO updateDto = new AutoBidUpdateDTO(auctionId, bidderId, maxBidAmount);
        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(BASE_URL + "/" + auctionId + "/auto-bid/update"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(updateDto)))
                .build();
        HttpResponse<String> response = NetworkClient.getInstance().send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private static String postJson(String uri, String jsonBody) throws Exception {
        HttpRequest request = NetworkClient.newRequestBuilder(URI.create(uri))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> response = NetworkClient.getInstance().send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
