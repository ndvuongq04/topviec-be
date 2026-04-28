package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.request.ReqCreateAppealDTO;
import com.topviec.topviec_be.dto.request.ReqUnsuspendDTO;
import com.topviec.topviec_be.dto.response.ResAppealDTO;

import java.util.List;

public interface AppealService {

    /** NTD nộp kháng cáo cho một báo cáo nhóm B */
    ResAppealDTO create(Long employerUserId, ReqCreateAppealDTO request);

    /** Admin xem toàn bộ danh sách kháng cáo của một NTD */
    List<ResAppealDTO> getByEmployer(Long employerId);

    /**
     * Admin duyệt kháng cáo, gỡ điểm vi phạm từ complaint liên quan và mở khóa tài khoản sớm.
     * Chỉ áp dụng khi appeal ở trạng thái pending.
     */
    ResAppealDTO unsuspend(Long adminUserId, Long employerId, ReqUnsuspendDTO request);
}
