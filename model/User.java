package com.server.model;

public abstract class User extends Entity {
    private String username;
    private String passwordHash; // Lưu băm (hash), không lưu plain text
    private String email;
    private String fullName;
    private String phoneNumber;
    private String address;

    private Status status; //  "ACTIVE", "INACTIVE", "BANNED"
    private Role role; //Mo rong thanh Enum BIDDER, SELLER, ADMIN

    public User(){};
    // Abstract dung cho con thoi
    protected User(long id, String username, String passwordHash, String email, String fullName, String phoneNumber, String address, Status status, Role role) {
        super(id);
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.status = status;
        this.role = role;
    }

    // Các thông tin cá nhân cơ bản có thể thay đổi
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email;}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName;  }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber;}

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    // Bảo mật lớp bên trong
    
    // Username
    public String getUsername() { return username; }
    public void setUsername(String username){this.username = username;}

    // Trả về chuỗi hash để so sánh lúc login, không có public setter
    public String getPasswordHash() { return passwordHash; }
    
    // Hàm cho đổi mặt khẩu
    public void updatePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public Status getStatus() { return status; }
    public void updateStatus(Status newStatus) {
        this.status = newStatus;
    }

    public Role getRole() { return role; }
    public void setRole(Role newRole) {
        this.role = newRole;
    }
}