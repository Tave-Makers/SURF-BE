package com.tavemakers.surf.domain.notification.service;

import com.tavemakers.surf.presentation.notification.dto.response.NotificationResDTO;
import com.tavemakers.surf.domain.notification.entity.Notification;
import com.tavemakers.surf.domain.notification.entity.NotificationCategory;
import com.tavemakers.surf.domain.notification.entity.NotificationType;
import com.tavemakers.surf.domain.notification.exception.NotificationNotFoundException;
import com.tavemakers.surf.domain.notification.repository.NotificationRepository;
import com.tavemakers.surf.application.notification.query.NotificationGetService;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationGetService notificationGetService;
    private final LogEventEmitter logEventEmitter;

    /** 회원의 알림 목록 조회 (카테고리별 필터링 가능) */
    @Transactional(readOnly = true)
    public List<NotificationResDTO> getNotifications(Long memberId, NotificationCategory category) {

        List<Notification> notifications;

        if (category == null) {
            // 전체 알림
            notifications = notificationRepository.findByMemberIdOrderByIdDesc(memberId);
        } else {
            // 해당 카테고리의 타입 목록 추출
            List<NotificationType> types = Arrays.stream(NotificationType.values())
                    .filter(t -> t.getCategory() == category)
                    .toList();

            notifications = notificationRepository.findByMemberIdAndTypeInOrderByIdDesc(memberId, types);
        }

        return notificationGetService.toDtoList(notifications);
    }

    /** 알림 읽음 처리 — 본인 소유 알림만 조회·갱신한다 */
    @Transactional
    public void markAsRead(Long notificationId, Long memberId) {
        Notification notification = notificationRepository.findByIdAndMemberId(notificationId, memberId)
                .orElseThrow(NotificationNotFoundException::new);
        boolean previousIsRead = notification.isRead();

        notificationRepository.markAsRead(notificationId, memberId);

        logEventEmitter.emit("notification.read", Map.of(
                "notification_id", notificationId,
                "previous_is_read", previousIsRead,
                "current_is_read", true
        ));
    }
}
