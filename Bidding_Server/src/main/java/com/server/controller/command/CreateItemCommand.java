package com.server.controller.command;

import com.server.util.ResponseUtils;
import com.shared.dto.CreateItemRequestDTO;
import com.server.service.ItemService;
import io.javalin.http.Context;

public class CreateItemCommand extends BaseApiCommand {

    private final ItemService itemService;

    public CreateItemCommand(ItemService itemService) {
        this.itemService = itemService;
    }

    @Override
    protected void execute(Context ctx) throws Exception {
        // 1. Giữ nguyên cách lấy JSON và parse bằng Gson
        String jsonBody = ctx.body();
        CreateItemRequestDTO requestDTO = gson.fromJson(jsonBody, CreateItemRequestDTO.class);

        // 2. Nhờ Service đúc Model và lưu DB (Đổi hàm này bên Service thành kiểu void)
        // Nếu có lỗi (VD: thiếu ảnh, sai DB), nó sẽ NÉM EXCEPTION bay ra ngoài BaseApiCommand.
        // BaseApiCommand sẽ tự động bắt lấy exception và gửi JSON lỗi cho Client.
        itemService.createNewItem(requestDTO);

        // 3. Nếu code chạy được xuống đến đây, chắc chắn 100% là LƯU THÀNH CÔNG (Happy Path)
        // Ta dùng nguyên xi bộ công cụ ResponseUtils để trả về.
        String jsonSuccess =
                gson.toJson(ResponseUtils.success("Đăng bán sản phẩm thành công!", null));
        ctx.status(200).result(jsonSuccess).contentType("application/json");
    }
}
