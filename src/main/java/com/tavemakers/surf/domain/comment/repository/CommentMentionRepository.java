package com.tavemakers.surf.domain.comment.repository;

import com.tavemakers.surf.domain.comment.entity.Comment;
import com.tavemakers.surf.domain.comment.entity.CommentMention;
import com.tavemakers.surf.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CommentMentionRepository extends JpaRepository<CommentMention, Long> {

    /** 특정 댓글 ID로 연결된 모든 멘션 조회 (mentionedMember 즉시 로딩 포함) */
    @Query("SELECT cm FROM CommentMention cm JOIN FETCH cm.mentionedMember WHERE cm.comment.id = :commentId")
    List<CommentMention> findByCommentIdWithMember(@Param("commentId") Long commentId);

    /** 댓글 ID 목록으로 멘션 일괄 조회 (N+1 방지) */
    @Query("SELECT cm FROM CommentMention cm JOIN FETCH cm.mentionedMember WHERE cm.comment.id IN :commentIds")
    List<CommentMention> findAllByCommentIdIn(@Param("commentIds") List<Long> commentIds);

    /** 특정 회원이 멘션된 CommentMention 목록 조회 */
    List<CommentMention> findAllByMentionedMember(Member member);

    /** 특정 댓글에 달린 멘션 데이터를 모두 삭제 (JPQL 명시) */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CommentMention cm WHERE cm.comment = :comment")
    void deleteAllByComment(@Param("comment") Comment comment);

    /** 게시글의 모든 댓글 멘션 삭제 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CommentMention cm WHERE cm.comment.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);
}
