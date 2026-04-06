package com.server.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Connect đến DB chính của hệ thống
public class DBConnection {
    // Attention: Chỉ tồn tại duy nhất 1 DB, -> singleton
	// Chỉ cho phép 1 db khi mọi người dùng dùng
	private static volatile DBConnection instance;
    private  Connection connection;
    private static final String URL = "jdbc:mysql://localhost:3306/auction_db";     //Tự sửa theo đúng cổng của từng máy

    private static final String USER = "root";
    private static final String PASSWORD = "";      //Tự sửa theo cấu hình tùy máy

    // khóa Constructer, không cho kởi tạo ngoài
    
	// Người điều hành, chỉ có thể xử lí 1 cái cùng 1 lúc, nếu nhiều người cùng 1 lúc bấm vào 
	// -> chưa kịp sinh ra -> lỗi RaceCondition
	
	// Để xử lí multiThreading -> dùng hư viện HikariCP 
	public static DBConnection getDBConnection(){
		if (instance == null){
			// Threas nhanh nhat vao, cacs Thread kia doi
			synchronized (DBConnection.class) {
				if (instance == null){
					instance = new DBConnection();
				}
			}
		}
		return instance;
	}

	public DBConnection(){
		// Khởi tạo trực tiesp DBConnection nếu chưa có
		try {
            if (connection == null || connection.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver"); // Nạp Driver vào mySQL

                // Connect DB
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Kết nối MySQL thành công");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Không tìm thấy thư viện MySQL Driver");
        } catch (SQLException e){
            System.err.println("Sai thông tin đăng nhập");
            e.printStackTrace();
        }}
		
	// Cáp mạng nối DB
	public Connection getConnection(){
        return connection;
    }
}