package com.server.model;

public class Admin extends User {
    private String roleLevel;
    private String lastLoginIp;
    public Admin() {}

    // Đã đổi kiểu Status
    public Admin(long id, String username, String passwordHash, String email, String fullName,
                 String phoneNumber, String address, Status status, String roleLevel) {
        // TRUYỀN TRỰC TIẾP ENUM
        super(id, username, passwordHash, email, fullName, phoneNumber, address, status, Role.ADMIN);
        this.roleLevel = roleLevel;
        this.lastLoginIp = "N/A";
    }

    // --- GETTERS & SETTERS ---
    public String getRoleLevel() { return roleLevel; }
    public void setRoleLevel(String roleLevel) { this.roleLevel = roleLevel; }

    public String getLastLoginIp() { return lastLoginIp; }
    public void updateLoginIp(String ipAddress) { this.lastLoginIp = ipAddress; }
}