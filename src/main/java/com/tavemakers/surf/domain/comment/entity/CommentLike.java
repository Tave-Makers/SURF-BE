package com.tavemakers.surf.domain.comment.entity;

import com.tavemakers.surf.domain.member.domain.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "comment_like",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"comment_id", "member_id"})
        }
) // 같은 사용자가 같은 댓글에 여러 번 좋아요 누르는 것 방지
public class CommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private CommentLike(Comment comment, Member member) {
        this.comment = comment;
        this.member = member;
    }

    public static CommentLike of(Comment comment, Member member) {
        return new CommentLike(comment, member);
    }
}
