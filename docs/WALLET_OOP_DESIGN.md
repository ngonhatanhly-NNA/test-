# Giải Pháp OOP Cho Hệ Thống Ví (Wallet)

## Vấn Đề Hiện Tại
- `Seller extends Bidder extends User`
- `walletBalance` được kế thừa từ `Bidder`
- Thêm wallet tạm thời hoặc sửa logic sẽ ảnh hưởng cascade:
  - `Bidder.java`, `Seller.java`
  - `SellerService`, `SellerRepository`
  - Database queries
  - DTOs, Network calls

---

## Giải Pháp 1: Composition Over Inheritance + Value Object (⭐ Khuyên Dùng)

### Nguyên Tắc
- Tạo lớp `Wallet` độc lập để quản lý tất cả logic ví
- `Bidder`, `Seller` *sở hữu* `Wallet` thay vì *kế thừa*
- Mợi thay đổi ví được isolate trong `Wallet`

### Cấu Trúc

```
User
├─ Bidder (has-a Wallet)
└─ Seller extends Bidder (inherits Wallet từ Bidder)

Wallet (Value Object)
├─ mainBalance (ví chính)
├─ tempBalance (ví tạm thời)
├─ addFunds()
├─ deductFunds()
└─ withdrawToBank()
```

### Code Implementation

**1. Tạo Wallet Value Object** (`com/server/model/Wallet.java`)
```java
public class Wallet {
    private BigDecimal mainBalance;      // Ví chính
    private BigDecimal tempBalance;      // Ví tạm thời (dự phòng)
    
    public Wallet() {
        this.mainBalance = BigDecimal.ZERO;
        this.tempBalance = BigDecimal.ZERO;
    }
    
    public Wallet(BigDecimal mainBalance, BigDecimal tempBalance) {
        this.mainBalance = mainBalance != null ? mainBalance : BigDecimal.ZERO;
        this.tempBalance = tempBalance != null ? tempBalance : BigDecimal.ZERO;
    }
    
    // --- BUSINESS LOGIC ---
    public void addFunds(BigDecimal amount, boolean isTemp) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền phải > 0");
        }
        
        if (isTemp) {
            this.tempBalance = this.tempBalance.add(amount);
        } else {
            this.mainBalance = this.mainBalance.add(amount);
        }
    }
    
    public boolean deductFunds(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        // Ưu tiên trừ từ mainBalance trước
        if (this.mainBalance.compareTo(amount) >= 0) {
            this.mainBalance = this.mainBalance.subtract(amount);
            return true;
        }
        
        // Nếu không đủ, kiểm tra cả tempBalance
        BigDecimal totalBalance = this.mainBalance.add(this.tempBalance);
        if (totalBalance.compareTo(amount) >= 0) {
            BigDecimal temp = amount.subtract(this.mainBalance);
            this.mainBalance = BigDecimal.ZERO;
            this.tempBalance = this.tempBalance.subtract(temp);
            return true;
        }
        
        return false;
    }
    
    // --- GETTERS ---
    public BigDecimal getMainBalance() { return mainBalance; }
    public BigDecimal getTempBalance() { return tempBalance; }
    public BigDecimal getTotalBalance() { 
        return mainBalance.add(tempBalance); 
    }
    
    public void setMainBalance(BigDecimal mainBalance) {
        this.mainBalance = mainBalance != null ? mainBalance : BigDecimal.ZERO;
    }
    
    public void setTempBalance(BigDecimal tempBalance) {
        this.tempBalance = tempBalance != null ? tempBalance : BigDecimal.ZERO;
    }
}
```

**2. Sửa Bidder class**
```java
public class Bidder extends User {
    private Wallet wallet;              // Thay vì walletBalance
    private String creditCardInfo;
    
    public Bidder() {
        this.wallet = new Wallet();
    }
    
    // Constructor cho Register
    public Bidder(String username, String passwordHash, String email, String fullname) {
        super(0L, username, passwordHash, email, fullname, null, null, Status.ACTIVE, Role.BIDDER);
        this.wallet = new Wallet();
    }
    
    // Constructor từ Database
    public Bidder(long id, String username, String passwordHash, String email, String fullName,
                  String phoneNumber, String address, Status status, Role role,
                  BigDecimal mainBalance, BigDecimal tempBalance, String creditCardInfo) {
        super(id, username, passwordHash, email, fullName, phoneNumber, address, status, role);
        this.wallet = new Wallet(mainBalance, tempBalance);
        this.creditCardInfo = creditCardInfo;
    }
    
    // --- GETTERS & SETTERS ---
    public Wallet getWallet() { return wallet; }
    public void setWallet(Wallet wallet) { this.wallet = wallet; }
    
    // Backward compatibility methods (nếu code cũ dùng getWalletBalance)
    public BigDecimal getWalletBalance() { 
        return wallet.getMainBalance(); 
    }
    public void setWalletBalance(BigDecimal balance) {
        wallet.setMainBalance(balance);
    }
    
    // --- BUSINESS LOGIC (delegate to Wallet) ---
    public void addFunds(BigDecimal amount, boolean isTemp) {
        wallet.addFunds(amount, isTemp);
    }
    
    public boolean deductFunds(BigDecimal amount) {
        return wallet.deductFunds(amount);
    }
}
```

**3. Seller class - không cần thay đổi gì**
```java
public class Seller extends Bidder {
    // Kế thừa Wallet từ Bidder
    // Nếu cần custom logic cho Seller:
    
    public void withdrawToBank(BigDecimal amount, BankService bankService) {
        if (wallet.deductFunds(amount)) {
            bankService.transferToBankAccount(this.bankAccountNumber, amount);
        } else {
            throw new IllegalStateException("Số dư không đủ");
        }
    }
}
```

