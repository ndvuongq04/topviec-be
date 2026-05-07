package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.response.*;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface LogQueryService {

    // ═══════ AUDIT LOG ═══════

    /** Danh sách audit log — có filter */
    ResultPaginationDTO getAuditLogs(
            List<Long> userIds,
            String action, String category, String severity, String status,
            LocalDate startDate, LocalDate endDate,
            Pageable pageable);

    /** Chi tiết 1 audit log */
    ResAuditLogDetailDTO getAuditLogDetail(Long id);

    // ═══════ BUSINESS EVENT LOG ═══════

    /** Danh sách business event log — có filter */
    ResultPaginationDTO getBusinessEventLogs(
            List<Long> userIds,
            String action, String category, String status,
            LocalDate startDate, LocalDate endDate,
            Pageable pageable);

    /** Chi tiết 1 business event log */
    ResBusinessEventLogDetailDTO getBusinessEventLogDetail(Long id);
}
