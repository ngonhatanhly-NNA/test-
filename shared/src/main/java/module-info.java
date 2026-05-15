module bidding.shared {
    // Các thư viện cần thiết dựa trên pom.xml
    requires com.google.gson;
    requires org.slf4j;

    // Cho phép Client và Server đọc các package này
    exports com.shared;
    exports com.shared.dto;
    exports com.shared.network;

    // Cho phép Gson đọc các DTO để parse/ép kiểu JSON
    opens com.shared.dto to com.google.gson;
    opens com.shared.network to com.google.gson;
}