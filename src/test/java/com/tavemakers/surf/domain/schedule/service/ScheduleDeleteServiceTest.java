package com.tavemakers.surf.domain.schedule.service;

import com.tavemakers.surf.domain.schedule.entity.Schedule;
import com.tavemakers.surf.domain.schedule.repository.ScheduleRepository;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class ScheduleDeleteServiceTest {
    @Test
    void deleteSchedule() {
        ScheduleRepository repository = mock(ScheduleRepository.class);
        Schedule schedule = Schedule.builder().id(5L).build();
        new ScheduleDeleteService(repository).deleteSchedule(schedule);
        verify(repository).delete(schedule);
    }
}
