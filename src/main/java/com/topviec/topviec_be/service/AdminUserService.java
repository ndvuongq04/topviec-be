package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.request.ReqCreateAdmin;
import com.topviec.topviec_be.dto.request.ReqUpdateAdmin;
import com.topviec.topviec_be.dto.response.ResAdminUser;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;

import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    // Tạo admin mới (chỉ super_admin được gọi)
    ResAdminUser createAdmin(ReqCreateAdmin request, Long createdByUserId);

    // Lấy thông tin admin theo id
    ResAdminUser getAdminById(Long adminUsersId);

    // Lấy tất cả admin chưa bị xóa
    ResultPaginationDTO getAllAdmins(Pageable pageable);

    // Cập nhật thông tin admin
    ResAdminUser updateAdmin(Long adminUsersId, ReqUpdateAdmin request, Long updatedByUserId);

    // Bật/tắt trạng thái active (vô hiệu hóa tạm thời)
    ResAdminUser toggleActive(Long adminUsersId, Long updatedByUserId);

    // Soft delete admin
    void deleteAdmin(Long adminUsersId, Long deletedByUserId);
}