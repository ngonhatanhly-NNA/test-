package com.client.util;

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class SpriteAnimation extends Transition {

    private final ImageView imageView;  //Cái màn chiếu
    private final int count;    //Tổng số khung ảnh (6 khung)
    private final int width;    // Chiều rộng của SPRITESHEET (Chiều dài cuộn phim)

    // Constructor nhỏ gọn, chỉ nhận dải ngang
    public SpriteAnimation(ImageView imageView, Duration duration, int count) {
        this.imageView = imageView;
        this.count = count;
        this.width = (int) imageView.getImage().getWidth(); //Tổng chiều dài gốc

        // Bí thuật: Ép JavaFX phóng to điểm ảnh sắc nét
        this.imageView.setSmooth(false);    //Tắt chế độ làm mượt ảnh -> đúng chất 16-bit
        //TODO: Đặt duration tùy chỉnh cho phù hợp
        setCycleDuration(Duration.millis(2000));         //Set duration x000 milisec
        setInterpolator(Interpolator.LINEAR); // Chuyển động đều
    }

    @Override
    protected void interpolate(double frac) {       //Hàm quay phim 60FPS (Công cụ là màn chiếu ở trên)
        // frac là tỷ lệ hoàn thành (từ 0.0 đến 1.0)
        // Ta tính toán xem nên hiển thị hình thứ mấy
        final int index = Math.min((int) Math.floor(frac * count), count - 1);

        // Ta tính toán chiều rộng của một khung hình (dựa trên dải ngang)
        int frameWidth = width / count;

        // Ta tính toán vị trí trục X của khung hình đó trên dải ngang
        int x = index * frameWidth;

        // Ta dịch chuyển cái "khung nhìn" (Viewport) của ImageView đến vị trí X đó
        imageView.setViewport(new Rectangle2D(x, 0, frameWidth, imageView.getImage().getHeight()));
    }
    /* Viewport (Khung nhìn): Thay vì hiển thị cả cuộn phim dài ngoẵng, ta dùng cái "kéo" Rectangle2D cắt đúng
     một hình chữ nhật nhỏ bắt đầu từ tọa độ x, chiều dài là frameWidth, chiều cao bằng đúng chiều cao ảnh.
     Từng khung hình sẽ thay nhau hiện lên, tạo thành chuyển động kéo rèm!*/
}