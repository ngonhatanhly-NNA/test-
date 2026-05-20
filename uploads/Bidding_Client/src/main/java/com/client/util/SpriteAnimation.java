package com.client.util;

import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class SpriteAnimation extends Transition {

    private final ImageView imageView;  
    private final int count;    
    private final int columns; 
    private final int frameWidth; 
    private final int frameHeight;

    //Nhận vào số cột và số hàng của tấm ảnh để chia lưới
    public SpriteAnimation(ImageView imageView, Duration duration, int count, int columns, int rows) {
        this.imageView = imageView;
        this.count = count;
        this.columns = columns;
        
        // Tự động tính kích thước chuẩn của 1 con Pet
        this.frameWidth = (int) imageView.getImage().getWidth() / columns;
        this.frameHeight = (int) imageView.getImage().getHeight() / rows;

        this.imageView.setSmooth(false);    
        setCycleDuration(duration);         
        setInterpolator(Interpolator.LINEAR); 
    }

    @Override
    protected void interpolate(double frac) {       
        final int index = Math.min((int) Math.floor(frac * count), count - 1);

        int x = (index % columns) * frameWidth;
        int y = 0; // Lấy hàng trên cùng. (Nếu muốn lấy hàng 2 thì sửa thành: 1 * frameHeight)

        // 1 ô vuông!
        imageView.setViewport(new Rectangle2D(x, y, frameWidth, frameHeight));
    }
}