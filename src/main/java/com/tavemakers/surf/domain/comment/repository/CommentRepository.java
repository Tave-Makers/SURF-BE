package com.tavemakers.surf.domain.comment.repository;

import com.tavemakers.surf.domain.comment.entity.Comment;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByMemberId(Long memberId);

    /** 게시글 내 모든 댓글 + 대댓글 조회 (작성 시간순) */
    Slice<Comment> findByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);

    /** 댓글 총 개수 */
    long countByPostId(Long postId);

    /** 게시글 삭제 시 대댓글(자식) 먼저 삭제 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Comment c where c.post.id = :postId and c.parent is not null")
    void deleteRepliesByPostId(@Param("postId") Long postId);

    /** 게시글 삭제 시 루트 댓글 삭제 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Comment c where c.post.id = :postId and c.parent is null")
    void deleteRootCommentsByPostId(@Param("postId") Long postId);

    /** 댓글 작성자 ID 조회 */
    @Query("""
        select c.member.id
        from Comment c
        where c.id = :commentId
    """)
    Long findCommentOwnerId(@Param("commentId") Long commentId);

    /** 부모 댓글 삭제 시 자식 댓글의 parent 참조 해제 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
        update Comment c
        set c.parent = null
        where c.parent.id = :parentId
    """)
    void detachChildren(@Param("parentId") Long parentId);
}