**4. Sửa Service Layer** (`SellerService.java`)
```java
public class SellerService {
    // ... existing code ...
    
    /**
     * Nạp tiền cho Seller
     */
    public Response addFundsToSeller(long sellerId, BigDecimal amount, boolean isTemp) {
        Seller seller = sellerRepository.findSellerByUserId(sellerId);
        if (seller == null) {
            return new Response("FAIL", "Không tìm thấy Seller", null);
        }
        
        seller.addFunds(amount, isTemp);
        sellerRepository.updateWallet(sellerId, seller.getWallet());
        
        return new Response("SUCCESS", 
            "Nạp tiền thành công. Ví chính: " + seller.getWallet().getMainBalance(),
            null);
    }
    
    /**
     * Lấy thông tin ví chi tiết
     */
    public Response getWalletInfo(long sellerId) {
        Seller seller = sellerRepository.findSellerByUserId(sellerId);
        if (seller == null) {
            return new Response("FAIL", "Không tìm thấy Seller", null);
        }
        
        Map<String, BigDecimal> walletInfo = new HashMap<>();
        walletInfo.put("mainBalance", seller.getWallet().getMainBalance());
        walletInfo.put("tempBalance", seller.getWallet().getTempBalance());
        walletInfo.put("totalBalance", seller.getWallet().getTotalBalance());
        
        return new Response("SUCCESS", "Thông tin ví", walletInfo);
    }
}
```

**5. Sửa Repository** (`SellerRepository.java`)
```java
public class SellerRepository implements ISellerRepository {
    
    @Override
    public boolean updateWallet(long sellerId, Wallet wallet) {
        String sql = "UPDATE bidders SET mainBalance = ?, tempBalance = ? WHERE user_id = ?";
        
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, wallet.getMainBalance());
            pstmt.setBigDecimal(2, wallet.getTempBalance());
            pstmt.setLong(3, sellerId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Lỗi cập nhật ví", e);
            return false;
        }
    }
}
```

---

## Giải Pháp 2: Strategy Pattern (Nếu nhiều loại Wallet)

Nếu khách hàng, bán hàng, admin cần ví khác nhau:

```java
public interface WalletStrategy {
    void addFunds(BigDecimal amount);
    boolean deductFunds(BigDecimal amount);
    BigDecimal getBalance();
}

public class StandardWallet implements WalletStrategy {
    private BigDecimal balance;
    // Implementation...
}

public class SellerWallet implements WalletStrategy {
    private BigDecimal mainBalance;
    private BigDecimal tempBalance;
    // Implementation...
}

public class Bidder {
    private WalletStrategy wallet;
    // Có thể swap strategy mà không thay đổi Bidder
}
```

---

## Giải Pháp 3: Decorator Pattern (Nếu thêm tính năng)

```java
public interface Wallet {
    BigDecimal getBalance();
    void addFunds(BigDecimal amount);
}

public class BasicWallet implements Wallet {
    private BigDecimal balance;
    // ...
}

public class FreezeableWallet implements Wallet {
    private Wallet delegate;
    private BigDecimal frozenAmount;
    
    @Override
    public BigDecimal getBalance() {
        return delegate.getBalance().subtract(frozenAmount);
    }
}
```

---

## So Sánh Giải Pháp

| Tiêu Chí | Giải Pháp 1 (Value Object) | Giải Pháp 2 (Strategy) | Giải Pháp 3 (Decorator) |
|----------|---------------------------|----------------------|----------------------|
| **Dễ hiểu** | ✅ Dễ nhất | ⚠️ Trung bình | ❌ Phức tạp |
| **Types ví khác nhau** | ⚠️ Cần sửa Wallet | ✅ Tốt | ⚠️ Cần kết hợp |
| **Thêm tính năng** | ✅ Tốt | ⚠️ Cần thêm Strategy mới | ✅ Decorator mới |
| **Database** | ✅ Đơn giản | ✅ Đơn giản | ⚠️ Phức tạp |
| **Backward compatible** | ✅ Có thể giữ | ✅ Có thể giữ | ❌ Khó |

---

## Khuyến Cáo

**Nên dùng Giải Pháp 1** vì:
- ✅ Rõ ràng, dễ maintain
- ✅ Isolate wallet logic, dễ test
- ✅ Có thể thêm tempBalance, frozenBalance v.v. mà không ảnh hưởng User/Bidder
- ✅ Backward compatible với code cũ

**Nếu sau này cần multiple wallet types → thì chuyển sang Strategy Pattern**

---

## Database Migration

```sql
-- Alter bidders table to add tempBalance
ALTER TABLE bidders ADD COLUMN tempBalance DECIMAL(19,2) DEFAULT 0.00;

-- Rename walletBalance to mainBalance (optional)
-- ALTER TABLE bidders RENAME COLUMN walletBalance TO mainBalance;

-- Or keep walletBalance, map temp column separately
```

---

## Step-by-Step Implementation

1. ✅ Tạo `Wallet.java` class
2. ✅ Sửa `Bidder.java` để sử dụng Wallet
3. ✅ Sửa `SellerService` methods liên quan
4. ✅ Sửa `SellerRepository` để lưu/load Wallet
5. ✅ Cập nhật Database schema
6. ✅ Sửa DTOs nếu cần (WalletResponseDTO)
7. ✅ Update unit tests
