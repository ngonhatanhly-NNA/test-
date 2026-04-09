package com.shared.dto;

public class AdminProfileUpdateDTO extends BaseProfileUpdateDTO {
    private String roleLevel;
    private String lastLoginIp;

    public AdminProfileUpdateDTO() {
        super();
    }

    public String getRoleLevel() { return roleLevel; }
    public void setRoleLevel(String roleLevel) { this.roleLevel = roleLevel; }

    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }
}