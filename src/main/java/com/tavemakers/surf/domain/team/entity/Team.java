package com.tavemakers.surf.domain.team.entity;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "team")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 기수 */
    @Column(nullable = false)
    private Integer generation;

    /** 스터디/프로젝트 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeamType type;

    /** 팀명 */
    @Column(nullable = false, length = 50)
    private String name;

    /** 팀 소개 */
    @Column(length = 500)
    private String description;

    /** 팀장 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leader_member_id", nullable = false)
    private Member leader;

    /** 팀원(팀 내부 엔티티) - Team AR이 생명주기 관리 */
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<TeamMember> teamMembers = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Team(Integer generation, TeamType type, String name, String description, Member leader) {
        this.generation = generation;
        this.type = type;
        this.name = name;
        this.description = description;
        this.leader = leader;
    }

    public static Team of(Integer generation, TeamType type, String name, String description, Member leader) {
        Team g = Team.builder()
                .generation(generation)
                .type(type)
                .name(name)
                .description(description)
                .leader(leader)
                .build();

        g.addMemberInternal(leader); // 불변조건 강제
        return g;
    }

    public int getMemberCount() {
        return teamMembers.size();
    }

    public void changeInfo(Integer generation, TeamType type, String name, String description
    ) {
        if (generation == null)
            throw new IllegalArgumentException("generation is required");
        if (type == null)
            throw new IllegalArgumentException("type is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name is required");
        if (description == null || description.isBlank())
            throw new IllegalArgumentException("description is required");

        this.generation = generation;
        this.type = type;
        this.name = name;
        this.description = description;
    }

    public void changeLeader(Member newLeader) {
        if (!containsMember(newLeader.getId())) {
            addMemberInternal(newLeader);
        }
        this.leader = newLeader;
    }

    public void addMember(Member member) {
        if (containsMember(member.getId())) {
            throw new IllegalStateException("이미 팀에 속한 멤버입니다.");
        }
        addMemberInternal(member);
    }

    public void removeMember(Long memberId) {
        boolean removed = teamMembers.removeIf(gm -> gm.getMember().getId().equals(memberId));

        if (!removed) {
            throw new IllegalStateException("해당 멤버는 팀에 존재하지 않습니다.");
        }
    }

    private boolean containsMember(Long memberId) {
        return teamMembers.stream()
                .anyMatch(gm -> gm.getMember().getId().equals(memberId));
    }

    private void addMemberInternal(Member member) {
        teamMembers.add(TeamMember.of(this, member));
    }
}