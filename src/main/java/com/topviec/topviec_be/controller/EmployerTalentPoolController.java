package com.topviec.topviec_be.controller;

import com.topviec.topviec_be.dto.request.ReqAddToTalentPoolDTO;
import com.topviec.topviec_be.dto.response.ResTalentPoolDTO;
import com.topviec.topviec_be.service.CompanyService;
import com.topviec.topviec_be.service.TalentPoolService;
import com.topviec.topviec_be.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Base URL: /api/v1/employer/talent-pool
 */
@RestController
@RequestMapping("/employer/talent-pool")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EMPLOYER')")
public class EmployerTalentPoolController {

    private final TalentPoolService talentPoolService;
    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<ResTalentPoolDTO> addToTalentPool(
            @Valid @RequestBody ReqAddToTalentPoolDTO request) {

        Long userId = SecurityUtil.getCurrentUserId();
        Long companyId = companyService.getCompanyIdByUserId(userId);

        ResTalentPoolDTO response = talentPoolService.addToTalentPool(userId, companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
