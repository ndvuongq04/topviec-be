package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.request.ReqRegisterCandidateDTO;
import com.topviec.topviec_be.dto.request.ReqRegisterEmployerDTO;

public interface AuthService {
    void registerCandidate(ReqRegisterCandidateDTO request);

    void registerEmployer(ReqRegisterEmployerDTO request);

    void updateLastLogin(Long userId, String ip);

    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);

    void verifyEmail(String token);
}
