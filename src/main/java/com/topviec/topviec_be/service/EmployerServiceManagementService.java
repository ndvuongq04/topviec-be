package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.request.ReqApplyAddonDTO;
import com.topviec.topviec_be.dto.response.ResCompanyAddonDTO;
import com.topviec.topviec_be.dto.response.ResCompanyBrandingDTO;
import com.topviec.topviec_be.dto.response.ResCompanySubscriptionDTO;
import com.topviec.topviec_be.dto.response.ResJobPostAddonDTO;

import java.util.List;

public interface EmployerServiceManagementService {

    /** Lấy thông tin gói dịch vụ hiện tại NTD đang dùng và hạn mức còn lại */
    ResCompanySubscriptionDTO getMySubscription(Long userId);

    /** Lấy danh sách các dịch vụ lẻ mà NTD đã mua và số lượng còn lại */
    List<ResCompanyAddonDTO> getMyAddons(Long userId);

    /** Áp dụng dịch vụ lẻ cho một tin tuyển dụng */
    ResJobPostAddonDTO applyAddonToJobPost(Long userId, Long jobPostingId, ReqApplyAddonDTO request);

    /** Áp dụng dịch vụ Banner trang chủ cho công ty */
    ResCompanyBrandingDTO applyBannerToCompany(Long userId, ReqApplyAddonDTO request);
}
