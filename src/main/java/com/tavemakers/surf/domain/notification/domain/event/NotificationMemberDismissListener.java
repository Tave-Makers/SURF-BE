package com.tavemakers.surf.domain.notification.domain.event;

import com.tavemakers.surf.domain.member.domain.event.MemberDismissedEvent;
import com.tavemakers.surf.domain.notification.domain.repository.DeviceTokenRepository;
import com.tavemakers.surf.domain.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 회원 제명 시 notification 도메인 데이터 정리 — 제명 트랜잭션에 동기 참여한다 (D1).
 * 실패하면 제명 전체가 롤백되어야 하므로 @Async/AFTER_COMMIT을 쓰지 않는다.
 */
@Component
@RequiredArgsConstructor
public class NotificationMemberDismissListener {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    /** 제명 회원의 알림·디바이스 토큰 삭제 */
    @EventListener
    public void onMemberDismissed(MemberDismissedEvent event) {
        notificationRepository.deleteByMemberId(event.memberId());
        deviceTokenRepository.deleteByMemberId(event.memberId());
    }
}
