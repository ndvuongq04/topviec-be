package com.topviec.topviec_be.controller;

import com.topviec.topviec_be.dto.request.ReqAddToTalentPoolDTO;
import com.topviec.topviec_be.dto.request.ReqInviteFromTalentPoolDTO;
import com.topviec.topviec_be.dto.request.ReqUpdateTalentPoolNoteDTO;
import com.topviec.topviec_be.dto.response.ResApplicationDTO;
import com.topviec.topviec_be.dto.response.ResTalentPoolCandidateDetailDTO;
import com.topviec.topviec_be.dto.response.ResTalentPoolDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import com.topviec.topviec_be.service.CompanyService;
import com.topviec.topviec_be.service.TalentPoolService;
import com.topviec.topviec_be.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    @GetMapping
    public ResponseEntity<ResultPaginationDTO> getTalentPool(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        Long userId = SecurityUtil.getCurrentUserId();
        Long companyId = companyService.getCompanyIdByUserId(userId);

        return ResponseEntity.ok(talentPoolService.getTalentPool(companyId, source, search, pageable));
    }

    @GetMapping("/{talentPoolId}")
    public ResponseEntity<ResTalentPoolCandidateDetailDTO> getTalentPoolCandidateDetail(
            @PathVariable Long talentPoolId) {

        Long userId = SecurityUtil.getCurrentUserId();
        Long companyId = companyService.getCompanyIdByUserId(userId);

        return ResponseEntity.ok(talentPoolService.getTalentPoolCandidateDetail(companyId, talentPoolId));
    }

    @PatchMapping("/{talentPoolId}/note")
    public ResponseEntity<Void> updateNote(
            @PathVariable Long talentPoolId,
            @RequestBody ReqUpdateTalentPoolNoteDTO request) {

        Long userId = SecurityUtil.getCurrentUserId();
        Long companyId = companyService.getCompanyIdByUserId(userId);

        talentPoolService.updateNote(companyId, talentPoolId, request.getNote());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{talentPoolId}")
    public ResponseEntity<Void> removeFromTalentPool(@PathVariable Long talentPoolId) {
        Long userId = SecurityUtil.getCurrentUserId();
        Long companyId = companyService.getCompanyIdByUserId(userId);

        talentPoolService.removeFromTalentPool(companyId, talentPoolId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /employer/talent-pool/search-candidates?locationId=...
     * NTD tìm kiếm UV trong DB theo địa chỉ mong muốn để thêm vào talent pool.
     * locationId: bắt buộc — ID địa chỉ mong muốn của ứng viên.
     */
    @GetMapping("/search-candidates")
    public ResponseEntity<ResultPaginationDTO> searchCandidates(
            @RequestParam Integer locationId,
            @PageableDefault(size = 10) Pageable pageable) {

        Long userId = SecurityUtil.getCurrentUserId();
        Long companyId = companyService.getCompanyIdByUserId(userId);

        return ResponseEntity.ok(talentPoolService.searchCandidates(companyId, locationId, pageable));
    }

    @PostMapping
    public ResponseEntity<ResTalentPoolDTO> addToTalentPool(
            @Valid @RequestBody ReqAddToTalentPoolDTO request) {

        Long userId = SecurityUtil.getCurrentUserId();
        Long companyId = companyService.getCompanyIdByUserId(userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(talentPoolService.addToTalentPool(userId, companyId, request));
    }

    @PostMapping("/{talentPoolId}/invite")
    public ResponseEntity<ResApplicationDTO> invite(
            @PathVariable Long talentPoolId,
            @Valid @RequestBody ReqInviteFromTalentPoolDTO request) {

        Long userId = SecurityUtil.getCurrentUserId();
        Long companyId = companyService.getCompanyIdByUserId(userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(talentPoolService.invite(userId, companyId, talentPoolId, request));
    }
}
