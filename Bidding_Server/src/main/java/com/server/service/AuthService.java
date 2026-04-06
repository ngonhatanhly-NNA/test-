package com.server.service;

import com.server.DAO.UserRepository;
import com.server.model.User;
import com.shared.dto.*;
import com.server.model.Bidder;
import com.shared.network.Response;

public class AuthService {
    private UserRepository userRepository = new UserRepository();

    public Response register (RegisterRequestDTO dto){
        if (userRepository.getUserByUsername(dto.getUsername()) != null){
            return new Response("FAIL", "Lỗi: Tài khoản đã tồn tại!", null);
        }

        // Chuyển DTO thành Model, nội bộ trong server
        Bidder newBidder = new Bidder(dto.getUsername(), dto.getPassword(), dto.getEmail(), dto.getFullName(), "BIDDER");
        newBidder.updatePassword(dto.getPassword()); // Xử lí bảo mật // Có thể hash Password /.....

        boolean isSaved = userRepository.saveUser(newBidder);

        // Ddưa về controller để controller dóng gói lại và trả cho Client
        if (isSaved) {
            return new Response("SUCCESS", "Đăng ký thành công nha!", null);
        } else {
            return new Response("FAIL", "Lỗi: Tài khoản đã tồn tại!", null);
        }
    }

    public Response login (LoginRequestDTO loginData){
        User userInDb = userRepository.getUserByUsername(loginData.getUsername());

        // Kiểm tra tồn tại
        if (userInDb == null) {
            return new Response("FAIL", "Tài khoản không tồn tại!", null);
        }

        // So khớp mật khẩu (Logic quan trọng nhất)
        if (userInDb.getPasswordHash().equals(loginData.getPassword())) {

            // 1. Khai báo 1 cái ví rỗng mặc định
            double wallet = 0.0;

            // 2. Kĩ thuật KIỂM TRA KIỂU (instanceof)
            // Nếu thằng userInDb này thực chất là Bidder (hoặc Seller vì Seller là con Bidder)
            if (userInDb instanceof Bidder) {
                // Ép nó hiện nguyên hình thành Bidder rồi mới thò tay lấy ví (Downcasting)
                wallet = ((Bidder) userInDb).getWalletBalance();
            }

            // 3. Đóng gói Profile an toàn để gửi về Client
            UserProfileResponseDTO profileDTO = new UserProfileResponseDTO(
                    userInDb.getId(), userInDb.getUsername(), userInDb.getEmail(),
                    userInDb.getFullName(), userInDb.getPhoneNumber(),
                    userInDb.getAddress(), userInDb.getRole(), wallet // Truyền cái wallet vừa check an toàn vào đây!
            );
            return new Response("SUCCESS", "Đăng nhập thành công!", profileDTO);
        } else {
            return new Response("FAIL", "Sai mật khẩu rồi bạn ơi!", null);
        }
    }
}