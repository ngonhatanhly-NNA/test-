# Sử dụng môi trường có sẵn Maven và Java (chọn bản Java 17 hoặc 21 tùy team bạn đang dùng)
FROM maven:3.9.6-eclipse-temurin-25

# Đặt thư mục làm việc trong Docker
WORKDIR /app

COPY . .

# Build toàn bộ project (shared và Server) - bỏ qua bước Test cho nhanh
RUN mvn clean install -DskipTests

# Mở cổng 7070 (REST) và 8080 (WebSocket)
EXPOSE 7070 8080

# Lệnh khởi động Server
CMD ["mvn", "exec:java", "-pl", "Bidding_Server", "-Dexec.mainClass=com.server.ServerApp"]