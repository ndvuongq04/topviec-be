package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.request.ReqAddonPackageDTO;
import com.topviec.topviec_be.dto.response.ResAddonPackageDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import com.topviec.topviec_be.enums.services.AddonPackageGroup;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AddonPackageService {
    List<ResAddonPackageDTO> getPublicActiveAddonPackages(AddonPackageGroup groupCode);
    ResultPaginationDTO getAllAddonPackages(AddonPackageGroup groupCode, String keyword, Pageable pageable);
    ResAddonPackageDTO getAddonPackageById(Long id);
    ResAddonPackageDTO createAddonPackage(ReqAddonPackageDTO reqDTO);
    ResAddonPackageDTO updateAddonPackage(Long id, ReqAddonPackageDTO reqDTO);
}
