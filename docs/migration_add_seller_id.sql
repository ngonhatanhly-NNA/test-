-- =====================================================================
-- MIGRATION: Update bảng items để thêm cột seller_id
-- =====================================================================
-- Chạy script_db.sql trước để reset database

-- Hoặc nếu chỉ muốn update mà không reset:
-- 1. Kiểm tra xem seller_id có tồn tại chưa
SELECT * FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME='items' AND COLUMN_NAME='seller_id';

-- 2. Nếu chưa có, thêm cột seller_id
ALTER TABLE items ADD COLUMN seller_id INT;
ALTER TABLE items ADD FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE;

-- 3. Update các sản phẩm cũ (nếu cần set seller_id mặc định)
-- Ví dụ: Set tất cả sản phẩm hiện có là của seller có ID = 2
UPDATE items SET seller_id = 2 WHERE seller_id IS NULL;

-- 4. Thêm constraint NOT NULL
ALTER TABLE items MODIFY seller_id INT NOT NULL;

-- 5. Kiểm tra kết quả
SELECT id, seller_id, name, item_type FROM items;

-- 6. Kiểm tra items của seller ID = 2
SELECT id, seller_id, name, item_type FROM items WHERE seller_id = 2;

