//Một class riêng biệt chỉ làm đúng một nhiệm vụ là Lấy danh sách sản phẩm.

package com.server.controller.command;

import com.server.service.ItemService;
import com.shared.dto.ItemResponseDTO;
import com.shared.network.Response;
import io.javalin.http.Context;
import java.util.List;

public class GetAllItemsCommand extends BaseApiCommand {

    private final ItemService itemService;

    // Kỹ thuật Dependency Injection (Tiêm phụ thuộc) đúng chuẩn SOLID của nhóm
    public GetAllItemsCommand(ItemService itemService) {
        this.itemService = itemService;
    }

    @Override
    protected void execute(Context ctx) throws Exception {
        // 1. Nhờ Service lấy danh sách các hộp Bento (DTO)
        List<ItemResponseDTO> items = itemService.getAllItemsDTO();

        // 2. Tự dùng Gson biến thành chuỗi JSON, rồi trả thẳng về bằng ctx.result()
        String jsonResult = gson.toJson(new Response("SUCCESS", "Lấy danh sách thành công", items));

        ctx.result(jsonResult).contentType("application/json"); // Fix dứt điểm ở đây!
    }
}