package com.server.DAO;

import com.server.model.User;
import com.server.model.Role;
import com.shared.dto.BaseProfileUpdateDTO;

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

    boolean updateFullProfile(BaseProfileUpdateDTO dto, Role role, long userId);

    /**
     * Cập nhật mật khẩu User
     */
    boolean updatePassword(String username, String newPassword);
}