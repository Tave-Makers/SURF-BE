package com.tavemakers.surf.domain.comment.domain.entity;

import com.tavemakers.surf.domain.member.domain.entity.Member;
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
        name = "comment_mention",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"comment_id", "mentioned_member_id"})
        }
) // 한 댓글에 중복으로 같은 회원 멘션 금지
public class CommentMention extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어떤 댓글에 속한 멘션인지 (Mentions → Comment 단방향) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    /** 멘션된 회원 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentioned_member_id", nullable = false)
    private Member mentionedMember;

    @Builder
    private CommentMention(Comment comment, Member mentionedMember) {
        this.comment = comment;
        this.mentionedMember = mentionedMember;
    }

    public static CommentMention of(Comment comment, Member mentionedMember) {
        return CommentMention.builder()
                .comment(comment)
                .mentionedMember(mentionedMember)
                .build();
    }
}
