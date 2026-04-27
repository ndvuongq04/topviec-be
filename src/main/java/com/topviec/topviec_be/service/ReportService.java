package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.request.ReqCreateReportDTO;
import com.topviec.topviec_be.dto.request.ReqProcessReportDTO;
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
}
