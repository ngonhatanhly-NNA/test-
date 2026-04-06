package com.shared.network;


// Không cần implements Serializeable nữa khi làm việc với ưebsocket 
public class Request {
    private String action; // "REGISTER", "LOGIN"
    private String payload; // BẮT BUỘC LÀ STRING (Chứa chuỗi JSON)

    public Request(String action, String payload) {
        this.action = action;
        this.payload = payload;
    }

    public String getAction() { return this.action; }
    public String getPayload() { return this.payload; }
}