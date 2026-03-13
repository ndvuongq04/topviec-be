package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.request.ReqCreateCompanyDTO;
import com.topviec.topviec_be.dto.request.ReqSuspendCompanyDTO;
import com.topviec.topviec_be.dto.request.ReqUpdateCompanyDTO;
import com.topviec.topviec_be.dto.request.ReqVerifyCompanyDTO;
import com.topviec.topviec_be.dto.response.ResCompanyDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import org.springframework.data.domain.Pageable;

public interface CompanyService {

    // -------------------------------------------------------------------------
    // Employer
    // -------------------------------------------------------------------------

    /** Tạo hồ sơ công ty. Mỗi employer chỉ được tạo 1 công ty. */
    ResCompanyDTO createCompany(Long userId, ReqCreateCompanyDTO request);

    /** Employer xem hồ sơ công ty của chính mình. */
    ResCompanyDTO getMyCompany(Long userId);

    /** Employer cập nhật hồ sơ công ty của mình. */
    ResCompanyDTO updateMyCompany(Long userId, ReqUpdateCompanyDTO request);

    // -------------------------------------------------------------------------
    // Public
    // -------------------------------------------------------------------------

    /** Lấy thông tin công ty theo slug (public, dùng cho trang chi tiết). */
    ResCompanyDTO getBySlug(String slug);

    /** Lấy thông tin công ty theo id (public - chỉ active). */
    ResCompanyDTO getById(Long id);

    /** Admin lấy thông tin công ty theo id (mọi trạng thái). */
    ResCompanyDTO adminGetById(Long id);

    // -------------------------------------------------------------------------
    // Admin
    // -------------------------------------------------------------------------

    /** Admin lấy danh sách công ty chờ duyệt. */
    ResultPaginationDTO getPendingVerification(Pageable pageable);

    /** Admin duyệt hoặc từ chối hồ sơ công ty. */
    ResCompanyDTO verifyCompany(Long companyId, Long adminId, ReqVerifyCompanyDTO request);

    /** Admin suspend công ty vi phạm. */
    ResCompanyDTO suspendCompany(Long companyId, Long adminId, ReqSuspendCompanyDTO request);

    /** Admin mở khóa công ty bị suspend. */
    ResCompanyDTO unsuspendCompany(Long companyId, Long adminId);

    /** Admin sửa thông tin công ty. */
    ResCompanyDTO adminUpdateCompany(Long companyId, Long adminId, ReqUpdateCompanyDTO request);

    /** Admin xóa mềm công ty. */
    void deleteCompany(Long companyId, Long adminId);

    /** Admin lấy danh sách tất cả công ty. */
    ResultPaginationDTO getAllCompanies(String status, Pageable pageable);
}