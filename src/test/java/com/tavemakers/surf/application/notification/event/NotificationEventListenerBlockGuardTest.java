package com.tavemakers.surf.application.notification.event;

import com.tavemakers.surf.application.block.query.BlockGetService;
import com.tavemakers.surf.application.notification.usecase.NotificationUsecase;
import com.tavemakers.surf.application.post.query.PostGetService;
import com.tavemakers.surf.domain.board.entity.Board;
import com.tavemakers.surf.domain.board.entity.BoardType;
import com.tavemakers.surf.domain.comment.event.CommentCreatedEvent;
import com.tavemakers.surf.domain.comment.event.CommentLikedEvent;
import com.tavemakers.surf.domain.comment.event.CommentReplyEvent;
import com.tavemakers.surf.domain.letter.event.LetterSentEvent;
import com.tavemakers.surf.domain.notification.entity.NotificationType;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.event.PostLikedEvent;
import com.tavemakers.surf.domain.post.event.PostPublishedEvent;
import com.tavemakers.surf.global.logging.LogEventEmitterImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * 개인 알림 4종 + 쪽지 방어선에 차단 가드가 걸리는지, 공지 알림은 가드에서 제외되는지 검증한다.
 *
 * <p>가드가 createAndSend 호출 자체를 건너뛰는지가 핵심이다. Notification을 저장하고 FCM만
 * 막으면 알림함과 미읽음 뱃지에 차단한 상대의 이름이 계속 쌓인다.
 */
@ExtendWith(MockitoExtension.class)
class NotificationEventListenerBlockGuardTest {

    private static final Long RECEIVER = 1L;
    private static final Long ACTOR = 2L;
    private static final Long BOARD_ID = 10L;
    private static final Long POST_ID = 20L;

    @Mock
    private PostGetService postGetService;
    @Mock
    private NotificationUsecase notificationUsecase;
    @Mock
    private BlockGetService blockGetService;
    @Mock
    private LogEventEmitterImpl logEventEmitter;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    @DisplayName("차단 관계이면 댓글 생성 알림을 만들지 않는다")
    void 차단시_댓글_알림이_생성되지_않는다() {
        givenBlocked();

        listener.handleCommentCreated(commentCreatedEvent());

        thenNoNotificationCreated();
    }

    @Test
    @DisplayName("차단 관계이면 대댓글 알림을 만들지 않는다")
    void 차단시_대댓글_알림이_생성되지_않는다() {
        givenBlocked();

        listener.handleCommentReply(commentReplyEvent());

        thenNoNotificationCreated();
    }

    @Test
    @DisplayName("차단 관계이면 댓글 좋아요 알림을 만들지 않는다")
    void 차단시_댓글_좋아요_알림이_생성되지_않는다() {
        givenBlocked();

        listener.handleCommentLiked(commentLikedEvent());

        thenNoNotificationCreated();
    }

    @Test
    @DisplayName("차단 관계이면 게시글 좋아요 알림을 만들지 않는다")
    void 차단시_게시글_좋아요_알림이_생성되지_않는다() {
        givenBlocked();

        listener.handlePostLiked(postLikedEvent());

        thenNoNotificationCreated();
    }

    @Test
    @DisplayName("쪽지 알림은 발송 단계에서 이미 막히지만 방어선으로 한 번 더 검사한다")
    void 차단시_쪽지_알림이_생성되지_않는다() {
        givenBlocked();

        listener.handleLetterSent(new LetterSentEvent(RECEIVER, "보낸사람", ACTOR));

        thenNoNotificationCreated();
        then(logEventEmitter).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("차단 관계가 없으면 기존 알림 흐름이 그대로 유지된다")
    void 차단이_없으면_알림이_정상_생성된다() {
        given(blockGetService.existsBetween(RECEIVER, ACTOR)).willReturn(false);

        listener.handleCommentCreated(commentCreatedEvent());
        listener.handleCommentReply(commentReplyEvent());
        listener.handleCommentLiked(commentLikedEvent());
        listener.handlePostLiked(postLikedEvent());

        then(notificationUsecase).should().createAndSend(eq(RECEIVER), eq(NotificationType.POST_COMMENT), any());
        then(notificationUsecase).should().createAndSend(eq(RECEIVER), eq(NotificationType.COMMENT_REPLY), any());
        then(notificationUsecase).should().createAndSend(eq(RECEIVER), eq(NotificationType.COMMENT_LIKE), any());
        then(notificationUsecase).should().createAndSend(eq(RECEIVER), eq(NotificationType.POST_LIKE), any());
    }

    @Test
    @DisplayName("공지 알림은 개인 간 상호작용이 아니므로 차단을 조회하지도 않는다")
    void 공지_알림은_차단_가드를_타지_않는다() {
        Post notice = Post.builder().board(Board.of("공지사항", BoardType.NOTICE)).build();
        given(postGetService.readPost(POST_ID)).willReturn(notice);

        listener.handle(new PostPublishedEvent(POST_ID));

        then(notificationUsecase).should().notifyNoticePost(notice);
        then(blockGetService).shouldHaveNoInteractions();
    }

    /** 방향 무관 차단 — 어느 쪽이 차단했든 existsBetween은 true다 */
    private void givenBlocked() {
        given(blockGetService.existsBetween(RECEIVER, ACTOR)).willReturn(true);
    }

    /** 저장 이전 단계에서 끊겼는지 — createAndSend가 호출되면 Notification이 남는다 */
    private void thenNoNotificationCreated() {
        then(notificationUsecase).should(never()).createAndSend(anyLong(), any(), any());
    }

    private CommentCreatedEvent commentCreatedEvent() {
        return new CommentCreatedEvent(RECEIVER, "댓글작성자", ACTOR, BOARD_ID, POST_ID);
    }

    private CommentReplyEvent commentReplyEvent() {
        return new CommentReplyEvent(RECEIVER, "대댓글작성자", ACTOR, BOARD_ID, POST_ID);
    }

    private CommentLikedEvent commentLikedEvent() {
        return new CommentLikedEvent(RECEIVER, "좋아요누른사람", ACTOR, BOARD_ID, POST_ID);
    }

    private PostLikedEvent postLikedEvent() {
        return new PostLikedEvent(RECEIVER, "좋아요누른사람", ACTOR, BOARD_ID, POST_ID);
    }
}
