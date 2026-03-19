package com.topviec.topviec_be.util;

import com.topviec.topviec_be.enums.cvs.FileUploadType;
import com.topviec.topviec_be.exception.AppException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

@Component
public class FileValidator {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L; // 5MB
    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024L; // 2MB

    private static final Set<String> ALLOWED_CV_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp");

    private static final Set<String> ALLOWED_CV_EXTENSIONS = Set.of("pdf", "doc", "docx");
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private static final Map<FileUploadType, Long> SIZE_LIMIT = Map.of(
            FileUploadType.CV, MAX_FILE_SIZE,
            FileUploadType.AVATAR, MAX_IMAGE_SIZE,
            FileUploadType.COMPANY_COVER, MAX_IMAGE_SIZE);

    private static final Map<FileUploadType, Set<String>> MIME_LIMIT = Map.of(
            FileUploadType.CV, ALLOWED_CV_MIME_TYPES,
            FileUploadType.AVATAR, ALLOWED_IMAGE_MIME_TYPES,
            FileUploadType.COMPANY_COVER, ALLOWED_IMAGE_MIME_TYPES);

    private static final Map<FileUploadType, Set<String>> EXT_LIMIT = Map.of(
            FileUploadType.CV, ALLOWED_CV_EXTENSIONS,
            FileUploadType.AVATAR, ALLOWED_IMAGE_EXTENSIONS,
            FileUploadType.COMPANY_COVER, ALLOWED_IMAGE_EXTENSIONS);

    public void validate(MultipartFile file, FileUploadType type) {

        // ① File rỗng
        if (file == null || file.isEmpty()) {
            throw AppException.badRequest(resolveEmptyMessage(type));
        }

        // ② Kiểm tra dung lượng
        long maxSize = SIZE_LIMIT.get(type);
        if (file.getSize() > maxSize) {
            throw AppException.badRequest(resolveSizeMessage(type, maxSize));
        }

        // ③ Kiểm tra MIME type
        String contentType = file.getContentType();
        Set<String> allowedMimes = MIME_LIMIT.get(type);
        if (contentType == null || !allowedMimes.contains(contentType)) {
            throw AppException.badRequest(resolveFormatMessage(type));
        }

        // ④ Double-check extension — tránh giả mạo content-type
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw AppException.badRequest("Tên file không hợp lệ");
        }

        String extension = originalFilename
                .substring(originalFilename.lastIndexOf(".") + 1)
                .toLowerCase();

        if (!EXT_LIMIT.get(type).contains(extension)) {
            throw AppException.badRequest(resolveFormatMessage(type));
        }
    }

    private String resolveEmptyMessage(FileUploadType type) {
        return switch (type) {
            case CV -> "Vui lòng chọn file CV";
            case AVATAR -> "Vui lòng chọn ảnh đại diện";
            case COMPANY_COVER -> "Vui lòng chọn ảnh bìa công ty";
        };
    }

    private String resolveSizeMessage(FileUploadType type, long maxSize) {
        String sizeLabel = (maxSize == MAX_FILE_SIZE) ? "5MB" : "2MB";
        return switch (type) {
            case CV -> "File CV quá lớn, vui lòng chọn file nhỏ hơn " + sizeLabel;
            case AVATAR -> "Ảnh đại diện quá lớn, vui lòng chọn ảnh nhỏ hơn " + sizeLabel;
            case COMPANY_COVER -> "Ảnh bìa quá lớn, vui lòng chọn ảnh nhỏ hơn " + sizeLabel;
        };
    }

    private String resolveFormatMessage(FileUploadType type) {
        return switch (type) {
            case CV -> "CV chỉ chấp nhận định dạng PDF, DOC hoặc DOCX";
            case AVATAR -> "Ảnh đại diện chỉ chấp nhận JPG, PNG hoặc WEBP";
            case COMPANY_COVER -> "Ảnh bìa chỉ chấp nhận JPG, PNG hoặc WEBP";
        };
    }
}
