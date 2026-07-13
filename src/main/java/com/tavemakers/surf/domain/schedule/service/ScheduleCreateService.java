package com.tavemakers.surf.domain.schedule.service;

import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.schedule.entity.Schedule;
import com.tavemakers.surf.domain.schedule.repository.ScheduleRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 일정 생성 도메인 로직. DTO를 알지 못하며 원시값·엔티티만 다룬다.
 * 트랜잭션 경계는 호출자(ScheduleUsecase)가 소유한다.
 */
@Service
@RequiredArgsConstructor
public class ScheduleCreateService {
    private final ScheduleRepository scheduleRepository;

    /** 게시글 연동 일정 생성 */
    public Schedule createScheduleAtPost(String category, String title, LocalDateTime startAt, LocalDateTime endAt, String location, Post post) {
        Schedule schedule = Schedule.of(category, title, startAt, endAt, location, post);
        return scheduleRepository.save(schedule);
    }

    /** 개별 일정 생성 */
    public void createScheduleSingle(String category, String title, LocalDateTime startAt, LocalDateTime endAt, String location) {
        Schedule schedule = Schedule.from(category, title, startAt, endAt, location);
        scheduleRepository.save(schedule);
    }
}
