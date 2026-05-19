package com.client.controller.dashboard.strategy;

import com.client.controller.dashboard.UserProfileController;
import com.shared.dto.IUserProfileDTO;
import com.shared.dto.BaseProfileUpdateDTO;

public interface IProfileUIStrategy {
    // Hiển thị dữ liệu từ DTO lên các TextField tương ứng
    void displayProfile(UserProfileController controller, IUserProfileDTO profile);

    // Thu thập dữ liệu từ các TextField để đóng gói vào DTO gửi lên Server
    BaseProfileUpdateDTO collectData(UserProfileController controller);
}