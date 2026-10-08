package com.tavemakers.surf.application.schedule.usecase;

import com.tavemakers.surf.application.post.query.PostGetService;
import com.tavemakers.surf.application.schedule.query.ScheduleGetService;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.schedule.entity.Schedule;
import com.tavemakers.surf.domain.schedule.service.ScheduleCreateService;
import com.tavemakers.surf.domain.schedule.service.ScheduleDeleteService;
import com.tavemakers.surf.domain.schedule.service.SchedulePatchService;
import com.tavemakers.surf.presentation.schedule.dto.request.ScheduleCreateReqDTO;
import com.tavemakers.surf.presentation.schedule.dto.request.ScheduleUpdateReqDTO;
import com.tavemakers.surf.presentation.schedule.dto.response.ScheduleMonthlyResDTO;
import com.tavemakers.surf.presentation.schedule.dto.response.ScheduleResDTO;
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

    /** 캘린더에서 공지와 독립적인 일정을 생성 */
    @Transactional
    public void createScheduleSingle(ScheduleCreateReqDTO dto) {
        scheduleCreateService.createScheduleSingle(
                dto.category(), dto.title(), dto.startAt(), dto.endAt(), dto.location());
    }

    /** 기존 일정의 카테고리·제목·시간·장소를 수정 */
    @Transactional
    public void updateSchedule(ScheduleUpdateReqDTO dto, Long id) {
        Schedule schedule = scheduleGetService.getScheduleById(id);
        schedulePatchService.updateSchedule(
                schedule, dto.category(), dto.title(), dto.startAt(), dto.endAt(), dto.location());
    }

    /** 예약 공지를 포함한 모든 연결 공지의 태그를 해제한 뒤 일정을 삭제 (공지는 유지) */
    @Transactional
    public void deleteSchedule(Long id) {
        // 태그 연결 요청과 동일한 일정 행을 잠가 삭제 중 새 연결이 생기는 것을 방지한다.
        Schedule schedule = scheduleGetService.getScheduleForUpdate(id);
        // 일정 삭제 전에 참조를 해제하며, 태그 해제와 삭제는 같은 트랜잭션에서 처리한다.
        for (Post post : postGetService.getPostsByScheduleId(id)) {
            post.tagSchedule(null);
        }
        scheduleDeleteService.deleteSchedule(schedule);
    }

    /** 공지에 태그된 일정 하나를 조회 */
    @Transactional(readOnly = true)
    public ScheduleResDTO getScheduleByPost(Long postId) {
        return scheduleGetService.getScheduleSingleDTO(postId);
    }

    /** 회원 역할에 따라 월별 일정과 각 일정에 연결된 공개 공지 목록을 조회 */
    @Transactional(readOnly = true)
    public ScheduleMonthlyResDTO getScheduleMonthly(String memberRole, int year, int month) {
        return scheduleGetService.getScheduleMonthly(memberRole, year, month);
    }

    /** 캘린더에서 수정·삭제할 일정의 상세 정보와 연결된 공개 공지 목록을 조회 */
    @Transactional(readOnly = true)
    public ScheduleResDTO getScheduleSingleAtCalendar(Long scheduleId) {
        return scheduleGetService.getScheduleSingleAtCalendar(scheduleId);
    }
}
