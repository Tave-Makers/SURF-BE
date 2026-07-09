package com.tavemakers.surf.domain.schedule.service;

import com.tavemakers.surf.presentation.schedule.dto.request.ScheduleUpdateReqDTO;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.schedule.entity.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SchedulePatchService {

    /** 일정 정보 수정 */
    @Transactional
    public void updateSchedule(Schedule schedule, ScheduleUpdateReqDTO dto) {
        schedule.updateSchedule(dto);
    }

    /** 게시글의 일정 연동 상태 변경 */
    @Transactional
    public void updateHasSchedule(Post post, boolean hasSchedule) {
        post.changeHasSchedule(hasSchedule);
    }

    /** 게시글의 일정 ID를 null로 초기화 */
    @Transactional
    public void updateScheduleIdNull(Post post) {
        post.updateScheduleIdNull();
    }
}
