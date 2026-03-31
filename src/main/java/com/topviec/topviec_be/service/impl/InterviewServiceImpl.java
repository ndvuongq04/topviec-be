package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.dto.request.*;
import com.topviec.topviec_be.dto.response.*;
import com.topviec.topviec_be.entity.*;
import com.topviec.topviec_be.enums.application.ApplicationStatus;
import com.topviec.topviec_be.enums.interview.*;
import com.topviec.topviec_be.enums.jobs.JobPostStatus;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.*;
import com.topviec.topviec_be.service.InterviewService;
import com.topviec.topviec_be.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRoundRepository roundRepository;
    private final InterviewRoundInterviewerRepository interviewerRepository;
    private final InterviewSlotRepository slotRepository;
    private final InterviewRepository interviewRepository;
    private final InterviewResultRepository resultRepository;
    private final ApplicationRepository applicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final UserRepository userRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final TokenService tokenService;

    // =========================================================================
    // Vòng phỏng vấn
    // =========================================================================

    @Override
    @Transactional
    public ResInterviewRoundDTO createRound(Long jobPostId, Long userId, Long companyId,
            ReqCreateInterviewRoundDTO request) {

        JobPosting job = findJobAndValidateOwnership(jobPostId, companyId);

        if (JobPostStatus.COMPLETED.getValue().equals(job.getStatus())) {
            throw AppException.badRequest("Tin tuyển dụng đã hoàn thành, không thể tạo vòng phỏng vấn mới");
        }

        boolean hasInterviewing = !applicationRepository
                .findByJobPostIdAndStatusAndDeletedAtIsNull(jobPostId, ApplicationStatus.INTERVIEWING.getValue())
                .isEmpty();
        if (hasInterviewing) {
            throw AppException.badRequest("Không thể tạo vòng mới khi đã có ứng viên đang phỏng vấn");
        }

        int nextRoundNumber = roundRepository.findMaxRoundNumber(jobPostId) + 1;

        if (Boolean.TRUE.equals(request.getIsFinal())) {
            List<InterviewRound> existingRounds = roundRepository
                    .findByJobPostIdAndDeletedAtIsNullOrderByRoundNumberAsc(jobPostId);
            for (InterviewRound r : existingRounds) {
                if (Boolean.TRUE.equals(r.getIsFinal())) {
                    r.setIsFinal(false);
                    r.setUpdatedBy(userId);
                    roundRepository.save(r);
                }
            }
        }

        InterviewRound round = InterviewRound.builder()
                .jobPostId(jobPostId)
                .roundNumber(nextRoundNumber)
                .roundName(request.getRoundName())
                .description(request.getDescription())
                .isFinal(request.getIsFinal() != null ? request.getIsFinal() : false)
                .createdBy(userId)
                .build();

        round = roundRepository.save(round);

        if (request.getInterviewers() != null) {
            for (ReqCreateInterviewRoundDTO.InterviewerDTO dto : request.getInterviewers()) {
                InterviewRoundInterviewer interviewer = InterviewRoundInterviewer.builder()
                        .roundId(round.getId())
                        .interviewerName(dto.getName())
                        .interviewerEmail(dto.getEmail())
                        .interviewerPhone(dto.getPhone())
                        .createdBy(userId)
                        .build();
                interviewerRepository.save(interviewer);
            }
        }

        return toRoundResponse(round);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResInterviewRoundDTO> getRounds(Long jobPostId, Long companyId) {
        findJobAndValidateOwnership(jobPostId, companyId);

        List<InterviewRound> rounds = roundRepository
                .findByJobPostIdAndDeletedAtIsNullOrderByRoundNumberAsc(jobPostId);

        return rounds.stream().map(this::toRoundResponse).toList();
    }

    @Override
    @Transactional
    public ResInterviewRoundDTO updateRound(Long roundId, Long userId, Long companyId,
            ReqUpdateInterviewRoundDTO request) {

        InterviewRound round = roundRepository.findByIdAndDeletedAtIsNull(roundId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy vòng phỏng vấn"));

        findJobAndValidateOwnership(round.getJobPostId(), companyId);

        if (interviewRepository.findByApplicationIdAndRoundIdAndDeletedAtIsNull(null, roundId).isPresent()) {
            List<Interview> existingInterviews = interviewRepository
                    .findByJobPostId(round.getJobPostId(), roundId, null);
            if (!existingInterviews.isEmpty()) {
                throw AppException.badRequest("Không thể sửa vòng phỏng vấn đã có ứng viên tham gia");
            }
        }

        if (request.getRoundName() != null) {
            round.setRoundName(request.getRoundName());
        }
        if (request.getDescription() != null) {
            round.setDescription(request.getDescription());
        }
        if (request.getIsFinal() != null) {
            if (Boolean.TRUE.equals(request.getIsFinal())) {
                List<InterviewRound> existingRounds = roundRepository
                        .findByJobPostIdAndDeletedAtIsNullOrderByRoundNumberAsc(round.getJobPostId());
                for (InterviewRound r : existingRounds) {
                    if (Boolean.TRUE.equals(r.getIsFinal()) && !r.getId().equals(roundId)) {
                        r.setIsFinal(false);
                        r.setUpdatedBy(userId);
                        roundRepository.save(r);
                    }
                }
            }
            round.setIsFinal(request.getIsFinal());
        }

        round.setUpdatedBy(userId);
        round = roundRepository.save(round);

        if (request.getInterviewers() != null && !request.getInterviewers().isEmpty()) {
            interviewerRepository.deleteByRoundId(roundId);
            interviewerRepository.flush(); // Ép flush xuống DB trước khi insert
            for (ReqCreateInterviewRoundDTO.InterviewerDTO dto : request.getInterviewers()) {
                InterviewRoundInterviewer interviewer = InterviewRoundInterviewer.builder()
                        .roundId(round.getId())
                        .interviewerName(dto.getName())
                        .interviewerEmail(dto.getEmail())
                        .interviewerPhone(dto.getPhone())
                        .createdBy(userId)
                        .build();
                interviewerRepository.save(interviewer);
            }
        }

        return toRoundResponse(round);
    }

    @Override
    @Transactional
    public void deleteRound(Long roundId, Long userId, Long companyId) {
        InterviewRound round = roundRepository.findByIdAndDeletedAtIsNull(roundId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy vòng phỏng vấn"));

        findJobAndValidateOwnership(round.getJobPostId(), companyId);

        List<Interview> existingInterviews = interviewRepository
                .findByJobPostId(round.getJobPostId(), roundId, null);
        if (!existingInterviews.isEmpty()) {
            throw AppException.badRequest("Không thể xóa vòng phỏng vấn đã có ứng viên tham gia");
        }

        // Xóa dữ liệu liên quan theo thứ tự để tránh vi phạm FK
        interviewerRepository.deleteByRoundId(roundId); // xóa danh sách interviewer
        slotRepository.deleteByRoundId(roundId);        // xóa các slot đề xuất
        roundRepository.delete(round);                  // xóa thật vòng phỏng vấn
    }

    // =========================================================================
    // Lịch phỏng vấn — Cách 1: NTT đặt lịch thủ công
    // =========================================================================

    @Override
    @Transactional
    public ResInterviewScheduleDTO createSchedule(Long roundId, Long userId, Long companyId,
            ReqCreateInterviewScheduleDTO request) {

        InterviewRound round = roundRepository.findByIdAndDeletedAtIsNull(roundId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy vòng phỏng vấn"));

        findJobAndValidateOwnership(round.getJobPostId(), companyId);

        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn ứng tuyển"));

        if (!ApplicationStatus.INTERVIEWING.getValue().equals(application.getStatus())) {
            throw AppException.badRequest("Ứng viên không ở trạng thái INTERVIEWING");
        }

        softDeleteExistingInterview(application.getId(), roundId, userId);

        // Bỏ reminderCount — đã xóa khỏi entity Interview
        Interview interview = Interview.builder()
                .applicationId(application.getId())
                .roundId(roundId)
                .scheduledAt(request.getScheduledAt())
                .durationMinutes(request.getDurationMinutes())
                .interviewType(request.getInterviewType())
                .location(request.getLocation())
                .meetingLink(request.getMeetingLink())
                .interviewerNote(request.getInterviewerNote())
                .status(InterviewStatus.SCHEDULED.getValue())
                .confirmedByCandidate(false)
                .scheduledBy(userId)
                .build();

        interview = interviewRepository.save(interview);

        log.info("📧 [TODO] Gửi email xác nhận lịch PV cho application={}, round={}", application.getId(), roundId);

        return toScheduleResponse(interview, round, application);
    }

    // =========================================================================
    // Lịch phỏng vấn — Cách 2: Tạo slot cho UV chọn
    // =========================================================================

    @Override
    @Transactional
    public void createSlots(Long roundId, Long userId, Long companyId,
            ReqCreateInterviewSlotsDTO request) {

        InterviewRound round = roundRepository.findByIdAndDeletedAtIsNull(roundId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy vòng phỏng vấn"));

        findJobAndValidateOwnership(round.getJobPostId(), companyId);

        if (request.getDeadline().isBefore(LocalDateTime.now())) {
            throw AppException.badRequest("Deadline phải là thời gian trong tương lai");
        }

        // Check duplicate: slot đã tồn tại cho round này chưa
        boolean alreadyHasSlots = slotRepository.existsByRoundId(roundId);
        if (alreadyHasSlots) {
            throw AppException.badRequest("Vòng phỏng vấn này đã có slot rồi");
        }

        // Tạo slots 1 lần cho round — không còn loop theo UV
        for (ReqCreateInterviewSlotsDTO.SlotDTO slotDto : request.getSlots()) {
            InterviewSlot slot = InterviewSlot.builder()
                    .roundId(roundId)
                    .proposedAt(slotDto.getProposedAt())
                    .interviewType(slotDto.getInterviewType())
                    .location(slotDto.getLocation())
                    .meetingLink(slotDto.getMeetingLink())
                    .build();
            slotRepository.save(slot);
        }

        Duration ttl = Duration.between(LocalDateTime.now(), request.getDeadline());

        // Loop UV chỉ để: validate, lưu reminder Redis, generate token, đổi status
        for (Long applicationId : request.getApplicationIds()) {
            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn ứng tuyển: " + applicationId));

            if (!ApplicationStatus.INTERVIEWING.getValue().equals(application.getStatus())) {
                throw AppException.badRequest("Ứng viên " + applicationId + " không ở trạng thái INTERVIEWING");
            }

            // Lưu reminder info vào Redis thay vì lưu trong Interview entity
            tokenService.storeReminderInfo(applicationId, roundId, request.getDeadline(), ttl);

            String token = tokenService.generateInterviewSlotToken(applicationId, roundId, ttl);
            application.setStatus(ApplicationStatus.SCHEDULE_PENDING.getValue());
            applicationRepository.save(application);

            log.info("📧 [TODO] Gửi email slot cho application={}, round={}, token={}", applicationId, roundId, token);
        }
    }

    // =========================================================================
    // UV chọn slot (public, không cần auth)
    // =========================================================================

    @Override
    @Transactional
    public String confirmSlot(String token, Long slotId) {
        String payload = tokenService.verifyInterviewSlotToken(token);
        String[] parts = payload.split(":");
        Long tokenApplicationId = Long.parseLong(parts[0]);
        Long tokenRoundId = Long.parseLong(parts[1]);

        InterviewSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy ca phỏng vấn"));

        // Chỉ validate slot thuộc đúng round (không còn applicationId trong slot)
        if (!slot.getRoundId().equals(tokenRoundId)) {
            throw AppException.badRequest("Ca phỏng vấn không thuộc vòng phỏng vấn này");
        }

        // Check UV đã chọn slot cho round này chưa (thay vì check isSelected trên
        // slot)
        boolean alreadySelected = interviewRepository
                .existsByApplicationIdAndRoundIdAndDeletedAtIsNull(tokenApplicationId, tokenRoundId);
        if (alreadySelected) {
            throw AppException.badRequest("Bạn đã chọn lịch phỏng vấn cho vòng này rồi");
        }

        // Tạo Interview record — bỏ reminderCount, không còn update isSelected trên
        // slot
        Interview interview = Interview.builder()
                .applicationId(tokenApplicationId)
                .roundId(tokenRoundId)
                .slotId(slot.getId())
                .scheduledAt(slot.getProposedAt())
                .interviewType(slot.getInterviewType())
                .location(slot.getLocation())
                .meetingLink(slot.getMeetingLink())
                .status(InterviewStatus.CONFIRMED.getValue())
                .confirmedByCandidate(true)
                .scheduledBy(0L)
                .build();

        interviewRepository.save(interview);

        Application application = applicationRepository.findById(tokenApplicationId).orElse(null);
        if (application != null) {
            application.setStatus(ApplicationStatus.INTERVIEWING.getValue());
            applicationRepository.save(application);
        }

        // Xóa token + reminder info khỏi Redis sau khi UV chọn slot thành công
        tokenService.invalidateInterviewSlotToken(token);
        tokenService.deleteReminderInfo(tokenApplicationId, tokenRoundId);

        log.info("📧 [TODO] Gửi email xác nhận slot cho application={}, round={}", tokenApplicationId, tokenRoundId);

        return "Xác nhận lịch phỏng vấn thành công!";
    }

    // =========================================================================
    // Danh sách lịch PV
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResInterviewScheduleDTO> getSchedules(Long jobPostId, Long companyId,
            Long roundId, String status) {
        findJobAndValidateOwnership(jobPostId, companyId);

        List<Interview> interviews = interviewRepository.findByJobPostId(jobPostId, roundId, status);

        return interviews.stream().map(i -> {
            InterviewRound round = i.getRound();
            Application application = i.getApplication();
            return toScheduleResponse(i, round, application);
        }).toList();
    }

    @Override
    @Transactional
    public ResInterviewScheduleDTO updateSchedule(Long scheduleId, Long userId, Long companyId,
            ReqUpdateInterviewScheduleDTO request) {

        Interview interview = interviewRepository.findByIdAndDeletedAtIsNull(scheduleId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy lịch phỏng vấn"));

        InterviewRound round = interview.getRound();
        findJobAndValidateOwnership(round.getJobPostId(), companyId);

        if (InterviewStatus.COMPLETED.getValue().equals(interview.getStatus())) {
            throw AppException.badRequest("Buổi phỏng vấn đã diễn ra, không thể sửa");
        }

        if (request.getScheduledAt() != null) {
            interview.setScheduledAt(request.getScheduledAt());
        }
        if (request.getLocation() != null) {
            interview.setLocation(request.getLocation());
        }
        if (request.getMeetingLink() != null) {
            interview.setMeetingLink(request.getMeetingLink());
        }
        if (request.getInterviewerNote() != null) {
            interview.setInterviewerNote(request.getInterviewerNote());
        }

        interview.setUpdatedBy(userId);
        interview = interviewRepository.save(interview);

        log.info("📧 [TODO] Gửi email thông báo thay đổi lịch PV schedule={}", scheduleId);

        return toScheduleResponse(interview, round, interview.getApplication());
    }

    @Override
    @Transactional
    public void deleteSchedule(Long scheduleId, Long userId, Long companyId) {
        Interview interview = interviewRepository.findByIdAndDeletedAtIsNull(scheduleId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy lịch phỏng vấn"));

        InterviewRound round = interview.getRound();
        findJobAndValidateOwnership(round.getJobPostId(), companyId);

        if (InterviewStatus.COMPLETED.getValue().equals(interview.getStatus())) {
            throw AppException.badRequest("Buổi phỏng vấn đã diễn ra, không thể hủy");
        }

        interview.setStatus(InterviewStatus.CANCELLED.getValue());
        interview.setDeletedAt(LocalDateTime.now());
        interview.setUpdatedBy(userId);
        interviewRepository.save(interview);

        log.info("📧 [TODO] Gửi email thông báo hủy lịch PV schedule={}", scheduleId);
    }

    // =========================================================================
    // Kết quả phỏng vấn
    // =========================================================================

    @Override
    @Transactional
    public ResInterviewResultDTO createResult(Long scheduleId, Long userId, Long companyId,
            ReqInterviewResultDTO request) {

        Interview interview = interviewRepository.findByIdAndDeletedAtIsNull(scheduleId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy lịch phỏng vấn"));

        InterviewRound round = interview.getRound();
        findJobAndValidateOwnership(round.getJobPostId(), companyId);

        if (interview.getScheduledAt().isAfter(LocalDateTime.now())) {
            throw AppException.badRequest("Buổi phỏng vấn chưa diễn ra");
        }

        if (resultRepository.existsByInterviewId(scheduleId)) {
            throw AppException.badRequest("Đã có kết quả cho buổi phỏng vấn này");
        }

        interview.setStatus(InterviewStatus.COMPLETED.getValue());
        interview.setUpdatedBy(userId);
        interviewRepository.save(interview);

        InterviewResultStatus resultStatus = InterviewResultStatus.fromValue(request.getResult());

        InterviewResult result = InterviewResult.builder()
                .interviewId(scheduleId)
                .result(resultStatus.getValue())
                .rating(request.getRating())
                .note(request.getNote())
                .notifyCandidate(request.getNotifyCandidate() != null ? request.getNotifyCandidate() : false)
                .evaluatedBy(userId)
                .evaluatedAt(LocalDateTime.now())
                .build();

        result = resultRepository.save(result);

        Application application = applicationRepository.findById(interview.getApplicationId()).orElse(null);
        if (application != null) {
            handlePostResult(application, round, resultStatus, Boolean.TRUE.equals(request.getNotifyCandidate()));
        }

        return toResultResponse(result);
    }

    @Override
    @Transactional(readOnly = true)
    public ResInterviewResultDTO getResult(Long scheduleId, Long companyId) {
        Interview interview = interviewRepository.findByIdAndDeletedAtIsNull(scheduleId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy lịch phỏng vấn"));

        findJobAndValidateOwnership(interview.getRound().getJobPostId(), companyId);

        InterviewResult result = resultRepository.findByInterviewId(scheduleId)
                .orElseThrow(() -> AppException.notFound("Chưa có kết quả cho buổi phỏng vấn này"));

        return toResultResponse(result);
    }

    // =========================================================================
    // Lịch sử PV
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public ResInterviewHistoryDTO getInterviewHistory(Long applicationId, Long companyId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn ứng tuyển"));

        findJobAndValidateOwnership(application.getJobPostId(), companyId);

        List<InterviewRound> rounds = roundRepository
                .findByJobPostIdAndDeletedAtIsNullOrderByRoundNumberAsc(application.getJobPostId());

        List<Interview> interviews = interviewRepository
                .findByApplicationIdAndDeletedAtIsNullOrderByRoundId(applicationId);

        List<ResInterviewHistoryDTO.RoundHistory> roundHistories = new ArrayList<>();
        for (InterviewRound round : rounds) {
            ResInterviewHistoryDTO.RoundHistory.RoundHistoryBuilder builder = ResInterviewHistoryDTO.RoundHistory
                    .builder()
                    .roundNumber(round.getRoundNumber())
                    .roundName(round.getRoundName())
                    .isFinal(round.getIsFinal());

            Interview interview = interviews.stream()
                    .filter(i -> i.getRoundId().equals(round.getId()))
                    .findFirst().orElse(null);

            if (interview != null) {
                builder.scheduleId(interview.getId())
                        .scheduledAt(interview.getScheduledAt())
                        .interviewType(interview.getInterviewType())
                        .scheduleStatus(interview.getStatus());

                resultRepository.findByInterviewId(interview.getId()).ifPresent(result -> {
                    builder.result(result.getResult())
                            .rating(result.getRating())
                            .note(result.getNote())
                            .evaluatedAt(result.getEvaluatedAt());
                });
            }

            roundHistories.add(builder.build());
        }

        String candidateName = getCandidateName(application.getCandidateUserId());

        return ResInterviewHistoryDTO.builder()
                .applicationId(applicationId)
                .candidateName(candidateName)
                .currentStatus(application.getStatus())
                .rounds(roundHistories)
                .build();
    }

    // =========================================================================
    // Overdue
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ResOverdueApplicationDTO> getOverdueApplications(Long jobPostId, Long companyId) {
        findJobAndValidateOwnership(jobPostId, companyId);

        List<Application> overdueApps = applicationRepository
                .findByJobPostIdAndStatusAndDeletedAtIsNull(jobPostId, ApplicationStatus.OVERDUE.getValue());

        return overdueApps.stream().map(app -> {
            String candidateName = getCandidateName(app.getCandidateUserId());
            User user = userRepository.findById(app.getCandidateUserId()).orElse(null);
            CandidateProfile profile = candidateProfileRepository
                    .findByUserId(app.getCandidateUserId()).orElse(null);

            // Tìm round hiện tại của UV để lấy reminder info từ Redis
            List<InterviewRound> rounds = roundRepository
                    .findByJobPostIdAndDeletedAtIsNullOrderByRoundNumberAsc(jobPostId);
            InterviewRound currentRound = findCurrentRoundForApplication(app.getId(), rounds);

            // Đọc reminder info từ Redis
            ReminderInfo reminderInfo = currentRound != null
                    ? tokenService.getReminderInfo(app.getId(), currentRound.getId())
                    : null;

            return ResOverdueApplicationDTO.builder()
                    .applicationId(app.getId())
                    .candidateUserId(app.getCandidateUserId())
                    .candidateName(candidateName)
                    .candidateEmail(user != null ? user.getEmail() : null)
                    .candidatePhone(profile != null ? profile.getPhoneDisplay() : null)
                    .reminderCount(reminderInfo != null ? reminderInfo.getReminderCount() : 0)
                    .firstReminderAt(reminderInfo != null ? reminderInfo.getLastRemindedAt() : null)
                    .reminderDeadline(reminderInfo != null ? reminderInfo.getDeadline() : null)
                    .currentRoundName(currentRound != null ? currentRound.getRoundName() : null)
                    .currentRoundNumber(currentRound != null ? currentRound.getRoundNumber() : null)
                    .build();
        }).toList();
    }

    @Override
    @Transactional
    public void extendDeadline(Long applicationId, Long userId, Long companyId,
            ReqExtendDeadlineDTO request) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn ứng tuyển"));

        findJobAndValidateOwnership(application.getJobPostId(), companyId);

        if (!ApplicationStatus.OVERDUE.getValue().equals(application.getStatus())) {
            throw AppException.badRequest("Chỉ có thể gia hạn cho ứng viên quá hạn");
        }

        application.setStatus(ApplicationStatus.SCHEDULE_PENDING.getValue());
        applicationRepository.save(application);

        log.info("📧 [TODO] Gửi lại email slot cho application={}, gia hạn thêm {} ngày",
                applicationId, request.getExtendDays());
    }

    @Override
    @Transactional
    public ResInterviewScheduleDTO forceSchedule(Long applicationId, Long userId, Long companyId,
            ReqForceScheduleDTO request) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn ứng tuyển"));

        findJobAndValidateOwnership(application.getJobPostId(), companyId);

        if (!ApplicationStatus.OVERDUE.getValue().equals(application.getStatus())) {
            throw AppException.badRequest("Chỉ có thể đặt lịch hộ cho ứng viên quá hạn");
        }

        List<InterviewRound> rounds = roundRepository
                .findByJobPostIdAndDeletedAtIsNullOrderByRoundNumberAsc(application.getJobPostId());
        InterviewRound currentRound = findCurrentRoundForApplication(application.getId(), rounds);

        if (currentRound == null) {
            throw AppException.badRequest("Không tìm thấy vòng phỏng vấn phù hợp");
        }

        softDeleteExistingInterview(application.getId(), currentRound.getId(), userId);

        Interview interview = Interview.builder()
                .applicationId(application.getId())
                .roundId(currentRound.getId())
                .scheduledAt(request.getScheduledAt())
                .interviewType(request.getInterviewType())
                .location(request.getLocation())
                .meetingLink(request.getMeetingLink())
                .status(InterviewStatus.SCHEDULED.getValue())
                .confirmedByCandidate(false)
                .scheduledBy(userId)
                .build();

        interview = interviewRepository.save(interview);

        application.setStatus(ApplicationStatus.INTERVIEWING.getValue());
        applicationRepository.save(application);

        log.info("📧 [TODO] Gửi email xác nhận lịch PV (force) cho application={}", applicationId);

        return toScheduleResponse(interview, currentRound, application);
    }

    // =========================================================================
    // Offer
    // =========================================================================

    @Override
    @Transactional
    public ResEmployerApplicationDTO updateOffer(Long applicationId, Long userId, Long companyId,
            ReqOfferResultDTO request) {

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn ứng tuyển"));

        findJobAndValidateOwnership(application.getJobPostId(), companyId);

        if (!ApplicationStatus.OFFERED.getValue().equals(application.getStatus())) {
            throw AppException.badRequest("Ứng viên không ở trạng thái OFFERED");
        }

        if (request.getResult() == OfferResult.ACCEPTED) {
            // Giữ OFFERED, chờ bước hoàn thành tuyển dụng
        } else if (request.getResult() == OfferResult.DECLINED) {
            application.setStatus(ApplicationStatus.REJECTED.getValue());
            application.setRejectedAt(LocalDateTime.now());
            application.setRejectionReason("Ứng viên từ chối offer");
        }

        applicationRepository.save(application);

        return toOfferResponse(application);
    }

    // =========================================================================
    // Job Posting interview phase
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public ResInterviewReadinessDTO checkReadiness(Long jobPostId, Long companyId) {
        JobPosting job = findJobAndValidateOwnership(jobPostId, companyId);

        boolean isJobClosed = JobPostStatus.CLOSED.getValue().equals(job.getStatus());
        boolean hasRounds = roundRepository.countByJobPostIdActive(jobPostId) > 0;
        boolean hasCvPassed = !applicationRepository
                .findByJobPostIdAndStatusAndDeletedAtIsNull(jobPostId, ApplicationStatus.CV_PASSED.getValue())
                .isEmpty();

        return ResInterviewReadinessDTO.builder()
                .isJobClosed(isJobClosed)
                .hasRounds(hasRounds)
                .hasCvPassed(hasCvPassed)
                .ready(isJobClosed && hasRounds && hasCvPassed)
                .build();
    }

    @Override
    @Transactional
    public void startInterviewing(Long jobPostId, Long userId, Long companyId) {
        JobPosting job = findJobAndValidateOwnership(jobPostId, companyId);

        if (!JobPostStatus.CLOSED.getValue().equals(job.getStatus())) {
            throw AppException.badRequest("Tin tuyển dụng phải được đóng trước khi bắt đầu phỏng vấn");
        }

        if (roundRepository.countByJobPostIdActive(jobPostId) == 0) {
            throw AppException.badRequest("Cần tạo ít nhất 1 vòng phỏng vấn trước khi bắt đầu");
        }

        List<Application> cvPassedApps = applicationRepository
                .findByJobPostIdAndStatusAndDeletedAtIsNull(jobPostId, ApplicationStatus.CV_PASSED.getValue());
        if (cvPassedApps.isEmpty()) {
            throw AppException.badRequest("Không có ứng viên nào ở trạng thái CV_PASSED");
        }

        applicationRepository.bulkUpdateStatus(jobPostId,
                ApplicationStatus.CV_PASSED.getValue(),
                ApplicationStatus.INTERVIEWING.getValue());

        job.setStatus(JobPostStatus.INTERVIEWING.getValue());
        job.setUpdatedBy(userId);
        jobPostingRepository.save(job);
    }

    @Override
    @Transactional
    public void completeRecruitment(Long jobPostId, Long userId, Long companyId,
            ReqCompleteRecruitmentDTO request) {

        JobPosting job = findJobAndValidateOwnership(jobPostId, companyId);

        if (!JobPostStatus.INTERVIEWING.getValue().equals(job.getStatus())) {
            throw AppException.badRequest("Tin tuyển dụng phải ở trạng thái INTERVIEWING");
        }

        List<Application> offeredApps = applicationRepository
                .findByJobPostIdAndStatusAndDeletedAtIsNull(jobPostId, ApplicationStatus.OFFERED.getValue());
        if (offeredApps.isEmpty()) {
            throw AppException.badRequest("Cần có ít nhất 1 ứng viên ở trạng thái OFFERED");
        }

        for (Long appId : request.getApplicationIds()) {
            Application app = applicationRepository.findById(appId)
                    .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn ứng tuyển: " + appId));
            if (!ApplicationStatus.OFFERED.getValue().equals(app.getStatus())) {
                throw AppException.badRequest("Ứng viên " + appId + " không ở trạng thái OFFERED");
            }
            if (!app.getJobPostId().equals(jobPostId)) {
                throw AppException.badRequest("Ứng viên " + appId + " không thuộc tin tuyển dụng này");
            }
        }

        for (Long appId : request.getApplicationIds()) {
            Application app = applicationRepository.findById(appId).orElseThrow();
            app.setStatus(ApplicationStatus.HIRED.getValue());
            app.setHiredAt(LocalDateTime.now());
            applicationRepository.save(app);
        }

        applicationRepository.bulkRejectExcluding(jobPostId, request.getApplicationIds());

        job.setStatus(JobPostStatus.COMPLETED.getValue());
        job.setUpdatedBy(userId);
        jobPostingRepository.save(job);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private JobPosting findJobAndValidateOwnership(Long jobPostId, Long companyId) {
        JobPosting job = jobPostingRepository.findByIdAndDeletedAtIsNull(jobPostId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy tin tuyển dụng"));

        if (!job.getCompanyId().equals(companyId)) {
            throw AppException.forbidden("Bạn không có quyền truy cập tin tuyển dụng này");
        }

        return job;
    }

    private void softDeleteExistingInterview(Long applicationId, Long roundId, Long userId) {
        interviewRepository.findByApplicationIdAndRoundIdAndDeletedAtIsNull(applicationId, roundId)
                .ifPresent(existing -> {
                    existing.setDeletedAt(LocalDateTime.now());
                    existing.setUpdatedBy(userId);
                    interviewRepository.save(existing);
                });
    }

    private void handlePostResult(Application application, InterviewRound round,
            InterviewResultStatus resultStatus, boolean notifyCandidate) {

        if (resultStatus == InterviewResultStatus.FAIL) {
            application.setStatus(ApplicationStatus.REJECTED.getValue());
            application.setRejectedAt(LocalDateTime.now());
            applicationRepository.save(application);
            if (notifyCandidate) {
                log.info("📧 [TODO] Gửi email thông báo FAIL cho application={}", application.getId());
            }
        } else if (resultStatus == InterviewResultStatus.PASS) {
            if (Boolean.TRUE.equals(round.getIsFinal())) {
                application.setStatus(ApplicationStatus.OFFERED.getValue());
                applicationRepository.save(application);
                log.info("🎉 Application {} pass vòng cuối, chuyển OFFERED", application.getId());
            } else {
                roundRepository.findNextRound(round.getJobPostId(), round.getRoundNumber())
                        .ifPresent(nextRound -> {
                            log.info("➡️ Application {} pass vòng {}, tiếp tục vòng {}",
                                    application.getId(), round.getRoundNumber(), nextRound.getRoundNumber());
                            if (notifyCandidate) {
                                log.info("📧 [TODO] Gửi email thông báo PASS + slot vòng tiếp cho application={}",
                                        application.getId());
                            }
                        });
            }
        }
    }

    private InterviewRound findCurrentRoundForApplication(Long applicationId, List<InterviewRound> rounds) {
        List<Interview> interviews = interviewRepository
                .findByApplicationIdAndDeletedAtIsNullOrderByRoundId(applicationId);

        for (InterviewRound round : rounds) {
            boolean hasPassedThisRound = interviews.stream()
                    .filter(i -> i.getRoundId().equals(round.getId()))
                    .anyMatch(i -> {
                        InterviewResult result = resultRepository.findByInterviewId(i.getId()).orElse(null);
                        return result != null
                                && InterviewResultStatus.PASS.getValue().equals(result.getResult());
                    });
            if (!hasPassedThisRound) {
                return round;
            }
        }
        return null;
    }

    private String getCandidateName(Long candidateUserId) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(candidateUserId).orElse(null);
        if (profile != null && profile.getFullName() != null) {
            return profile.getFullName();
        }
        User user = userRepository.findById(candidateUserId).orElse(null);
        return user != null ? "User " + user.getId() : "Unknown";
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private ResInterviewRoundDTO toRoundResponse(InterviewRound round) {
        List<InterviewRoundInterviewer> interviewers = interviewerRepository.findByRoundId(round.getId());

        return ResInterviewRoundDTO.builder()
                .id(round.getId())
                .jobPostId(round.getJobPostId())
                .roundNumber(round.getRoundNumber())
                .roundName(round.getRoundName())
                .description(round.getDescription())
                .isFinal(round.getIsFinal())
                .interviewers(interviewers.stream()
                        .map(i -> ResInterviewRoundDTO.InterviewerInfo.builder()
                                .id(i.getId())
                                .name(i.getInterviewerName())
                                .email(i.getInterviewerEmail())
                                .phone(i.getInterviewerPhone())
                                .build())
                        .toList())
                .createdAt(round.getCreatedAt())
                .build();
    }

    private ResInterviewScheduleDTO toScheduleResponse(Interview interview, InterviewRound round,
            Application application) {
        String candidateName = getCandidateName(application.getCandidateUserId());
        User user = userRepository.findById(application.getCandidateUserId()).orElse(null);
        CandidateProfile profile = candidateProfileRepository.findByUserId(application.getCandidateUserId())
                .orElse(null);

        return ResInterviewScheduleDTO.builder()
                .id(interview.getId())
                .applicationId(interview.getApplicationId())
                .roundId(interview.getRoundId())
                .roundNumber(round != null ? round.getRoundNumber() : null)
                .roundName(round != null ? round.getRoundName() : null)
                .candidateName(candidateName)
                .candidateEmail(user != null ? user.getEmail() : null)
                .candidatePhone(profile != null ? profile.getPhoneDisplay() : null)
                .scheduledAt(interview.getScheduledAt())
                .durationMinutes(interview.getDurationMinutes())
                .interviewType(interview.getInterviewType())
                .location(interview.getLocation())
                .meetingLink(interview.getMeetingLink())
                .status(interview.getStatus())
                .confirmedByCandidate(interview.getConfirmedByCandidate())
                .interviewerNote(interview.getInterviewerNote())
                .createdAt(interview.getCreatedAt())
                .updatedAt(interview.getUpdatedAt())
                .build();
    }

    private ResInterviewResultDTO toResultResponse(InterviewResult result) {
        return ResInterviewResultDTO.builder()
                .id(result.getId())
                .interviewId(result.getInterviewId())
                .result(result.getResult())
                .rating(result.getRating())
                .note(result.getNote())
                .notifyCandidate(result.getNotifyCandidate())
                .evaluatedBy(result.getEvaluatedBy())
                .evaluatedAt(result.getEvaluatedAt())
                .build();
    }

    private ResEmployerApplicationDTO toOfferResponse(Application a) {
        User user = userRepository.findById(a.getCandidateUserId()).orElse(null);
        CandidateProfile profile = candidateProfileRepository.findByUserId(a.getCandidateUserId()).orElse(null);
        JobPosting job = a.getJobPosting();

        return ResEmployerApplicationDTO.builder()
                .id(a.getId())
                .jobPostId(a.getJobPostId())
                .jobTitle(job != null ? job.getTitle() : null)
                .candidateUserId(a.getCandidateUserId())
                .candidateName(
                        profile != null ? profile.getFullName() : (user != null ? "User " + user.getId() : "Unknown"))
                .candidateEmail(user != null ? user.getEmail() : null)
                .candidatePhone(profile != null ? profile.getPhoneDisplay() : null)
                .status(a.getStatus())
                .applyMethod(a.getApplyMethod())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}