package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.request.ReqAddToTalentPoolDTO;
import com.topviec.topviec_be.dto.response.ResTalentPoolDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import org.springframework.data.domain.Pageable;

public interface TalentPoolService {
    ResTalentPoolDTO addToTalentPool(Long userId, Long companyId, ReqAddToTalentPoolDTO request);

    ResultPaginationDTO getTalentPool(Long companyId, String source, String search, Pageable pageable);
}
