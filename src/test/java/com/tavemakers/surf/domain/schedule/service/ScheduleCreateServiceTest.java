package com.tavemakers.surf.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.schedule.entity.Schedule;
import com.tavemakers.surf.domain.schedule.exception.ScheduleTimeException;
import com.tavemakers.surf.domain.schedule.repository.ScheduleRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ScheduleCreateService 단위 테스트.
 * 핵심 검증: startAt > endAt 이면 ScheduleTimeException, 유효하면 repository.save 위임.
 */
@ExtendWith(MockitoExtension.class)
class ScheduleCreateServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private Post post;

    private ScheduleCreateService scheduleCreateService;

    private final LocalDateTime startAt = LocalDateTime.of(2026, 1, 10, 14, 0);
    private final LocalDateTime endAt = LocalDateTime.of(2026, 1, 10, 16, 0);

    @BeforeEach
    void setUp() {
        scheduleCreateService = new ScheduleCreateService(scheduleRepository);
    }

    @Test
    @DisplayName("createScheduleAtPost: 유효한 시간이면 Post가 연동된 일정을 저장하고 저장 결과를 반환한다")
    void createScheduleAtPost_유효한시간이면_저장하고_반환한다() {
        Schedule saved = Schedule.of("regular", "제목", startAt, endAt, "장소", post);
        given(scheduleRepository.save(any(Schedule.class))).willReturn(saved);

        Schedule result = scheduleCreateService.createScheduleAtPost(
                "regular", "제목", startAt, endAt, "장소", post);

        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        then(scheduleRepository).should().save(captor.capture());
        Schedule passed = captor.getValue();
        assertThat(passed.getCategory()).isEqualTo("regular");
        assertThat(passed.getTitle()).isEqualTo("제목");
        assertThat(passed.getStartAt()).isEqualTo(startAt);
        assertThat(passed.getEndAt()).isEqualTo(endAt);
        assertThat(passed.getLocation()).isEqualTo("장소");
        assertThat(passed.getPost()).isSameAs(post);
        assertThat(result).isSameAs(saved);
    }

    @Test
    @DisplayName("createScheduleAtPost: startAt이 endAt보다 늦으면 ScheduleTimeException이 발생하고 저장을 시도하지 않는다")
    void createScheduleAtPost_시간역전이면_예외발생하고_저장하지않는다() {
        LocalDateTime invalidStartAt = endAt.plusHours(1);

        assertThatThrownBy(() -> scheduleCreateService.createScheduleAtPost(
                "regular", "제목", invalidStartAt, endAt, "장소", post))
                .isInstanceOf(ScheduleTimeException.class);

        then(scheduleRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createScheduleSingle: 유효한 시간이면 Post 없는 개별 일정을 저장한다")
    void createScheduleSingle_유효한시간이면_저장한다() {
        scheduleCreateService.createScheduleSingle("other", "개별일정", startAt, endAt, "장소2");

        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        then(scheduleRepository).should().save(captor.capture());
        Schedule passed = captor.getValue();
        assertThat(passed.getCategory()).isEqualTo("other");
        assertThat(passed.getTitle()).isEqualTo("개별일정");
        assertThat(passed.getPost()).isNull();
    }

    @Test
    @DisplayName("createScheduleSingle: startAt이 endAt보다 늦으면 ScheduleTimeException이 발생하고 저장을 시도하지 않는다")
    void createScheduleSingle_시간역전이면_예외발생하고_저장하지않는다() {
        LocalDateTime invalidStartAt = endAt.plusMinutes(1);

        assertThatThrownBy(() -> scheduleCreateService.createScheduleSingle(
                "other", "개별일정", invalidStartAt, endAt, "장소2"))
                .isInstanceOf(ScheduleTimeException.class);

        then(scheduleRepository).should(never()).save(any());
    }
}
