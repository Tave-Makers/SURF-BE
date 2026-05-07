package com.tavemakers.surf.domain.comment.repository;

import com.tavemakers.surf.domain.comment.entity.Comment;
import com.tavemakers.surf.domain.comment.entity.CommentLike;
import com.tavemakers.surf.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    /** 특정 댓글에 좋아요를 누른 회원 목록 조회 */
    @Query("SELECT cl.member FROM CommentLike cl WHERE cl.comment.id = :commentId")
    List<Member> findMembersWhoLiked(@Param("commentId") Long commentId);

    @Query("""
        select cl
        from CommentLike cl
        join fetch cl.comment c
        where cl.member.id = :memberId
    """)
    List<CommentLike> findAllByMemberId(@Param("memberId") Long memberId);

    /** 특정 댓글에 특정 회원이 좋아요를 눌렀는지 여부 */
    boolean existsByCommentAndMember(Comment comment, Member member);

    /** 특정 댓글에 달린 전체 좋아요 개수 */
    long countByComment(Comment comment);

    /** 특정 댓글 + 회원 조합으로 좋아요 삭제 (1개 삭제 시 1, 없으면 0 반환) */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CommentLike cl WHERE cl.comment = :comment AND cl.member = :member")
    int deleteByCommentAndMember(@Param("comment") Comment comment, @Param("member") Member member);

    /** 특정 댓글에 달린 모든 좋아요 삭제 */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CommentLike cl WHERE cl.comment = :comment")
    void deleteAllByComment(@Param("comment") Comment comment);

    /** 게시글의 모든 댓글 좋아요 삭제 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);
}
