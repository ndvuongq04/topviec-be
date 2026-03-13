package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.dto.request.ReqCreateCompanyDTO;
import com.topviec.topviec_be.dto.request.ReqSuspendCompanyDTO;
import com.topviec.topviec_be.dto.request.ReqUpdateCompanyDTO;
import com.topviec.topviec_be.dto.request.ReqVerifyCompanyDTO;
import com.topviec.topviec_be.dto.response.ResCompanyDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import com.topviec.topviec_be.entity.Company;
import com.topviec.topviec_be.enums.company.CompanySize;
import com.topviec.topviec_be.enums.company.CompanyStatus;
import com.topviec.topviec_be.enums.company.VerificationStatus;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.CompanyRepository;
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

    // -------------------------------------------------------------------------
    // Employer — Create
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ResCompanyDTO createCompany(Long userId, ReqCreateCompanyDTO request) {
        // Mỗi employer chỉ được tạo 1 công ty
        if (companyRepository.existsByCreatedBy(userId)) {
            throw AppException.conflict("Bạn đã có hồ sơ công ty rồi");
        }

        if (companyRepository.existsBySlug(request.getSlug())) {
            throw AppException.conflict("Slug đã được sử dụng, vui lòng chọn slug khác");
        }

        if (request.getTaxCode() != null && companyRepository.existsByTaxCode(request.getTaxCode())) {
            throw AppException.conflict("Mã số thuế đã được đăng ký");
        }

        Company company = Company.builder()
                .slug(request.getSlug())
                .name(request.getName())
                .logoUrl(request.getLogoUrl())
                .coverUrl(request.getCoverUrl())
                .description(request.getDescription())
                .industryId(request.getIndustryId())
                .companySize(request.getCompanySize() != null ? request.getCompanySize().getValue() : null)
                .foundedYear(request.getFoundedYear())
                .website(request.getWebsite())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .provinceId(request.getProvinceId())
                .taxCode(request.getTaxCode())
                .businessLicenseUrl(request.getBusinessLicenseUrl())
                .culture(request.getCulture())
                .benefits(request.getBenefits())
                .socialLinks(request.getSocialLinks())
                .createdBy(userId)
                .build();

        return toResponse(companyRepository.save(company));
    }

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

        // Công ty đang bị suspend hoặc deleted thì không cho sửa
        if (CompanyStatus.SUSPENDED.getValue().equals(company.getStatus())) {
            throw AppException.badRequest("Công ty đang bị tạm khóa, không thể cập nhật hồ sơ");
        }
        if (CompanyStatus.DELETED.getValue().equals(company.getStatus())) {
            throw AppException.badRequest("Công ty đã bị xóa");
        }

        applyUpdate(company, request, userId);

        // Nếu đang ở trạng thái rejected → chuyển về pending để admin duyệt lại
        if (VerificationStatus.REJECTED.getValue().equals(company.getVerificationStatus())) {
            company.setVerificationStatus(VerificationStatus.PENDING.getValue());
            company.setRejectionReason(null);
        }

        return toResponse(companyRepository.save(company));
    }

    // -------------------------------------------------------------------------
    // Public
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ResCompanyDTO getBySlug(String slug) {
        Company company = companyRepository.findBySlug(slug)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy công ty"));

        // Chỉ trả về công ty đang active
        if (!CompanyStatus.ACTIVE.getValue().equals(company.getStatus())) {
            throw AppException.notFound("Không tìm thấy công ty");
        }

        return toResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
    public ResCompanyDTO getById(Long id) {
        Company company = findByIdOrThrow(id);

        // Chỉ trả về công ty đang active
        if (!CompanyStatus.ACTIVE.getValue().equals(company.getStatus())) {
            throw AppException.notFound("Không tìm thấy công ty");
        }

        return toResponse(company);
    }

    @Override
    @Transactional(readOnly = true)
    public ResCompanyDTO adminGetById(Long id) {
        // Admin xem được mọi trạng thái: pending, active, suspended, deleted
        return toResponse(findByIdOrThrow(id));
    }

    // -------------------------------------------------------------------------
    // Admin — Verification
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getPendingVerification(Pageable pageable) {
        Page<Company> page = companyRepository
                .findAllByVerificationStatus(VerificationStatus.PENDING.getValue(), pageable);
        return toResultPagination(page, pageable);
    }

    @Override
    @Transactional
    public ResCompanyDTO verifyCompany(Long companyId, Long adminId, ReqVerifyCompanyDTO request) {
        Company company = findByIdOrThrow(companyId);

        // Chỉ duyệt được khi đang ở trạng thái pending
        if (!VerificationStatus.PENDING.getValue().equals(company.getVerificationStatus())) {
            throw AppException.badRequest("Chỉ có thể duyệt hồ sơ đang ở trạng thái chờ duyệt");
        }

        if (Boolean.TRUE.equals(request.getApproved())) {
            // Duyệt → verified + active
            company.setVerificationStatus(VerificationStatus.VERIFIED.getValue());
            company.setStatus(CompanyStatus.ACTIVE.getValue());
            company.setVerifiedAt(LocalDateTime.now());
            company.setVerifiedBy(adminId);
            company.setRejectionReason(null);
        } else {
            // Từ chối → rejected, bắt buộc phải có lý do
            if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                throw AppException.badRequest("Vui lòng nhập lý do từ chối");
            }
            company.setVerificationStatus(VerificationStatus.REJECTED.getValue());
            company.setRejectionReason(request.getRejectionReason());
            company.setVerifiedBy(adminId);
        }

        company.setUpdatedBy(adminId);
        return toResponse(companyRepository.save(company));
    }

    // -------------------------------------------------------------------------
    // Admin — Suspend / Unsuspend
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ResCompanyDTO suspendCompany(Long companyId, Long adminId, ReqSuspendCompanyDTO request) {
        Company company = findByIdOrThrow(companyId);

        if (CompanyStatus.SUSPENDED.getValue().equals(company.getStatus())) {
            throw AppException.badRequest("Công ty đã bị tạm khóa rồi");
        }
        if (CompanyStatus.DELETED.getValue().equals(company.getStatus())) {
            throw AppException.badRequest("Công ty đã bị xóa, không thể suspend");
        }

        company.setStatus(CompanyStatus.SUSPENDED.getValue());
        company.setSuspendedAt(LocalDateTime.now());
        company.setSuspendedReason(request.getSuspendedReason());
        company.setUpdatedBy(adminId);

        return toResponse(companyRepository.save(company));
    }

    @Override
    @Transactional
    public ResCompanyDTO unsuspendCompany(Long companyId, Long adminId) {
        Company company = findByIdOrThrow(companyId);

        if (!CompanyStatus.SUSPENDED.getValue().equals(company.getStatus())) {
            throw AppException.badRequest("Công ty không đang bị tạm khóa");
        }

        company.setStatus(CompanyStatus.ACTIVE.getValue());
        company.setSuspendedAt(null);
        company.setSuspendedReason(null);
        company.setUpdatedBy(adminId);

        return toResponse(companyRepository.save(company));
    }

    // -------------------------------------------------------------------------
    // Admin — Update / Delete
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ResCompanyDTO adminUpdateCompany(Long companyId, Long adminId, ReqUpdateCompanyDTO request) {
        Company company = findByIdOrThrow(companyId);
        applyUpdate(company, request, adminId);
        return toResponse(companyRepository.save(company));
    }

    @Override
    @Transactional
    public void deleteCompany(Long companyId, Long adminId) {
        Company company = findByIdOrThrow(companyId);
        company.setDeletedAt(LocalDateTime.now());
        company.setStatus(CompanyStatus.DELETED.getValue());
        company.setUpdatedBy(adminId);
        companyRepository.save(company);
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getAllCompanies(String status, Pageable pageable) {
        Page<Company> page;
        if (status != null && !status.isBlank()) {
            page = companyRepository.findAllByStatus(status, pageable);
        } else {
            page = companyRepository.findAll(pageable);
        }
        return toResultPagination(page, pageable);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Company findByIdOrThrow(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy công ty"));
    }

    private Company findByCreatedByOrThrow(Long userId) {
        return companyRepository.findByCreatedBy(userId)
                .orElseThrow(() -> AppException.notFound("Bạn chưa có hồ sơ công ty"));
    }

    /**
     * Áp dụng partial update — field nào null trong request thì giữ nguyên giá trị
     * DB.
     * Dùng chung cho cả employer update và admin update.
     */
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

    /**
     * Chuyển Page<Company> sang ResultPaginationDTO theo chuẩn project.
     */
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

    /**
     * Map entity → response DTO.
     */
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
}