package com.topviec.topviec_be.scheduler;

import com.topviec.topviec_be.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ComplaintScheduler {

    private final ReportService reportService;

    /**
     * Chạy mỗi giờ: ẩn tin và đóng báo cáo nhóm A khi NTD không sửa trong 48h.
     */
    @Scheduled(fixedRate = 60 * 60 * 1_000)
    public void autoCloseExpiredGroupAComplaints() {
        reportService.autoCloseExpiredGroupAComplaints();
    }
}
