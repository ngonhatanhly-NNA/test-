package com.server.service;

import com.server.DAO.UserRepository;
import com.server.model.User;
import com.server.model.Bidder;
import com.shared.dto.*;
import com.shared.network.Response;

public class AuthService {
    // GIỮ NGUYÊN: Tự khởi tạo UserRepository bên trong
    private UserRepository userRepository = new UserRepository();

    public Response register (RegisterRequestDTO dto){
        if (userRepository.getUserByUsername(dto.getUsername()) != null){
            return new Response("FAIL", "Lỗi: Tài khoản đã tồn tại!", null);
        }

        // FIX OOP: Dùng Constructor 4 tham số của Bidder mà chúng ta đã làm
        // Nó sẽ tự động set Role.BIDDER và Status.ACTIVE bên trong class rồi, không cần truyền chuỗi "BIDDER" nữa.
        Bidder newBidder = new Bidder(dto.getUsername(), dto.getPassword(), dto.getEmail(), dto.getFullName());

        // Đoạn này nếu bạn có logic hash mật khẩu riêng thì cứ để
        // newBidder.updatePassword(dto.getPassword());

        boolean isSaved = userRepository.saveUser(newBidder);

        // Đưa về controller để controller đóng gói lại và trả cho Client
        if (isSaved) {
            return new Response("SUCCESS", "Đăng ký thành công nha!", null);
        } else {
            return new Response("FAIL", "Lỗi: Đăng ký thất bại do hệ thống!", null); // Đổi text tí cho chuẩn logic
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
            // Nhờ OOP: Seller là con của Bidder, nên hàm này sẽ "bắt" được cả Bidder và Seller
            if (userInDb instanceof Bidder) {
                // Ép kiểu và chuyển BigDecimal sang double
                wallet = ((Bidder) userInDb).getWalletBalance().doubleValue();
            }

            // 3. Đóng gói Profile an toàn để gửi về Client
            // FIX OOP: userInDb.getRole() giờ đang trả về Object Enum, bạn thêm .name() để biến nó thành String nhé!
            UserProfileResponseDTO profileDTO = new UserProfileResponseDTO(
                    (int) userInDb.getId(), // Ép kiểu long về int nếu DTO của bạn xài int
                    userInDb.getUsername(),
                    userInDb.getEmail(),
                    userInDb.getFullName(),
                    userInDb.getPhoneNumber(),
                    userInDb.getAddress(),
                    userInDb.getRole().name(), // Thêm .name() ở đây là hết đỏ!
                    wallet
            );
            return new Response("SUCCESS", "Đăng nhập thành công!", profileDTO);
        } else {
            return new Response("FAIL", "Sai mật khẩu rồi bạn ơi!", null);
        }
    }
}