package com.tavemakers.surf.domain.schedule.service;

import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.schedule.entity.Schedule;
import com.tavemakers.surf.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 일정 삭제 도메인 로직. 엔티티만 다룬다.
 * 트랜잭션 경계는 호출자(usecase)가 소유한다.
 */
@Service
@RequiredArgsConstructor
public class ScheduleDeleteService {

    private final ScheduleRepository scheduleRepository;

    /** 일정 삭제 */
    public void deleteSchedule(Schedule schedule) {
        scheduleRepository.delete(schedule);
    }

    /** 게시글 연동 일정 삭제 */
    public void deleteByPost(Post post) {
        scheduleRepository.deleteByPost(post);
    }
}
