package com.tavemakers.surf.application.schedule.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

import com.tavemakers.surf.application.post.query.PostGetService;
import com.tavemakers.surf.application.schedule.query.ScheduleGetService;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.schedule.entity.Schedule;
import com.tavemakers.surf.domain.schedule.service.ScheduleCreateService;
import com.tavemakers.surf.domain.schedule.service.ScheduleDeleteService;
import com.tavemakers.surf.domain.schedule.service.SchedulePatchService;
import com.tavemakers.surf.presentation.schedule.dto.request.ScheduleCreateReqDTO;
import com.tavemakers.surf.presentation.schedule.dto.request.ScheduleUpdateReqDTO;
import com.tavemakers.surf.presentation.schedule.dto.response.ScheduleResDTO;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ScheduleUsecase 단위 테스트.
 * 도메인 서비스는 전부 mock 처리하고, usecase가 협력자에게 올바른 인자로 위임하는지
 * (DTO 원시값 전달, 조회한 엔티티 재사용, Post 상태 갱신 순서)만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class ScheduleUsecaseTest {

    @Mock
    private ScheduleCreateService scheduleCreateService;

    @Mock
    private ScheduleGetService scheduleGetService;

    @Mock
    private SchedulePatchService schedulePatchService;

    @Mock
    private ScheduleDeleteService scheduleDeleteService;

    @Mock
    private PostGetService postGetService;

    private ScheduleUsecase scheduleUsecase;

    private final LocalDateTime startAt = LocalDateTime.of(2026, 3, 1, 10, 0);
    private final LocalDateTime endAt = LocalDateTime.of(2026, 3, 1, 12, 0);

    @BeforeEach
    void setUp() {
        scheduleUsecase = new ScheduleUsecase(
                scheduleCreateService, scheduleGetService, schedulePatchService,
                scheduleDeleteService, postGetService);
    }

    @Test
    @DisplayName("createScheduleAtPost: 게시글을 조회해 일정을 생성한 뒤, 생성된 일정의 ID와 연동 여부를 게시글에 반영한다")
    void 게시글연동일정생성_후_게시글상태갱신() {
        Long postId = 1L;
        ScheduleCreateReqDTO dto = new ScheduleCreateReqDTO("regular", "제목", startAt, endAt, "장소");
        Post post = Mockito.mock(Post.class);
        Schedule savedSchedule = Mockito.mock(Schedule.class);

        given(postGetService.findPostById(postId)).willReturn(post);
        given(savedSchedule.getId()).willReturn(10L);
        given(scheduleCreateService.createScheduleAtPost(
                dto.category(), dto.title(), dto.startAt(), dto.endAt(), dto.location(), post))
                .willReturn(savedSchedule);

        scheduleUsecase.createScheduleAtPost(dto, postId);

        then(post).should().addScheduleId(10L);
        then(post).should().changeHasSchedule(true);
    }

    @Test
    @DisplayName("createScheduleSingle: DTO 원시값을 그대로 도메인 서비스에 전달한다")
    void 개별일정생성_DTO값_그대로_위임() {
        ScheduleCreateReqDTO dto = new ScheduleCreateReqDTO("other", "개별일정", startAt, endAt, "장소2");

        scheduleUsecase.createScheduleSingle(dto);

        then(scheduleCreateService).should().createScheduleSingle(
                "other", "개별일정", startAt, endAt, "장소2");
    }

    @Test
    @DisplayName("updateSchedule: id로 조회한 일정 엔티티에 DTO 값으로 수정을 위임한다")
    void 일정수정_조회한엔티티에_위임() {
        Long id = 5L;
        ScheduleUpdateReqDTO dto = new ScheduleUpdateReqDTO("regular", "새제목", startAt, endAt, "새장소");
        Schedule schedule = Mockito.mock(Schedule.class);
        given(scheduleGetService.getScheduleById(id)).willReturn(schedule);

        scheduleUsecase.updateSchedule(dto, id);

        then(schedulePatchService).should().updateSchedule(
                schedule, "regular", "새제목", startAt, endAt, "새장소");
    }

    @Test
    @DisplayName("deleteSchedule: id로 조회한 일정 엔티티를 그대로 삭제에 위임한다")
    void 일정삭제_조회한엔티티_삭제위임() {
        Long id = 7L;
        Schedule schedule = Mockito.mock(Schedule.class);
        given(scheduleGetService.getScheduleById(id)).willReturn(schedule);

        scheduleUsecase.deleteSchedule(id);

        then(scheduleDeleteService).should().deleteSchedule(schedule);
    }

    @Test
    @DisplayName("deleteScheduleAtPost: 일정 삭제 후 게시글의 일정 연동 상태를 해제하고 scheduleId를 null로 되돌린다")
    void 게시글연동일정삭제_후_게시글상태해제_순서보장() {
        Long postId = 2L;
        Long scheduleId = 9L;
        Schedule schedule = Mockito.mock(Schedule.class);
        Post post = Mockito.mock(Post.class);
        given(scheduleGetService.getScheduleById(scheduleId)).willReturn(schedule);
        given(postGetService.getPost(postId)).willReturn(post);

        scheduleUsecase.deleteScheduleAtPost(postId, scheduleId);

        InOrder inOrder = inOrder(scheduleDeleteService, schedulePatchService);
        inOrder.verify(scheduleDeleteService).deleteSchedule(schedule);
        inOrder.verify(schedulePatchService).updateHasSchedule(post, false);
        inOrder.verify(schedulePatchService).updateScheduleIdNull(post);
    }

    @Test
    @DisplayName("getScheduleByPost: 조회 결과 DTO를 그대로 반환한다 (대표 위임 검증)")
    void 게시글별일정조회_결과_그대로반환() {
        Long postId = 3L;
        ScheduleResDTO resDTO = new ScheduleResDTO(3L, "regular", "제목", startAt, endAt, "장소", true, postId);
        given(scheduleGetService.getScheduleSingleDTO(postId)).willReturn(resDTO);

        ScheduleResDTO result = scheduleUsecase.getScheduleByPost(postId);

        assertThat(result).isSameAs(resDTO);
    }
}
