package com.tavemakers.surf.domain.comment.repository;

import com.tavemakers.surf.domain.comment.entity.Comment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("select c from Comment c join fetch c.post where c.member.id = :memberId")
    List<Comment> findAllByMemberId(@Param("memberId") Long memberId);

    /** 게시글 내 모든 댓글 + 대댓글 조회 (작성 시간순) */
    Slice<Comment> findByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);

    /** 댓글 총 개수 */
    long countByPostId(Long postId);

    // ===== 차단 필터 변형 (이슈 #370) =====
    // 대댓글은 별도 테이블이 아니라 같은 Slice 의 depth>0 행이므로 이 두 쿼리로 함께 걸러진다.
    // Slice 와 count 에 반드시 같은 집합을 넘겨야 한다 — 다르면 "댓글 3개"인데 0개가 보이는 불일치가 생긴다.
    // excludedAuthorIds 는 BlockGetService.getMyBlockedMemberIds 가 돌려주는 값이라 절대 비어 있지 않다.

    /** 게시글 내 댓글 + 대댓글 조회 (작성 시간순) — 차단 작성자 제외 */
    @Query("""
        select c from Comment c
        where c.post.id = :postId
          and c.member.id not in :excludedAuthorIds
        order by c.createdAt asc
    """)
    Slice<Comment> findByPostIdExcludingAuthors(@Param("postId") Long postId,
                                                @Param("excludedAuthorIds") Set<Long> excludedAuthorIds,
                                                Pageable pageable);

    /** 댓글 총 개수 — 차단 작성자 제외 */
    @Query("""
        select count(c) from Comment c
        where c.post.id = :postId
          and c.member.id not in :excludedAuthorIds
    """)
    long countByPostIdExcludingAuthors(@Param("postId") Long postId,
                                       @Param("excludedAuthorIds") Set<Long> excludedAuthorIds);

    /**
     * 직전 중복 댓글 감지 — 같은 작성자가 같은 게시글·같은 부모에 같은 내용을
     * 짧은 시간 안에 다시 보낸 경우(더블 클릭·네트워크 재시도) true.
     */
    @Query("""
        select count(c) > 0
        from Comment c
        where c.post.id = :postId
          and c.member.id = :memberId
          and c.content = :content
          and ((:parentId is null and c.parent is null) or c.parent.id = :parentId)
          and c.createdAt > :after
    """)
    boolean existsRecentDuplicate(@Param("postId") Long postId,
                                  @Param("memberId") Long memberId,
                                  @Param("parentId") Long parentId,
                                  @Param("content") String content,
                                  @Param("after") LocalDateTime after);

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

    /** 좋아요 수 원자적 증가 (동시 요청 lost update 방지) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Comment c set c.likeCount = c.likeCount + 1 where c.id = :commentId")
    void increaseLikeCount(@Param("commentId") Long commentId);

    /** 좋아요 수 원자적 감소 (0 미만 방지) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Comment c set c.likeCount = c.likeCount - 1 where c.id = :commentId and c.likeCount > 0")
    void decreaseLikeCount(@Param("commentId") Long commentId);
}
