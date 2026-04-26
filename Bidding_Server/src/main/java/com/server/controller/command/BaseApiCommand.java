// Mục đích của class này là gom tất cả các đoạn code
// lặp đi lặp lại (như khối try-catch, xử lý lỗi, parse JSON chung) vào một chỗ.
// Các chức năng cụ thể chỉ việc kế thừa nó.

package com.server.controller.command;

import com.google.gson.Gson;
import com.server.exception.AppException;
import com.server.util.ResponseUtils;
import io.javalin.http.Context;
import io.javalin.http.Handler;

import java.util.logging.Level;
import java.util.logging.Logger;

// Class này implements Handler của Javalin để có thể nhét thẳng vào Router
public abstract class BaseApiCommand implements Handler {

    // Cung cấp sẵn Gson cho các class con dùng
    protected final Gson gson = new Gson();
    private static final Logger logger = Logger.getLogger(BaseApiCommand.class.getName());

    @Override
    public void handle(Context ctx) {
        try {
            execute(ctx);
        } catch (AppException e) {
            String json = gson.toJson(ResponseUtils.fromAppException(e));
            ctx.status(e.getHttpStatus()).result(json).contentType("application/json");
        } catch (IllegalArgumentException e) {
            // Sửa dòng ctx.json thành ctx.result
            String errorJson = gson.toJson(ResponseUtils.fail("VALIDATION", e.getMessage()));
            ctx.status(400).result(errorJson).contentType("application/json");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unhandled server error", e);

            String fatalJson = gson.toJson(ResponseUtils.internalError("Lỗi máy chủ."));
            ctx.status(500).result(fatalJson).contentType("application/json");
        }
    }

    // Hàm trừu tượng (BẮT BUỘC các class con (Create, Update, Delete) phải tự viết nội dung)
    protected abstract void execute(Context ctx) throws Exception;
    // Template method pattern: Định nghĩa một quy trình chuẩn trong BaseApiCommand, nhưng để các
    // bước cụ thể (execute) do class con quyết định thực hiện như thế nào.
}
