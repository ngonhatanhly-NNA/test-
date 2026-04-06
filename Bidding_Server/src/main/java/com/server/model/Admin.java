package com.server.model;

public class Admin extends User {
    private String roleLevel; 
    private String lastLoginIp;


    public Admin(){};
    public Admin(int id, String username, String passwordHash, String email, String fullName, String phoneNumber, String address, String status, String roleLevel) {
        super(id, username, passwordHash, email, fullName, phoneNumber, address, status, "ADMIN");
        this.roleLevel = roleLevel;
        this.lastLoginIp = "N/A"; // Initial
    }

    public String getRoleLevel() { return roleLevel; }
    
    // Nâng quyền hạn Admin(Bởi Admin khác level cao hơn, or server)
    public void setRoleLevel(String roleLevel) {
        this.roleLevel = roleLevel;
    }

    public String getLastLoginIp() { return lastLoginIp; }
    
    // Admin login thành công
    public void updateLoginIp(String ipAddress) {
        this.lastLoginIp = ipAddress;
    }
}