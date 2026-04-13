package com.server.DAO;

import com.server.model.Admin;
import com.server.model.Seller;
import com.server.model.Status;

public interface IAdminRepository {
    Admin getAdminByUsername(String username);
    void updateLastLoginIp(long adminId, String ip);

    // Ham thay doi trang thai (CAM/MO CAM) tai khoan
    boolean updateUserStatus(long userId, Status newStatus);

    // Ham nang cap len nguoi ban
    boolean promoteToSeller(Seller seller);
}