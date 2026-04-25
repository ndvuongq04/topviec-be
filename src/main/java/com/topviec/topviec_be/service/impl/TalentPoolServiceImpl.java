package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.dto.request.ReqAddToTalentPoolDTO;
import com.topviec.topviec_be.dto.response.ResTalentPoolCandidateDTO;
import com.topviec.topviec_be.dto.response.ResTalentPoolDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import com.topviec.topviec_be.entity.CandidateProfile;
import com.topviec.topviec_be.entity.Location;
import com.topviec.topviec_be.entity.TalentPool;
import com.topviec.topviec_be.entity.User;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.CandidateProfileRepository;
import com.topviec.topviec_be.repository.LocationRepository;
import com.topviec.topviec_be.repository.TalentPoolRepository;
import com.topviec.topviec_be.repository.UserRepository;
import com.topviec.topviec_be.service.TalentPoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TalentPoolServiceImpl implements TalentPoolService {

    private final TalentPoolRepository talentPoolRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;

    // -------------------------------------------------------------------------
    // POST — thêm UV vào talent pool
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ResTalentPoolDTO addToTalentPool(Long userId, Long companyId, ReqAddToTalentPoolDTO request) {
        Long candidateUserId = request.getCandidateUserId();

        CandidateProfile candidateProfile = candidateProfileRepository.findByUserId(candidateUserId)
                .orElseThrow(() -> AppException.notFound("Ứng viên không tồn tại"));

        if (talentPoolRepository.existsByCompanyIdAndCandidateUserId(companyId, candidateUserId)) {
            throw AppException.badRequest("Ứng viên đã có trong talent pool của công ty");
        }

        TalentPool entry = TalentPool.builder()
                .companyId(companyId)
                .candidateUserId(candidateUserId)
                .addedBy(userId)
                .source(request.getSource().getValue())
                .note(request.getNote())
                .build();

        TalentPool saved = talentPoolRepository.save(entry);

        return ResTalentPoolDTO.builder()
                .id(saved.getId())
                .companyId(saved.getCompanyId())
                .candidateUserId(saved.getCandidateUserId())
                .candidateName(candidateProfile.getFullName())
                .candidateAvatarUrl(candidateProfile.getAvatarUrl())
                .addedBy(saved.getAddedBy())
                .source(saved.getSource())
                .note(saved.getNote())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    // -------------------------------------------------------------------------
    // GET — danh sách UV trong talent pool
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getTalentPool(Long companyId, String source, String search, Pageable pageable) {
        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<TalentPool> page = talentPoolRepository.findByCompanyWithFilter(
                companyId, source, searchParam, pageable);

        List<TalentPool> entries = page.getContent();

        // Batch load — tránh N+1
        List<Long> candidateUserIds = entries.stream()
                .map(TalentPool::getCandidateUserId)
                .distinct()
                .toList();

        Map<Long, CandidateProfile> profileMap = candidateUserIds.isEmpty() ? Map.of()
                : candidateProfileRepository.findByUserIdIn(candidateUserIds).stream()
                        .collect(Collectors.toMap(CandidateProfile::getUserId, cp -> cp));

        Map<Long, User> userMap = candidateUserIds.isEmpty() ? Map.of()
                : userRepository.findAllById(candidateUserIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        Set<Long> locationIds = profileMap.values().stream()
                .filter(cp -> cp.getPreferredLocationId() != null)
                .map(cp -> cp.getPreferredLocationId().longValue())
                .collect(Collectors.toSet());

        Map<Integer, String> locationNameMap = locationIds.isEmpty() ? Map.of()
                : locationRepository.findAllById(locationIds).stream()
                        .collect(Collectors.toMap(l -> l.getId().intValue(), Location::getName));

        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber());
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotals(page.getTotalElements());

        ResultPaginationDTO result = new ResultPaginationDTO();
        result.setMeta(meta);
        result.setResult(entries.stream()
                .map(tp -> toListResponse(tp, profileMap, userMap, locationNameMap))
                .toList());

        return result;
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private ResTalentPoolCandidateDTO toListResponse(
            TalentPool tp,
            Map<Long, CandidateProfile> profileMap,
            Map<Long, User> userMap,
            Map<Integer, String> locationNameMap) {

        CandidateProfile profile = profileMap.get(tp.getCandidateUserId());
        User user = userMap.get(tp.getCandidateUserId());

        return ResTalentPoolCandidateDTO.builder()
                .talentPoolId(tp.getId())
                .source(tp.getSource())
                .note(tp.getNote())
                .addedAt(tp.getCreatedAt())
                .candidateUserId(tp.getCandidateUserId())
                .candidateName(profile != null ? profile.getFullName() : null)
                .candidateEmail(user != null ? user.getEmail() : null)
                .candidateAvatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .preferredJobTitle(profile != null ? profile.getPreferredJobTitle() : null)
                .preferredWorkType(profile != null ? profile.getPreferredWorkType() : null)
                .preferredLocationId(profile != null ? profile.getPreferredLocationId() : null)
                .preferredLocationName(profile != null && profile.getPreferredLocationId() != null
                        ? locationNameMap.get(profile.getPreferredLocationId())
                        : null)
                .expectedSalaryMin(profile != null ? profile.getExpectedSalaryMin() : null)
                .expectedSalaryMax(profile != null ? profile.getExpectedSalaryMax() : null)
                .salaryNegotiable(profile != null ? profile.getSalaryNegotiable() : null)
                .jobSeekingStatus(profile != null ? profile.getJobSeekingStatus() : null)
                .build();
    }
}
