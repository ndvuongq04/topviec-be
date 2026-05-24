package com.topviec.topviec_be.service.activation;

import com.topviec.topviec_be.entity.CompanyAddon;
import com.topviec.topviec_be.enums.services.ServiceCategory;
import com.topviec.topviec_be.enums.services.SubscriptionStatus;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.CompanyAddonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Category-level service cho nhóm CANDIDATE (Hồ sơ ứng viên).
 *
 * Hiện hỗ trợ:
 * - CV_SEARCH_BASIC: Tìm kiếm CV ứng viên (trừ theo lượt)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateActivationService {

        public static final ServiceCategory CATEGORY = ServiceCategory.CANDIDATE;
        public static final String CANDIDATE_CV_SEARCH = "CANDIDATE_CV_SEARCH";

        private final CompanyAddonRepository companyAddonRepository;

        /**
         * Kiểm tra & trừ 1 lượt tìm kiếm CV cho công ty.
         *
         * - Tìm CompanyAddon có addon code = CANDIDATE_CV_SEARCH, status = ACTIVE, còn lượt
         * - Trừ quantityRemaining -= 1
         * - Nếu không tìm thấy hoặc hết lượt → throw 403 Forbidden
         *
         * @param companyId ID công ty
         */
        @Transactional
        public void consumeCvSearchQuota(Long companyId) {
                CompanyAddon companyAddon = companyAddonRepository
                                .findFirstActiveByCompanyIdAndAddonCode(
                                                companyId, CANDIDATE_CV_SEARCH, SubscriptionStatus.ACTIVE)
                                .orElseThrow(() -> AppException.forbidden(
                                                "Công ty chưa mua dịch vụ Tìm kiếm CV hoặc đã hết lượt sử dụng."));

                if (companyAddon.getQuantityRemaining() <= 0) {
                        throw AppException.forbidden(
                                        "Đã hết lượt tìm kiếm CV. Vui lòng mua thêm dịch vụ.");
                }

                companyAddon.setQuantityRemaining(companyAddon.getQuantityRemaining() - 1);
                companyAddonRepository.save(companyAddon);

                log.info("[CvSearch] Trừ 1 lượt cho công ty #{}, còn lại: {}",
                                companyId, companyAddon.getQuantityRemaining());
        }

        /**
         * Chỉ kiểm tra xem công ty có quyền tìm kiếm CV hay không (không trừ lượt).
         * Dùng khi chuyển trang (page > 0).
         *
         * @param companyId ID công ty
         */
        @Transactional(readOnly = true)
        public void checkCvSearchQuota(Long companyId) {
                CompanyAddon companyAddon = companyAddonRepository
                                .findFirstActiveByCompanyIdAndAddonCode(
                                                companyId, CANDIDATE_CV_SEARCH, SubscriptionStatus.ACTIVE)
                                .orElseThrow(() -> AppException.forbidden(
                                                "Công ty chưa mua dịch vụ Tìm kiếm CV hoặc đã hết lượt sử dụng."));

                if (companyAddon.getQuantityRemaining() <= 0) {
                        throw AppException.forbidden(
                                        "Đã hết lượt tìm kiếm CV. Vui lòng mua thêm dịch vụ.");
                }
        }
}
