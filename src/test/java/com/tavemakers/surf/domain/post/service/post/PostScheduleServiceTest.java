package com.tavemakers.surf.domain.post.service.post;

import com.tavemakers.surf.application.schedule.query.ScheduleGetService;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.exception.InvalidScheduleTagException;
import com.tavemakers.surf.domain.schedule.entity.Schedule;
import com.tavemakers.surf.domain.schedule.exception.ScheduleNotFoundException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostScheduleServiceTest {
    private final ScheduleGetService schedules = mock(ScheduleGetService.class);
    private final PostScheduleService service = new PostScheduleService(schedules);
    private final Schedule first = Schedule.builder().id(5L).build();

    private Post notice() {
        return Post.builder().board(Board.of("공지", BoardType.NOTICE)).hasSchedule(false).build();
    }

    @Test
    void multipleNoticesCanTagSameScheduleAndDetachingOneKeepsOther() {
        when(schedules.getScheduleForUpdate(5L)).thenReturn(first);
        Post a = notice();
        Post b = notice();
        service.updateScheduleTag(a, 5L, null);
        service.updateScheduleTag(b, 5L, null);
        service.updateScheduleTag(a, null, false);
        assertThat(a.getScheduleId()).isNull();
        assertThat(a.getHasSchedule()).isFalse();
        assertThat(b.getScheduleId()).isEqualTo(5L);
        assertThat(b.getHasSchedule()).isTrue();
    }

    @Test
    void replacementAndOmittedFields() {
        Schedule second = Schedule.builder().id(6L).build();
        when(schedules.getScheduleForUpdate(6L)).thenReturn(second);
        Post post = notice();
        post.tagSchedule(first);
        service.updateScheduleTag(post, null, null);
        assertThat(post.getScheduleId()).isEqualTo(5L);
        service.updateScheduleTag(post, 6L, null);
        assertThat(post.getScheduleId()).isEqualTo(6L);
    }

    @Test
    void missingScheduleDoesNotChangeExistingTag() {
        when(schedules.getScheduleForUpdate(99L)).thenThrow(new ScheduleNotFoundException());
        Post post = notice();
        post.tagSchedule(first);
        assertThatThrownBy(() -> service.updateScheduleTag(post, 99L, null))
                .isInstanceOf(ScheduleNotFoundException.class);
        assertThat(post.getScheduleId()).isEqualTo(5L);
    }

    @Test
    void generalBoardCannotTagSchedule() {
        Post post = Post.builder().board(Board.of("자유", BoardType.GENERAL)).build();
        assertThatThrownBy(() -> service.updateScheduleTag(post, 5L, null))
                .isInstanceOf(InvalidScheduleTagException.class);
        verifyNoInteractions(schedules);
    }
}
