package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.cvonline.CvOnlineExtraDataDTO;
import com.topviec.topviec_be.dto.request.ReqChangeOnlineCvTemplateDTO;
import com.topviec.topviec_be.dto.request.ReqCreateOnlineCvDTO;
import com.topviec.topviec_be.dto.request.ReqShareCvDTO;
import com.topviec.topviec_be.dto.request.ReqCreateShareTokenDTO;
import com.topviec.topviec_be.dto.request.ReqUpdateOnlineCvDTO;
import com.topviec.topviec_be.dto.request.ReqUploadCvDTO;
import com.topviec.topviec_be.dto.response.ResCvDTO;
import com.topviec.topviec_be.dto.response.ResCvOnlineEditorPayloadDTO;
import com.topviec.topviec_be.dto.response.ResCvOnlineDetailDTO;
import com.topviec.topviec_be.dto.response.ResShareTokenDTO;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface CvService {

    ResCvDTO uploadCv(Long userId, MultipartFile file, ReqUploadCvDTO request);

    List<ResCvDTO> getMyCvs(Long userId);

    ResCvDTO renameCv(Long userId, Long id, String newTitle);

    ResCvDTO setDefaultCv(Long userId, Long id);

    ResCvDTO duplicateCv(Long userId, Long id);

    void deleteCv(Long userId, Long id);

    ResCvDTO shareCv(Long userId, Long id, ReqShareCvDTO request);

    ResShareTokenDTO createShareToken(Long userId, Long id, ReqCreateShareTokenDTO request);

    ResCvDTO getPublicCv(String shareToken);

    ResCvDTO getCvById(Long userId, Long id);

    CvOnlineExtraDataDTO getOnlineCvPrefill(Long userId);

    ResCvOnlineEditorPayloadDTO getOnlineCvEditorPayloadByTemplate(Long userId, Long templateId);

    ResCvOnlineEditorPayloadDTO getOnlineCvEditorPayloadById(Long userId, Long id);

    ResCvOnlineDetailDTO createOnlineCv(Long userId, ReqCreateOnlineCvDTO request);

    ResCvOnlineDetailDTO getOnlineCvById(Long userId, Long id);

    ResCvOnlineDetailDTO updateOnlineCv(Long userId, Long id, ReqUpdateOnlineCvDTO request);

    ResCvOnlineDetailDTO changeOnlineCvTemplate(Long userId, Long id, ReqChangeOnlineCvTemplateDTO request);
}
