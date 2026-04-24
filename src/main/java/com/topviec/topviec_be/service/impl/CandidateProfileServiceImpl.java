package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.dto.request.ReqCreateCandidateProfileDTO;
import com.topviec.topviec_be.dto.request.ReqUpdateCandidateProfileDTO;
import com.topviec.topviec_be.dto.request.ReqUpdateCandidateProfileVisibilityDTO;
import com.topviec.topviec_be.dto.response.ResCandidateProfileDTO;
import com.topviec.topviec_be.entity.CandidateProfile;
import com.topviec.topviec_be.enums.candidateProfile.PreferredWorkType;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.CandidateProfileRepository;
import com.topviec.topviec_be.service.CandidateProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CandidateProfileServiceImpl implements CandidateProfileService {

    private final CandidateProfileRepository candidateProfileRepository;

    // -------------------------------------------------------------------------
    // Default profile
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void createDefaultProfile(Long userId, String email) {
        CandidateProfile profile = CandidateProfile.builder()
                .userId(userId)
                .fullName(email) // tạm dùng email, ứng viên cập nhật sau
                .createdBy(userId)
                .build();

        candidateProfileRepository.save(profile);
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ResCandidateProfileDTO createProfile(Long userId, ReqCreateCandidateProfileDTO request) {
        if (candidateProfileRepository.existsByUserId(userId)) {
            throw AppException.conflict("Hồ sơ cá nhân đã tồn tại");
        }

        validateSalaryRange(request.getExpectedSalaryMin(), request.getExpectedSalaryMax(),
                request.getSalaryNegotiable());

        CandidateProfile profile = CandidateProfile.builder()
                .userId(userId)
                .fullName(request.getFullName())
                .avatarUrl(request.getAvatarUrl())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .phoneDisplay(request.getPhoneDisplay())
                .bio(request.getBio())
                .linkedinUrl(request.getLinkedinUrl())
                .githubUrl(request.getGithubUrl())
                .personalWebsite(request.getPersonalWebsite())
                .expectedSalaryMin(request.getExpectedSalaryMin())
                .expectedSalaryMax(request.getExpectedSalaryMax())
                .salaryNegotiable(Boolean.TRUE.equals(request.getSalaryNegotiable()))
                .jobSeekingStatus(request.getJobSeekingStatus() != null
                        ? request.getJobSeekingStatus().getValue()
                        : "active")
                .preferredWorkType(request.getPreferredWorkType() != null
                        ? request.getPreferredWorkType().getValue()
                        : null)
                .preferredLocationId(request.getPreferredLocationId())
                .profileCompletionPct(0)
                .cvPublic(Boolean.TRUE.equals(request.getIsCvPublic()) || request.getIsCvPublic() == null)
                .hidePhone(Boolean.TRUE.equals(request.getHidePhone()))
                .hideEmail(Boolean.TRUE.equals(request.getHideEmail()))
                .hideDateOfBirth(Boolean.TRUE.equals(request.getHideDateOfBirth()))
                .hideExpectedSalary(Boolean.TRUE.equals(request.getHideExpectedSalary()))
                .createdBy(userId)
                .build();

        profile = candidateProfileRepository.save(profile);

        // Tính % hoàn thiện sau khi lưu
        int pct = calculateCompletionPct(profile);
        candidateProfileRepository.updateProfileCompletionPct(userId, pct);
        profile.setProfileCompletionPct(pct);

        return toResponse(profile);
    }

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ResCandidateProfileDTO getMyProfile(Long userId) {
        CandidateProfile profile = findByUserIdOrThrow(userId);
        return toResponse(profile);
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ResCandidateProfileDTO updateProfile(Long userId, ReqUpdateCandidateProfileDTO request) {
        CandidateProfile profile = findByUserIdOrThrow(userId);

        // Dùng giá trị từ request nếu có, fallback về giá trị hiện tại trong DB
        // Double salaryMin = request.getExpectedSalaryMin() != null ?
        // request.getExpectedSalaryMin()
        // : profile.getExpectedSalaryMin();
        // Double salaryMax = request.getExpectedSalaryMax() != null ?
        // request.getExpectedSalaryMax()
        // : profile.getExpectedSalaryMax();
        // Boolean salaryNegotiable = request.getSalaryNegotiable() != null ?
        // request.getSalaryNegotiable()
        // : profile.getSalaryNegotiable();
        // validateSalaryRange(salaryMin, salaryMax, salaryNegotiable);

        Double salaryMin = request.getExpectedSalaryMin() != null ? request.getExpectedSalaryMin()
                : profile.getExpectedSalaryMin();
        Double salaryMax = request.getExpectedSalaryMax() != null ? request.getExpectedSalaryMax()
                : profile.getExpectedSalaryMax();
        Boolean salaryNegotiable = request.getSalaryNegotiable() != null ? request.getSalaryNegotiable()
                : profile.getSalaryNegotiable();

        System.out.println("min=" + salaryMin + ", max=" + salaryMax + ", negotiable=" + salaryNegotiable);
        validateSalaryRange(salaryMin, salaryMax, salaryNegotiable);

        // Chỉ update field nào có giá trị trong request (null = giữ nguyên)
        if (request.getFullName() != null)
            profile.setFullName(request.getFullName());
        if (request.getAvatarUrl() != null)
            profile.setAvatarUrl(request.getAvatarUrl());
        if (request.getDateOfBirth() != null)
            profile.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null)
            profile.setGender(request.getGender());
        if (request.getPhoneDisplay() != null)
            profile.setPhoneDisplay(request.getPhoneDisplay());
        if (request.getBio() != null)
            profile.setBio(request.getBio());
        if (request.getLinkedinUrl() != null)
            profile.setLinkedinUrl(request.getLinkedinUrl());
        if (request.getGithubUrl() != null)
            profile.setGithubUrl(request.getGithubUrl());
        if (request.getPersonalWebsite() != null)
            profile.setPersonalWebsite(request.getPersonalWebsite());
        if (request.getExpectedSalaryMin() != null)
            profile.setExpectedSalaryMin(request.getExpectedSalaryMin());
        if (request.getExpectedSalaryMax() != null)
            profile.setExpectedSalaryMax(request.getExpectedSalaryMax());
        if (request.getSalaryNegotiable() != null)
            profile.setSalaryNegotiable(request.getSalaryNegotiable());
        if (request.getJobSeekingStatus() != null)
            profile.setJobSeekingStatus(request.getJobSeekingStatus().getValue());
        if (request.getPreferredWorkType() != null)
            profile.setPreferredWorkType(request.getPreferredWorkType().getValue());
        if (request.getPreferredLocationId() != null)
            profile.setPreferredLocationId(request.getPreferredLocationId());
        if (request.getCvPublic() != null)
            profile.setCvPublic(request.getCvPublic());
        if (request.getHidePhone() != null)
            profile.setHidePhone(request.getHidePhone());
        if (request.getHideEmail() != null)
            profile.setHideEmail(request.getHideEmail());
        if (request.getHideDateOfBirth() != null)
            profile.setHideDateOfBirth(request.getHideDateOfBirth());
        if (request.getHideExpectedSalary() != null)
            profile.setHideExpectedSalary(request.getHideExpectedSalary());

        profile.setUpdatedBy(userId);

        profile = candidateProfileRepository.save(profile);

        // Tính lại % hoàn thiện
        int pct = calculateCompletionPct(profile);
        candidateProfileRepository.updateProfileCompletionPct(userId, pct);
        profile.setProfileCompletionPct(pct);

        return toResponse(profile);
    }

    @Override
    @Transactional
    public ResCandidateProfileDTO updateVisibility(Long userId, ReqUpdateCandidateProfileVisibilityDTO request) {
        CandidateProfile profile = findByUserIdOrThrow(userId);

        profile.setHidePhone(request.getHidePhone());
        profile.setHideEmail(request.getHideEmail());
        profile.setHideDateOfBirth(request.getHideDateOfBirth());
        profile.setHideExpectedSalary(request.getHideExpectedSalary());
        profile.setUpdatedBy(userId);

        profile = candidateProfileRepository.save(profile);
        return toResponse(profile);
    }

    // -------------------------------------------------------------------------
    // Delete (soft)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteProfile(Long userId) {
        CandidateProfile profile = findByUserIdOrThrow(userId);
        profile.setDeletedAt(LocalDateTime.now());
        profile.setUpdatedBy(userId);
        candidateProfileRepository.save(profile);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private CandidateProfile findByUserIdOrThrow(Long userId) {
        return candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> AppException.notFound("Hồ sơ cá nhân không tồn tại"));
    }

    /**
     * Validate salary range theo rule nghiệp vụ:
     * - salaryNegotiable = true → min/max không bắt buộc, bỏ trống được
     * - salaryNegotiable = false → bắt buộc điền cả 2, và min phải <= max
     */
    private void validateSalaryRange(Double min, Double max, Boolean salaryNegotiable) {
        boolean isNegotiable = Boolean.TRUE.equals(salaryNegotiable);

        if (!isNegotiable) {
            if (min == null || max == null) {
                throw AppException.badRequest(
                        "Vui lòng điền đầy đủ mức lương tối thiểu và tối đa, hoặc chọn 'Thỏa thuận'");
            }
            if (min > max) {
                throw AppException.badRequest("Mức lương tối thiểu không được lớn hơn mức lương tối đa");
            }
        }
    }

    /**
     * Tính tỷ lệ hoàn thiện hồ sơ (0–100).
     * Mỗi field được coi là 1 điểm, tổng cộng 10 điểm → quy đổi ra %.
     */
    private int calculateCompletionPct(CandidateProfile p) {
        int score = 0;
        if (p.getFullName() != null && !p.getFullName().isBlank())
            score++;
        if (p.getAvatarUrl() != null && !p.getAvatarUrl().isBlank())
            score++;
        if (p.getDateOfBirth() != null)
            score++;
        if (p.getGender() != null && !p.getGender().isBlank())
            score++;
        if (p.getPhoneDisplay() != null && !p.getPhoneDisplay().isBlank())
            score++;
        if (p.getBio() != null && !p.getBio().isBlank())
            score++;
        if (p.getLinkedinUrl() != null && !p.getLinkedinUrl().isBlank())
            score++;
        if (p.getExpectedSalaryMin() != null || Boolean.TRUE.equals(p.getSalaryNegotiable()))
            score++;
        if (p.getPreferredWorkType() != null && !p.getPreferredWorkType().isBlank())
            score++;
        if (p.getPreferredLocationId() != null)
            score++;

        return (score * 100) / 10;
    }

    /**
     * Map entity → response DTO (thủ công, không dùng MapStruct).
     */
    private ResCandidateProfileDTO toResponse(CandidateProfile p) {

        return ResCandidateProfileDTO.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .fullName(p.getFullName())
                .avatarUrl(p.getAvatarUrl())
                .dateOfBirth(p.getDateOfBirth())
                .gender(p.getGender())
                .phoneDisplay(p.getPhoneDisplay())
                .bio(p.getBio())
                .linkedinUrl(p.getLinkedinUrl())
                .githubUrl(p.getGithubUrl())
                .personalWebsite(p.getPersonalWebsite())
                .expectedSalaryMin(p.getExpectedSalaryMin())
                .expectedSalaryMax(p.getExpectedSalaryMax())
                .salaryNegotiable(p.getSalaryNegotiable())
                .jobSeekingStatus(p.getJobSeekingStatus() != null
                        ? com.topviec.topviec_be.enums.candidateProfile.JobSeekingStatus
                                .fromValue(p.getJobSeekingStatus())
                        : null)
                .preferredWorkType(
                        p.getPreferredWorkType() != null ? PreferredWorkType.fromValue(p.getPreferredWorkType()) : null)
                .preferredLocationId(p.getPreferredLocationId())
                .profileCompletionPct(p.getProfileCompletionPct())
                .isCvPublic(p.getCvPublic())
                .hidePhone(p.getHidePhone())
                .hideEmail(p.getHideEmail())
                .hideDateOfBirth(p.getHideDateOfBirth())
                .hideExpectedSalary(p.getHideExpectedSalary())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
