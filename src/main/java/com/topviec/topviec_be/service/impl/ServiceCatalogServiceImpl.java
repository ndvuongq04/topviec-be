package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.dto.request.ReqServiceDTO;
import com.topviec.topviec_be.dto.response.ResServiceDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import com.topviec.topviec_be.entity.Services;
import com.topviec.topviec_be.enums.services.ServiceCategory;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.ServiceRepository;
import com.topviec.topviec_be.service.ServiceCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceCatalogServiceImpl implements ServiceCatalogService {

    private final ServiceRepository serviceRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ResServiceDTO> getActiveServices(ServiceCategory category) {
        List<Services> services = category != null
                ? serviceRepository.findByIsActiveTrueAndCategory(category)
                : serviceRepository.findByIsActiveTrue();
        return services.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getAllServices(ServiceCategory category, String keyword, Pageable pageable) {
        Page<Services> page = serviceRepository.searchAll(category, keyword, pageable);

        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(page.getTotalPages());
        meta.setTotals(page.getTotalElements());

        List<ResServiceDTO> results = page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        ResultPaginationDTO response = new ResultPaginationDTO();
        response.setMeta(meta);
        response.setResult(results);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ResServiceDTO getServiceById(Long id) {
        Services service = serviceRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy dịch vụ với ID: " + id));
        return mapToDTO(service);
    }

    @Override
    @Transactional
    public ResServiceDTO createService(ReqServiceDTO reqDTO) {
        if (serviceRepository.existsByCode(reqDTO.getCode())) {
            throw AppException.badRequest("Mã dịch vụ đã tồn tại, vui lòng chọn mã khác.");
        }

        Services service = Services.builder()
                .code(reqDTO.getCode())
                .name(reqDTO.getName())
                .category(reqDTO.getCategory())
                .unit(reqDTO.getUnit())
                .description(reqDTO.getDescription())
                .isActive(reqDTO.getIsActive() != null ? reqDTO.getIsActive() : true)
                .build();

        return mapToDTO(serviceRepository.save(service));
    }

    @Override
    @Transactional
    public ResServiceDTO updateService(Long id, ReqServiceDTO reqDTO) {
        Services service = serviceRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy dịch vụ với ID: " + id));

        if (serviceRepository.existsByCodeAndIdNot(reqDTO.getCode(), id)) {
            throw AppException.badRequest("Mã dịch vụ đã tồn tại, vui lòng chọn mã khác.");
        }

        service.setCode(reqDTO.getCode());
        service.setName(reqDTO.getName());
        service.setCategory(reqDTO.getCategory());
        service.setUnit(reqDTO.getUnit());
        service.setDescription(reqDTO.getDescription());
        if (reqDTO.getIsActive() != null) {
            service.setIsActive(reqDTO.getIsActive());
        }

        return mapToDTO(serviceRepository.save(service));
    }

    private ResServiceDTO mapToDTO(Services entity) {
        return ResServiceDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .category(entity.getCategory())
                .categoryName(entity.getCategory() != null ? entity.getCategory().getValue() : null)
                .unit(entity.getUnit())
                .description(entity.getDescription())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
