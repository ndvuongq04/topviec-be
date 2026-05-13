package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.request.ReqCreateCvTemplateDTO;
import com.topviec.topviec_be.dto.request.ReqUpdateCvTemplateContentDTO;
import com.topviec.topviec_be.dto.request.ReqUpdateCvTemplateDTO;
import com.topviec.topviec_be.dto.response.ResCvTemplateDTO;
import com.topviec.topviec_be.dto.response.ResCvTemplateDetailDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CvTemplateService {

    ResultPaginationDTO getAdminTemplates(String keyword, Pageable pageable);

    ResCvTemplateDetailDTO getAdminTemplateDetail(Long id);

    ResCvTemplateDetailDTO createTemplate(Long adminUserId, ReqCreateCvTemplateDTO request);

    ResCvTemplateDetailDTO updateTemplateMetadata(Long adminUserId, Long id, ReqUpdateCvTemplateDTO request);

    ResCvTemplateDetailDTO updateTemplateContent(Long adminUserId, Long id, ReqUpdateCvTemplateContentDTO request);

    ResCvTemplateDetailDTO activateTemplate(Long adminUserId, Long id);

    ResCvTemplateDetailDTO deactivateTemplate(Long adminUserId, Long id);

    ResCvTemplateDetailDTO setDefaultTemplate(Long adminUserId, Long id);

    List<ResCvTemplateDTO> getActiveTemplates();

    ResCvTemplateDetailDTO getActiveTemplateDetail(Long id);
}
