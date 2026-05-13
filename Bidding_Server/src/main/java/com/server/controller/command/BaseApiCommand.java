// Mục đích của class này là gom tất cả các đoạn code
// lặp đi lặp lại (như khối try-catch, xử lý lỗi, parse JSON chung) vào một chỗ.
// Các chức năng cụ thể chỉ việc kế thừa nó.

package com.server.controller.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonDeserializationContext;
import com.server.exception.AppException;
import com.server.util.ResponseUtils;
import io.javalin.http.Context;
import io.javalin.http.Handler;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

// Class này implements Handler của Javalin để có thể nhét thẳng vào Router
public abstract class BaseApiCommand implements Handler {

    // [GIÁO SƯ ĐÃ SỬA]: Huấn luyện Gson ở Server giống hệt Client
    // Dạy nó cách chuyển đổi qua lại giữa LocalDateTime và Chuỗi ISO-8601
    // Điều này sẽ chặn đứng lỗi "Failed making field accessible" khi ép DTO lịch sử sang JSON!
    protected final Gson gson = new GsonBuilder()
            // Dạy cách đọc (Deserialize)
            .registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
                    try {
                        return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    } catch (Exception e) {
                        return LocalDateTime.now();
                    }
                }
            })
            // Dạy cách viết (Serialize) - Quan trọng nhất!
            .registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                @Override
                public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                    return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                }
            })
            .create();

    private static final Logger logger = Logger.getLogger(BaseApiCommand.class.getName());

    @Override
    public void handle(Context ctx) {
        try {
            execute(ctx);
        } catch (AppException e) {
            String json = gson.toJson(ResponseUtils.fromAppException(e));
            ctx.status(e.getHttpStatus()).result(json).contentType("application/json");
        } catch (IllegalArgumentException e) {
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
    // Template method pattern: Định nghĩa một quy trình chuẩn trong BaseApiCommand, nhưng để các bước cụ thể (execute) do class con quyết định thực hiện như thế nào.
}