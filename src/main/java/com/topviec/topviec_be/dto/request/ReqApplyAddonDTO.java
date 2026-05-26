package com.topviec.topviec_be.dto.request;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

@Data
public class ReqApplyAddonDTO {

    private String serviceCode;

    private Long companyAddonId;

    @AssertTrue(message = "Can truyen serviceCode hoac companyAddonId.")
    public boolean isValidTarget() {
        return companyAddonId != null || (serviceCode != null && !serviceCode.isBlank());
    }
}
