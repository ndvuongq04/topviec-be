package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.dto.request.ReqServicePackageDTO;
import com.topviec.topviec_be.dto.response.ResServicePackageDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import com.topviec.topviec_be.entity.ServicePackage;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.ServicePackageRepository;
import com.topviec.topviec_be.service.ServicePackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServicePackageServiceImpl implements ServicePackageService {

    private final ServicePackageRepository servicePackageRepository;

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getAllServicePackages(Pageable pageable) {
        Page<ServicePackage> page = servicePackageRepository.findAll(pageable);

        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotals(page.getTotalElements());

        List<ResServicePackageDTO> results = page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        ResultPaginationDTO response = new ResultPaginationDTO();
        response.setMeta(meta);
        response.setResult(results);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ResServicePackageDTO getServicePackageById(Long id) {
        ServicePackage servicePackage = servicePackageRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy gói dịch vụ với ID: " + id));
        return mapToDTO(servicePackage);
    }

    @Override
    @Transactional
    public ResServicePackageDTO createServicePackage(ReqServicePackageDTO reqDTO) {
        if (servicePackageRepository.existsByCode(reqDTO.getCode())) {
            throw AppException.badRequest("Mã gói dịch vụ đã tồn tại, vui lòng chọn mã khác.");
        }

        ServicePackage servicePackage = ServicePackage.builder()
                .name(reqDTO.getName())
                .code(reqDTO.getCode())
                .billingCycle(reqDTO.getBillingCycle())
                .price(reqDTO.getPrice())
                .features(reqDTO.getFeatures())
                .description(reqDTO.getDescription())
                .isActive(reqDTO.getIsActive() != null ? reqDTO.getIsActive() : true)
                .sortOrder(reqDTO.getSortOrder())
                .build();

        return mapToDTO(servicePackageRepository.save(servicePackage));
    }

    @Override
    @Transactional
    public ResServicePackageDTO updateServicePackage(Long id, ReqServicePackageDTO reqDTO) {
        ServicePackage servicePackage = servicePackageRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy gói dịch vụ với ID: " + id));

        if (servicePackageRepository.existsByCodeAndIdNot(reqDTO.getCode(), id)) {
            throw AppException.badRequest("Mã gói dịch vụ đã tồn tại, vui lòng chọn mã khác.");
        }

        servicePackage.setName(reqDTO.getName());
        servicePackage.setCode(reqDTO.getCode());
        servicePackage.setBillingCycle(reqDTO.getBillingCycle());
        servicePackage.setPrice(reqDTO.getPrice());
        servicePackage.setFeatures(reqDTO.getFeatures());
        servicePackage.setDescription(reqDTO.getDescription());
        
        if (reqDTO.getIsActive() != null) {
            servicePackage.setIsActive(reqDTO.getIsActive());
        }
        if (reqDTO.getSortOrder() != null) {
            servicePackage.setSortOrder(reqDTO.getSortOrder());
        }

        return mapToDTO(servicePackageRepository.save(servicePackage));
    }

    private ResServicePackageDTO mapToDTO(ServicePackage entity) {
        return ResServicePackageDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .billingCycle(entity.getBillingCycle())
                .price(entity.getPrice())
                .features(entity.getFeatures())
                .description(entity.getDescription())
                .isActive(entity.getIsActive())
                .sortOrder(entity.getSortOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
