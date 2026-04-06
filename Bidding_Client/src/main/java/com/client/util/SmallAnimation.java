package com.client.util;

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class SmallAnimation extends Transition {

    private final ImageView imageView;
    private final int count;      // Tổng số khung hình (frame)
    private final int columns;    // Số cột trên sprite sheet
    private final int offsetX;    // Tọa độ X bắt đầu cắt
    private final int offsetY;    // Tọa độ Y bắt đầu cắt
    private final int width;      // Chiều rộng của 1 frame
    private final int height;     // Chiều cao của 1 frame

    private int lastIndex;

    public SmallAnimation(ImageView imageView,
                           Duration duration,
                           int count, int columns,
                           int offsetX, int offsetY,
                           int width, int height) {
        this.imageView = imageView;
        this.count = count;
        this.columns = columns;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.width = width;
        this.height = height;

        // Cài đặt thời gian chạy của 1 chu kỳ animation
        setCycleDuration(duration);
        // Chuyển động tuyến tính, không bị nhanh dần hay chậm dần
        setInterpolator(Interpolator.LINEAR);
    }

    @Override
    protected void interpolate(double k) { // k là thời gian đi hiện tại, đảm bảo thời gian chạy trong khoảng miliseconds
        // sẽ không quá index cho khung huinhf -> Ì
        // Tính toán frame hiện tại dựa trên tiến trình thời gian k (từ 0.0 đến 1.0)
        final int index = Math.min((int) Math.floor(k * count), count - 1);

        // Nếu chuyển sang frame mới thì mới cập nhật Viewport
        if (index != lastIndex) {
            final int x = (index % columns) * width + offsetX;
            final int y = (index / columns) * height + offsetY;

            // Cắt đúng 1 ô hình vuông để hiển thị
            imageView.setViewport(new Rectangle2D(x, y, width, height));
            lastIndex = index; // Tối ưu hiệu năng, nếu không , máy tính phải setViewpost liên túc
            // lưu tại index này, spritesheet là 1
        }
    }
}