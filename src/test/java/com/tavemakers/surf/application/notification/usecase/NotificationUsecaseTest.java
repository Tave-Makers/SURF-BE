package com.tavemakers.surf.application.notification.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.application.notification.query.NotificationGetService;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.notification.entity.Notification;
import com.tavemakers.surf.domain.notification.entity.NotificationType;
import com.tavemakers.surf.domain.notification.event.NotificationCreatedEvent;
import com.tavemakers.surf.domain.notification.service.NotificationCreateService;
import com.tavemakers.surf.domain.notification.service.NotificationService;
import com.tavemakers.surf.domain.post.entity.Post;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * NotificationUsecase 단위 테스트.
 * 도메인 서비스/조회 협력자는 mock 처리하고, 활성 회원 분기·공지 발행 방어로직·payload 매핑을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class NotificationUsecaseTest {

    @Mock
    private MemberGetService memberGetService;

    @Mock
    private NotificationGetService notificationGetService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationCreateService notificationCreateService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NotificationUsecase notificationUsecase;

    @Test
    @DisplayName("createAndSend는 탈퇴한(비활성) 수신자면 알림을 생성하지도, 이벤트를 발행하지도 않는다")
    void createAndSend_비활성_수신자면_아무일도_하지_않는다() {
        given(memberGetService.existsByIdAndStatusNot(5L, MemberStatus.WITHDRAWN)).willReturn(false);

        notificationUsecase.createAndSend(5L, NotificationType.POST_LIKE, Map.of());

        then(notificationCreateService).should(never()).create(any(), any(), any());
        then(eventPublisher).should(never()).publishEvent(any());
    }

    @Test
    @DisplayName("createAndSend는 활성 수신자면 알림을 생성하고 생성된 알림ID로 이벤트를 발행한다")
    void createAndSend_활성_수신자면_알림을_생성하고_이벤트를_발행한다() {
        given(memberGetService.existsByIdAndStatusNot(5L, MemberStatus.WITHDRAWN)).willReturn(true);
        Notification created = Notification.of(5L, NotificationType.POST_LIKE, "{}");
        ReflectionTestUtils.setField(created, "id", 500L);
        given(notificationCreateService.create(5L, NotificationType.POST_LIKE, Map.of("actorId", 1)))
                .willReturn(created);

        notificationUsecase.createAndSend(5L, NotificationType.POST_LIKE, Map.of("actorId", 1));

        ArgumentCaptor<NotificationCreatedEvent> captor = ArgumentCaptor.forClass(NotificationCreatedEvent.class);
        then(eventPublisher).should().publishEvent(captor.capture());
        assertThat(captor.getValue().notificationId()).isEqualTo(500L);
        assertThat(captor.getValue().receiverId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("notifyNoticePost는 공지 게시판이 아니면(방어로직) 대상 조회조차 하지 않는다")
    void notifyNoticePost_공지게시판이_아니면_무시한다() {
        Board generalBoard = Board.of("자유게시판", BoardType.GENERAL);
        Post post = Post.builder().board(generalBoard).build();

        notificationUsecase.notifyNoticePost(post);

        then(memberGetService).should(never()).getActiveMemberIds();
        then(notificationCreateService).should(never()).createAndSendBulk(any(), any(), any());
    }

    @Test
    @DisplayName("notifyNoticePost는 공지 게시판이어도 활성 대상이 없으면 알림을 생성하지 않는다")
    void notifyNoticePost_대상회원이_없으면_알림을_생성하지_않는다() {
        Board noticeBoard = Board.of("공지사항", BoardType.NOTICE);
        Post post = Post.builder().board(noticeBoard).build();
        given(memberGetService.getActiveMemberIds()).willReturn(List.of());

        notificationUsecase.notifyNoticePost(post);

        then(notificationCreateService).should(never()).createAndSendBulk(any(), any(), any());
    }

    @Test
    @DisplayName("notifyNoticePost는 공지 게시판이고 대상이 있으면 게시글 정보를 payload로 매핑해 일괄 생성한다")
    void notifyNoticePost_대상이_있으면_게시글_정보로_payload를_매핑해_일괄생성한다() {
        Board noticeBoard = Board.of("공지사항", BoardType.NOTICE);
        ReflectionTestUtils.setField(noticeBoard, "id", 7L);
        Post post = Post.builder().board(noticeBoard).build();
        ReflectionTestUtils.setField(post, "id", 42L);
        given(memberGetService.getActiveMemberIds()).willReturn(List.of(1L, 2L, 3L));

        notificationUsecase.notifyNoticePost(post);

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        then(notificationCreateService).should().createAndSendBulk(
                eq(List.of(1L, 2L, 3L)),
                eq(NotificationType.NOTICE),
                payloadCaptor.capture()
        );
        assertThat(payloadCaptor.getValue())
                .containsEntry("boardName", "공지사항")
                .containsEntry("boardId", 7L)
                .containsEntry("postId", 42L);
    }
}
