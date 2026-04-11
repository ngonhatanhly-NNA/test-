//Mục đích của class này là gom tất cả các đoạn code
//lặp đi lặp lại (như khối try-catch, xử lý lỗi, parse JSON chung) vào một chỗ.
//Các chức năng cụ thể chỉ việc kế thừa nó.

package com.server.controller;

import com.google.gson.Gson;
import com.shared.network.Response;
import io.javalin.http.Context;
import io.javalin.http.Handler;

// Class này implements Handler của Javalin để có thể nhét thẳng vào Router
public abstract class BaseApiCommand implements Handler {

    // Cung cấp sẵn Gson cho các class con dùng
    protected final Gson gson = new Gson();

    @Override
    public void handle(Context ctx) {
        try {
            execute(ctx);
        } catch (IllegalArgumentException e) {
            // Sửa dòng ctx.json thành ctx.result
            String errorJson = gson.toJson(new Response("FAIL", e.getMessage(), null));
            ctx.status(400).result(errorJson).contentType("application/json");
        } catch (Exception e) {
            e.printStackTrace();
            // Sửa dòng ctx.json thành ctx.result
            String fatalJson = gson.toJson(new Response("ERROR", "Lỗi máy chủ: " + e.getMessage(), null));
            ctx.status(500).result(fatalJson).contentType("application/json");
        }
    }

    // Hàm trừu tượng (BẮT BUỘC các class con (Create, Update, Delete) phải tự viết nội dung)
    protected abstract void execute(Context ctx) throws Exception;
    //Template method pattern: Định nghĩa một quy trình chuẩn trong BaseApiCommand, nhưng để các bước cụ thể (execute) do class con quyết định thực hiện như thế nào.
}