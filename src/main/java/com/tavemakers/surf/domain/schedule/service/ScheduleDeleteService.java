package com.tavemakers.surf.domain.schedule.service;

import com.tavemakers.surf.domain.schedule.entity.Schedule;
import com.tavemakers.surf.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScheduleDeleteService {
    private final ScheduleRepository scheduleRepository;

    /** 일정 삭제 */
    public void deleteSchedule(Schedule schedule) {
        scheduleRepository.delete(schedule);
    }
}
