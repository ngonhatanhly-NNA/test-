package com.server.service;
import com.server.model.BidTransaction;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionItem {
    // --- CÁC BIẾN (VARIABLES) ---
    private int id;                    // id: Mã số định danh của sản phẩm
    private String title;              // title: Tên của sản phẩm (ví dụ: Laptop Dell)
    private double currentPrice;       // currentPrice: Giá tiền hiện tại đang cao nhất
    private double bidIncrement;       // bidIncrement: Bước giá (số tiền ít nhất phải cộng thêm)
    private String highestBidder;      // highestBidder: Tên người đang trả giá cao nhất
    private LocalDateTime startTime;   // startTime: Thời gian bắt đầu đấu giá
    private LocalDateTime endTime;     // endTime: Thời gian kết thúc đấu giá
    private boolean isFinished;        // isFinished: Đã kết thúc hay chưa (Đúng/Sai)
    private String status;             // status: Trạng thái (Ví dụ: "RUNNING" - đang chạy)
    private List<BidHistory> history = new ArrayList<>(); // luu tru lich su dau gia
    public static class BidHistory {
        public String bidder;
        public double price;
        public LocalDateTime time;

        public BidHistory(String bidder, double price, LocalDateTime time) {
            this.bidder = bidder;
            this.price = price;
            this.time = time;
        }
    }
    // --- HÀM KHỞI TẠO (CONSTRUCTOR) ---
    public AuctionItem(int id, String title, double startPrice, double bidIncrement, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.title = title;
        this.currentPrice = startPrice; // Lúc mới tạo, giá hiện tại chính là giá khởi điểm
        this.bidIncrement = bidIncrement;
        this.highestBidder = "None";    // Lúc mới tạo, chưa có ai đặt giá
        this.startTime = startTime;
        this.endTime = endTime;
        this.isFinished = false;        // Mới tạo thì chưa kết thúc
        this.status = "RUNNING";
    }
    // --- CÁC HÀM GETTER/SETTER (Để Service có thể đọc và sửa dữ liệu) ---
    public int getId() { return id; }
    public String getTitle() { return title; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public double getBidIncrement() { return bidIncrement; }

    public String getHighestBidder() { return highestBidder; }
    public void setHighestBidder(String highestBidder) { this.highestBidder = highestBidder; }

    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public boolean isFinished() { return isFinished; }
    public void setFinished(boolean finished) { isFinished = finished; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<BidHistory> getHistory() {return history;}
    public void addHistoryRecord(String bidder, double price) {this.history.add(new BidHistory(bidder, price, LocalDateTime.now()));}



}