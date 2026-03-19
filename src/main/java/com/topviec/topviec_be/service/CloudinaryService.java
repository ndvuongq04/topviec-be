package com.topviec.topviec_be.service;

import com.topviec.topviec_be.enums.cvs.FileUploadType;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    /**
     * Upload file lên Cloudinary
     * 
     * @param file   file cần upload
     * @param userId ID của user
     * @param type   loại file: CV, AVATAR, COMPANY_COVER
     * @return URL công khai của file
     */
    String uploadFile(MultipartFile file, Long userId, FileUploadType type);

    /**
     * Xóa file khỏi Cloudinary
     * 
     * @param fileUrl URL đầy đủ của file cần xóa
     * @param type    loại file
     */
    void deleteFile(String fileUrl, FileUploadType type);

    // /**
    // * Tạo signed URL có TTL — dùng cho share CV
    // * URL tự hết hạn sau ttlSeconds giây, không thể dùng lại
    // *
    // * @param fileUrl URL gốc từ Cloudinary (secure_url)
    // * @param ttlSeconds thời gian sống tính bằng giây
    // * @return signed URL có expiry
    // */
    // String generateSignedUrl(String fileUrl, long ttlSeconds);
}