package com.tavemakers.surf.domain.notification.entity;

import com.tavemakers.surf.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 수신자 */
    @Column(nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    /** 알림 변수 데이터 */
    @Column(nullable = false, columnDefinition = "json")
    private String payload;

    /** 읽음 여부 */
    @Column(nullable = false)
    private boolean isRead = false;


    @Builder
    private Notification(Long memberId, NotificationType type, String payload) {
        this.memberId = memberId;
        this.type = type;
        this.payload = payload;
    }

    public static Notification of(Long memberId, NotificationType type, String payload) {
        return Notification.builder()
                .memberId(memberId)
                .type(type)
                .payload(payload)
                .build();
    }

    public void read() {
        this.isRead = true;
    }
}