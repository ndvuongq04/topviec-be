package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.request.ReqCreateReportDTO;
import com.topviec.topviec_be.dto.request.ReqProcessReportDTO;
import com.topviec.topviec_be.dto.response.ResCandidateReportSummaryDTO;
import com.topviec.topviec_be.dto.response.ResEmployerComplaintDetailDTO;
import com.topviec.topviec_be.dto.response.ResReportDetailDTO;
import com.topviec.topviec_be.dto.response.ResViolationReasonDTO;
import com.topviec.topviec_be.dto.response.ResultPaginationDTO;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    ResReportDetailDTO create(Long reporterUserId, ReqCreateReportDTO request);

    ResultPaginationDTO getReports(
            String search,
            String status,
            String group,
            String complaintType,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable);

    ResReportDetailDTO getDetail(Long reportId);

    ResReportDetailDTO process(Long adminUserId, Long reportId, ReqProcessReportDTO request);

    List<ResViolationReasonDTO> getViolationReasons();

    ResultPaginationDTO getMyReports(Long reporterUserId, String status, Pageable pageable);

    // ── NTD xem khiếu nại bị báo cáo ─────────────────────────────────────────

    /** Danh sách khiếu nại nhắm vào tin của NTD (ẩn danh người báo cáo) */
    ResultPaginationDTO getEmployerReports(Long employerUserId, String search, String status, String group, String complaintType, Pageable pageable);

    /** Chi tiết một khiếu nại — chỉ cho phép nếu tin thuộc công ty của NTD */
    ResEmployerComplaintDetailDTO getEmployerReportDetail(Long employerUserId, Long reportId);

    /** NTD xác nhận đã sửa tin → tự đóng báo cáo nhóm A */
    ResEmployerComplaintDetailDTO respondToReport(Long employerUserId, Long reportId);
}
