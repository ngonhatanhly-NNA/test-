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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageController {
    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    private final String UPLOAD_DIR = "uploads";

    public ImageController() {
        new File(UPLOAD_DIR).mkdirs();
    }

    public void uploadImage(Context ctx) {
        UploadedFile uploadedFile = ctx.uploadedFile("image");
        if (uploadedFile == null) {
            ctx.status(400).json(new Response("ERROR", "Không có file ảnh nào được tải lên.", null));
            return;
        }

        try (InputStream inputStream = uploadedFile.content()) {
            String extension = getFileExtension(uploadedFile.filename());
            String uniqueFileName = UUID.randomUUID().toString() + "." + extension;
            Path destinationPath = Paths.get(UPLOAD_DIR, uniqueFileName);

            try (FileOutputStream outputStream = new FileOutputStream(destinationPath.toFile())) {
                inputStream.transferTo(outputStream);
            }

            String fileUrl = "/api/images/" + uniqueFileName;
            logger.info("Tải ảnh lên thành công: {} -> {}", uploadedFile.filename(), uniqueFileName);
            ctx.status(201).json(new Response("SUCCESS", "Tải ảnh lên thành công.", fileUrl));

        } catch (Exception e) {
            logger.error("Lỗi khi lưu file ảnh: {}", e.getMessage(), e);
            ctx.status(500).json(new Response("ERROR", "Lỗi khi lưu file: " + e.getMessage(), null));
        }
    }

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
            logger.error("Lỗi khi đọc file ảnh '{}': {}", filename, e.getMessage(), e);
            ctx.status(500).json(new Response("ERROR", "Lỗi khi đọc file: " + e.getMessage(), null));
        }
    }

    private String getFileExtension(String filename) {
        int lastIndexOf = filename.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return filename.substring(lastIndexOf + 1);
    }
}

