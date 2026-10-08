package com.tavemakers.surf.application.schedule.usecase;

import com.tavemakers.surf.application.post.query.PostGetService;
import com.tavemakers.surf.application.schedule.query.ScheduleGetService;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.schedule.entity.Schedule;
import com.tavemakers.surf.domain.schedule.service.ScheduleCreateService;
import com.tavemakers.surf.domain.schedule.service.ScheduleDeleteService;
import com.tavemakers.surf.domain.schedule.service.SchedulePatchService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleUsecaseTest {
    @Mock ScheduleCreateService createService;
    @Mock ScheduleGetService getService;
    @Mock SchedulePatchService patchService;
    @Mock ScheduleDeleteService deleteService;
    @Mock PostGetService postGetService;
    @InjectMocks ScheduleUsecase usecase;

    @Test
    void deletingScheduleDetachesAllPostsIncludingReservations() {
        Schedule schedule = Schedule.builder().id(7L).build();
        Post published = Post.builder().schedule(schedule).hasSchedule(true).build();
        Post reserved = Post.builder().schedule(schedule).hasSchedule(true).isReserved(true).build();
        when(getService.getScheduleForUpdate(7L)).thenReturn(schedule);
        when(postGetService.getPostsByScheduleId(7L)).thenReturn(List.of(published, reserved));

        doAnswer(invocation -> {
            assertThat(published.getScheduleId()).isNull();
            assertThat(published.getHasSchedule()).isFalse();
            assertThat(reserved.getScheduleId()).isNull();
            assertThat(reserved.getHasSchedule()).isFalse();
            return null;
        }).when(deleteService).deleteSchedule(schedule);

        usecase.deleteSchedule(7L);
        verify(deleteService).deleteSchedule(schedule);
    }
}
