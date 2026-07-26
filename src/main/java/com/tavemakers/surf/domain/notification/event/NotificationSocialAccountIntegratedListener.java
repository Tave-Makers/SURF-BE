package com.tavemakers.surf.domain.notification.event;

import com.tavemakers.surf.domain.member.event.SocialAccountIntegratedEvent;
import com.tavemakers.surf.domain.notification.repository.DeviceTokenRepository;
import com.tavemakers.surf.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 계정 통합 시 notification 도메인 데이터 정리 — 통합 트랜잭션에 동기 참여한다 (§3.6.3).
 * 실패하면 통합 전체가 롤백되어야 하므로 @Async/AFTER_COMMIT을 쓰지 않는다.
 */
@Component
@RequiredArgsConstructor
public class NotificationSocialAccountIntegratedListener {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    /** 통합으로 삭제되는 임시 회원의 알림·디바이스 토큰 삭제 */
    @EventListener
    public void onSocialAccountIntegrated(SocialAccountIntegratedEvent event) {
        notificationRepository.deleteByMemberId(event.tempMemberId());
        deviceTokenRepository.deleteByMemberId(event.tempMemberId());
    }
}
