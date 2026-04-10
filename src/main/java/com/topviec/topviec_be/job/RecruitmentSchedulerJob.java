package com.topviec.topviec_be.job;

import com.topviec.topviec_be.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Các cron job định kỳ liên quan đến quy trình tuyển dụng.
 * Thêm method mới vào đây khi cần thêm job.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecruitmentSchedulerJob {

    private final ApplicationRepository applicationRepository;

    /**
     * Tự động chuyển application từ SCHEDULE_PENDING → OVERDUE
     * khi tất cả invitation của UV đã qua deadline.
     *
     * Dùng LocalDateTime.now() từ Java thay vì CURRENT_TIMESTAMP của DB
     * để tránh timezone mismatch.
     *
     * Chạy mỗi 5 phút, trễ tối đa 5 phút so với deadline thực tế.
     */
    @Scheduled(cron = "0 0/5 * * * *")
    @Transactional
    public void markOverdueApplications() {
        LocalDateTime now = LocalDateTime.now();
        log.info("[Scheduler] markOverdueApplications chạy lúc: {}", now);
        int updated = applicationRepository.bulkMarkOverdue(now);
        if (updated > 0) {
            log.info("[Scheduler] markOverdueApplications: chuyển {} application sang OVERDUE", updated);
        }
    }
}
