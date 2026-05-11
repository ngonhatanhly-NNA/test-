module bidding.server {
    // Require module chung của dự án
    requires bidding.shared;

    // Các thư viện khai báo trong pom.xml
    requires java.sql;                 // JDBC API
    requires java.naming;              // Bắt buộc cho HikariCP
    requires com.zaxxer.hikari;        // HikariCP (Connection Pool)
    requires org.slf4j;                // Ghi log
    requires com.google.gson;          // JSON
    requires org.java_websocket;       // WebSocket
    requires io.javalin;               // Javalin (Web Framework)
    requires jbcrypt;                  // Mã hóa mật khẩu
    requires com.auth0.jwt;            // Tạo và verify JWT
    requires com.fasterxml.jackson.databind; // Jackson (đi kèm Javalin hoặc dùng ngoài)

    // Export các package nội bộ (để test hoặc truy cập từ xa nếu có)
    exports com.server;
    exports com.server.config;
    exports com.server.controller;
    exports com.server.model;
    exports com.server.service;

    // Mở quyền truy cập cho thư viện Gson và Jackson (đọc data JSON)
    opens com.server.model to com.google.gson, com.fasterxml.jackson.databind;
    opens com.server.controller to com.google.gson, io.javalin;
}