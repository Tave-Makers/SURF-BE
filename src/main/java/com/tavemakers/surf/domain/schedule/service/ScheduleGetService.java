package com.tavemakers.surf.domain.schedule.service;

import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.schedule.dto.response.ScheduleMonthlyResDTO;
import com.tavemakers.surf.domain.schedule.dto.response.ScheduleResDTO;
import com.tavemakers.surf.domain.schedule.entity.Schedule;
import com.tavemakers.surf.domain.schedule.exception.ScheduleNotFoundException;
import com.tavemakers.surf.domain.schedule.repository.ScheduleRepository;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogParam;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleGetService {
    private final ScheduleRepository scheduleRepository;

    /** 월별 일정 목록 조회 */
    @Transactional(readOnly = true)
    @LogEvent("calendar.view")
    public ScheduleMonthlyResDTO getScheduleMonthly(
            String memberRole,
            int year,
            @LogParam("month_view") int month
    ) {
        List<Schedule> schedules =
                getSchedulesByMonth(memberRole, year, month);

        List<ScheduleResDTO> scheduleResDTOS = getScheduleResDTOs(schedules);
        return getScheduleMonthlyResDTOs(year, month, scheduleResDTOS);
    }

    //특정 월의 일정 조회 (예약글과 연동된 일정은 제외)
    @Transactional(readOnly = true)
    public List<Schedule> getSchedulesByMonth(String memberRole,  int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<String> categories = List.of("regular", "other");

        if(Objects.equals(memberRole, MemberRole.MEMBER.toString())) {
            return scheduleRepository.findByStartAtBetweenAndCategoryInExcludingReserved(
                    startOfMonth, endOfMonth, categories);
        } else {
            return scheduleRepository.findByStartAtBetweenExcludingReserved(
                    startOfMonth, endOfMonth);
        }
    }

    //개별 일정 응답 생성
    @Transactional(readOnly = true)
    protected List<ScheduleResDTO> getScheduleResDTOs(List<Schedule> schedules) {
        return ScheduleResDTO.fromEntities(schedules);
    }

    //월별 일정 응답 생성
    @Transactional(readOnly = true)
    protected ScheduleMonthlyResDTO getScheduleMonthlyResDTOs(int year, int month, List<ScheduleResDTO> dto) {
        return ScheduleMonthlyResDTO.of(year,month,dto);
    }

    /** 일정 ID로 엔티티 조회 */
    @Transactional(readOnly = true)
    public Schedule getScheduleById(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(ScheduleNotFoundException::new);
    }

    /** 게시글 ID로 연동 일정 조회 */
    @Transactional(readOnly = true)
    public ScheduleResDTO getScheduleSingleDTO(Long postId) {
        Schedule schedule = scheduleRepository.findByPostId(postId)
                .orElseThrow(ScheduleNotFoundException::new);
        return ScheduleResDTO.fromEntity(schedule);
    }

    /** 캘린더에서 일정 상세 조회 */
    @Transactional(readOnly = true)
    @LogEvent("calendar.summary.open")
    public ScheduleResDTO getScheduleSingleAtCalendar(@LogParam("schedule_id") Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(ScheduleNotFoundException::new);
        return ScheduleResDTO.fromEntity(schedule);
    }

    /** 카테고리와 시간 기준으로 이후 첫 번째 일정 조회 (홈 화면용) */
    @Transactional(readOnly = true)
    public Optional<Schedule> findFirstScheduleAfter(String category, LocalDateTime dateTime) {
        return scheduleRepository.findFirstByCategoryAndStartAtAfterOrderByStartAtAsc(category, dateTime);
    }

    /** 카테고리와 시간 기준으로 이전/같은 첫 번째 일정 조회 (홈 화면용) */
    @Transactional(readOnly = true)
    public Optional<Schedule> findFirstScheduleBefore(String category, LocalDateTime dateTime) {
        return scheduleRepository.findFirstByCategoryAndStartAtLessThanEqualOrderByStartAtDesc(category, dateTime);
    }
}
