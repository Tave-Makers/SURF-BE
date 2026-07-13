package com.tavemakers.surf.domain.schedule.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.schedule.entity.Schedule;
import com.tavemakers.surf.domain.schedule.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ScheduleDeleteService 단위 테스트.
 * 순수 위임 서비스이므로, 두 메서드가 서로 다른 repository 메서드/인자로 올바르게 라우팅되는지만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ScheduleDeleteServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private Post post;

    @Mock
    private Schedule schedule;

    private ScheduleDeleteService scheduleDeleteService;

    @BeforeEach
    void setUp() {
        scheduleDeleteService = new ScheduleDeleteService(scheduleRepository);
    }

    @Test
    @DisplayName("deleteSchedule은 repository.delete(schedule)에 위임한다")
    void deleteSchedule_repository_delete_위임() {
        scheduleDeleteService.deleteSchedule(schedule);

        then(scheduleRepository).should().delete(schedule);
        then(scheduleRepository).should(never()).deleteByPost(any());
    }

    @Test
    @DisplayName("deleteByPost는 repository.deleteByPost(post)에 위임한다")
    void deleteByPost_repository_deleteByPost_위임() {
        scheduleDeleteService.deleteByPost(post);

        then(scheduleRepository).should().deleteByPost(post);
        then(scheduleRepository).should(never()).delete(any());
    }
}
