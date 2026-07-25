package com.tavemakers.surf.domain.member.entity;

import com.tavemakers.surf.domain.member.entity.enums.MemberBlacklistActionType;
import com.tavemakers.surf.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        indexes = {
                @Index(name = "idx_member_blacklist_email", columnList = "email"),
                @Index(name = "idx_member_blacklist_phone_number", columnList = "phone_number")
        }
)
public class MemberBlacklist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_blacklist_id")
    private Long id;

    private Long memberId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberBlacklistActionType actionType;

    @Column(nullable = false)
    private Long processedBy;

    @Builder
    private MemberBlacklist(
            Long memberId,
            String name,
            String email,
            String phoneNumber,
            MemberBlacklistActionType actionType,
            Long processedBy
    ) {
        this.memberId = memberId;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.actionType = actionType;
        this.processedBy = processedBy;
    }

    /** 재가입 차단 기준은 통합 이메일/전화번호(회원 기준)다 — provider 식별자(kakaoId 등)는 저장하지 않는다 (5.A-8). */
    public static MemberBlacklist of(
            Member member,
            MemberBlacklistActionType actionType,
            Long processedBy,
            String normalizedEmail,
            String normalizedPhoneNumber
    ) {
        return MemberBlacklist.builder()
                .memberId(member.getId())
                .name(member.getName())
                .email(normalizedEmail)
                .phoneNumber(normalizedPhoneNumber)
                .actionType(actionType)
                .processedBy(processedBy)
                .build();
    }
}
