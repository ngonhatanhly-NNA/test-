# SOLID Refactoring Guide - Team13 Bidding System

## 📋 Tổng quan các cải tiến

Dự án đã được refactor để tuân thủ các nguyên tắc SOLID và áp dụng các design pattern phù hợp. Dưới đây là chi tiết từng thay đổi:

---

## 1️⃣ SINGLE RESPONSIBILITY PRINCIPLE (SRP)

### Vấn đề cũ:
- `UserProfileController` xử lý: UI binding, network calls, data parsing, role-specific logic
- `UserRepository` có 250+ dòng code, mixing query logic với business logic

### Cải tiến:
- **UserProfileController**: Chỉ xử lý UI interaction
- **UserProfileService**: Xử lý business logic, network coordination
- **UserQueryFactory**: Tách queries riêng biệt theo role
- **ProfileUIStrategy**: Tách role-specific UI setup

### File mới:
```
✅ UserProfileService.java - Tập trung business logic
✅ ProfileUIStrategyFactory.java - Quản lý UI strategies
✅ AdminProfileUIStrategy.java - Admin UI logic
✅ BidderProfileUIStrategy.java - Bidder UI logic
✅ SellerProfileUIStrategy.java - Seller UI logic
✅ UserQueryFactory.java - Query management
```

---

## 2️⃣ OPEN/CLOSED PRINCIPLE (OCP)

### Vấn đề cũ:
- Khi thêm role mới → phải sửa `UserProfileController`, `UserRepository`, v.v.
- Chuỗi `if-else` dài → khó bảo trì, dễ phạm sai

### Cải tiến:
- **Strategy Pattern**: Thêm role mới chỉ cần tạo strategy mới, không sửa code cũ
- **Factory Pattern**: Delegating instance creation, dễ mở rộng

### Ví dụ - Thêm role PREMIUM_SELLER:
```java
// Tạo strategy mới
public class PremiumSellerProfileUIStrategy implements IProfileUIStrategy {
    @Override
    public void setupUI(IUserProfileDTO profile) { /* ... */ }
    
    @Override
    public boolean isApplicable(String role) {
        return "PREMIUM_SELLER".equals(role);
    }
}

// Đăng ký vào factory - GIA HẠN CODE CỌ
profileUIStrategyFactory.registerStrategy(new PremiumSellerProfileUIStrategy(...));
```

---

## 3️⃣ LISKOV SUBSTITUTION PRINCIPLE (LSP)

### Vấn đề cũ:
- `Seller extends Bidder` - Seller không phải "loại Bidder" mà "có Bidder properties"
- Tạo ra mixing hierarchy → khó thực thi interface contract

### Cải tiến hiện tại (Phase 1):
- Giữ nguyên Seller extends Bidder để tránh break database
- Sẽ refactor thành Composition trong Phase 2

### Đề xuất Phase 2:
```java
public class User {
    private BidderProfile bidderProfile; // Composition instead of inheritance
    private SellerProfile sellerProfile;
}
```

---

## 4️⃣ INTERFACE SEGREGATION PRINCIPLE (ISP)

### Vấn đề cũ:
- `UserProfileUpdateDTO` chứa 15+ fields cho TẤT CẢ roles
- Client phải biết về fields không cần thiết (roleLevel nếu là BIDDER)

### Cải tiến:
```
✅ IUserProfileDTO - Interface tối thiểu (chỉ common fields)
✅ BaseProfileDTO - Base class
✅ BidderProfileDTO - Bidder-specific
✅ SellerProfileDTO - Seller-specific  
✅ AdminProfileDTO - Admin-specific
```

Mỗi role chỉ biết về fields của nó → Reduce Coupling

---

## 5️⃣ DEPENDENCY INVERSION PRINCIPLE (DIP)

### Vấn đề cũ:
```java
// ❌ Tight coupling
private final AuthNetwork authNetwork = new AuthNetwork();
userProfileService = new UserProfileService(); // hardcoded
```

### Cải tiến:
```java
// ✅ Constructor Injection
public UserProfileService(AuthNetwork authNetwork, Gson gson) {
    this.authNetwork = authNetwork;
    this.gson = gson;
}

// ✅ Repository Interface
public interface IUserRepository {
    User getUserByUsername(String username);
    boolean saveUser(User user);
}

public class UserRepository implements IUserRepository { }
```

