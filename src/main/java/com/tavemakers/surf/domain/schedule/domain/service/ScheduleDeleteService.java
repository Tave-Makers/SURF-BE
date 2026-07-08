package com.tavemakers.surf.domain.schedule.domain.service;

import com.tavemakers.surf.domain.post.domain.entity.Post;
import com.tavemakers.surf.domain.schedule.domain.entity.Schedule;
import com.tavemakers.surf.domain.schedule.domain.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleDeleteService {

    private final ScheduleRepository scheduleRepository;

    /** 일정 삭제 */
    @Transactional
    public void deleteSchedule(Schedule schedule) {
        scheduleRepository.delete(schedule);
    }

    /** 게시글 연동 일정 삭제 */
    @Transactional
    public void deleteByPost(Post post) {
        scheduleRepository.deleteByPost(post);
    }
}
