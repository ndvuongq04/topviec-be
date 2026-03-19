package com.topviec.topviec_be.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.topviec.topviec_be.config.CloudinaryConfig;
import com.topviec.topviec_be.enums.cvs.FileUploadType;
import com.topviec.topviec_be.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;
    private final CloudinaryConfig cloudinaryConfig;

    @Override
    public String uploadFile(MultipartFile file, Long userId, FileUploadType type) {
        try {
            byte[] fileBytes = file.getBytes();
            String folderPath = resolveFolder(type) + "/user_" + userId;

            String publicId = UUID.randomUUID().toString();
            String resourceType = "auto";

            Map uploadResult = cloudinary.uploader().upload(fileBytes, ObjectUtils.asMap(
                    "folder", folderPath,
                    "public_id", publicId,
                    "resource_type", resourceType,
                    "use_filename", false,
                    "overwrite", false));

            String fileUrl = (String) uploadResult.get("secure_url");
            log.info("Upload {} thành công - userId: {}, url: {}", type, userId, fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("Upload {} thất bại - userId: {}", type, userId, e);
            throw new RuntimeException("Không thể upload file lên Cloudinary", e);
        }
    }

    @Override
    public void deleteFile(String fileUrl, FileUploadType type) {
        try {
            String publicId = extractPublicId(fileUrl);

            // Xác định resource_type từ URL
            String resourceType = fileUrl.contains("/raw/upload/") ? "raw" : "image";

            cloudinary.uploader().destroy(publicId,
                    ObjectUtils.asMap("resource_type", resourceType));
            log.info("Xóa file Cloudinary thành công - publicId: {}", publicId);

        } catch (IOException e) {
            log.warn("Không thể xóa file Cloudinary - url: {}", fileUrl, e);
        }
    }

    // @Override
    // public String generateSignedUrl(String fileUrl, long ttlSeconds) {
    // try {
    // String publicId = extractPublicId(fileUrl);
    // String resourceType = fileUrl.contains("/raw/upload/") ? "raw" : "image";
    // long expiresAt = (System.currentTimeMillis() / 1000L) + ttlSeconds;
    // String extension = fileUrl.substring(fileUrl.lastIndexOf(".") +
    // 1).split("\\?")[0];

    // com.cloudinary.Url url = cloudinary.url()
    // .resourceType(resourceType)
    // .type("upload")
    // .signed(true)
    // .format(extension);

    // // Set expires_at trực tiếp vào params map của Url object
    // url.getParams().put("expires_at", expiresAt);

    // String signedUrl = url.generate(publicId);

    // log.info("Tạo signed URL - publicId: {}, format: {}, expiresAt: {}",
    // publicId, extension, expiresAt);
    // return signedUrl;

    // } catch (Exception e) {
    // log.error("Tạo signed URL thất bại - fileUrl: {}", fileUrl, e);
    // throw new RuntimeException("Không thể tạo signed URL", e);
    // }
    // }

    private String resolveFolder(FileUploadType type) {
        String subFolder = switch (type) {
            case CV -> cloudinaryConfig.getFolderCv();
            case AVATAR -> cloudinaryConfig.getFolderAvatar();
            case COMPANY_COVER -> cloudinaryConfig.getFolderCompanyCover();
        };
        return cloudinaryConfig.getFolderRoot() + "/" + subFolder;
    }

    private String extractPublicId(String fileUrl) {
        String marker = "/upload/";
        int idx = fileUrl.indexOf(marker);
        if (idx == -1) {
            log.warn("URL Cloudinary không hợp lệ: {}", fileUrl);
            return fileUrl;
        }

        String afterUpload = fileUrl.substring(idx + marker.length());

        // Bỏ version prefix nếu có (v1234567/)
        if (afterUpload.startsWith("v") && afterUpload.contains("/")) {
            afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
        }

        return afterUpload;
    }

}
