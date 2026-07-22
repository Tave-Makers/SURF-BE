package com.tavemakers.surf.domain.notification.event;

import com.tavemakers.surf.domain.member.event.SocialAccountIntegratedEvent;
import com.tavemakers.surf.domain.notification.repository.DeviceTokenRepository;
import com.tavemakers.surf.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class NotificationSocialAccountIntegratedListenerTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private DeviceTokenRepository deviceTokenRepository;

    @InjectMocks private NotificationSocialAccountIntegratedListener listener;

    @Test
    @DisplayName("통합 이벤트 수신 시 삭제되는 임시 회원의 알림·디바이스 토큰을 함께 삭제한다")
    void onSocialAccountIntegrated_deletesNotificationData() {
        listener.onSocialAccountIntegrated(new SocialAccountIntegratedEvent(2L));

        then(notificationRepository).should().deleteByMemberId(2L);
        then(deviceTokenRepository).should().deleteByMemberId(2L);
    }
}
