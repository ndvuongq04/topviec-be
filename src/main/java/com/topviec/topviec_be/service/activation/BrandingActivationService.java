package com.topviec.topviec_be.service.activation;

import com.topviec.topviec_be.enums.services.ServiceCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Category-level service cho nhóm BRANDING (Thương hiệu).
 *
 * Nhóm này sẽ bao gồm các dịch vụ nhỏ liên quan đến thương hiệu
 * (ví dụ: banner quảng cáo, trang công ty nổi bật, ...).
 *
 * Sẽ triển khai chi tiết ở giai đoạn sau.
 */
@Service
@Slf4j
public class BrandingActivationService {

    public static final ServiceCategory CATEGORY = ServiceCategory.BRANDING;

    // TODO: Định nghĩa các service code cho nhóm BRANDING
    // public static final String CODE_BANNER_AD = "BRANDING_BANNER";
    // public static final String CODE_FEATURED_COMPANY = "BRANDING_FEATURED_COMPANY";
}
