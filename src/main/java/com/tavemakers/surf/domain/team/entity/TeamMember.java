package com.tavemakers.surf.domain.team.entity;

import com.tavemakers.surf.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "team_member",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_team_member",
                columnNames = {"team_id", "member_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소유자: Team */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /** 참여자: Member */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private TeamMember(Team team, Member member) {
        this.team = team;
        this.member = member;
    }

    public static TeamMember of(Team team, Member member) {
        return new TeamMember(team, member);
    }
}