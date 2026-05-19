package com.client.util.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Trạm phát sóng trung tâm. Bất kỳ ai cũng có thể gửi (publish) 
 * và đăng ký nghe (subscribe) sự kiện.
 */
public class EventBus {
    private static final EventBus instance = new EventBus();
    
    // Lưu trữ danh sách người nghe theo từng loại sự kiện
    private final Map<Class<?>, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

    private EventBus() {}

    public static EventBus getInstance() {
        return instance;
    }

    // Đăng ký nghe một loại sự kiện cụ thể
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>())
                 .add(obj -> listener.accept(eventType.cast(obj)));
    }

    // Phát sóng sự kiện cho tất cả những ai đang nghe
    public void publish(Object event) {
        List<Consumer<Object>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (Consumer<Object> listener : eventListeners) {
                listener.accept(event);
            }
        }
    }
}