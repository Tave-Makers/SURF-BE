package com.tavemakers.surf.domain.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tavemakers.surf.domain.schedule.entity.Schedule;
import com.tavemakers.surf.domain.schedule.exception.ScheduleTimeException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SchedulePatchService 단위 테스트.
 * Schedule.updateSchedule의 분기(둘 다 변경/한쪽만 변경)와 시간 검증 예외를 겨냥한다.
 * 협력자가 없는 순수 위임 서비스이므로 Mockito 없이 실제 엔티티로 검증한다.
 */
class SchedulePatchServiceTest {

    private final SchedulePatchService schedulePatchService = new SchedulePatchService();

    private final LocalDateTime originalStartAt = LocalDateTime.of(2026, 1, 10, 14, 0);
    private final LocalDateTime originalEndAt = LocalDateTime.of(2026, 1, 10, 16, 0);

    private Schedule schedule;

    @BeforeEach
    void setUp() {
        schedule = Schedule.from("regular", "원본제목", originalStartAt, originalEndAt, "원본장소");
    }

    @Test
    @DisplayName("startAt·endAt을 함께 유효한 값으로 변경하면 둘 다 갱신된다")
    void 시작종료시간_함께변경() {
        LocalDateTime newStartAt = originalStartAt.plusDays(1);
        LocalDateTime newEndAt = originalEndAt.plusDays(1);

        schedulePatchService.updateSchedule(schedule, null, null, newStartAt, newEndAt, null);

        assertThat(schedule.getStartAt()).isEqualTo(newStartAt);
        assertThat(schedule.getEndAt()).isEqualTo(newEndAt);
    }

    @Test
    @DisplayName("startAt·endAt을 함께 변경할 때 startAt이 endAt보다 늦으면 ScheduleTimeException이 발생하고 기존 값이 유지된다")
    void 시작종료시간_함께변경_시간역전이면_예외() {
        LocalDateTime invalidStartAt = originalEndAt.plusDays(2);
        LocalDateTime newEndAt = originalEndAt.plusDays(1);

        assertThatThrownBy(() -> schedulePatchService.updateSchedule(
                schedule, null, null, invalidStartAt, newEndAt, null))
                .isInstanceOf(ScheduleTimeException.class);

        assertThat(schedule.getStartAt()).isEqualTo(originalStartAt);
        assertThat(schedule.getEndAt()).isEqualTo(originalEndAt);
    }

    @Test
    @DisplayName("startAt만 변경할 때 기존 endAt보다 늦으면 ScheduleTimeException이 발생한다")
    void 시작시간만변경_기존종료시간보다늦으면_예외() {
        LocalDateTime invalidStartAt = originalEndAt.plusHours(1);

        assertThatThrownBy(() -> schedulePatchService.updateSchedule(
                schedule, null, null, invalidStartAt, null, null))
                .isInstanceOf(ScheduleTimeException.class);

        assertThat(schedule.getStartAt()).isEqualTo(originalStartAt);
    }

    @Test
    @DisplayName("endAt만 변경할 때 기존 startAt보다 이르면 ScheduleTimeException이 발생한다")
    void 종료시간만변경_기존시작시간보다이르면_예외() {
        LocalDateTime invalidEndAt = originalStartAt.minusHours(1);

        assertThatThrownBy(() -> schedulePatchService.updateSchedule(
                schedule, null, null, null, invalidEndAt, null))
                .isInstanceOf(ScheduleTimeException.class);

        assertThat(schedule.getEndAt()).isEqualTo(originalEndAt);
    }

    @Test
    @DisplayName("null이 아닌 필드만 갱신되고 나머지는 기존 값을 유지한다")
    void null이아닌필드만_갱신된다() {
        schedulePatchService.updateSchedule(schedule, "other", null, null, null, "새장소");

        assertThat(schedule.getCategory()).isEqualTo("other");
        assertThat(schedule.getTitle()).isEqualTo("원본제목");
        assertThat(schedule.getLocation()).isEqualTo("새장소");
        assertThat(schedule.getStartAt()).isEqualTo(originalStartAt);
        assertThat(schedule.getEndAt()).isEqualTo(originalEndAt);
    }
}