**Lợi ích:**
- Dễ mock trong unit tests
- Dễ thay thế implementation
- Loose coupling

---

## 🎨 DESIGN PATTERNS ĐƯỢC ÁP DỤNG

### 1. **STRATEGY PATTERN** 
**Vị trí**: ProfileUIStrategy interface + implementations
**Mục đích**: Cho phép thay đổi UI behavior theo role tại runtime
```java
IProfileUIStrategy strategy = new BidderProfileUIStrategy(...);
strategy.setupUI(profile);
```

### 2. **FACTORY PATTERN**
**Vị trí**: 
- `UserRowMapperFactory` - Tạo mapper theo role
- `UserProfileDTOFactory` - Tạo DTO theo role  
- `ProfileUIStrategyFactory` - Quản lý strategies

**Mục đích**: Encapsulate object creation logic

### 3. **REPOSITORY PATTERN**
**Vị trí**: `IUserRepository` interface + `UserRepository` implementation
**Mục đích**: Abstract database operations, dễ testing

### 4. **DEPENDENCY INJECTION**
**Vị trí**: Constructor parameters thay vì hardcoded `new`
**Mục đích**: Loose coupling, testability

---

## 🔄 Quy trình Refactoring

### Phase 1 (Hiện tại) ✅
- ✅ Extract Repository Interface
- ✅ Create Service Layer  
- ✅ Implement Strategy Pattern cho UI
- ✅ Split DTO theo role
- ✅ Apply Dependency Injection

### Phase 2 (Đề xuất)
- Replace Seller extends Bidder with Composition
- Migrate database schema
- Extract query logic thêm
- Unit tests với mocks

### Phase 3 (Tương lai)
- Event-driven architecture
- CQRS pattern nếu cần
- Reactive streams

---

## 📊 Before vs After

| Aspect | Before | After |
|--------|--------|-------|
| **UserProfileController** | 150+ lines, mixed concerns | ~80 lines, UI only |
| **UserRepository** | 250+ lines, complex queries | 200 lines + separate factories |
| **Role-specific logic** | Scattered if-else | Strategies, easy to extend |
| **Testing** | Hard (tight coupling) | Easy (DI, interfaces) |
| **Adding new role** | Modify multiple files | Add new Strategy file |

---

## 🚀 Cách sử dụng

### Thêm role mới (e.g., MODERATOR):

```java
// 1. Tạo Strategy
public class ModeratorProfileUIStrategy implements IProfileUIStrategy {
    @Override
    public void setupUI(IUserProfileDTO profile) { /* ... */ }
    
    @Override
    public boolean isApplicable(String role) {
        return "MODERATOR".equals(role);
    }
}

// 2. Đăng ký
profileUIStrategyFactory.registerStrategy(new ModeratorProfileUIStrategy(...));

// 3. Ready! Code cũ không cần sửa
```

### Mock AuthNetwork trong test:
```java
@Test
public void testUserProfileService() {
    AuthNetwork mockNetwork = mock(AuthNetwork.class);
    UserProfileService service = new UserProfileService(mockNetwork, new Gson());
    
    // Verify interactions
    service.updateProfile(dto);
    verify(mockNetwork).updateProfile(dto);
}
```

---

## ⚠️ Migration Notes

- **Backward Compatibility**: UserRepository vẫn implement old methods
- **Database**: Không thay đổi schema, chỉ refactor code
- **DTOs**: UserProfileUpdateDTO vẫn hoạt động, nhưng nên dùng specific DTOs

---

## 📚 Reference

**SOLID Principles:**
- Single Responsibility: Mỗi class có một lý do để thay đổi
- Open/Closed: Mở để mở rộng, đóng để sửa đổi
- Liskov Substitution: Subclass có thể thay thế parent
- Interface Segregation: Nhiều interface nhỏ hơn một interface lớn
- Dependency Inversion: Depend on abstractions, not concrete

**Design Patterns:**
- Strategy: Encapsulate algorithms
- Factory: Object creation
- Repository: Data access abstraction
- Dependency Injection: Loose coupling

---

**Ngày cập nhật**: 09/04/2026
**Phiên bản**: 1.0 (Phase 1)

