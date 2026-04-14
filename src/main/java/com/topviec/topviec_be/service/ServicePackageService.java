package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.request.ReqServicePackageDTO;
import com.topviec.topviec_be.dto.response.ResServicePackageDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import org.springframework.data.domain.Pageable;

public interface ServicePackageService {
    ResultPaginationDTO getAllServicePackages(Pageable pageable);
    ResServicePackageDTO getServicePackageById(Long id);
    ResServicePackageDTO createServicePackage(ReqServicePackageDTO reqDTO);
    ResServicePackageDTO updateServicePackage(Long id, ReqServicePackageDTO reqDTO);
}
