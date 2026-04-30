package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.response.ResAdminCandidateDetailDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import org.springframework.data.domain.Pageable;
import java.util.List;
import com.topviec.topviec_be.dto.response.ResCvDTO;
import com.topviec.topviec_be.dto.response.ResCompanyFollowDTO;

public interface AdminCandidateService {

    ResultPaginationDTO getCandidates(String status, String keyword, Pageable pageable);

    ResAdminCandidateDetailDTO getCandidateDetail(Long userId);

    ResultPaginationDTO getCandidateCvs(Long userId, Pageable pageable);

    ResultPaginationDTO getCandidateApplications(Long userId, Pageable pageable);

    ResultPaginationDTO getCandidateFollowedCompanies(Long userId, Pageable pageable);

    ResultPaginationDTO getCandidateSavedJobs(Long userId, Pageable pageable);
}
