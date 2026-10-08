package com.tavemakers.surf.domain.schedule.service;

import com.tavemakers.surf.domain.schedule.entity.Schedule;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class SchedulePatchService {

    /** 일정 정보 수정 */
    public void updateSchedule(Schedule schedule, String category, String title, LocalDateTime startAt,
                               LocalDateTime endAt, String location) {
        schedule.updateSchedule(category, title, startAt, endAt, location);
    }
}
