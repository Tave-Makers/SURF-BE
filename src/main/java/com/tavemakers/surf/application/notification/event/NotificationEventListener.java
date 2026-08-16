package com.tavemakers.surf.application.notification.event;

import com.tavemakers.surf.application.block.query.BlockGetService;
import com.tavemakers.surf.domain.comment.event.CommentCreatedEvent;
import com.tavemakers.surf.domain.comment.event.CommentLikedEvent;
import com.tavemakers.surf.domain.comment.event.CommentReplyEvent;
import com.tavemakers.surf.domain.letter.event.LetterSentEvent;
import com.tavemakers.surf.domain.notification.entity.NotificationType;
import com.tavemakers.surf.application.notification.usecase.NotificationUsecase;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.event.PostLikedEvent;
import com.tavemakers.surf.application.post.query.PostGetService;
import com.tavemakers.surf.domain.post.event.PostPublishedEvent;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.global.logging.LogEventEmitterImpl;
import java.util.Map;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final PostGetService postGetService;
    private final NotificationUsecase notificationUsecase;
    private final BlockGetService blockGetService;
    private final LogEventEmitterImpl logEventEmitter;

    /**
     * 차단 관계이면 개인 알림을 만들지 않는다.
     *
     * <p>어느 방향이든 차단이 있으면 막는다(쪽지와 동일한 상호작용 정책). 콘텐츠 숨김의
     * 단방향 필터와 달리, 알림은 직접 접촉이라 한쪽만 차단해도 접촉 경로가 남으면 안 된다.
     *
     * <p><b>가드는 반드시 알림 생성 이전에 둔다.</b> Notification을 저장하고 FCM만 막으면
     * 알림함 목록과 미읽음 뱃지에는 차단한 상대의 이름이 계속 쌓인다.
     *
     * <p>공지 알림은 개인 간 상호작용이 아니므로 이 가드를 적용하지 않는다.
     */
    private boolean blockedBetween(Long receiverId, Long actorId, NotificationType type) {
        if (!blockGetService.existsBetween(receiverId, actorId)) {
            return false;
        }
        log.info("[BlockedNotification] skipped type={} receiverId={} actorId={}", type, receiverId, actorId);
        return true;
    }

    @Async
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PostPublishedEvent event) {
        Post post = postGetService.readPost(event.getPostId());

        if (!post.getBoard().isNotice()) {
            return;
        }

        notificationUsecase.notifyNoticePost(post);

        log.info("[NoticeNotification] sent for postId={}", post.getId());
    }

    /**
     * 댓글 생성 알림 - 게시글 작성자에게
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentCreated(CommentCreatedEvent event) {
        if (blockedBetween(event.getReceiverId(), event.getActorId(), NotificationType.POST_COMMENT)) {
            return;
        }

        notificationUsecase.createAndSend(
                event.getReceiverId(),
                NotificationType.POST_COMMENT,
                Map.of(
                        "actorName", event.getActorName(),
                        "actorId", event.getActorId(),
                        "boardId", event.getBoardId(),
                        "postId", event.getPostId()
                )
        );
        log.info("[CommentNotification] sent for postId={}", event.getPostId());
    }

    /**
     * 대댓글 생성 알림 - 부모 댓글 작성자에게
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentReply(CommentReplyEvent event) {
        if (blockedBetween(event.getReceiverId(), event.getActorId(), NotificationType.COMMENT_REPLY)) {
            return;
        }

        notificationUsecase.createAndSend(
                event.getReceiverId(),
                NotificationType.COMMENT_REPLY,
                Map.of(
                        "actorName", event.getActorName(),
                        "actorId", event.getActorId(),
                        "boardId", event.getBoardId(),
                        "postId", event.getPostId()
                )
        );
        log.info("[CommentReplyNotification] sent for postId={}", event.getPostId());
    }

    /**
     * 댓글 좋아요 알림 - 댓글 작성자에게
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentLiked(CommentLikedEvent event) {
        if (blockedBetween(event.getReceiverId(), event.getActorId(), NotificationType.COMMENT_LIKE)) {
            return;
        }

        notificationUsecase.createAndSend(
                event.getReceiverId(),
                NotificationType.COMMENT_LIKE,
                Map.of(
                        "actorName", event.getActorName(),
                        "actorId", event.getActorId(),
                        "boardId", event.getBoardId(),
                        "postId", event.getPostId()
                )
        );
        log.info("[CommentLikeNotification] sent for postId={}", event.getPostId());
    }

    /**
     * 게시글 좋아요 알림 - 게시글 작성자에게
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostLiked(PostLikedEvent event) {
        if (blockedBetween(event.getReceiverId(), event.getActorId(), NotificationType.POST_LIKE)) {
            return;
        }

        notificationUsecase.createAndSend(
                event.getReceiverId(),
                NotificationType.POST_LIKE,
                Map.of(
                        "actorName", event.getActorName(),
                        "actorId", event.getActorId(),
                        "boardId", event.getBoardId(),
                        "postId", event.getPostId()
                )
        );
        log.info("[PostLikeNotification] sent for postId={}", event.getPostId());
    }

    /**
     * 쪽지 발송 알림 - 쪽지 수신자에게
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLetterSent(LetterSentEvent event) {
        // 방어선 — 쪽지는 발송 단계에서 이미 거부되므로 이 이벤트 자체가 발행되지 않아야 한다.
        // 여기서 걸린다면 LetterUsecase의 차단 가드가 우회된 것이므로 경고로 남긴다.
        if (blockedBetween(event.getReceiverId(), event.getSenderId(), NotificationType.MESSAGE)) {
            log.warn("[LetterNotification] 쪽지 발송 가드를 통과한 차단 관계 - receiverId={} senderId={}",
                    event.getReceiverId(), event.getSenderId());
            return;
        }

        logEventEmitter.emit("letter_notification_requested", Map.of(
                "sender_id", event.getSenderId(),
                "receiver_id", event.getReceiverId(),
                "sender_name", event.getSenderName()
        ));
        try {
            notificationUsecase.createAndSend(
                    event.getReceiverId(),
                    NotificationType.MESSAGE,
                    Map.of(
                            "actorName", event.getSenderName(),
                            "actorId", event.getSenderId()
                    )
            );
            log.info("[LetterNotification] sent to receiverId={}", event.getReceiverId());
            logEventEmitter.emit("letter_notification_succeeded", Map.of(
                    "receiver_id", event.getReceiverId(),
                    "sender_id", event.getSenderId(),
                    "delivered", true
            ));
        } catch (Exception e) {
            log.error("[LetterNotification] failed for receiverId={}", event.getReceiverId(), e);
            logEventEmitter.emitError("letter_notification_failed", Map.of(
                    "receiver_id", event.getReceiverId(),
                    "sender_id", event.getSenderId(),
                    "fail_reason", e.getMessage() != null ? e.getMessage() : "unknown"
            ), "쪽지 알림 발송 실패");
        } finally {
            logEventEmitter.flush(); // 비동기 스레드: 요청 필터 밖이므로 수동 flush
        }
    }
}
