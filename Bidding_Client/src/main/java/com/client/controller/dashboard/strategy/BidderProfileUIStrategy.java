package com.client.controller.dashboard.strategy;

import com.client.controller.dashboard.UserProfileController;
import com.shared.dto.IUserProfileDTO;
import com.shared.dto.BaseProfileUpdateDTO;
import com.shared.dto.BidderProfileUpdateDTO;

public class BidderProfileUIStrategy implements IProfileUIStrategy {

    @Override
    public void displayProfile(UserProfileController controller, IUserProfileDTO profile) {
        if (profile instanceof BidderProfileUpdateDTO) {
            BidderProfileUpdateDTO bidderProfile = (BidderProfileUpdateDTO) profile;
            if (controller.getTxtCreditCardInfo() != null) {
                controller.getTxtCreditCardInfo().setText(
                    bidderProfile.getCreditCardInfo() != null ? bidderProfile.getCreditCardInfo() : ""
                );
            }
        }
    }

    @Override
    public BaseProfileUpdateDTO collectData(UserProfileController controller) {
        BidderProfileUpdateDTO data = new BidderProfileUpdateDTO();
        data.setFullName(controller.getTxtFullName().getText());
        data.setEmail(controller.getTxtEmail().getText());
        data.setPhoneNumber(controller.getTxtPhone().getText());
        data.setAddress(controller.getTxtAddress().getText());
        
        if (controller.getTxtCreditCardInfo() != null) {
            data.setCreditCardInfo(controller.getTxtCreditCardInfo().getText());
        }
        
        return data;
    }
}