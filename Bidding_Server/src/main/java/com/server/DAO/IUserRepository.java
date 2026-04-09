package com.server.DAO;

import com.server.model.User;
import com.shared.dto.UserProfileUpdateDTO;

/**
 * Interface Repository cho User - thỏa mãn Dependency Inversion Principle
 * Cho phép dễ dàng mock trong testing và thay thế implementation
 */
public interface IUserRepository {
    /**
     * Lưu User vào database
     */
    boolean saveUser(User user);

    /**
     * Lấy User theo username
     */
    User getUserByUsername(String username);

    /**
     * Cập nhật toàn bộ profile của User
     */
    boolean updateFullProfile(UserProfileUpdateDTO dto, long userId);

    /**
     * Cập nhật mật khẩu User
     */
    boolean updatePassword(String username, String newPassword);
}

