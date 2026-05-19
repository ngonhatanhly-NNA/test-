package com.server.model;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Value Object: Đóng gói toàn bộ logic quản lý ví
 * - mainBalance: Ví chính (từ nạp tiền hoặc rút ra)
 * - tempBalance: Ví tạm thời (dự phòng cho các tính năng mở rộng)
 * 
 * Lợi ích:
 * - Isolate wallet logic từ Bidder/Seller
 * - Dễ test riêng biệt
 * - Thêm feature mà không ảnh hưởng User hierarchy
 */
public class Wallet {
    private static final Logger logger = LoggerFactory.getLogger(Wallet.class);
    
    private BigDecimal mainBalance;      // Ví chính
    private BigDecimal tempBalance;      // Ví tạm thời

    // --- CONSTRUCTORS ---
    public Wallet() {
        this.mainBalance = BigDecimal.ZERO;
        this.tempBalance = BigDecimal.ZERO;
    }

    public Wallet(BigDecimal mainBalance, BigDecimal tempBalance) {
        this.mainBalance = mainBalance != null ? mainBalance : BigDecimal.ZERO;
        this.tempBalance = tempBalance != null ? tempBalance : BigDecimal.ZERO;
    }

    // Copy constructor
    public Wallet(Wallet other) {
        this.mainBalance = new BigDecimal(other.mainBalance.toPlainString());
        this.tempBalance = new BigDecimal(other.tempBalance.toPlainString());
    }

    // --- GETTERS & SETTERS ---
    public BigDecimal getMainBalance() {
        return mainBalance;
    }

    public void setMainBalance(BigDecimal mainBalance) {
        this.mainBalance = mainBalance != null ? mainBalance : BigDecimal.ZERO;
    }

    public BigDecimal getTempBalance() {
        return tempBalance;
    }

    public void setTempBalance(BigDecimal tempBalance) {
        this.tempBalance = tempBalance != null ? tempBalance : BigDecimal.ZERO;
    }

    public BigDecimal getTotalBalance() {
        return mainBalance.add(tempBalance);
    }

    // --- BUSINESS LOGIC ---

    /**
     * Thêm tiền vào ví
     * @param amount Số tiền cần thêm
     * @param isTemp true = ví tạm thời, false = ví chính
     * @return true nếu thành công
     */
    public boolean addFunds(BigDecimal amount, boolean isTemp) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("Lỗi: Số tiền phải lớn hơn 0");
            return false;
        }

        if (isTemp) {
            this.tempBalance = this.tempBalance.add(amount);
        } else {
            this.mainBalance = this.mainBalance.add(amount);
        }
        
        logger.info("Thêm tiền thành công. Số tiền: {}, loại ví: {}", amount, isTemp ? "TẠM" : "CHÍNH");
        return true;
    }

    /**
     * Trừ tiền từ ví (ưu tiên mainBalance)
     * @param amount Số tiền cần trừ
     * @return true nếu đủ tiền, false nếu không đủ
     */
    public boolean deductFunds(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("Lỗi: Số tiền không hợp lệ");
            return false;
        }

        // Ưu tiên trừ từ mainBalance
        if (this.mainBalance.compareTo(amount) >= 0) {
            this.mainBalance = this.mainBalance.subtract(amount);
            logger.info("Trừ tiền từ ví chính thành công. Số tiền: {}", amount);
            return true;
        }

        // Nếu mainBalance không đủ, kiểm tra cảng tổng số dư
        BigDecimal totalBalance = getTotalBalance();
        if (totalBalance.compareTo(amount) >= 0) {
            // Trừ hết mainBalance, phần còn lại từ tempBalance
            BigDecimal remaining = amount.subtract(this.mainBalance);
            this.mainBalance = BigDecimal.ZERO;
            this.tempBalance = this.tempBalance.subtract(remaining);
            logger.info("Trừ tiền từ cả hai ví thành công. Số tiền: {}", amount);
            return true;
        }

        logger.error("Lỗi: Số dư không đủ. Cần: {}, có: {}", amount, totalBalance);
        return false;
    }

    /**
     * Rút tiền từ ví chính về tài khoản ngân hàng (chỉ rút từ mainBalance)
     * @param amount Số tiền cần rút
     * @return true nếu thành công
     */
    public boolean withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("Lỗi: Số tiền rút phải lớn hơn 0");
            return false;
        }

        if (this.mainBalance.compareTo(amount) >= 0) {
            this.mainBalance = this.mainBalance.subtract(amount);
            logger.info("Rút tiền từ ví chính thành công. Số tiền: {}", amount);
            return true;
        }

        logger.error("Lỗi: Ví chính không đủ để rút. Cần: {}, có: {}", amount, mainBalance);
        return false;
    }

    /**
     * Chuyển tiền giữa mainBalance và tempBalance
     * @param amount Số tiền cần chuyển
     * @param fromTemp true = chuyển từ temp sang main, false = main sang temp
     * @return true nếu thành công
     */
    public boolean transfer(BigDecimal amount, boolean fromTemp) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("Lỗi: Số tiền chuyển phải lớn hơn 0");
            return false;
        }

        if (fromTemp) {
            if (this.tempBalance.compareTo(amount) >= 0) {
                this.tempBalance = this.tempBalance.subtract(amount);
                this.mainBalance = this.mainBalance.add(amount);
                logger.info("Chuyển tiền từ ví tạm sang ví chính thành công. Số tiền: {}", amount);
                return true;
            }
            logger.error("Lỗi: Ví tạm không đủ để chuyển");
            return false;
        } else {
            if (this.mainBalance.compareTo(amount) >= 0) {
                this.mainBalance = this.mainBalance.subtract(amount);
                this.tempBalance = this.tempBalance.add(amount);
                logger.info("Chuyển tiền từ ví chính sang ví tạm thành công. Số tiền: {}", amount);
                return true;
            }
            logger.error("Lỗi: Ví chính không đủ để chuyển");
            return false;
        }
    }

    /**
     * Reset ví (cho testing hoặc admin operation)
     * @param amount Số tiền reset về
     */
    public void reset(BigDecimal amount) {
        this.mainBalance = amount != null ? amount : BigDecimal.ZERO;
        this.tempBalance = BigDecimal.ZERO;
        logger.warn("Ví đã được reset. Balance mới: {}", this.mainBalance);
    }

    @Override
    public String toString() {
        return "Wallet{" +
                "mainBalance=" + mainBalance +
                ", tempBalance=" + tempBalance +
                ", totalBalance=" + getTotalBalance() +
                '}';
    }
}
