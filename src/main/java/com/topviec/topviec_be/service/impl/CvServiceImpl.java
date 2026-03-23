package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.dto.request.ReqCreateShareTokenDTO;
import com.topviec.topviec_be.dto.request.ReqShareCvDTO;
import com.topviec.topviec_be.dto.request.ReqUploadCvDTO;
import com.topviec.topviec_be.dto.response.ResCvDTO;
import com.topviec.topviec_be.dto.response.ResShareTokenDTO;
import com.topviec.topviec_be.entity.Cvs;
import com.topviec.topviec_be.enums.cvs.CvParseStatus;
import com.topviec.topviec_be.enums.cvs.CvType;
import com.topviec.topviec_be.enums.cvs.CvVisibility;
import com.topviec.topviec_be.enums.cvs.FileUploadType;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.CvsRepository;
import com.topviec.topviec_be.service.CloudinaryService;
import com.topviec.topviec_be.service.CvService;
import com.topviec.topviec_be.util.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final CloudinaryService cloudinaryService;
    private final FileValidator fileValidator;
    private final org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    private static final int MAX_CV_PER_USER = 5;

    @Override
    @Transactional
    public ResCvDTO uploadCv(Long userId, MultipartFile file, ReqUploadCvDTO request) {

        // ① Validate file (format + size)
        fileValidator.validate(file, FileUploadType.CV);

        // ② Kiểm tra giới hạn 5 CV
        long currentCount = cvsRepository.countByUserId(userId);
        if (currentCount >= MAX_CV_PER_USER) {
            throw AppException.badRequest(
                    "Đã đạt giới hạn " + MAX_CV_PER_USER + " CV, vui lòng xóa CV cũ trước");
        }

        // ③ Kiểm tra tên CV trùng
        if (cvsRepository.existsByUserIdAndTitle(userId, request.getTitle())) {
            throw AppException.conflict(
                    "Tên CV '" + request.getTitle() + "' đã tồn tại");
        }

        // ④ Upload file lên Cloudinary
        String fileUrl = cloudinaryService.uploadFile(file, userId, FileUploadType.CV);

        // ⑤ Nếu isDefault = true hoặc CV đầu tiên → tắt CV mặc định cũ
        boolean shouldBeDefault = request.isDefault() || currentCount == 0;
        if (shouldBeDefault) {
            cvsRepository.findDefaultByUserId(userId).ifPresent(oldDefault -> {
                oldDefault.setIsDefault(false);
                cvsRepository.save(oldDefault);
            });
        }

        // ⑥ Tạo entity và lưu DB
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
    public ResCvDTO setDefaultCv(Long userId, Long id) {
        Cvs cv = cvsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy CV"));

        if (Boolean.TRUE.equals(cv.getIsDefault())) {
            return mapToDTO(cv);
        }

        // Tắt mặc định cũ
        cvsRepository.findDefaultByUserId(userId).ifPresent(old -> {
            old.setIsDefault(false);
            cvsRepository.save(old);
        });

        cv.setIsDefault(true);
        cv.setUpdatedBy(userId);
        return mapToDTO(cvsRepository.save(cv));
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
                .fileUrl(original.getFileUrl())
                .pdfUrl(original.getPdfUrl())
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
    public ResShareTokenDTO createShareToken(Long userId, Long id,
            ReqCreateShareTokenDTO request) {
        Cvs cv = cvsRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy CV"));

        long minutes = 0;
        if (request.getDays() != null)
            minutes += request.getDays() * 24 * 60;
        if (request.getHours() != null)
            minutes += request.getHours() * 60;
        if (request.getMinutes() != null)
            minutes += request.getMinutes();

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

    private ResCvDTO mapToDTO(Cvs cv) {
        return ResCvDTO.builder()
                .id(cv.getId())
                .title(cv.getTitle())
                .cvType(cv.getCvType())
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
}