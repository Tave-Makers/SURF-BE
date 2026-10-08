package com.tavemakers.surf.application.schedule.query;

import com.tavemakers.surf.application.post.query.PostGetService;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.schedule.entity.Schedule;
import com.tavemakers.surf.domain.schedule.exception.ScheduleNotFoundException;
import com.tavemakers.surf.domain.schedule.repository.ScheduleRepository;
import com.tavemakers.surf.presentation.schedule.dto.response.SchedulePostResDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ScheduleGetServiceTest {
    private final ScheduleRepository repository = mock(ScheduleRepository.class);
    private final PostGetService posts = mock(PostGetService.class);
    private final ScheduleGetService service = new ScheduleGetService(repository, posts);
    private final Schedule schedule = Schedule.builder().id(5L).title("만남의 장").build();

    @Test
    void monthlyCalendarReturnsAllNoticeTitlesAndStandaloneSchedules() {
        Schedule standalone = Schedule.builder().id(6L).title("독립 일정").build();
        when(repository.findByStartAtBetweenOrderByStartAtAscIdAsc(any(), any()))
                .thenReturn(List.of(schedule, standalone));
        Post a = Post.builder().id(11L).title("공지 A").schedule(schedule).build();
        Post b = Post.builder().id(12L).title("공지 B").schedule(schedule).build();
        when(posts.getPublishedPostsByScheduleIds(List.of(5L, 6L))).thenReturn(List.of(b, a));

        var result = service.getScheduleMonthly("ADMIN", 2026, 6).scheduleResDTOList();
        assertThat(result.get(0).posts()).containsExactly(
                new SchedulePostResDTO(12L, "공지 B"), new SchedulePostResDTO(11L, "공지 A"));
        assertThat(result.get(0).mappedByPost()).isTrue();
        assertThat(result.get(1).posts()).isEmpty();
        assertThat(result.get(1).mappedByPost()).isFalse();
        verify(posts, times(1)).getPublishedPostsByScheduleIds(anyList());
    }

    @Test
    void ordinaryMembersKeepCategoryFilter() {
        when(repository.findByStartAtBetweenAndCategoryInOrderByStartAtAscIdAsc(
                any(), any(), eq(List.of("regular", "other")))).thenReturn(List.of());
        assertThat(service.getScheduleMonthly("MEMBER", 2026, 6).scheduleResDTOList()).isEmpty();
        verifyNoInteractions(posts);
    }

    @Test
    void postScheduleLookupUsesPostReference() {
        when(posts.findPostById(11L)).thenReturn(Post.builder().schedule(schedule).build());
        when(repository.findById(5L)).thenReturn(Optional.of(schedule));
        when(posts.getPublishedPostsByScheduleIds(List.of(5L))).thenReturn(List.of());
        assertThat(service.getScheduleSingleDTO(11L).scheduleId()).isEqualTo(5L);
        when(posts.findPostById(12L)).thenReturn(Post.builder().build());
        assertThatThrownBy(() -> service.getScheduleSingleDTO(12L))
                .isInstanceOf(ScheduleNotFoundException.class);
    }
}
