## 🔧 HƯỚNG DẪN: Fix My Inventory không hiển thị sản phẩm

### ✅ Các bước cần thực hiện:

#### **1. Update Database Schema**
```sql
-- Mở MySQL Workbench hoặc HeidiSQL
-- Chạy file: script_db.sql (để reset database)
-- Hoặc chạy file: migration_add_seller_id.sql (để thêm cột mà không xóa dữ liệu cũ)
```

**Nếu chọn không reset database, hãy chạy này:**
```sql
-- Kiểm tra xem seller_id có chưa
SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME='items' AND COLUMN_NAME='seller_id';

-- Nếu chưa có, thêm cột
ALTER TABLE items ADD COLUMN seller_id INT;
ALTER TABLE items ADD FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE;

-- Update các sản phẩm cũ (thay ID_CỦA_SELLER bằng ID thực tế)
UPDATE items SET seller_id = ID_CỦA_SELLER WHERE seller_id IS NULL;
ALTER TABLE items MODIFY seller_id INT NOT NULL;
```

#### **2. Rebuild Project**
```bash
# CD vào thư mục project
cd C:\Users\DELL\IdeaProjects\untitled\Team13_Bidding_System

# Clean build
./mvnw clean install
```

#### **3. Restart Server và Client**
- Tắt server (nếu đang chạy)
- Tắt client (nếu đang chạy)
- Chạy Server lại
- Chạy Client lại

#### **4. Test**
1. **Đăng nhập** vào hệ thống (với tài khoản SELLER)
2. **Tạo sản phẩm mới** bằng nút "Create Item"
3. **Kiểm tra My Inventory** xem sản phẩm vừa tạo có hiện không
4. **Kiểm tra Console/Logs** xem các thông báo debug:

**Server Console sẽ hiển thị:**
```
ItemRepository.getItemsBySellerId: Executing query for seller_id = [ID]
ItemRepository: Found item [ID] (type=..., sellerId=..., name=...)
ItemRepository.getItemsBySellerId: Found X items for seller_id = [ID]
ItemService.getItemsBySellerIdDTO: Fetching items for seller ID: [ID]
ItemService.getItemsBySellerIdDTO: Found X raw items
GetItemsBySellerIdCommand: Fetching items for seller ID: [ID]
GetItemsBySellerIdCommand: Found X items for seller [ID]
```

**Client Console sẽ hiển thị:**
```
ItemNetwork.getMyItems: Calling URL: http://localhost:7070/api/items/seller/[ID]
ItemNetwork.getMyItems: Response: {"status":"SUCCESS", "message":"...", "data":[...]}
ItemNetwork.getMyItems: Successfully loaded X items
```

---

### 🚨 Nếu vẫn không hoạt động, kiểm tra:

1. **Seller ID đúng chưa?**
   ```sql
   SELECT id, username, role FROM users WHERE role = 'SELLER';
   ```

2. **Sản phẩm trong database có seller_id chưa?**
   ```sql
   SELECT id, seller_id, name FROM items WHERE seller_id IS NOT NULL;
   ```

3. **API endpoint có hoạt động chưa?**
   - Dùng postman test: `GET http://localhost:7070/api/items/seller/2`
   - Xem response có dữ liệu không

4. **Kiểm tra logs:**
   - File: `logs/server.log`
   - File: `logs/client.log`
   - Tìm từ khóa: `ERROR`, `EXCEPTION`

---

### 📝 Chi tiết những thay đổi đã làm:

**Database:**
- ✅ Thêm cột `seller_id` vào bảng `items`
- ✅ Thêm `FOREIGN KEY` để liên kết với bảng `users`

**Server:**
- ✅ Thêm field `sellerId` vào model `Item`
- ✅ Cập nhật `ItemFactory` để xử lý `sellerId`
- ✅ Cập nhật `ItemRepository.saveItem()` để lưu `seller_id`
- ✅ Thêm `ItemRepository.getItemsBySellerId()` để lấy items theo seller
- ✅ Thêm `ItemService.getItemsBySellerIdDTO()` để convert DTO
- ✅ Tạo `GetItemsBySellerIdCommand` để handle API request
- ✅ Thêm route `/api/items/seller/{sellerId}` vào `ApiRouter`

**Client:**
- ✅ Thêm `ClientSession.getUserId()` khi tạo sản phẩm (truyền sellerId)
- ✅ Thêm `ItemNetwork.getMyItems(sellerId)` để call API mới
- ✅ Sửa `MyInventoryController.loadMyItems()` để gọi `getMyItems()` thay vì `getAllItems()`
- ✅ Thêm logging để debug

---

### 💡 Nguyên nhân vấn đề cũ:

❌ **Trước đây:**
- `MyInventoryController` gọi `itemNetwork.getAllItems()` → lấy **TẤT CẢ** items trong DB
- Bảng `items` **không có** cột `seller_id` → không biết item của ai
- Sản phẩm chưa có auction → không thể lọc qua JOIN auctions

✅ **Bây giờ:**
- `MyInventoryController` gọi `itemNetwork.getMyItems(sellerId)` → lấy **chỉ** items của seller hiện tại
- Bảng `items` có cột `seller_id` → luôn biết item của ai
- Không cần join auctions → sản phẩm chưa có auction cũng hiển thị được


