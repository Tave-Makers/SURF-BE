package com.tavemakers.surf.application.notification.usecase;

import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.application.notification.query.NotificationGetService;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.notification.entity.Notification;
import com.tavemakers.surf.domain.notification.entity.NotificationCategory;
import com.tavemakers.surf.domain.notification.entity.NotificationType;
import com.tavemakers.surf.domain.notification.event.NotificationCreatedEvent;
import com.tavemakers.surf.domain.notification.service.NotificationCreateService;
import com.tavemakers.surf.domain.notification.service.NotificationService;
import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.presentation.notification.dto.response.NotificationSliceResDTO;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 Usecase — 트랜잭션 경계를 소유하고 도메인 서비스/조회 결과를 표현형(DTO)으로 매핑한다.
 * 도메인 계층은 DTO를 알지 못한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationUsecase {

    private final MemberGetService memberGetService;
    private final NotificationGetService notificationGetService;
    private final NotificationService notificationService;
    private final NotificationCreateService notificationCreateService;
    private final ApplicationEventPublisher eventPublisher;

    /** 회원의 알림 목록 조회 (카테고리별 필터링 가능, 무한스크롤) */
    @Transactional(readOnly = true)
    public NotificationSliceResDTO getNotifications(Long memberId, NotificationCategory category, Pageable pageable) {
        return NotificationSliceResDTO.from(
                notificationGetService.getNotifications(memberId, category, pageable));
    }

    /** 알림 읽음 처리 */
    @Transactional
    public void markAsRead(Long notificationId, Long memberId) {
        notificationService.markAsRead(notificationId, memberId);
    }

    /**
     * 단건 알림 저장 + FCM 전송 (활성 회원에 한함)
     */
    @Transactional
    public void createAndSend(
            Long receiverId,
            NotificationType type,
            Map<String, Object> payload
    ) {
        boolean activeReceiver =
                memberGetService.existsByIdAndStatusNot(receiverId, MemberStatus.WITHDRAWN);
        if (!activeReceiver) return;

        Notification notification = notificationCreateService.create(receiverId, type, payload);

        eventPublisher.publishEvent(
                new NotificationCreatedEvent(notification.getId(), receiverId)
        );
    }

    /**
     * 공지 게시글 발행 시
     * 이번 활동 기수(또는 활성 유저) 전체에게 알림 전송
     */
    @Transactional
    public void notifyNoticePost(Post post) {

        // 방어 로직 (이중 체크)
        if (!post.getBoard().isNotice()) {
            return;
        }

        // 1 알림 대상 조회
        List<Long> targetIds = memberGetService.getActiveMemberIds();

        if (targetIds.isEmpty()) {
            log.info("[NoticeNotification] no target members, postId={}", post.getId());
            return;
        }

        // 2 Notification 엔티티 일괄 생성 (N+1 방지)
        notificationCreateService.createAndSendBulk(
                targetIds,
                NotificationType.NOTICE,
                Map.of(
                        "boardName", post.getBoard().getName(),
                        "boardId", post.getBoard().getId(),
                        "postId", post.getId()
                )
        );

        log.info(
                "[NoticeNotification] sent to {} members, postId={}",
                targetIds.size(),
                post.getId()
        );
    }
}
