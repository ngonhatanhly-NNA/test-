package com.server.service;

import com.server.DAO.UserRepository;
import com.server.exception.AppException;
import com.server.exception.AuthValidationException;
import com.server.exception.DuplicateUserException;
import com.server.exception.InvalidCredentialException;
import com.server.exception.UserNotFoundException;
import com.server.model.User;
import com.server.model.Bidder;
import com.server.util.ResponseUtils;
import com.shared.dto.*;
import com.shared.network.Response;

public class AuthService {
    // GIỮ NGUYÊN: Tự khởi tạo UserRepository bên trong
    private final UserRepository userRepository;

    public AuthService() {
        this(new UserRepository());
    }

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Response register (RegisterRequestDTO dto){
        if (dto == null) {
            throw new AuthValidationException("Dữ liệu đăng ký không hợp lệ!");
        }
        if (isBlank(dto.getUsername())) {
            throw new AuthValidationException("Vui lòng nhập tài khoản!");
        }
        if (isBlank(dto.getPassword())) {
            throw new AuthValidationException("Vui lòng nhập mật khẩu!");
        }
        if (isBlank(dto.getEmail())) {
            throw new AuthValidationException("Vui lòng nhập email!");
        }
        if (isBlank(dto.getFullName())) {
            throw new AuthValidationException("Vui lòng nhập họ và tên!");
        }

        if (userRepository.getUserByUsername(dto.getUsername()) != null){
            throw new DuplicateUserException(dto.getUsername());
        }

        // FIX OOP: Dùng Constructor 4 tham số của Bidder mà chúng ta đã làm
        // Nó sẽ tự động set Role.BIDDER và Status.ACTIVE bên trong class rồi, không cần truyền chuỗi "BIDDER" nữa.
        Bidder newBidder = new Bidder(dto.getUsername(), dto.getPassword(), dto.getEmail(), dto.getFullName());

        // Đoạn này nếu bạn có logic hash mật khẩu riêng thì cứ để
        // newBidder.updatePassword(dto.getPassword());

        boolean isSaved = userRepository.saveUser(newBidder);

        // Đưa về controller để controller đóng gói lại và trả cho Client
        if (isSaved) {
            return ResponseUtils.success("Đăng ký thành công!", null);
        } else {
            throw new AppException("REGISTER_FAILED", "Đăng ký thất bại do hệ thống!", 500);
        }
    }

    public Response login (LoginRequestDTO loginData){
        if (loginData == null) {
            throw new AuthValidationException("Dữ liệu đăng nhập không hợp lệ!");
        }
        if (isBlank(loginData.getUsername())) {
            throw new AuthValidationException("Vui lòng nhập tài khoản!");
        }
        if (isBlank(loginData.getPassword())) {
            throw new AuthValidationException("Vui lòng nhập mật khẩu!");
        }

        User userInDb = userRepository.getUserByUsername(loginData.getUsername());

        // Kiểm tra tồn tại
        if (userInDb == null) {
            throw new UserNotFoundException(loginData.getUsername());
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
            return ResponseUtils.success("Đăng nhập thành công!", null);
        } else {
            throw new InvalidCredentialException();
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}