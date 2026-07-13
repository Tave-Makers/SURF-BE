package com.tavemakers.surf.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.tavemakers.surf.domain.notification.entity.Notification;
import com.tavemakers.surf.domain.notification.entity.NotificationType;
import com.tavemakers.surf.domain.notification.exception.NotificationNotFoundException;
import com.tavemakers.surf.domain.notification.repository.NotificationRepository;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * NotificationService.markAsRead 단위 테스트.
 * 본인 소유 알림만 조회·갱신하며, 읽음 전이 전 상태를 로그로 남기는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private LogEventEmitter logEventEmitter;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("본인 소유가 아니거나 존재하지 않는 알림이면 NotificationNotFoundException을 던지고 갱신하지 않는다")
    void markAsRead_소유하지_않은_알림이면_예외를_던진다() {
        given(notificationRepository.findByIdAndMemberId(1L, 99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 99L))
                .isInstanceOf(NotificationNotFoundException.class);

        then(notificationRepository).should(never()).markAsRead(any(), any());
        then(logEventEmitter).should(never()).emit(any(), any());
    }

    @Test
    @DisplayName("읽지 않은 알림을 읽음 처리하면 갱신 전 상태(false)를 previous_is_read로 로그에 남긴다")
    void markAsRead_읽음처리_전_상태를_로그로_남긴다() {
        Notification notification = Notification.of(1L, NotificationType.POST_LIKE, "{}");
        given(notificationRepository.findByIdAndMemberId(10L, 1L)).willReturn(Optional.of(notification));

        notificationService.markAsRead(10L, 1L);

        then(notificationRepository).should().markAsRead(10L, 1L);
        then(logEventEmitter).should().emit(
                eq("notification.read"),
                eq(Map.of(
                        "notification_id", 10L,
                        "previous_is_read", false,
                        "current_is_read", true
                ))
        );
    }
}
