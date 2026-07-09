package com.tavemakers.surf.domain.badge.entity;

import com.tavemakers.surf.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "member_badge",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"member_id", "badge_id"})
        }
)

public class MemberBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_badge_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(nullable = false)
    private LocalDate awardedAt; //수여일자

    private MemberBadge(Member member, Badge badge) {
        this.member = member;
        this.badge = badge;
        this.awardedAt = LocalDate.now();
    }

    public static MemberBadge create(Member member, Badge badge) {
        return new MemberBadge(member, badge);
    }
}
