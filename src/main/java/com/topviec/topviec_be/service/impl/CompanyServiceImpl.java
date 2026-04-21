package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.dto.request.ReqAdminUpdateCompanyDTO;
import com.topviec.topviec_be.dto.request.ReqUpdateCompanyDTO;
import com.topviec.topviec_be.dto.response.ResCompanyDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import com.topviec.topviec_be.entity.Company;
import com.topviec.topviec_be.enums.company.CompanySize;
import com.topviec.topviec_be.enums.company.CompanyStatus;
import com.topviec.topviec_be.enums.company.VerificationStatus;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.CompanyRepository;
import com.topviec.topviec_be.repository.CompanyMemberRepository;
import com.topviec.topviec_be.entity.CompanyMember;
import com.topviec.topviec_be.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMemberRepository companyMemberRepository;

    // -------------------------------------------------------------------------
    // Employer — Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ResCompanyDTO getMyCompany(Long userId) {
        return toResponse(findByCreatedByOrThrow(userId));
    }

    // -------------------------------------------------------------------------
    // Employer — Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ResCompanyDTO updateMyCompany(Long userId, ReqUpdateCompanyDTO request) {
        Company company = findByCreatedByOrThrow(userId);

        if (CompanyStatus.SUSPENDED.getValue().equals(company.getStatus())) {
            throw AppException.badRequest("Công ty đang bị tạm khóa, không thể cập nhật hồ sơ");
        }
        if (CompanyStatus.DELETED.getValue().equals(company.getStatus())) {
            throw AppException.badRequest("Công ty đã bị xóa");
        }

        applyUpdate(company, request, userId);

        // Nếu đang rejected → chuyển về pending để admin duyệt lại
        if (VerificationStatus.REJECTED.getValue().equals(company.getVerificationStatus())) {
            company.setVerificationStatus(VerificationStatus.PENDING.getValue());
            company.setRejectionReason(null);
        }

        return toResponse(companyRepository.save(company));
    }

    // -------------------------------------------------------------------------
    // Public — UV
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ResCompanyDTO getBySlug(String slug) {
        Company company = companyRepository.findBySlug(slug)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy công ty"));

        if (!CompanyStatus.ACTIVE.getValue().equals(company.getStatus())) {
            throw AppException.notFound("Không tìm thấy công ty");
        }

        return toResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
    public ResCompanyDTO getById(Long id) {
        Company company = findByIdOrThrow(id);

        if (!CompanyStatus.ACTIVE.getValue().equals(company.getStatus())) {
            throw AppException.notFound("Không tìm thấy công ty");
        }

        return toResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getPublicCompanies(String keyword, Integer provinceId,
            Long industryId, Boolean isBanner, Boolean isTopEmployer, Boolean isBrandVerified,
            Pageable pageable) {

        String keywordParam = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        Page<Company> page = companyRepository.findPublicCompanies(
                keywordParam, provinceId, industryId, isBanner, isTopEmployer, isBrandVerified, pageable);

        return toResultPagination(page, pageable);
    }

    // -------------------------------------------------------------------------
    // Admin — Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ResCompanyDTO adminGetById(Long id) {
        return toResponse(findByIdOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getAllCompanies(String status, String verificationStatus,
            String keyword, Pageable pageable) {

        String statusParam = (status != null && !status.isBlank()) ? status.trim().toLowerCase() : null;
        String verificationStatusParam = (verificationStatus != null && !verificationStatus.isBlank())
                ? verificationStatus.trim().toLowerCase()
                : null;
        String keywordParam = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        Page<Company> page = companyRepository.findAllWithFilter(
                statusParam, verificationStatusParam, keywordParam, pageable);

        return toResultPagination(page, pageable);
    }

    // -------------------------------------------------------------------------
    // Admin — Update (gộp status + info thành 1)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ResCompanyDTO adminUpdateCompany(Long companyId, Long adminId,
            ReqAdminUpdateCompanyDTO request) {

        Company company = findByIdOrThrow(companyId);

        // Bước 1: Xử lý action status nếu có
        if (request.getAction() != null && !request.getAction().isBlank()) {
            switch (request.getAction().toLowerCase()) {

                case "verify" -> {
                    if (!VerificationStatus.PENDING.getValue().equals(company.getVerificationStatus())
                            && !VerificationStatus.REJECTED.getValue().equals(company.getVerificationStatus())) {
                        throw AppException
                                .badRequest("Chỉ có thể duyệt hồ sơ đang ở trạng thái chờ duyệt hoặc đã từ chối");
                    }
                    if (Boolean.TRUE.equals(request.getApproved())) {
                        company.setVerificationStatus(VerificationStatus.VERIFIED.getValue());
                        company.setStatus(CompanyStatus.ACTIVE.getValue());
                        company.setVerifiedAt(LocalDateTime.now());
                        company.setVerifiedBy(adminId);
                        company.setRejectionReason(null);
                    } else {
                        if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                            throw AppException.badRequest("Vui lòng nhập lý do từ chối");
                        }
                        company.setVerificationStatus(VerificationStatus.REJECTED.getValue());
                        company.setRejectionReason(request.getRejectionReason());
                        company.setVerifiedBy(adminId);
                    }
                }

                case "suspend" -> {
                    if (CompanyStatus.SUSPENDED.getValue().equals(company.getStatus())) {
                        throw AppException.badRequest("Công ty đã bị tạm khóa rồi");
                    }
                    if (CompanyStatus.DELETED.getValue().equals(company.getStatus())) {
                        throw AppException.badRequest("Công ty đã bị xóa, không thể suspend");
                    }
                    if (request.getSuspendedReason() == null || request.getSuspendedReason().isBlank()) {
                        throw AppException.badRequest("Vui lòng nhập lý do suspend");
                    }
                    company.setStatus(CompanyStatus.SUSPENDED.getValue());
                    company.setSuspendedAt(LocalDateTime.now());
                    company.setSuspendedReason(request.getSuspendedReason());
                }

                case "unsuspend" -> {
                    if (!CompanyStatus.SUSPENDED.getValue().equals(company.getStatus())) {
                        throw AppException.badRequest("Công ty không đang bị tạm khóa");
                    }
                    company.setStatus(CompanyStatus.ACTIVE.getValue());
                    company.setSuspendedAt(null);
                    company.setSuspendedReason(null);
                }

                default -> throw AppException.badRequest(
                        "action không hợp lệ. Chỉ chấp nhận: verify | suspend | unsuspend");
            }
        }

        // Bước 2: Partial update thông tin công ty nếu có field nào được truyền lên
        if (request.getSlug() != null) {
            if (!request.getSlug().equals(company.getSlug())
                    && companyRepository.existsBySlug(request.getSlug())) {
                throw AppException.conflict("Slug đã được sử dụng, vui lòng chọn slug khác");
            }
            company.setSlug(request.getSlug());
        }
        if (request.getName() != null)
            company.setName(request.getName());
        if (request.getLogoUrl() != null)
            company.setLogoUrl(request.getLogoUrl());
        if (request.getCoverUrl() != null)
            company.setCoverUrl(request.getCoverUrl());
        if (request.getDescription() != null)
            company.setDescription(request.getDescription());
        if (request.getIndustryId() != null)
            company.setIndustryId(request.getIndustryId());
        if (request.getCompanySize() != null)
            company.setCompanySize(request.getCompanySize().getValue());
        if (request.getFoundedYear() != null)
            company.setFoundedYear(request.getFoundedYear());
        if (request.getWebsite() != null)
            company.setWebsite(request.getWebsite());
        if (request.getEmail() != null)
            company.setEmail(request.getEmail());
        if (request.getPhone() != null)
            company.setPhone(request.getPhone());
        if (request.getAddress() != null)
            company.setAddress(request.getAddress());
        if (request.getProvinceId() != null)
            company.setProvinceId(request.getProvinceId());
        if (request.getTaxCode() != null) {
            if (!request.getTaxCode().equals(company.getTaxCode())
                    && companyRepository.existsByTaxCode(request.getTaxCode())) {
                throw AppException.conflict("Mã số thuế đã được đăng ký");
            }
            company.setTaxCode(request.getTaxCode());
        }
        if (request.getBusinessLicenseUrl() != null)
            company.setBusinessLicenseUrl(request.getBusinessLicenseUrl());
        if (request.getCulture() != null)
            company.setCulture(request.getCulture());
        if (request.getBenefits() != null)
            company.setBenefits(request.getBenefits());
        if (request.getSocialLinks() != null)
            company.setSocialLinks(request.getSocialLinks());

        company.setUpdatedBy(adminId);
        return toResponse(companyRepository.save(company));
    }

    // -------------------------------------------------------------------------
    // Admin — Delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteCompany(Long companyId, Long adminId) {
        Company company = findByIdOrThrow(companyId);
        company.setDeletedAt(LocalDateTime.now());
        company.setStatus(CompanyStatus.DELETED.getValue());
        company.setUpdatedBy(adminId);
        companyRepository.save(company);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Company findByIdOrThrow(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy công ty"));
    }

    private Company findByCreatedByOrThrow(Long userId) {
        // 1. Kiểm tra xem user có phải người tạo công ty không
        java.util.Optional<Company> companyOpt = companyRepository.findByCreatedBy(userId);
        if (companyOpt.isPresent()) {
            return companyOpt.get();
        }

        // 2. Nếu không, kiểm tra xem user có phải là thành viên active của công ty nào không
        java.util.Optional<CompanyMember> memberOpt = companyMemberRepository.findFirstByUserIdAndStatusAndDeletedAtIsNull(userId, "active");
        if (memberOpt.isPresent()) {
            return companyRepository.findById(memberOpt.get().getCompanyId())
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy công ty của bạn"));
        }

        throw AppException.notFound("Bạn chưa có hồ sơ công ty");
    }

    // Dùng cho employer update — giữ nguyên ReqUpdateCompanyDTO
    private void applyUpdate(Company company, ReqUpdateCompanyDTO request, Long updatedBy) {
        if (request.getSlug() != null) {
            if (!request.getSlug().equals(company.getSlug())
                    && companyRepository.existsBySlug(request.getSlug())) {
                throw AppException.conflict("Slug đã được sử dụng, vui lòng chọn slug khác");
            }
            company.setSlug(request.getSlug());
        }
        if (request.getName() != null)
            company.setName(request.getName());
        if (request.getLogoUrl() != null)
            company.setLogoUrl(request.getLogoUrl());
        if (request.getCoverUrl() != null)
            company.setCoverUrl(request.getCoverUrl());
        if (request.getDescription() != null)
            company.setDescription(request.getDescription());
        if (request.getIndustryId() != null)
            company.setIndustryId(request.getIndustryId());
        if (request.getCompanySize() != null)
            company.setCompanySize(request.getCompanySize().getValue());
        if (request.getFoundedYear() != null)
            company.setFoundedYear(request.getFoundedYear());
        if (request.getWebsite() != null)
            company.setWebsite(request.getWebsite());
        if (request.getEmail() != null)
            company.setEmail(request.getEmail());
        if (request.getPhone() != null)
            company.setPhone(request.getPhone());
        if (request.getAddress() != null)
            company.setAddress(request.getAddress());
        if (request.getProvinceId() != null)
            company.setProvinceId(request.getProvinceId());
        if (request.getTaxCode() != null) {
            if (!request.getTaxCode().equals(company.getTaxCode())
                    && companyRepository.existsByTaxCode(request.getTaxCode())) {
                throw AppException.conflict("Mã số thuế đã được đăng ký");
            }
            company.setTaxCode(request.getTaxCode());
        }
        if (request.getBusinessLicenseUrl() != null)
            company.setBusinessLicenseUrl(request.getBusinessLicenseUrl());
        if (request.getCulture() != null)
            company.setCulture(request.getCulture());
        if (request.getBenefits() != null)
            company.setBenefits(request.getBenefits());
        if (request.getSocialLinks() != null)
            company.setSocialLinks(request.getSocialLinks());
        company.setUpdatedBy(updatedBy);
    }

    private ResultPaginationDTO toResultPagination(Page<Company> page, Pageable pageable) {
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber());
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotals(page.getTotalElements());

        ResultPaginationDTO result = new ResultPaginationDTO();
        result.setMeta(meta);
        result.setResult(page.getContent().stream().map(this::toResponse).toList());
        return result;
    }

    private ResCompanyDTO toResponse(Company c) {
        return ResCompanyDTO.builder()
                .id(c.getId())
                .slug(c.getSlug())
                .name(c.getName())
                .logoUrl(c.getLogoUrl())
                .coverUrl(c.getCoverUrl())
                .description(c.getDescription())
                .industryId(c.getIndustryId())
                .companySize(c.getCompanySize() != null ? CompanySize.fromValue(c.getCompanySize()) : null)
                .foundedYear(c.getFoundedYear())
                .website(c.getWebsite())
                .email(c.getEmail())
                .phone(c.getPhone())
                .address(c.getAddress())
                .provinceId(c.getProvinceId())
                .taxCode(c.getTaxCode())
                .businessLicenseUrl(c.getBusinessLicenseUrl())
                .culture(c.getCulture())
                .benefits(c.getBenefits())
                .socialLinks(c.getSocialLinks())
                .verificationStatus(c.getVerificationStatus() != null
                        ? VerificationStatus.fromValue(c.getVerificationStatus())
                        : null)
                .verifiedAt(c.getVerifiedAt())
                .verifiedBy(c.getVerifiedBy())
                .rejectionReason(c.getRejectionReason())
                .status(c.getStatus() != null ? CompanyStatus.fromValue(c.getStatus()) : null)
                .violationScore(c.getViolationScore())
                .suspendedAt(c.getSuspendedAt())
                .suspendedReason(c.getSuspendedReason())
                .createdBy(c.getCreatedBy())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getCompanyIdByUserId(Long userId) {
        return companyRepository.findByCreatedBy(userId)
                .orElseThrow(() -> AppException.notFound("Bạn chưa có hồ sơ công ty"))
                .getId();
    }
}