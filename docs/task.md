
---

## 📊 Tóm tắt Mức độ Ưu tiên

| Chức Năng | Vấn Đề | Độ Ưu tiên | Khó độ |
|-----------|--------|-----------|--------|
| **Auto-Bid** | Thiếu check wallet | 🔴 Critical | ⭐⭐⭐ |
| | Deactivate không broadcast | 🟠 High | ⭐⭐ |
| **Anti-Snipe** | Không broadcast time extend | 🔴 Critical | ⭐⭐⭐ |
| | Không có extension limit | 🟠 High | ⭐⭐ |
| | Auction config cứng | 🟠 High | ⭐⭐ |
| **Real-time Chart** | Dữ liệu cũ bị mất | 🟠 High | ⭐⭐ |
| | X-axis overlap | 🟠 High | ⭐⭐ |
| | Không auto-scale Y-axis | 🟡 Medium | ⭐ |
| | Không tooltip | 🟡 Medium | ⭐ |
| | Client join chart trống | 🔴 Critical | ⭐⭐⭐ |
| | Socket disconnect | 🟠 High | ⭐⭐ |

---

## 🚀 Đề nghị thứ tự fix:

**Giai đoạn 1 (Critical):**
1. ✅ Auto-bid: Thêm wallet balance check
2. ✅ Anti-snipe: Broadcast time extension
3. ✅ Chart: Client join → fetch bid history

**Giai đoạn 2 (High Priority):**
4. ✅ Auto-bid: Broadcast deactivate notification
5. ✅ Anti-snipe: Thêm extension limit config
6. ✅ Chart: X-axis rotate labels + memory cleanup

**Giai đoạn 3 (Nice to have):**
7. ✅ Anti-snipe: Auction-specific config (DB columns)
8. ✅ Chart: Y-axis auto-scale + Tooltip
9. ✅ Chart: Full history toggle + responsive

---

Bạn muốn tôi bắt đầu từ đâu? Hay có vấn đề nào khác bạn muốn prioritize trước?