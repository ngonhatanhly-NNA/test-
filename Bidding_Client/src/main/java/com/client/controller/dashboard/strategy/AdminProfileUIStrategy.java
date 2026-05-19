package com.client.controller.dashboard.strategy;

import com.client.controller.dashboard.UserProfileController;
import com.shared.dto.IUserProfileDTO;
import com.shared.dto.BaseProfileUpdateDTO;
import com.shared.dto.AdminProfileUpdateDTO;

public class AdminProfileUIStrategy implements IProfileUIStrategy {

    @Override
    public void displayProfile(UserProfileController controller, IUserProfileDTO profile) {
        if (profile instanceof AdminProfileUpdateDTO) {
            AdminProfileUpdateDTO adminProfile = (AdminProfileUpdateDTO) profile;
            if (controller.getTxtRoleLevel() != null) {
                controller.getTxtRoleLevel().setText(
                    adminProfile.getRoleLevel() != null ? adminProfile.getRoleLevel() : "N/A"
                );
            }
            if (controller.getTxtLastLoginIp() != null) {
                controller.getTxtLastLoginIp().setText(
                    adminProfile.getLastLoginIp() != null ? adminProfile.getLastLoginIp() : "N/A"
                );
            }
        }
    }

    @Override
    public BaseProfileUpdateDTO collectData(UserProfileController controller) {
        AdminProfileUpdateDTO data = new AdminProfileUpdateDTO();
        data.setFullName(controller.getTxtFullName().getText());
        data.setEmail(controller.getTxtEmail().getText());
        data.setPhoneNumber(controller.getTxtPhone().getText());
        data.setAddress(controller.getTxtAddress().getText());
        
        if (controller.getTxtRoleLevel() != null) {
            data.setRoleLevel(controller.getTxtRoleLevel().getText());
        }
        
        return data;
    }
}
