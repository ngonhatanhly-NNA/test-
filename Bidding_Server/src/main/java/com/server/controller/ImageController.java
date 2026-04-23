package com.server.controller;

import com.shared.network.Response;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class ImageController {

    // Thư mục để lưu ảnh, nên được cấu hình từ bên ngoài trong ứng dụng thực tế
    private final String UPLOAD_DIR = "uploads";

    public ImageController() {
        // Tạo thư mục uploads nếu nó chưa tồn tại
        new File(UPLOAD_DIR).mkdirs();
    }

    /**
     * POST /api/images/upload
     * Tải ảnh lên server.
     */
    public void uploadImage(Context ctx) {
        UploadedFile uploadedFile = ctx.uploadedFile("image");
        if (uploadedFile == null) {
            ctx.status(400).json(new Response("ERROR", "Không có file ảnh nào được tải lên.", null));
            return;
        }

        try (InputStream inputStream = uploadedFile.content()) {
            // Tạo một tên file duy nhất để tránh trùng lặp
            String extension = getFileExtension(uploadedFile.filename());
            String uniqueFileName = UUID.randomUUID().toString() + "." + extension;
            Path destinationPath = Paths.get(UPLOAD_DIR, uniqueFileName);

            // Lưu file
            try (FileOutputStream outputStream = new FileOutputStream(destinationPath.toFile())) {
                inputStream.transferTo(outputStream);
            }

            // Trả về đường dẫn tương đối của ảnh
            String fileUrl = "/api/images/" + uniqueFileName;
            ctx.status(201).json(new Response("SUCCESS", "Tải ảnh lên thành công.", fileUrl));

        } catch (Exception e) {
            ctx.status(500).json(new Response("ERROR", "Lỗi khi lưu file: " + e.getMessage(), null));
        }
    }

    /**
     * GET /api/images/{filename}
     * Phục vụ file ảnh cho client.
     */
    public void serveImage(Context ctx) {
        String filename = ctx.pathParam("filename");
        Path imagePath = Paths.get(UPLOAD_DIR, filename);
        File imageFile = imagePath.toFile();

        if (!imageFile.exists() || imageFile.isDirectory()) {
            ctx.status(404).json(new Response("ERROR", "Không tìm thấy ảnh.", null));
            return;
        }

        try (FileInputStream fileInputStream = new FileInputStream(imageFile)) {
            String mimeType = Files.probeContentType(imagePath);
            ctx.contentType(mimeType != null ? mimeType : "application/octet-stream");
            ctx.result(fileInputStream);
        } catch (Exception e) {
            ctx.status(500).json(new Response("ERROR", "Lỗi khi đọc file: " + e.getMessage(), null));
        }
    }

    private String getFileExtension(String filename) {
        int lastIndexOf = filename.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // không có đuôi file
        }
        return filename.substring(lastIndexOf + 1);
    }
}
