package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.dto.request.ReqAddToTalentPoolDTO;
import com.topviec.topviec_be.dto.response.ResTalentPoolDTO;
import com.topviec.topviec_be.entity.CandidateProfile;
import com.topviec.topviec_be.entity.TalentPool;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.CandidateProfileRepository;
import com.topviec.topviec_be.repository.TalentPoolRepository;
import com.topviec.topviec_be.service.TalentPoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TalentPoolServiceImpl implements TalentPoolService {

    private final TalentPoolRepository talentPoolRepository;
    private final CandidateProfileRepository candidateProfileRepository;

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
}
