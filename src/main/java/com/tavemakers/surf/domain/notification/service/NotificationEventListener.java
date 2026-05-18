package com.tavemakers.surf.domain.notification.service;

import com.tavemakers.surf.domain.comment.event.CommentCreatedEvent;
import com.tavemakers.surf.domain.comment.event.CommentLikedEvent;
import com.tavemakers.surf.domain.comment.event.CommentReplyEvent;
import com.tavemakers.surf.domain.letter.event.LetterSentEvent;
import com.tavemakers.surf.domain.notification.entity.NotificationType;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.post.event.PostLikedEvent;
import com.tavemakers.surf.domain.post.service.post.PostGetService;
import com.tavemakers.surf.domain.post.service.support.PostPublishedEvent;
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
    private final NotificationCreateService notificationCreateService;
    private final LogEventEmitterImpl logEventEmitter;

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
    @EventListener
    public void handleCommentCreated(CommentCreatedEvent event) {
        notificationCreateService.createAndSend(
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
    @EventListener
    public void handleCommentReply(CommentReplyEvent event) {
        notificationCreateService.createAndSend(
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
    @EventListener
    public void handleCommentLiked(CommentLikedEvent event) {
        notificationCreateService.createAndSend(
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
    @EventListener
    public void handlePostLiked(PostLikedEvent event) {
        notificationCreateService.createAndSend(
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
    @EventListener
    public void handleLetterSent(LetterSentEvent event) {
        logEventEmitter.emit("letter_notification_requested", Map.of(
                "sender_id", event.getSenderId(),
                "receiver_id", event.getReceiverId(),
                "sender_name", event.getSenderName()
        ));
        try {
            notificationCreateService.createAndSend(
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
