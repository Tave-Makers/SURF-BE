package com.tavemakers.surf.domain.schedule.service;

import com.tavemakers.surf.domain.schedule.dto.request.ScheduleCreateReqDTO;
import com.tavemakers.surf.domain.schedule.dto.request.ScheduleUpdateReqDTO;
import com.tavemakers.surf.domain.schedule.dto.response.ScheduleMonthlyResDTO;
import com.tavemakers.surf.domain.schedule.dto.response.ScheduleResDTO;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.schedule.entity.Schedule;
import com.tavemakers.surf.domain.post.service.post.PostGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleUsecase {

    private final ScheduleCreateService scheduleCreateService;
    private final ScheduleGetService scheduleGetService;
    private final SchedulePatchService schedulePatchService;
    private final ScheduleDeleteService scheduleDeleteService;
    private final PostGetService postGetService;

    /** 게시글 생성 시 연동 일정 생성 */
    @Transactional
    public void createScheduleAtPost(ScheduleCreateReqDTO dto, Long postId) {
        Post post = postGetService.findPostById(postId);
        Long scheduleId = scheduleCreateService.createScheduleAtPost(dto, post);
        post.addScheduleId(scheduleId);
    }

    /** 캘린더에서 개별 일정 생성 */
    @Transactional
    public void createScheduleSingle(ScheduleCreateReqDTO dto){
        scheduleCreateService.createScheduleSingle(dto);
    }

    /** 일정 수정 */
    @Transactional
    public void updateSchedule(ScheduleUpdateReqDTO dto, Long id) {
        Schedule schedule = scheduleGetService.getScheduleById(id);
        schedulePatchService.updateSchedule(schedule, dto);
    }

    /** 개별 일정 삭제 */
    @Transactional
    public void deleteSchedule(Long id) {
        Schedule schedule = scheduleGetService.getScheduleById(id);
        scheduleDeleteService.deleteSchedule(schedule);
    }

    /** 게시글 연동 일정 삭제 */
    @Transactional
    public void deleteScheduleAtPost(Long postId, Long scheduleId) {
        Schedule schedule = scheduleGetService.getScheduleById(scheduleId);
        scheduleDeleteService.deleteSchedule(schedule);

        Post post = postGetService.getPost(postId);
        schedulePatchService.updateHasSchedule(post,false);
        schedulePatchService.updateScheduleIdNull(post);
    }

    /** 게시글별 연동 일정 조회 */
    @Transactional(readOnly = true)
    public ScheduleResDTO getScheduleByPost(Long postId) {
           return scheduleGetService.getScheduleSingleDTO(postId);
    }

    /** 월별 일정 조회 */
    @Transactional(readOnly = true)
    public ScheduleMonthlyResDTO getScheduleMonthly(String memberRole, int year, int month) {
        return scheduleGetService.getScheduleMonthly(memberRole, year, month);
    }

    /** 특정 일정 단건 조회 (캘린더용) */
    @Transactional(readOnly = true)
    public ScheduleResDTO getScheduleSingleAtCalendar(Long scheduleId) {
        return scheduleGetService.getScheduleSingleAtCalendar(scheduleId);
    }
}
