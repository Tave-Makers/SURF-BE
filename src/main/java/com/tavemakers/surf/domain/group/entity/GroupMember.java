package com.tavemakers.surf.domain.group.entity;

import com.tavemakers.surf.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "group_member",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_group_member",
                columnNames = {"group_id", "member_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소유자: Group */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    /** 참여자: Member */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private GroupMember(Group group, Member member) {
        this.group = group;
        this.member = member;
    }

    public static GroupMember of(Group group, Member member) {
        return new GroupMember(group, member);
    }
}