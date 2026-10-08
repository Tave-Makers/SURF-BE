package com.tavemakers.surf.domain.schedule.service;

import com.tavemakers.surf.domain.schedule.exception.ScheduleTimeException;
import com.tavemakers.surf.domain.schedule.repository.ScheduleRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ScheduleCreateServiceTest {
    @Test
    void invalidTimeDoesNotCreateSchedule() {
        ScheduleRepository repository = mock(ScheduleRepository.class);
        LocalDateTime end = LocalDateTime.of(2026, 6, 20, 18, 0);
        assertThatThrownBy(() -> new ScheduleCreateService(repository).createScheduleSingle(
                "regular", "만남의 장", end.plusHours(1), end, "장소"))
                .isInstanceOf(ScheduleTimeException.class);
        verifyNoInteractions(repository);
    }
}
