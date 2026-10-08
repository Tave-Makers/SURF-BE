package com.tavemakers.surf.application.schedule.query;

import com.tavemakers.surf.application.post.query.PostGetService;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.schedule.entity.Schedule;
import com.tavemakers.surf.domain.schedule.exception.ScheduleNotFoundException;
import com.tavemakers.surf.domain.schedule.repository.ScheduleRepository;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogParam;
import com.tavemakers.surf.presentation.schedule.dto.response.ScheduleMonthlyResDTO;
import com.tavemakers.surf.presentation.schedule.dto.response.SchedulePostResDTO;
import com.tavemakers.surf.presentation.schedule.dto.response.ScheduleResDTO;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleGetService {
    private final ScheduleRepository scheduleRepository;
    private final PostGetService postGetService;

    /** 월별 일정 및 연결된 공지를 일괄 조회 */
    public ScheduleMonthlyResDTO getScheduleMonthly(String memberRole, int year, int month) {
        List<Schedule> schedules = getSchedulesByMonth(memberRole, year, month);
        if (schedules.isEmpty()) {
            return ScheduleMonthlyResDTO.of(year, month, List.of());
        }
        List<Long> ids = schedules.stream().map(Schedule::getId).toList();
        Map<Long, List<Post>> posts = postGetService.getPublishedPostsByScheduleIds(ids).stream()
                .collect(Collectors.groupingBy(Post::getScheduleId));
        List<ScheduleResDTO> responses = schedules.stream()
                .map(s -> toResponse(s, posts.getOrDefault(s.getId(), List.of())))
                .toList();
        return ScheduleMonthlyResDTO.of(year, month, responses);
    }

    /** 일정은 독립적으로 노출하며 예약 공지는 바로가기 목록에서만 제외 */
    public List<Schedule> getSchedulesByMonth(String memberRole, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999999999);
        if (Objects.equals(memberRole, MemberRole.MEMBER.toString())) {
            return scheduleRepository.findByStartAtBetweenAndCategoryInOrderByStartAtAscIdAsc(
                    start, end, List.of("regular", "other"));
        }
        return scheduleRepository.findByStartAtBetweenOrderByStartAtAscIdAsc(start, end);
    }

    /** 일정 ID로 엔티티 조회 */
    public Schedule getScheduleById(Long scheduleId) {
        return scheduleRepository.findById(scheduleId).orElseThrow(ScheduleNotFoundException::new);
    }

    /** 호출자 쓰기 트랜잭션에서 일정 행 잠금을 유지한다 */
    @Transactional
    public Schedule getScheduleForUpdate(Long scheduleId) {
        return scheduleRepository.findByIdForUpdate(scheduleId).orElseThrow(ScheduleNotFoundException::new);
    }

    /** 공지가 참조하는 일정 하나를 반환한다 */
    public ScheduleResDTO getScheduleSingleDTO(Long postId) {
        Post post = postGetService.findPostById(postId);
        if (post.getScheduleId() == null) {
            throw new ScheduleNotFoundException();
        }
        return getScheduleResponse(getScheduleById(post.getScheduleId()));
    }

    @LogEvent("calendar.summary.open")
    public ScheduleResDTO getScheduleSingleAtCalendar(@LogParam("schedule_id") Long scheduleId) {
        return getScheduleResponse(getScheduleById(scheduleId));
    }

    private ScheduleResDTO getScheduleResponse(Schedule schedule) {
        return toResponse(schedule, postGetService.getPublishedPostsByScheduleIds(List.of(schedule.getId())));
    }

    private ScheduleResDTO toResponse(Schedule schedule, List<Post> posts) {
        return ScheduleResDTO.fromEntity(schedule, posts.stream().map(SchedulePostResDTO::from).toList());
    }

    public Optional<Schedule> findFirstScheduleAfter(String category, LocalDateTime dateTime) {
        return scheduleRepository.findFirstByCategoryAndStartAtAfterOrderByStartAtAsc(category, dateTime);
    }

    public Optional<Schedule> findFirstScheduleBefore(String category, LocalDateTime dateTime) {
        return scheduleRepository.findFirstByCategoryAndStartAtLessThanEqualOrderByStartAtDesc(category, dateTime);
    }
}
