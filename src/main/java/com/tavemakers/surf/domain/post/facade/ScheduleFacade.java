package com.tavemakers.surf.domain.post.facade;

import com.tavemakers.surf.domain.post.dto.req.ScheduleCreateReqDTO;
import com.tavemakers.surf.domain.post.dto.req.ScheduleUpdateReqDTO;
import com.tavemakers.surf.domain.post.dto.res.ScheduleResDTO;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.entity.Schedule;
import com.tavemakers.surf.domain.post.service.PostGetService;
import com.tavemakers.surf.domain.post.service.PostService;
import com.tavemakers.surf.domain.post.service.ScheduleCreateService;
import com.tavemakers.surf.domain.post.service.ScheduleDeleteService;
import com.tavemakers.surf.domain.post.service.ScheduleGetService;
import com.tavemakers.surf.domain.post.service.SchedulePatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ScheduleFacade {

    private final ScheduleCreateService scheduleCreateService;
    private final ScheduleGetService scheduleGetService;
    private final SchedulePatchService schedulePatchService;
    private final ScheduleDeleteService scheduleDeleteService;
    private final PostService postService;
    private final PostGetService postGetService;

    @Transactional
    public void createScheduleAtPost(ScheduleCreateReqDTO dto, Long postId) {
        Post post = postService.findPostById(postId);
        Long scheduleId = scheduleCreateService.createScheduleAtPost(dto, post);
        post.addScheduleId(scheduleId);
    }

    @Transactional
    public void createScheduleSingle(ScheduleCreateReqDTO dto){
        scheduleCreateService.createScheduleSingle(dto);
    }

    @Transactional
    public void updateSchedule(ScheduleUpdateReqDTO dto, Long id) {
        Schedule schedule = scheduleGetService.getScheduleById(id);
        schedulePatchService.updateSchedule(schedule, dto);
    }

    @Transactional
    public void deleteSchedule(Long id) {
        Schedule schedule = scheduleGetService.getScheduleById(id);
        scheduleDeleteService.deleteSchedule(schedule);
    }

    @Transactional
    public void deleteScheduleAtPost(Long postId, Long scheduleId) {
        Schedule schedule = scheduleGetService.getScheduleById(scheduleId);
        scheduleDeleteService.deleteSchedule(schedule);

        Post post = postGetService.getPost(postId);
        schedulePatchService.updateHasSchedule(post,false);
        schedulePatchService.updateScheduleIdNull(post);
    }

    @Transactional(readOnly = true)
    public ScheduleResDTO getScheduleByPost(Long postId) {
           return scheduleGetService.getScheduleSingleDTO(postId);
    }
}
