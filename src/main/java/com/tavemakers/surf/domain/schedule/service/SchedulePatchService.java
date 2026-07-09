package com.tavemakers.surf.domain.schedule.service;

import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.schedule.entity.Schedule;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 일정 수정 도메인 로직. DTO를 알지 못하며 원시값·엔티티만 다룬다.
 * 트랜잭션 경계는 호출자(ScheduleUsecase)가 소유한다.
 */
@Service
@RequiredArgsConstructor
public class SchedulePatchService {

    /** 일정 정보 수정 */
    public void updateSchedule(Schedule schedule, String category, String title, LocalDateTime startAt, LocalDateTime endAt, String location) {
        schedule.updateSchedule(category, title, startAt, endAt, location);
    }

    /** 게시글의 일정 연동 상태 변경 */
    public void updateHasSchedule(Post post, boolean hasSchedule) {
        post.changeHasSchedule(hasSchedule);
    }

    /** 게시글의 일정 ID를 null로 초기화 */
    public void updateScheduleIdNull(Post post) {
        post.updateScheduleIdNull();
    }
}
