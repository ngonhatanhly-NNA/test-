package com.shared.dto;

import java.util.Objects;
import com.google.gson.annotations.SerializedName;
// Ánh xạ khi truyền dữ liệu

public class AdminProfileUpdateDTO extends BaseProfileUpdateDTO {
    @SerializedName("roleLevel")
    private String roleLevel;

    @SerializedName("lastLoginIp")
    private String lastLoginIp;

    public AdminProfileUpdateDTO() {
        super();
    }

    public AdminProfileUpdateDTO(String email, String fullName, String phoneNumber, String address, 
                                 String roleLevel, String lastLoginIp) {
        super(email, fullName, phoneNumber, address);
        this.roleLevel = roleLevel;
        this.lastLoginIp = lastLoginIp;
    }

    public String getRoleLevel() { return roleLevel; }
    public void setRoleLevel(String roleLevel) { this.roleLevel = roleLevel; }

    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AdminProfileUpdateDTO that = (AdminProfileUpdateDTO) o;
        return Objects.equals(roleLevel, that.roleLevel) && Objects.equals(lastLoginIp, that.lastLoginIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), roleLevel, lastLoginIp);
    }

    @Override
    public String toString() {
        return "AdminProfileUpdateDTO{" +
                "roleLevel='" + roleLevel + '\'' +
                ", lastLoginIp='" + lastLoginIp + '\'' +
                ", " + super.toString() +
                '}';
    }
}