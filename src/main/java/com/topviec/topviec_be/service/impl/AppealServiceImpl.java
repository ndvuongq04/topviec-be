package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.dto.request.ReqCreateAppealDTO;
import com.topviec.topviec_be.dto.response.ResAppealDTO;
import com.topviec.topviec_be.entity.AdminUser;
import com.topviec.topviec_be.entity.Complaint;
import com.topviec.topviec_be.entity.ComplaintAppeal;
import com.topviec.topviec_be.entity.Company;
import com.topviec.topviec_be.entity.JobPosting;
import com.topviec.topviec_be.enums.complaints.AppealStatus;
import com.topviec.topviec_be.enums.complaints.ViolationGroup;
import com.topviec.topviec_be.enums.users.UserType;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.AdminUserRepository;
import com.topviec.topviec_be.repository.ComplaintAppealRepository;
import com.topviec.topviec_be.repository.ComplaintRepository;
import com.topviec.topviec_be.repository.CompanyRepository;
import com.topviec.topviec_be.repository.JobPostingRepository;
import com.topviec.topviec_be.repository.UserRepository;
import com.topviec.topviec_be.service.AppealService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppealServiceImpl implements AppealService {

    private final ComplaintAppealRepository appealRepository;
    private final ComplaintRepository complaintRepository;
    private final JobPostingRepository jobPostingRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;

    @Override
    @Transactional
    public ResAppealDTO create(Long employerUserId, ReqCreateAppealDTO request) {
        // Validate employer account exists and is EMPLOYER type
        userRepository.findById(employerUserId)
                .filter(u -> u.getUserType() == UserType.EMPLOYER)
                .orElseThrow(() -> AppException.forbidden("Chỉ nhà tuyển dụng mới được nộp kháng cáo"));

        // Validate complaint exists and is Group B + RESOLVED
        Complaint complaint = complaintRepository.findByIdAndDeletedAtIsNull(request.getComplaintId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy báo cáo"));

        if (!ViolationGroup.B.getValue().equalsIgnoreCase(complaint.getViolationGroup())) {
            throw AppException.badRequest("Chỉ có thể kháng cáo với báo cáo thuộc nhóm vi phạm B");
        }

        if (!"resolved".equalsIgnoreCase(complaint.getStatus())) {
            throw AppException.badRequest("Chỉ có thể kháng cáo khi báo cáo đã được xử lý (resolved)");
        }

        // Validate employer is the one penalized (company of job post belongs to employer)
        JobPosting jobPosting = jobPostingRepository.findById(complaint.getJobPostId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy tin tuyển dụng liên quan"));

        Company company = companyRepository.findById(jobPosting.getCompanyId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy công ty liên quan"));

        Long companyOwnerUserId = company.getUserId() != null ? company.getUserId() : company.getCreatedBy();
        if (!employerUserId.equals(companyOwnerUserId)) {
            throw AppException.forbidden("Bạn không có quyền kháng cáo báo cáo này");
        }

        // Prevent duplicate appeals (pending or approved already exist)
        boolean alreadyAppealed = appealRepository.existsByComplaintIdAndEmployerIdAndStatusIn(
                request.getComplaintId(),
                employerUserId,
                List.of(AppealStatus.PENDING.getValue(), AppealStatus.APPROVED.getValue()));
        if (alreadyAppealed) {
            throw AppException.conflict("Bạn đã nộp kháng cáo cho báo cáo này rồi");
        }

        ComplaintAppeal appeal = appealRepository.save(ComplaintAppeal.builder()
                .complaintId(request.getComplaintId())
                .employerId(employerUserId)
                .content(request.getContent().trim())
                .status(AppealStatus.PENDING.getValue())
                .build());

        return toResponse(appeal, complaint, jobPosting, company, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResAppealDTO> getByEmployer(Long employerId) {
        userRepository.findById(employerId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy người dùng"));

        List<ComplaintAppeal> appeals = appealRepository.findByEmployerIdOrderByCreatedAtDesc(employerId);
        if (appeals.isEmpty()) {
            return Collections.emptyList();
        }

        // Batch load complaints
        List<Long> complaintIds = appeals.stream().map(ComplaintAppeal::getComplaintId).distinct().toList();
        Map<Long, Complaint> complaintMap = complaintRepository.findAllById(complaintIds).stream()
                .collect(Collectors.toMap(Complaint::getId, c -> c));

        // Batch load job postings and companies
        List<Long> jobPostIds = complaintMap.values().stream()
                .map(Complaint::getJobPostId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, JobPosting> jobPostMap = jobPostIds.isEmpty()
                ? Collections.emptyMap()
                : jobPostingRepository.findAllById(jobPostIds).stream()
                        .collect(Collectors.toMap(JobPosting::getId, j -> j));

        List<Long> companyIds = jobPostMap.values().stream()
                .map(JobPosting::getCompanyId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Company> companyMap = companyIds.isEmpty()
                ? Collections.emptyMap()
                : companyRepository.findAllById(companyIds).stream()
                        .collect(Collectors.toMap(Company::getId, c -> c));

        // Batch load admin reviewers
        List<Long> adminIds = appeals.stream()
                .map(ComplaintAppeal::getReviewedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, AdminUser> adminMap = adminIds.isEmpty()
                ? Collections.emptyMap()
                : adminUserRepository.findAllById(adminIds).stream()
                        .collect(Collectors.toMap(AdminUser::getAdminUsersId, a -> a));

        return appeals.stream()
                .map(appeal -> {
                    Complaint complaint = complaintMap.get(appeal.getComplaintId());
                    JobPosting jobPosting = complaint != null ? jobPostMap.get(complaint.getJobPostId()) : null;
                    Company company = jobPosting != null ? companyMap.get(jobPosting.getCompanyId()) : null;
                    AdminUser reviewer = appeal.getReviewedBy() != null ? adminMap.get(appeal.getReviewedBy()) : null;
                    return toResponse(appeal, complaint, jobPosting, company, reviewer);
                })
                .toList();
    }

    private ResAppealDTO toResponse(
            ComplaintAppeal appeal,
            Complaint complaint,
            JobPosting jobPosting,
            Company company,
            AdminUser reviewer) {

        ResAppealDTO.ComplaintInfo complaintInfo = null;
        if (complaint != null) {
            complaintInfo = ResAppealDTO.ComplaintInfo.builder()
                    .id(complaint.getId())
                    .reportCode(String.format("RP%06d", complaint.getId()))
                    .complaintType(complaint.getComplaintType())
                    .violationGroup(complaint.getViolationGroup())
                    .status(complaint.getStatus())
                    .jobPostId(complaint.getJobPostId())
                    .jobPostTitle(jobPosting != null ? jobPosting.getTitle() : null)
                    .companyName(company != null ? company.getName() : null)
                    .createdAt(complaint.getCreatedAt())
                    .build();
        }

        ResAppealDTO.AdminInfo adminInfo = reviewer == null ? null : ResAppealDTO.AdminInfo.builder()
                .adminUserId(reviewer.getAdminUsersId())
                .fullName(reviewer.getFullName())
                .build();

        return ResAppealDTO.builder()
                .id(appeal.getId())
                .employerId(appeal.getEmployerId())
                .complaint(complaintInfo)
                .content(appeal.getContent())
                .status(appeal.getStatus())
                .adminNote(appeal.getAdminNote())
                .reviewedByAdmin(adminInfo)
                .reviewedAt(appeal.getReviewedAt())
                .createdAt(appeal.getCreatedAt())
                .updatedAt(appeal.getUpdatedAt())
                .build();
    }
}
