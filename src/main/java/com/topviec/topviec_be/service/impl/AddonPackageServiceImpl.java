package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.dto.request.ReqAddonPackageDTO;
import com.topviec.topviec_be.dto.response.ResAddonPackageDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import com.topviec.topviec_be.entity.AddonPackage;
import com.topviec.topviec_be.enums.services.AddonPackageGroup;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.AddonPackageRepository;
import com.topviec.topviec_be.service.AddonPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddonPackageServiceImpl implements AddonPackageService {

    private final AddonPackageRepository addonPackageRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ResAddonPackageDTO> getPublicActiveAddonPackages(AddonPackageGroup groupCode) {
        List<AddonPackage> packages;
        if (groupCode != null) {
            packages = addonPackageRepository.findByIsActiveTrueAndGroupCode(groupCode);
        } else {
            packages = addonPackageRepository.findByIsActiveTrue();
        }
        return packages.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getAllAddonPackages(AddonPackageGroup groupCode, Pageable pageable) {
        Page<AddonPackage> page;
        if (groupCode != null) {
            page = addonPackageRepository.findByGroupCode(groupCode, pageable);
        } else {
            page = addonPackageRepository.findAll(pageable);
        }

        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotals(page.getTotalElements());

        List<ResAddonPackageDTO> results = page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        ResultPaginationDTO response = new ResultPaginationDTO();
        response.setMeta(meta);
        response.setResult(results);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ResAddonPackageDTO getAddonPackageById(Long id) {
        AddonPackage addonPackage = addonPackageRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy gói addon với ID: " + id));
        return mapToDTO(addonPackage);
    }

    @Override
    @Transactional
    public ResAddonPackageDTO createAddonPackage(ReqAddonPackageDTO reqDTO) {
        if (addonPackageRepository.existsByCode(reqDTO.getCode())) {
            throw AppException.badRequest("Mã gói addon đã tồn tại, vui lòng chọn mã khác.");
        }

        AddonPackage addonPackage = AddonPackage.builder()
                .groupCode(reqDTO.getGroupCode())
                .name(reqDTO.getName())
                .code(reqDTO.getCode())
                .price(reqDTO.getPrice())
                .durationDays(reqDTO.getDurationDays())
                .description(reqDTO.getDescription())
                .isActive(reqDTO.getIsActive() != null ? reqDTO.getIsActive() : true)
                .build();

        return mapToDTO(addonPackageRepository.save(addonPackage));
    }

    @Override
    @Transactional
    public ResAddonPackageDTO updateAddonPackage(Long id, ReqAddonPackageDTO reqDTO) {
        AddonPackage addonPackage = addonPackageRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy gói addon với ID: " + id));

        if (addonPackageRepository.existsByCodeAndIdNot(reqDTO.getCode(), id)) {
            throw AppException.badRequest("Mã gói addon đã tồn tại, vui lòng chọn mã khác.");
        }

        addonPackage.setGroupCode(reqDTO.getGroupCode());
        addonPackage.setName(reqDTO.getName());
        addonPackage.setCode(reqDTO.getCode());
        addonPackage.setPrice(reqDTO.getPrice());
        addonPackage.setDurationDays(reqDTO.getDurationDays());
        addonPackage.setDescription(reqDTO.getDescription());
        
        if (reqDTO.getIsActive() != null) {
            addonPackage.setIsActive(reqDTO.getIsActive());
        }

        return mapToDTO(addonPackageRepository.save(addonPackage));
    }

    private ResAddonPackageDTO mapToDTO(AddonPackage entity) {
        return ResAddonPackageDTO.builder()
                .id(entity.getId())
                .groupCode(entity.getGroupCode())
                .groupName(entity.getGroupName())
                .name(entity.getName())
                .code(entity.getCode())
                .price(entity.getPrice())
                .durationDays(entity.getDurationDays())
                .description(entity.getDescription())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
