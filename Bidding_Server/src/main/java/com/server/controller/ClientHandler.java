package com.server.controller;

import com.server.service.AuctionService;
import com.server.model.Bidder;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.net.Socket;

// Lớp này là Controller, chạy mỗi khi có 1 Client kết nối vào
public class ClientHandler extends Thread {
    private Socket socket;
    private AuctionService auctionService;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket socket, AuctionService service) {
        this.socket = socket;
        this.auctionService = service;
    }

    @Override
    public void run() {
        try {
            // Nhận request từ Client
            String request = (String) in.readObject();

            if (request.startsWith("PLACE_BID")) {
                // Tách data từ request
                int auctionId = 1;
                Bidder bidder = new Bidder(); // Lấy từ session
                BigDecimal amount = BigDecimal.valueOf(5000);

                // Controller gọi Service
                boolean success = auctionService.placeBid(auctionId, bidder, amount);

                // Trả kết quả về Client
                if(success) {
                    out.writeObject("BID_SUCCESS");
                } else {
                    out.writeObject("BID_FAIL");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}