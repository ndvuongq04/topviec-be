package com.topviec.topviec_be.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.topviec.topviec_be.dto.cvonline.CvOnlineExtraDataDTO;
import com.topviec.topviec_be.dto.request.ReqChangeOnlineCvTemplateDTO;
import com.topviec.topviec_be.dto.request.ReqCreateOnlineCvDTO;
import com.topviec.topviec_be.dto.request.ReqCreateShareTokenDTO;
import com.topviec.topviec_be.dto.request.ReqShareCvDTO;
import com.topviec.topviec_be.dto.request.ReqUpdateOnlineCvDTO;
import com.topviec.topviec_be.dto.request.ReqUploadCvDTO;
import com.topviec.topviec_be.dto.response.ResCvDTO;
import com.topviec.topviec_be.dto.response.ResCvOnlineDetailDTO;
import com.topviec.topviec_be.dto.response.ResCvTemplateDetailDTO;
import com.topviec.topviec_be.dto.response.ResShareTokenDTO;
import com.topviec.topviec_be.entity.CvTemplate;
import com.topviec.topviec_be.entity.Cvs;
import com.topviec.topviec_be.enums.cvs.CvParseStatus;
import com.topviec.topviec_be.enums.cvs.CvType;
import com.topviec.topviec_be.enums.cvs.CvVisibility;
import com.topviec.topviec_be.enums.cvs.FileUploadType;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.CvTemplateRepository;
import com.topviec.topviec_be.repository.CvsRepository;
import com.topviec.topviec_be.service.CvService;
import com.topviec.topviec_be.service.FileStorageService;
import com.topviec.topviec_be.util.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CvServiceImpl implements CvService {

    private final CvsRepository cvsRepository;
    private final CvTemplateRepository cvTemplateRepository;
    private final FileStorageService fileStorageService;
    private final FileValidator fileValidator;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final int MAX_CV_PER_USER = 5;

    @Override
    @Transactional
    public ResCvDTO uploadCv(Long userId, MultipartFile file, ReqUploadCvDTO request) {

        fileValidator.validate(file, FileUploadType.CV);

        long currentCount = cvsRepository.countByUserId(userId);
        if (currentCount >= MAX_CV_PER_USER) {
            throw AppException.badRequest(
                    "Đã đạt giới hạn " + MAX_CV_PER_USER + " CV, vui lòng xóa CV cũ trước");
        }

        if (cvsRepository.existsByUserIdAndTitle(userId, request.getTitle())) {
            throw AppException.conflict(
                    "Tên CV '" + request.getTitle() + "' đã tồn tại");
        }

        String fileUrl = fileStorageService.uploadFile(file, userId, FileUploadType.CV);

        boolean shouldBeDefault = request.isDefault() || currentCount == 0;
        if (shouldBeDefault) {
            clearCurrentDefaultCv(userId);
        }

        Cvs cv = Cvs.builder()
                .userId(userId)
                .title(request.getTitle())
                .cvType(CvType.uploaded)
                .fileUrl(fileUrl)
                .isDefault(shouldBeDefault)
                .visibility(CvVisibility.private_cv)
                .parseStatus(CvParseStatus.skipped)
                .viewCount(0)
                .createdBy(userId)
                .build();

        Cvs saved = cvsRepository.save(cv);
        log.info("Upload CV thành công - userId: {}, cvId: {}", userId, saved.getId());

        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public ResCvOnlineDetailDTO createOnlineCv(Long userId, ReqCreateOnlineCvDTO request) {
        long currentCount = cvsRepository.countByUserId(userId);
        if (currentCount >= MAX_CV_PER_USER) {
            throw AppException.badRequest("Đã đạt giới hạn " + MAX_CV_PER_USER + " CV");
        }

        String title = request.getTitle().trim();
        if (cvsRepository.existsByUserIdAndTitle(userId, title)) {
            throw AppException.conflict("Tên CV '" + title + "' đã tồn tại");
        }

        CvTemplate template = findActiveTemplateOrThrow(request.getTemplateId());
        boolean shouldBeDefault = Boolean.TRUE.equals(request.getIsDefault()) || currentCount == 0;
        if (shouldBeDefault) {
            clearCurrentDefaultCv(userId);
        }

        Cvs cv = Cvs.builder()
                .userId(userId)
                .title(title)
                .cvType(CvType.online)
                .templateId(template.getId())
                .extraData(toExtraDataJson(request.getExtraData()))
                .pdfUrl(null)
                .isDefault(shouldBeDefault)
                .visibility(CvVisibility.private_cv)
                .parseStatus(CvParseStatus.skipped)
                .viewCount(0)
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        return mapToOnlineDetail(cvsRepository.save(cv), template);
    }

    @Override
    public List<ResCvDTO> getMyCvs(Long userId) {
        return cvsRepository.findAllByUserId(userId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public ResCvDTO getCvById(Long userId, Long id) {
        Cvs cv = cvsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy CV"));
        return mapToDTO(cv);
    }

    @Override
    @Transactional(readOnly = true)
    public ResCvOnlineDetailDTO getOnlineCvById(Long userId, Long id) {
        Cvs cv = findOwnedOnlineCvOrThrow(userId, id);
        CvTemplate template = findTemplateByIdOrThrow(cv.getTemplateId());
        return mapToOnlineDetail(cv, template);
    }

    @Override
    @Transactional
    public ResCvDTO renameCv(Long userId, Long id, String newTitle) {
        Cvs cv = cvsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy CV"));

        if (!cv.getTitle().equals(newTitle) && cvsRepository.existsByUserIdAndTitle(userId, newTitle)) {
            throw AppException.conflict("Tên CV '" + newTitle + "' đã tồn tại");
        }

        cv.setTitle(newTitle);
        cv.setUpdatedBy(userId);
        return mapToDTO(cvsRepository.save(cv));
    }

    @Override
    @Transactional
    public ResCvOnlineDetailDTO updateOnlineCv(Long userId, Long id, ReqUpdateOnlineCvDTO request) {
        Cvs cv = findOwnedOnlineCvOrThrow(userId, id);
        String title = request.getTitle().trim();

        if (!cv.getTitle().equals(title) && cvsRepository.existsByUserIdAndTitle(userId, title)) {
            throw AppException.conflict("Tên CV '" + title + "' đã tồn tại");
        }

        cv.setTitle(title);
        cv.setExtraData(toExtraDataJson(request.getExtraData()));
        cv.setPdfUrl(null);
        cv.setUpdatedBy(userId);

        Cvs saved = cvsRepository.save(cv);
        CvTemplate template = findTemplateByIdOrThrow(saved.getTemplateId());
        return mapToOnlineDetail(saved, template);
    }

    @Override
    @Transactional
    public ResCvDTO setDefaultCv(Long userId, Long id) {
        Cvs cv = cvsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy CV"));

        if (Boolean.TRUE.equals(cv.getIsDefault())) {
            return mapToDTO(cv);
        }

        clearCurrentDefaultCv(userId);

        cv.setIsDefault(true);
        cv.setUpdatedBy(userId);
        return mapToDTO(cvsRepository.save(cv));
    }

    @Override
    @Transactional
    public ResCvOnlineDetailDTO changeOnlineCvTemplate(Long userId, Long id, ReqChangeOnlineCvTemplateDTO request) {
        Cvs cv = findOwnedOnlineCvOrThrow(userId, id);
        CvTemplate template = findActiveTemplateOrThrow(request.getTemplateId());

        cv.setTemplateId(template.getId());
        cv.setPdfUrl(null);
        cv.setUpdatedBy(userId);

        return mapToOnlineDetail(cvsRepository.save(cv), template);
    }

    @Override
    @Transactional
    public ResCvDTO duplicateCv(Long userId, Long id) {
        Cvs original = cvsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy CV"));

        long currentCount = cvsRepository.countByUserId(userId);
        if (currentCount >= MAX_CV_PER_USER) {
            throw AppException.badRequest("Đã đạt giới hạn " + MAX_CV_PER_USER + " CV");
        }

        String newTitle = original.getTitle() + " (Copy)";
        int i = 1;
        while (cvsRepository.existsByUserIdAndTitle(userId, newTitle)) {
            newTitle = original.getTitle() + " (Copy " + (i++) + ")";
        }

        Cvs copy = Cvs.builder()
                .userId(userId)
                .title(newTitle)
                .cvType(original.getCvType())
                .templateId(original.getTemplateId())
                .fileUrl(original.getFileUrl())
                .pdfUrl(original.getPdfUrl())
                .extraData(original.getExtraData())
                .isDefault(false)
                .visibility(original.getVisibility())
                .parseStatus(original.getParseStatus())
                .viewCount(0)
                .createdBy(userId)
                .build();

        return mapToDTO(cvsRepository.save(copy));
    }

    @Override
    @Transactional
    public void deleteCv(Long userId, Long id) {
        Cvs cv = cvsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy CV"));

        if (Boolean.TRUE.equals(cv.getIsDefault())) {
            throw AppException.badRequest("Không thể xóa CV mặc định, vui lòng đổi CV mặc định khác trước");
        }

        cv.setDeletedAt(LocalDateTime.now());
        cv.setUpdatedBy(userId);
        cvsRepository.save(cv);

        if (cv.getFileUrl() != null && cvsRepository.countActiveByFileUrl(cv.getFileUrl()) == 0) {
            fileStorageService.deleteFile(cv.getFileUrl(), FileUploadType.CV);
        }
    }

    @Override
    @Transactional
    public ResCvDTO shareCv(Long userId, Long id, ReqShareCvDTO request) {
        Cvs cv = cvsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy CV"));

        cv.setVisibility(request.getVisibility());
        cv.setUpdatedBy(userId);
        return mapToDTO(cvsRepository.save(cv));
    }

    @Override
    public ResShareTokenDTO createShareToken(Long userId, Long id, ReqCreateShareTokenDTO request) {
        Cvs cv = cvsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy CV"));

        long minutes = 0;
        if (request.getDays() != null) {
            minutes += request.getDays() * 24 * 60;
        }
        if (request.getHours() != null) {
            minutes += request.getHours() * 60;
        }
        if (request.getMinutes() != null) {
            minutes += request.getMinutes();
        }

        if (minutes <= 0) {
            throw AppException.badRequest("Thời gian phải lớn hơn 0");
        }

        String token = java.util.UUID.randomUUID().toString();
        String key = "cv-share:" + token;

        redisTemplate.opsForValue().set(key, String.valueOf(cv.getId()), java.time.Duration.ofMinutes(minutes));

        return ResShareTokenDTO.builder()
                .shareToken(token)
                .expiresAt(LocalDateTime.now().plusMinutes(minutes))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResCvDTO getPublicCv(String shareToken) {
        String key = "cv-share:" + shareToken;
        String cvIdStr = redisTemplate.opsForValue().get(key);

        if (cvIdStr == null) {
            throw AppException.notFound("Không tìm thấy CV hoặc link đã hết hạn");
        }

        Long cvId = Long.parseLong(cvIdStr);
        Cvs cv = cvsRepository.findActiveById(cvId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy CV"));

        if (cv.getVisibility() != CvVisibility.public_cv) {
            throw AppException.notFound("Không tìm thấy CV hoặc link đã hết hạn");
        }

        return mapToDTO(cv);
    }

    private void clearCurrentDefaultCv(Long userId) {
        cvsRepository.findDefaultByUserId(userId).ifPresent(oldDefault -> {
            oldDefault.setIsDefault(false);
            cvsRepository.save(oldDefault);
        });
    }

    private Cvs findOwnedOnlineCvOrThrow(Long userId, Long id) {
        Cvs cv = cvsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy CV"));
        if (cv.getCvType() != CvType.online) {
            throw AppException.badRequest("CV này không phải CV online");
        }
        return cv;
    }

    private CvTemplate findActiveTemplateOrThrow(Long templateId) {
        return cvTemplateRepository.findActiveById(templateId)
                .orElseThrow(() -> AppException.badRequest("Template không tồn tại hoặc không active"));
    }

    private CvTemplate findTemplateByIdOrThrow(Long templateId) {
        if (templateId == null) {
            throw AppException.badRequest("CV online chưa có template");
        }
        return cvTemplateRepository.findActiveOrInactiveById(templateId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy template của CV"));
    }

    private String toExtraDataJson(CvOnlineExtraDataDTO extraData) {
        try {
            return objectMapper.writeValueAsString(extraData == null ? CvOnlineExtraDataDTO.empty() : extraData);
        } catch (JsonProcessingException ex) {
            throw AppException.badRequest("Không thể serialize dữ liệu CV online");
        }
    }

    private CvOnlineExtraDataDTO parseExtraData(String extraDataJson) {
        if (extraDataJson == null || extraDataJson.isBlank()) {
            return CvOnlineExtraDataDTO.empty();
        }
        try {
            return objectMapper.readValue(extraDataJson, CvOnlineExtraDataDTO.class);
        } catch (JsonProcessingException ex) {
            throw AppException.badRequest("Dữ liệu CV online không đúng schema JSON");
        }
    }

    private ResCvDTO mapToDTO(Cvs cv) {
        return ResCvDTO.builder()
                .id(cv.getId())
                .title(cv.getTitle())
                .cvType(cv.getCvType())
                .templateId(cv.getTemplateId())
                .fileUrl(cv.getFileUrl())
                .pdfUrl(cv.getPdfUrl())
                .isDefault(cv.getIsDefault())
                .visibility(cv.getVisibility())
                .shareToken(cv.getShareToken())
                .shareExpiresAt(cv.getShareExpiresAt())
                .parseStatus(cv.getParseStatus())
                .viewCount(cv.getViewCount())
                .createdBy(cv.getCreatedBy())
                .createdAt(cv.getCreatedAt())
                .updatedAt(cv.getUpdatedAt())
                .build();
    }

    private ResCvOnlineDetailDTO mapToOnlineDetail(Cvs cv, CvTemplate template) {
        return ResCvOnlineDetailDTO.builder()
                .id(cv.getId())
                .title(cv.getTitle())
                .cvType(cv.getCvType())
                .templateId(cv.getTemplateId())
                .template(mapTemplateDetail(template))
                .extraData(parseExtraData(cv.getExtraData()))
                .pdfUrl(cv.getPdfUrl())
                .isDefault(cv.getIsDefault())
                .visibility(cv.getVisibility())
                .parseStatus(cv.getParseStatus())
                .viewCount(cv.getViewCount())
                .createdAt(cv.getCreatedAt())
                .updatedAt(cv.getUpdatedAt())
                .build();
    }

    private ResCvTemplateDetailDTO mapTemplateDetail(CvTemplate template) {
        return ResCvTemplateDetailDTO.builder()
                .id(template.getId())
                .name(template.getName())
                .slug(template.getSlug())
                .description(template.getDescription())
                .thumbnailUrl(template.getThumbnailUrl())
                .htmlContent(template.getHtmlContent())
                .cssContent(template.getCssContent())
                .isActive(template.getIsActive())
                .isDefault(template.getIsDefault())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
