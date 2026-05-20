package com.client.controller.dashboard.strategy;

import com.client.controller.dashboard.UserProfileController;
import com.shared.dto.IUserProfileDTO;
import com.shared.dto.BaseProfileUpdateDTO;
import com.shared.dto.SellerProfileUpdateDTO;

public class SellerProfileUIStrategy implements IProfileUIStrategy {

    @Override
    public void displayProfile(UserProfileController controller, IUserProfileDTO profile) {
        if (profile instanceof SellerProfileUpdateDTO) {
            SellerProfileUpdateDTO sellerProfile = (SellerProfileUpdateDTO) profile;
            if (controller.getTxtShopName() != null) {
                controller.getTxtShopName().setText(
                    sellerProfile.getShopName() != null ? sellerProfile.getShopName() : ""
                );
            }
            if (controller.getTxtBankAccountNumber() != null) {
                controller.getTxtBankAccountNumber().setText(
                    sellerProfile.getBankAccountNumber() != null ? sellerProfile.getBankAccountNumber() : ""
                );
            }
            if (controller.getTxtCreditCardInfo() != null) {
                controller.getTxtCreditCardInfo().setText(
                    sellerProfile.getCreditCardInfo() != null ? sellerProfile.getCreditCardInfo() : ""
                );
            }
        }
    }

    @Override
    public BaseProfileUpdateDTO collectData(UserProfileController controller) {
        SellerProfileUpdateDTO data = new SellerProfileUpdateDTO();
        data.setFullName(controller.getTxtFullName().getText());
        data.setEmail(controller.getTxtEmail().getText());
        data.setPhoneNumber(controller.getTxtPhone().getText());
        data.setAddress(controller.getTxtAddress().getText());
        
        if (controller.getTxtCreditCardInfo() != null) {
            data.setCreditCardInfo(controller.getTxtCreditCardInfo().getText());
        }
        if (controller.getTxtShopName() != null) {
            data.setShopName(controller.getTxtShopName().getText());
        }
        if (controller.getTxtBankAccountNumber() != null) {
            data.setBankAccountNumber(controller.getTxtBankAccountNumber().getText());
        }
        
        return data;
    }
}
