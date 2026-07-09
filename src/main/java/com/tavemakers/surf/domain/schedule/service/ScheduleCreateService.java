package com.tavemakers.surf.domain.schedule.service;

import com.tavemakers.surf.presentation.schedule.dto.request.ScheduleCreateReqDTO;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.schedule.entity.Schedule;
import com.tavemakers.surf.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleCreateService {
    private final ScheduleRepository scheduleRepository;

    /** 게시글 연동 일정 생성 */
    @Transactional
    public Long createScheduleAtPost(ScheduleCreateReqDTO dto, Post post) {
        Schedule schedule = Schedule.of(dto, post);
        return scheduleRepository.save(schedule).getId();
    }

    /** 개별 일정 생성 */
    @Transactional
    public void createScheduleSingle(ScheduleCreateReqDTO dto) {
        Schedule schedule = Schedule.from(dto);
        scheduleRepository.save(schedule);
    }
}
