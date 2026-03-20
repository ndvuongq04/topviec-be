package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.request.ReqCreateJobPostingDTO;
import com.topviec.topviec_be.dto.request.ReqUpdateJobPostingDTO;
import com.topviec.topviec_be.dto.response.ResJobPostingDetail;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import org.springframework.data.domain.Pageable;

public interface JobPostingService {

    /** Employer tạo tin tuyển dụng mới, mặc định ở trạng thái draft. */
    ResJobPostingDetail create(ReqCreateJobPostingDTO request, Long createdByUserId, Long companyId);

    /**
     * Lấy danh sách tin tuyển dụng (Employer/Admin), hỗ trợ filter + phân trang.
     */
    ResultPaginationDTO getList(String keyword, Long companyId, Long industryId,
            Long levelId, String workType, String status,
            Boolean isFeatured, Boolean isUrgent,
            Long salaryMin, Long salaryMax,
            Integer experienceYearsMin, Integer experienceYearsMax,
            Pageable pageable);

    /** Lấy danh sách tin published (ứng viên), hỗ trợ filter + phân trang. */
    ResultPaginationDTO getPublicList(String keyword, Long companyId, Long industryId,
            Long levelId, String workType,
            Boolean isFeatured, Boolean isUrgent,
            Long salaryMin, Long salaryMax,
            Integer experienceYearsMin, Integer experienceYearsMax,
            Pageable pageable);

    /** Xem chi tiết tin tuyển dụng, tự động tăng view_count. */
    ResJobPostingDetail getDetail(Long id);

    /** Employer chỉnh sửa tin tuyển dụng. */
    ResJobPostingDetail update(Long id, ReqUpdateJobPostingDTO request, Long updatedByUserId, Long companyId);
}