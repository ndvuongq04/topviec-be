package com.topviec.topviec_be.service.activation;

import com.topviec.topviec_be.dto.internal.ServiceQuotaAllocation;
import com.topviec.topviec_be.enums.services.ServiceCategory;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.service.CompanyServiceQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateActivationService {

        public static final ServiceCategory CATEGORY = ServiceCategory.CANDIDATE;
        public static final String CANDIDATE_CV_SEARCH = "CANDIDATE_CV_SEARCH";

        private final CompanyServiceQuotaService quotaService;

        @Transactional
        public void consumeCvSearchQuota(Long companyId) {
                ServiceQuotaAllocation quota = quotaService.findAvailableQuotaForUpdate(companyId, CANDIDATE_CV_SEARCH);
                quotaService.consume(quota);

                log.info("[CvSearch] Consume 1 quota for company #{}, service={}, source={}",
                                companyId, CANDIDATE_CV_SEARCH, quota.getSourceType());
        }

        @Transactional(readOnly = true)
        public void checkCvSearchQuota(Long companyId) {
                if (!quotaService.hasAvailableQuota(companyId, CANDIDATE_CV_SEARCH)) {
                        throw AppException.forbidden(
                                        "Cong ty chua mua dich vu Tim kiem CV hoac da het luot su dung.");
                }
        }
}
