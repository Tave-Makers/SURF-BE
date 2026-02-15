package com.tavemakers.surf.domain.group.entity;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Group extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 기수 */
    @Column(nullable = false)
    private Integer generation;

    /** 스터디/프로젝트 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupType type;

    /** 그룹명 */
    @Column(nullable = false, length = 50)
    private String name;

    /** 그룹 소개 */
    @Column(length = 500)
    private String description;

    /** 팀장 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leader_member_id", nullable = false)
    private Member leader;

    /** 멤버십(그룹 내부 엔티티) - Group AR이 생명주기 관리 */
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<GroupMember> groupMembers = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private Group(Integer generation, GroupType type, String name, String description, Member leader) {
        this.generation = generation;
        this.type = type;
        this.name = name;
        this.description = description;
        this.leader = leader;
    }

    public static Group of(Integer generation, GroupType type, String name, String description, Member leader) {
        Group g = Group.builder()
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
        return groupMembers.size();
    }

    public void changeInfo(Integer generation, GroupType type, String name, String description
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
            throw new IllegalStateException("이미 그룹에 속한 멤버입니다.");
        }
        addMemberInternal(member);
    }

    public void removeMember(Long memberId) {
        if (leader.getId().equals(memberId)) {
            throw new IllegalStateException("그룹장은 제거할 수 없습니다.");
        }

        boolean removed = groupMembers.removeIf(gm -> gm.getMember().getId().equals(memberId));

        if (!removed) {
            throw new IllegalStateException("해당 멤버는 그룹에 존재하지 않습니다.");
        }
    }

    private boolean containsMember(Long memberId) {
        return groupMembers.stream()
                .anyMatch(gm -> gm.getMember().getId().equals(memberId));
    }

    private void addMemberInternal(Member member) {
        groupMembers.add(GroupMember.of(this, member));
    }
}