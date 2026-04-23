package com.server.service;

import com.server.DAO.UserRepository;
import com.server.exception.AppException;
import com.server.exception.AuthValidationException;
import com.server.exception.DuplicateUserException;
import com.server.exception.InvalidCredentialException;
import com.server.exception.UserNotFoundException;
import com.server.model.User;
import com.server.model.Bidder;
import com.shared.dto.*;
import org.mindrot.jbcrypt.BCrypt;


public class AuthService {
    private final UserRepository userRepository;

    public AuthService() {
        this(new UserRepository());
    }

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void register (RegisterRequestDTO dto){
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
            throw new DuplicateUserException(DuplicateUserException.ErrorCode.USERNAME_EXISTED,dto.getUsername());
        }
        String hashedPassword = BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt());
        Bidder newBidder = new Bidder(dto.getUsername(), hashedPassword, dto.getEmail(), dto.getFullName());

        // Đoạn này nếu bạn có logic hash mật khẩu riêng thì cứ để
        // newBidder.updatePassword(dto.getPassword());

        boolean isSaved = userRepository.saveUser(newBidder);

        // Đưa về controller để controller đóng gói lại và trả cho Client
        if (!isSaved) {
            throw new AppException("REGISTER_FAILED", "Đăng ký thất bại do hệ thống!", 500);
        }
    }

    public UserProfileResponseDTO login (LoginRequestDTO loginData){
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

        if (!isPasswordValid(loginData.getPassword(), userInDb.getPasswordHash())) {
            throw new InvalidCredentialException();
        }

        return createUserProfile(userInDb);
    }

    /**
     * Lấy thông tin profile của user bằng username
     */
    public UserProfileResponseDTO getUserProfile(String username) {
        if (isBlank(username)) {
            throw new AuthValidationException("Username không được để trống!");
        }

        User user = userRepository.getUserByUsername(username);
        if (user == null) {
            throw new UserNotFoundException(username);
        }

        return createUserProfile(user);
    }

    /**
     * Cập nhật thông tin profile của user
     */
    public void updateProfile(BaseProfileUpdateDTO updateData) {
        if (updateData == null || updateData.getId() <= 0) {
            throw new AuthValidationException("Dữ liệu cập nhật không hợp lệ!");
        }

        User user = userRepository.getUserById(updateData.getId());
        if (user == null) {
            throw new UserNotFoundException("User ID: " + updateData.getId());
        }

        // Cập nhật các trường nếu có dữ liệu
        if (updateData.getFullName() != null && !updateData.getFullName().trim().isEmpty()) {
            user.setFullName(updateData.getFullName());
        }
        if (updateData.getEmail() != null && !updateData.getEmail().trim().isEmpty()) {
            user.setEmail(updateData.getEmail());
        }
        if (updateData.getPhoneNumber() != null && !updateData.getPhoneNumber().trim().isEmpty()) {
            user.setPhoneNumber(updateData.getPhoneNumber());
        }
        if (updateData.getAddress() != null && !updateData.getAddress().trim().isEmpty()) {
            user.setAddress(updateData.getAddress());
        }

        boolean isUpdated = userRepository.updateUser(user);
        if (!isUpdated) {
            throw new AppException("UPDATE_FAILED", "Cập nhật thông tin thất bại!", 500);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean isPasswordValid(String rawPassword, String storedHash) {
        if (isBlank(storedHash)) {
            return false;
        }

        try {
            return BCrypt.checkpw(rawPassword, storedHash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static UserProfileResponseDTO createUserProfile(User user) {
        double wallet = 0.0;
        if (user instanceof Bidder) {
            wallet = ((Bidder) user).getWalletBalance().doubleValue();
        }

        return new UserProfileResponseDTO(
                (int) user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getAddress(),
                user.getRole().name(),
                wallet
        );
    }
}