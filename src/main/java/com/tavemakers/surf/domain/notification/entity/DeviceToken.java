package com.tavemakers.surf.domain.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 알림 수신자 */
    @Column(nullable = false)
    private Long memberId;

    /** FCM 토큰 */
    @Column(nullable = false, unique = true, length = 255)
    private String token;

    /** 플랫폼 (WEB / ANDROID / IOS) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Platform platform;

    /** 사용 여부 */
    @Column(nullable = false)
    private boolean enabled = true;

    /** 마지막 사용 시각 */
    private LocalDateTime lastSeenAt;

    @Builder
    private DeviceToken(Long memberId, String token, Platform platform) {
        this.memberId = memberId;
        this.token = token;
        this.platform = platform;
        this.enabled = true;
        this.lastSeenAt = LocalDateTime.now();
    }

    public void touch() {
        this.lastSeenAt = LocalDateTime.now();
        this.enabled = true;
    }

    public void updateOwner(Long newMemberId){
        this.memberId = newMemberId;
        this.touch();
    }
}