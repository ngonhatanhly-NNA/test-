package com.server.controller;

import com.shared.network.Response;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ImageControllerTest {

    @Mock
    private Context ctx;

    @Mock
    private UploadedFile uploadedFile; // Giả lập UploadedFile

    private ImageController imageController;
    private final String UPLOAD_DIR = "uploads"; // Sử dụng thư mục thật
    private final List<String> createdFiles = new ArrayList<>(); // Lưu tên các file đã tạo để dọn dẹp

    @BeforeEach
    void setUp() {
        imageController = new ImageController();
        // Tạo thư mục uploads nếu chưa có
        new File(UPLOAD_DIR).mkdirs();
    }

    @AfterEach
    void tearDown() {
        // Dọn dẹp các file đã được tạo trong quá trình test
        for (String fileName : createdFiles) {
            try {
                Files.deleteIfExists(Paths.get(UPLOAD_DIR, fileName));
            } catch (IOException e) {
                System.err.println("Failed to delete test file: " + fileName);
            }
        }
    }

    @Test
    void uploadImage_whenFileIsPresent_shouldSaveFileAndReturnUrl() throws IOException {
        // Arrange
        String originalFileName = "test-image.jpg";
        String fileContent = "dummy-image-data";
        InputStream inputStream = new ByteArrayInputStream(fileContent.getBytes());

        // Dạy cho đối tượng UploadedFile giả cách hành xử
        when(uploadedFile.content()).thenReturn(inputStream);
        when(uploadedFile.filename()).thenReturn(originalFileName);

        // Dạy cho Context giả: khi được hỏi, hãy trả về file giả của chúng ta
        when(ctx.uploadedFile("image")).thenReturn(uploadedFile);

        // Act
        imageController.uploadImage(ctx);

        // Assert
        // 1. Kiểm tra controller có trả về status 201 Created không
        verify(ctx).status(201);

        // 2. Lấy URL file được trả về
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).json(responseCaptor.capture());

        Response response = responseCaptor.getValue();
        assertEquals("SUCCESS", response.getStatus());
        String fileUrl = (String) response.getData();
        assertTrue(fileUrl.startsWith("/api/images/"));
        assertTrue(fileUrl.endsWith(".jpg"));

        // 3. Kiểm tra xem file có thực sự được tạo ra trên đĩa không
        String savedFileName = fileUrl.substring("/api/images/".length());
        createdFiles.add(savedFileName); // Thêm vào danh sách để dọn dẹp sau
        Path savedFilePath = Paths.get(UPLOAD_DIR, savedFileName);

        assertTrue(Files.exists(savedFilePath));
        assertEquals(fileContent, Files.readString(savedFilePath));
    }

    @Test
    void uploadImage_whenFileIsMissing_shouldReturn400BadRequest() {
        // Arrange
        when(ctx.uploadedFile("image")).thenReturn(null);

        // Act
        imageController.uploadImage(ctx);

        // Assert
        verify(ctx).status(400);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).json(responseCaptor.capture());
        assertEquals("ERROR", responseCaptor.getValue().getStatus());
    }

    @Test
    void serveImage_whenImageExists_shouldReturnFileStream() throws IOException {
        // Arrange
        String fileName = "test-serve-image.png";
        String fileContent = "dummy-serve-data";
        Path imagePath = Paths.get(UPLOAD_DIR, fileName);
        Files.write(imagePath, fileContent.getBytes());
        createdFiles.add(fileName); // Thêm vào danh sách để dọn dẹp

        when(ctx.pathParam("filename")).thenReturn(fileName);

        // Act
        imageController.serveImage(ctx);

        // Assert
        // 1. Kiểm tra content type có được set không
        verify(ctx).contentType(anyString());

        // 2. Lấy stream được trả về và kiểm tra nội dung
        ArgumentCaptor<InputStream> streamCaptor = ArgumentCaptor.forClass(InputStream.class);
        verify(ctx).result(streamCaptor.capture());

        String resultString = new String(streamCaptor.getValue().readAllBytes());
        assertEquals(fileContent, resultString);
    }

    @Test
    void serveImage_whenImageDoesNotExist_shouldReturn404NotFound() {
        // Arrange
        String fileName = "not-found-image.jpg";
        when(ctx.pathParam("filename")).thenReturn(fileName);

        // Act
        imageController.serveImage(ctx);

        // Assert
        verify(ctx).status(404);
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(ctx).json(responseCaptor.capture());
        assertEquals("ERROR", responseCaptor.getValue().getStatus());
    }
}
