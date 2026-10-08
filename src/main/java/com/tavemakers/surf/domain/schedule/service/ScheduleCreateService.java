package com.tavemakers.surf.domain.schedule.service;

import com.tavemakers.surf.domain.schedule.entity.Schedule;
import com.tavemakers.surf.domain.schedule.repository.ScheduleRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScheduleCreateService {
    private final ScheduleRepository scheduleRepository;

    /** 캘린더에서 독립적인 일정 생성 */
    public void createScheduleSingle(String category, String title, LocalDateTime startAt,
                                     LocalDateTime endAt, String location) {
        scheduleRepository.save(Schedule.from(category, title, startAt, endAt, location));
    }
}
