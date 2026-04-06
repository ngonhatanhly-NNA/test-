package com.server.service;

import com.server.DAO.ItemRepository;
import com.server.model.Item;
// Import thư viện biến đổi JSON của nhóm (Thường là Gson hoặc Jackson)
import com.google.gson.Gson;
import java.util.List;

public class ItemService {

    // Gọi thằng nhân viên đi kho (DAO)
    private ItemRepository itemRepo;
    // Cái máy xay JSON
    private Gson gson;

    public ItemService() {
        this.itemRepo = new ItemRepository();
        this.gson = new Gson();
    }

    // Hàm này sẽ được ServerApp gọi
    public String getAllItems() {
        // Sai thằng DAO xuống DB lấy danh sách lên và gửi cho dashboard của client
        List<Item> items = itemRepo.getAllItems();

        // 2. Cho vào máy xay, biến List Object thành chuỗi String JSON
        String jsonResponse = gson.toJson(items);

        // 3. Trả cục JSON đó cho ServerApp
        return jsonResponse;
    }
}