package com.client.network;
import com.shared.dto.*;
// Đường mạng đến Controller để nối server

import com.google.gson.Gson;
import com.shared.dto.BidRequestDTO;
import java.net.URI;
import java.net.http.*;

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
    public static String placeBid (long auctionId, long bidderId, java.math.BigDecimal amount) throws Exception{
        BidRequestDTO dto = new BidRequestDTO(auctionId, bidderId, amount);
        String jsonBody = gson.toJson(dto);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(SERVER_URL + "/bid"))
                .header("Bid-Transaction", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();

        // tóm request, và tự động biên dịch byte thô thành Sirng và trả về
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
