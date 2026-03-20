package com.topviec.topviec_be.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.topviec.topviec_be.dto.request.ReqCreateJobPostingDTO;
import com.topviec.topviec_be.dto.request.ReqJobPostLocationDTO;
import com.topviec.topviec_be.dto.request.ReqJobPostSkillDTO;
import com.topviec.topviec_be.dto.request.ReqUpdateJobPostingDTO;
import com.topviec.topviec_be.dto.response.ResJobPostingDetail;
import com.topviec.topviec_be.dto.response.ResJobPostingSummary;
import com.topviec.topviec_be.dto.response.ResJobPostLocationDTO;
import com.topviec.topviec_be.dto.response.ResJobPostSkillDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import com.topviec.topviec_be.entity.JobPostEditLog;
import com.topviec.topviec_be.entity.JobPostLocation;
import com.topviec.topviec_be.entity.JobPostSkill;
import com.topviec.topviec_be.entity.JobPosting;
import com.topviec.topviec_be.enums.jobs.EditType;
import com.topviec.topviec_be.enums.jobs.JobPostStatus;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.JobPostEditLogRepository;
import com.topviec.topviec_be.repository.JobPostLocationRepository;
import com.topviec.topviec_be.repository.JobPostSkillRepository;
import com.topviec.topviec_be.repository.JobPostingRepository;
import com.topviec.topviec_be.service.JobPostingService;
import com.topviec.topviec_be.specification.JobPostingSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.topviec.topviec_be.dto.request.ReqExtendJobPostDTO;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class JobPostingServiceImpl implements JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final JobPostSkillRepository jobPostSkillRepository;
    private final JobPostLocationRepository jobPostLocationRepository;
    private final JobPostEditLogRepository jobPostEditLogRepository;
    private final ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Employer — Create
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ResJobPostingDetail create(ReqCreateJobPostingDTO request, Long createdByUserId, Long companyId) {
        String slug = generateUniqueSlug(request.getTitle());

        JobPosting jobPosting = JobPosting.builder()
                .companyId(companyId)
                .createdByUserId(createdByUserId)
                .title(request.getTitle())
                .slug(slug)
                .description(request.getDescription())
                .requirements(request.getRequirements())
                .benefits(request.getBenefits())
                .industryId(request.getIndustryId())
                .levelId(request.getLevelId())
                .experienceYearsMin(request.getExperienceYearsMin())
                .experienceYearsMax(request.getExperienceYearsMax())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .salaryNegotiable(request.getSalaryNegotiable())
                .workType(request.getWorkType())
                .headcount(request.getHeadcount())
                .deadline(request.getDeadline())
                .status(JobPostStatus.DRAFT.getValue())
                .isFeatured(Boolean.TRUE.equals(request.getIsFeatured()))
                .isUrgent(Boolean.TRUE.equals(request.getIsUrgent()))
                .build();

        JobPosting saved = jobPostingRepository.save(jobPosting);

        saveLocations(saved.getId(), request.getLocations());

        if (request.getSkills() != null) {
            saveSkills(saved.getId(), request.getSkills());
        }

        return toDetailResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Employer / Admin — Read (list)
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getList(String keyword, Long companyId, Long industryId,
            Long levelId, String workType, String status,
            Boolean isFeatured, Boolean isUrgent,
            Long salaryMin, Long salaryMax,
            Integer experienceYearsMin, Integer experienceYearsMax,
            Pageable pageable) {

        Specification<JobPosting> spec = JobPostingSpecification.withFilter(
                keyword, companyId, industryId, levelId, workType, status,
                isFeatured, isUrgent, salaryMin, salaryMax,
                experienceYearsMin, experienceYearsMax);

        return toResultPagination(jobPostingRepository.findAll(spec, pageable), pageable);
    }

    // -------------------------------------------------------------------------
    // Public — UV Read (chỉ published)
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getPublicList(String keyword, Long companyId, Long industryId,
            Long levelId, String workType,
            Boolean isFeatured, Boolean isUrgent,
            Long salaryMin, Long salaryMax,
            Integer experienceYearsMin, Integer experienceYearsMax,
            Pageable pageable) {

        Specification<JobPosting> spec = JobPostingSpecification.withPublicFilter(
                keyword, companyId, industryId, levelId, workType,
                isFeatured, isUrgent, salaryMin, salaryMax,
                experienceYearsMin, experienceYearsMax);

        return toResultPagination(jobPostingRepository.findAll(spec, pageable), pageable);
    }

    // -------------------------------------------------------------------------
    // Read — Detail
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ResJobPostingDetail getDetail(Long id) {
        JobPosting jobPosting = findByIdOrThrow(id);
        jobPostingRepository.incrementViewCount(id);
        return toDetailResponse(jobPosting);
    }

    // -------------------------------------------------------------------------
    // Employer — Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ResJobPostingDetail update(Long id, ReqUpdateJobPostingDTO request, Long updatedByUserId, Long companyId) {
        JobPosting jobPosting = findByIdOrThrow(id);

        if (!jobPosting.getCompanyId().equals(companyId)) {
            throw AppException.forbidden("Bạn không có quyền chỉnh sửa tin tuyển dụng của công ty khác");
        }

        validateEditable(jobPosting);
        saveEditLog(jobPosting, updatedByUserId);

        if (!jobPosting.getTitle().equals(request.getTitle())) {
            jobPosting.setSlug(generateUniqueSlugExclude(request.getTitle(), id));
        }

        jobPosting.setTitle(request.getTitle());
        jobPosting.setDescription(request.getDescription());
        jobPosting.setRequirements(request.getRequirements());
        jobPosting.setBenefits(request.getBenefits());
        jobPosting.setIndustryId(request.getIndustryId());
        jobPosting.setLevelId(request.getLevelId());
        jobPosting.setExperienceYearsMin(request.getExperienceYearsMin());
        jobPosting.setExperienceYearsMax(request.getExperienceYearsMax());
        jobPosting.setSalaryMin(request.getSalaryMin());
        jobPosting.setSalaryMax(request.getSalaryMax());
        jobPosting.setSalaryNegotiable(request.getSalaryNegotiable());
        jobPosting.setWorkType(request.getWorkType());
        jobPosting.setHeadcount(request.getHeadcount());
        jobPosting.setDeadline(request.getDeadline());
        jobPosting.setUpdatedBy(updatedByUserId);

        if (request.getIsFeatured() != null)
            jobPosting.setIsFeatured(request.getIsFeatured());
        if (request.getIsUrgent() != null)
            jobPosting.setIsUrgent(request.getIsUrgent());

        // Chỉ tăng editCount khi đang published
        // để kiểm soát chỉ được sửa 1 lần sau khi đăng
        if (JobPostStatus.PUBLISHED.getValue().equals(jobPosting.getStatus())) {
            jobPosting.setEditCount(jobPosting.getEditCount() + 1);

            // Nếu đang published → chuyển về draft
            jobPosting.setStatus(JobPostStatus.DRAFT.getValue());
        }

        JobPosting updated = jobPostingRepository.save(jobPosting);

        jobPostLocationRepository.deleteByJobPostId(id);
        jobPostLocationRepository.flush();
        saveLocations(id, request.getLocations());

        jobPostSkillRepository.deleteByJobPostId(id);
        jobPostSkillRepository.flush();
        if (request.getSkills() != null) {
            saveSkills(id, request.getSkills());
        }

        return toDetailResponse(updated);
    }

    // -------------------------------------------------------------------------
    // Employer — Lifecycle Methods
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ResJobPostingDetail pause(Long id, Long companyId, Long updatedByUserId) {
        JobPosting jobPosting = findByIdOrThrow(id);
        if (!jobPosting.getCompanyId().equals(companyId)) {
            throw AppException.forbidden("Bạn không có quyền thao tác trên tin tuyển dụng của công ty khác");
        }
        if (!JobPostStatus.PUBLISHED.getValue().equals(jobPosting.getStatus())) {
            throw AppException.badRequest("Chỉ có thể tạm dừng tin khi đang ở trạng thái PUBLISHED");
        }
        saveEditLog(jobPosting, updatedByUserId);
        jobPosting.setStatus(JobPostStatus.PAUSED.getValue());
        jobPosting.setUpdatedBy(updatedByUserId);
        JobPosting saved = jobPostingRepository.save(jobPosting);
        return toDetailResponse(saved);
    }

    @Override
    @Transactional
    public ResJobPostingDetail resume(Long id, Long companyId, Long updatedByUserId) {
        JobPosting jobPosting = findByIdOrThrow(id);
        if (!jobPosting.getCompanyId().equals(companyId)) {
            throw AppException.forbidden("Bạn không có quyền thao tác trên tin tuyển dụng của công ty khác");
        }
        if (!JobPostStatus.PAUSED.getValue().equals(jobPosting.getStatus())) {
            throw AppException.badRequest("Chỉ có thể mở lại tin khi đang ở trạng thái PAUSED");
        }
        saveEditLog(jobPosting, updatedByUserId);
        jobPosting.setStatus(JobPostStatus.PUBLISHED.getValue());
        jobPosting.setUpdatedBy(updatedByUserId);
        JobPosting saved = jobPostingRepository.save(jobPosting);
        return toDetailResponse(saved);
    }

    @Override
    @Transactional
    public ResJobPostingDetail close(Long id, Long companyId, Long updatedByUserId) {
        JobPosting jobPosting = findByIdOrThrow(id);
        if (!jobPosting.getCompanyId().equals(companyId)) {
            throw AppException.forbidden("Bạn không có quyền thao tác trên tin tuyển dụng của công ty khác");
        }
        String status = jobPosting.getStatus();
        if (!JobPostStatus.PUBLISHED.getValue().equals(status) && !JobPostStatus.PAUSED.getValue().equals(status)) {
            throw AppException.badRequest("Chỉ có thể đóng tin khi đang ở trạng thái PUBLISHED hoặc PAUSED");
        }
        saveEditLog(jobPosting, updatedByUserId);
        jobPosting.setStatus(JobPostStatus.CLOSED.getValue());
        jobPosting.setUpdatedBy(updatedByUserId);
        JobPosting saved = jobPostingRepository.save(jobPosting);
        return toDetailResponse(saved);
    }

    @Override
    @Transactional
    public ResJobPostingDetail extend(Long id, Long companyId, Long updatedByUserId, ReqExtendJobPostDTO request) {
        JobPosting jobPosting = findByIdOrThrow(id);
        if (!jobPosting.getCompanyId().equals(companyId)) {
            throw AppException.forbidden("Bạn không có quyền thao tác trên tin tuyển dụng của công ty khác");
        }
        if (!JobPostStatus.EXPIRED.getValue().equals(jobPosting.getStatus())) {
            throw AppException.badRequest("Chỉ có thể gia hạn tin khi đã hết hạn (EXPIRED)");
        }
        saveEditLog(jobPosting, updatedByUserId);
        jobPosting.setDeadline(request.getNewDeadline());
        jobPosting.setStatus(JobPostStatus.RENEWED.getValue());
        jobPosting.setUpdatedBy(updatedByUserId);
        JobPosting saved = jobPostingRepository.save(jobPosting);
        return toDetailResponse(saved);
    }

    @Override
    @Transactional
    public ResJobPostingDetail refresh(Long id, Long companyId, Long updatedByUserId) {
        JobPosting jobPosting = findByIdOrThrow(id);
        if (!jobPosting.getCompanyId().equals(companyId)) {
            throw AppException.forbidden("Bạn không có quyền thao tác trên tin tuyển dụng của công ty khác");
        }
        if (!JobPostStatus.PUBLISHED.getValue().equals(jobPosting.getStatus())
                && !JobPostStatus.RENEWED.getValue().equals(jobPosting.getStatus())) {
            throw AppException.badRequest("Chỉ có thể làm mới tin khi đang ở trạng thái PUBLISHED hoặc RENEWED");
        }
        jobPosting.setPublishedAt(java.time.LocalDateTime.now());
        jobPosting.setRefreshedAt(java.time.LocalDateTime.now());
        jobPosting.setUpdatedBy(updatedByUserId);
        JobPosting saved = jobPostingRepository.save(jobPosting);
        return toDetailResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private JobPosting findByIdOrThrow(Long id) {
        return jobPostingRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy tin tuyển dụng"));
    }

    private void validateEditable(JobPosting jobPosting) {
        String status = jobPosting.getStatus();

        // DRAFT và REJECTED — sửa thoải mái không giới hạn
        if (JobPostStatus.DRAFT.getValue().equals(status)
                || JobPostStatus.REJECTED.getValue().equals(status)
                || JobPostStatus.RENEWED.getValue().equals(status)) {
            return;
        }

        // PUBLISHED — chỉ được sửa 1 lần (editCount phải = 0)
        if (JobPostStatus.PUBLISHED.getValue().equals(status)) {
            if (jobPosting.getEditCount() >= 1) {
                throw AppException.badRequest("Tin đã được chỉnh sửa 1 lần sau khi đăng, không thể chỉnh sửa thêm");
            }
            return;
        }

        // Các trạng thái còn lại — không được sửa
        throw AppException.badRequest("Không thể chỉnh sửa tin ở trạng thái: " + status);
    }

    private void saveEditLog(JobPosting jobPosting, Long editedBy) {
        try {
            String snapshot = objectMapper.writeValueAsString(jobPosting);
            String editType = JobPostStatus.DRAFT.getValue().equals(jobPosting.getStatus())
                    ? EditType.DRAFT_EDIT.getValue()
                    : EditType.POST_PUBLISH_EDIT.getValue();

            jobPostEditLogRepository.save(JobPostEditLog.builder()
                    .jobPostId(jobPosting.getId())
                    .editedBy(editedBy)
                    .snapshotBefore(snapshot)
                    .editType(editType)
                    .build());
        } catch (Exception e) {
            // Không để lỗi audit chặn nghiệp vụ chính
        }
    }

    private void saveLocations(Long jobPostId, List<ReqJobPostLocationDTO> locationRequests) {
        List<JobPostLocation> locations = locationRequests.stream()
                .map(req -> JobPostLocation.builder()
                        .jobPostId(jobPostId)
                        .provinceId(req.getProvinceId())
                        .addressDetail(req.getAddressDetail())
                        .isRemote(req.getIsRemote())
                        .build())
                .toList();
        jobPostLocationRepository.saveAll(locations);
    }

    private void saveSkills(Long jobPostId, List<ReqJobPostSkillDTO> skillRequests) {
        List<JobPostSkill> skills = skillRequests.stream()
                .map(req -> JobPostSkill.builder()
                        .jobPostId(jobPostId)
                        .skillId(req.getSkillId())
                        .isRequired(req.getIsRequired())
                        .proficiencyMin(req.getProficiencyMin())
                        .build())
                .toList();
        jobPostSkillRepository.saveAll(skills);
    }

    private String generateUniqueSlug(String title) {
        String baseSlug = toSlug(title);
        String slug = baseSlug;
        int count = 1;
        while (jobPostingRepository.existsBySlugAndDeletedAtIsNull(slug)) {
            slug = baseSlug + "-" + count++;
        }
        return slug;
    }

    private String generateUniqueSlugExclude(String title, Long excludeId) {
        String baseSlug = toSlug(title);
        String slug = baseSlug;
        int count = 1;
        while (jobPostingRepository.existsBySlugAndIdNotAndDeletedAtIsNull(slug, excludeId)) {
            slug = baseSlug + "-" + count++;
        }
        return slug;
    }

    private String toSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized)
                .replaceAll("")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }

    // ── Mapper: dùng cho danh sách (gọn, không kèm locations/skills) ─────────

    private ResultPaginationDTO toResultPagination(Page<JobPosting> page, Pageable pageable) {
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber());
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotals(page.getTotalElements());

        ResultPaginationDTO result = new ResultPaginationDTO();
        result.setMeta(meta);
        result.setResult(page.getContent().stream().map(this::toSummaryResponse).toList());
        return result;
    }

    private ResJobPostingSummary toSummaryResponse(JobPosting j) {
        return ResJobPostingSummary.builder()
                .id(j.getId())
                .title(j.getTitle())
                .slug(j.getSlug())
                .companyId(j.getCompanyId())
                .industryId(j.getIndustryId())
                .levelId(j.getLevelId())
                .workType(j.getWorkType())
                .status(j.getStatus())
                .salaryMin(j.getSalaryMin())
                .salaryMax(j.getSalaryMax())
                .salaryNegotiable(j.getSalaryNegotiable())
                .isFeatured(j.getIsFeatured())
                .isUrgent(j.getIsUrgent())
                .viewCount(j.getViewCount())
                .deadline(j.getDeadline())
                .publishedAt(j.getPublishedAt())
                .createdAt(j.getCreatedAt())
                .build();
    }

    // ── Mapper: dùng cho chi tiết (kèm locations + skills) ───────────────────

    private ResJobPostingDetail toDetailResponse(JobPosting j) {
        List<ResJobPostLocationDTO> locations = jobPostLocationRepository
                .findByJobPostId(j.getId())
                .stream()
                .map(loc -> ResJobPostLocationDTO.builder()
                        .id(loc.getId())
                        .provinceId(loc.getProvinceId())
                        .addressDetail(loc.getAddressDetail())
                        .isRemote(loc.getIsRemote())
                        .build())
                .toList();

        List<ResJobPostSkillDTO> skills = jobPostSkillRepository
                .findByJobPostId(j.getId())
                .stream()
                .map(skill -> ResJobPostSkillDTO.builder()
                        .id(skill.getId())
                        .skillId(skill.getSkillId())
                        .isRequired(skill.getIsRequired())
                        .proficiencyMin(skill.getProficiencyMin())
                        .build())
                .toList();

        return ResJobPostingDetail.builder()
                .id(j.getId())
                .title(j.getTitle())
                .slug(j.getSlug())
                .description(j.getDescription())
                .requirements(j.getRequirements())
                .benefits(j.getBenefits())
                .companyId(j.getCompanyId())
                .industryId(j.getIndustryId())
                .levelId(j.getLevelId())
                .experienceYearsMin(j.getExperienceYearsMin())
                .experienceYearsMax(j.getExperienceYearsMax())
                .salaryMin(j.getSalaryMin())
                .salaryMax(j.getSalaryMax())
                .salaryNegotiable(j.getSalaryNegotiable())
                .workType(j.getWorkType())
                .headcount(j.getHeadcount())
                .deadline(j.getDeadline())
                .status(j.getStatus())
                .isFeatured(j.getIsFeatured())
                .isUrgent(j.getIsUrgent())
                .viewCount(j.getViewCount())
                .editCount(j.getEditCount())
                .publishedAt(j.getPublishedAt())
                .createdAt(j.getCreatedAt())
                .updatedAt(j.getUpdatedAt())
                .locations(locations)
                .skills(skills)
                .build();
    }
}