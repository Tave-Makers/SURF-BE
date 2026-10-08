package com.tavemakers.surf.domain.schedule.repository;

import com.tavemakers.surf.application.post.query.PostGetService;
import com.tavemakers.surf.application.schedule.query.ScheduleGetService;
import com.tavemakers.surf.application.schedule.usecase.ScheduleUsecase;
import com.tavemakers.surf.domain.board.entity.*;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.*;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.repository.PostRepository;
import com.tavemakers.surf.domain.schedule.entity.Schedule;
import com.tavemakers.surf.domain.schedule.service.*;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DataJpaTest
class NoticeSchedulePersistenceTest {
    @Autowired EntityManager em;
    @Autowired PostRepository posts;
    @Autowired ScheduleRepository schedules;
    private Board board;
    private BoardCategory category;
    private Member writer;

    @BeforeEach
    void setup() {
        board = Board.of("공지", BoardType.NOTICE);
        em.persist(board);
        category = BoardCategory.of(board, "행사", "event");
        em.persist(category);
        writer = member("writer");
    }

    private Member member(String prefix) {
        Member result = Member.builder().name(prefix).email(prefix + "@test.com")
                .phoneNumber(prefix).status(MemberStatus.APPROVED).role(MemberRole.ADMIN)
                .memberType(MemberType.YB).activityStatus(true).build();
        em.persist(result);
        return result;
    }

    private Schedule schedule() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 20, 14, 0);
        return schedules.save(Schedule.from("regular", "만남의 장", start, start.plusHours(4), "장소"));
    }

    private Post post(String title, Schedule schedule, boolean reserved, Member author) {
        Post post = Post.of(title, "본문", false, reserved, false, board, category, author);
        post.tagSchedule(schedule);
        return posts.save(post);
    }

    @Test
    void twoNoticesPersistAgainstSameScheduleAndDeletingNoticeKeepsSchedule() {
        Schedule schedule = schedule();
        Post a = post("공지 A", schedule, false, writer);
        Post b = post("공지 B", schedule, false, writer);
        Long scheduleId = schedule.getId();
        Long bId = b.getId();
        em.flush();
        posts.delete(a);
        em.flush();
        em.clear();
        assertThat(schedules.existsById(scheduleId)).isTrue();
        assertThat(posts.findById(bId).orElseThrow().getScheduleId()).isEqualTo(scheduleId);
        assertThat(posts.findAllBySchedule_Id(scheduleId)).hasSize(1);
    }

    @Test
    void replacingAndDetachingTagsPersistsWithoutDeletingSchedules() {
        Schedule first = schedule();
        Schedule second = schedule();
        Post post = post("공지", first, false, writer);
        post.tagSchedule(second);
        em.flush();
        em.clear();
        post = posts.findById(post.getId()).orElseThrow();
        assertThat(post.getScheduleId()).isEqualTo(second.getId());
        post.tagSchedule(null);
        em.flush();
        em.clear();
        Post detached = posts.findById(post.getId()).orElseThrow();
        assertThat(detached.getScheduleId()).isNull();
        assertThat(detached.getHasSchedule()).isFalse();
        assertThat(schedules.count()).isEqualTo(2);
    }

    @Test
    void publicNoticeQueryExcludesReservationsAndBlockedAuthorsAndOrdersLatestFirst() {
        Schedule schedule = schedule();
        Post first = post("공지 A", schedule, false, writer);
        Post second = post("공지 B", schedule, false, writer);
        post("예약 공지", schedule, true, writer);
        Member blocked = member("blocked");
        post("차단 공지", schedule, false, blocked);
        em.flush();
        em.clear();
        assertThat(posts.findPublishedByScheduleIds(List.of(schedule.getId()), Set.of(blocked.getId())))
                .extracting(Post::getId).containsExactly(second.getId(), first.getId());
    }

    @Test
    void deletingScheduleDetachesAllNoticesAndKeepsUnrelatedSchedule() {
        Schedule target = schedule();
        Schedule other = schedule();
        Post published = post("공지", target, false, writer);
        Post reserved = post("예약 공지", target, true, writer);
        Post unrelated = post("다른 일정 공지", other, false, writer);
        em.flush();

        PostGetService postQueries = mock(PostGetService.class);
        when(postQueries.getPostsByScheduleId(target.getId()))
                .thenAnswer(ignored -> posts.findAllBySchedule_Id(target.getId()));
        var usecase = new ScheduleUsecase(new ScheduleCreateService(schedules),
                new ScheduleGetService(schedules, postQueries), new SchedulePatchService(),
                new ScheduleDeleteService(schedules), postQueries);
        usecase.deleteSchedule(target.getId());
        em.flush();
        em.clear();

        assertThat(schedules.existsById(target.getId())).isFalse();
        for (Long id : List.of(published.getId(), reserved.getId())) {
            Post result = posts.findById(id).orElseThrow();
            assertThat(result.getScheduleId()).isNull();
            assertThat(result.getHasSchedule()).isFalse();
        }
        assertThat(posts.findById(unrelated.getId()).orElseThrow().getScheduleId()).isEqualTo(other.getId());
    }
}
