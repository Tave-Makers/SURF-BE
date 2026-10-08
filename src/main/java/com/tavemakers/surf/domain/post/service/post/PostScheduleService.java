package com.tavemakers.surf.domain.post.service.post;

import com.tavemakers.surf.application.schedule.query.ScheduleGetService;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.exception.InvalidScheduleTagException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 공지의 기존 일정 태그·교체·해제 */
@Service
@RequiredArgsConstructor
public class PostScheduleService {
    private final ScheduleGetService scheduleGetService;

    /** ID가 있으면 연결·교체, false이면 해제, 둘 다 생략하면 유지 */
    public void updateScheduleTag(Post post, Long scheduleId, Boolean hasSchedule) {
        if (scheduleId != null) {
            if (!post.getBoard().isNotice()) {
                throw new InvalidScheduleTagException();
            }
            post.tagSchedule(scheduleGetService.getScheduleForUpdate(scheduleId));
        } else if (Boolean.FALSE.equals(hasSchedule)) {
            post.tagSchedule(null);
        }
    }
}
