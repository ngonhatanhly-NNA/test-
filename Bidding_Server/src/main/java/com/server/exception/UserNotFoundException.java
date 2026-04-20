package com.server.exception;

/**
 * Exception được ném ra khi không tìm thấy một người dùng trong hệ thống.
 */
public class UserNotFoundException extends AppException {

    /**
     * Exception khi không tìm thấy người dùng bằng username.
     * @param username Tên tài khoản đã tìm.
     */
    public UserNotFoundException(String username) {
        super("USER_NOT_FOUND", "Không tìm thấy người dùng có tài khoản: '" + username + "'");
    }

    /**
     * Exception khi không tìm thấy người dùng bằng ID.
     * @param id ID của người dùng đã tìm.
     */
    public UserNotFoundException(long id) {
        super("USER_NOT_FOUND", "Không tìm thấy người dùng có ID: " + id);
    }
}
