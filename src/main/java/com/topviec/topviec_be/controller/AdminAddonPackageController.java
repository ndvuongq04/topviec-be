package com.topviec.topviec_be.controller;

import com.topviec.topviec_be.dto.request.ReqAddonPackageDTO;
import com.topviec.topviec_be.dto.response.ResAddonPackageDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import com.topviec.topviec_be.enums.adminUsers.AdminRoleConstants;
import com.topviec.topviec_be.enums.services.AddonPackageGroup;
import com.topviec.topviec_be.service.AddonPackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/addon-packages")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAddonPackageController {

    private final AddonPackageService addonPackageService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') and @adminSecurity.hasAnyRole(authentication, '"
            + AdminRoleConstants.SUPER_ADMIN + "', '"
            + AdminRoleConstants.FINANCE_ADMIN + "', '"
            + AdminRoleConstants.SUPPORT_ADMIN + "', '"
            + AdminRoleConstants.CONTENT_MODERATOR + "')")
    public ResponseEntity<ResultPaginationDTO> getAllAddonPackages(
            @RequestParam(required = false) AddonPackageGroup groupCode,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(addonPackageService.getAllAddonPackages(groupCode, keyword, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') and @adminSecurity.hasAnyRole(authentication, '"
            + AdminRoleConstants.SUPER_ADMIN + "', '"
            + AdminRoleConstants.FINANCE_ADMIN + "')")
    public ResponseEntity<ResAddonPackageDTO> createAddonPackage(
            @Valid @RequestBody ReqAddonPackageDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addonPackageService.createAddonPackage(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and @adminSecurity.hasAnyRole(authentication, '"
            + AdminRoleConstants.SUPER_ADMIN + "', '"
            + AdminRoleConstants.FINANCE_ADMIN + "')")
    public ResponseEntity<ResAddonPackageDTO> updateAddonPackage(
            @PathVariable Long id,
            @Valid @RequestBody ReqAddonPackageDTO request) {
        return ResponseEntity.ok(addonPackageService.updateAddonPackage(id, request));
    }
}
