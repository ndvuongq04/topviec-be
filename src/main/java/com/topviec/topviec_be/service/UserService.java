package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.request.ReqRegisterCandidateDTO;
import com.topviec.topviec_be.dto.request.ReqRegisterEmployerDTO;

public interface UserService {
    void registerCandidate(ReqRegisterCandidateDTO request);

    void registerEmployer(ReqRegisterEmployerDTO request);

    void updateLastLogin(Long userId, String ip);
}
