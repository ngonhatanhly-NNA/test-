package com.server.DAO;

import com.server.model.User;
import com.server.model.Role;
import com.server.model.Status;
import com.shared.dto.BaseProfileUpdateDTO;

public interface IUserRepository {
    boolean saveUser(User user);
    User getUserByUsername(String username);
    boolean updateFullProfile(BaseProfileUpdateDTO dto, Role role, long userId);
    boolean updatePassword(String username, String newPassword);
    boolean updateUserStatus(long userId, Status newStatus);
    
    /**
     * Cập nhật các thông tin cơ bản của User.
     */
    boolean updateUser(User user);
}
