package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.request.ReqAddToTalentPoolDTO;
import com.topviec.topviec_be.dto.response.ResTalentPoolDTO;

public interface TalentPoolService {
    ResTalentPoolDTO addToTalentPool(Long userId, Long companyId, ReqAddToTalentPoolDTO request);
}
