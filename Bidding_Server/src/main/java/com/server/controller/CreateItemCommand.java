package com.server.controller;

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
        // 1. Lấy chuỗi JSON mà Client gửi lên từ Body của Request
        String jsonBody = ctx.body();

        // 2. Dùng Gson ép JSON vào cái hộp DTO của em
        CreateItemRequestDTO requestDTO = gson.fromJson(jsonBody, CreateItemRequestDTO.class);

        // 3. Nhờ Service đúc thành Object Model và lưu xuống DB
        boolean isSuccess = itemService.createNewItem(requestDTO);

        // 4. Báo cáo kết quả về cho Client
        if (isSuccess) {
            String jsonSuccess = gson.toJson(ResponseUtils.success("Đăng bán sản phẩm thành công!", null));
            ctx.status(200).result(jsonSuccess).contentType("application/json");
        } else {
            String jsonFail = gson.toJson(ResponseUtils.fail("ITEM_SAVE_FAILED", "Lỗi khi lưu sản phẩm vào cơ sở dữ liệu."));
            ctx.status(500).result(jsonFail).contentType("application/json");
        }
    }
}