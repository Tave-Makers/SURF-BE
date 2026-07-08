package com.tavemakers.surf.domain.post.domain.repository;

import com.tavemakers.surf.domain.post.domain.entity.Post;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByMemberId(Long memberId);

    Slice<Post> findByBoardId(Long boardId, Pageable pageable);

    Slice<Post> findByBoardIdAndCategoryId(Long boardId, Long categoryId, Pageable pageable);

    Slice<Post> findByBoardIdAndIsReservedFalse(Long boardId, Pageable pageable);

    Slice<Post> findByBoardIdAndCategoryIdAndIsReservedFalse(Long boardId, Long categoryId, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    Slice<Post> findByMemberId(Long memberId, Pageable pageable);

    /** 스크랩 수 원자적 증가 (동시 요청 lost update 방지) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.scrapCount = p.scrapCount + 1 where p.id = :id")
    void increaseScrapCount(@Param("id") Long id);

    /** 스크랩 수 원자적 감소 (0 미만 방지) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.scrapCount = p.scrapCount - 1 " +
            "where p.id = :id and p.scrapCount > 0")
    void decreaseScrapCount(@Param("id") Long id);

    /** 좋아요 수 원자적 증가 (동시 요청 lost update 방지) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.likeCount = p.likeCount + 1 where p.id = :id")
    void increaseLikeCount(@Param("id") Long postId);

    /** 좋아요 수 원자적 감소 (0 미만 방지) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.likeCount = p.likeCount - 1" +
            " where p.id = :id and p.likeCount > 0")
    void decreaseLikeCount(@Param("id") Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Post p SET p.likeCount = GREATEST(p.likeCount - 1, 0) WHERE p.id IN :postIds")
    void decreaseLikeCountBulk(@Param("postIds") List<Long> postIds);

    Slice<Post> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
            String title, String content, Pageable pageable);

    @Query("""
        select p.member.id
        from Post p
        where p.id = :postId
    """)
    Long findPostOwnerId(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Post p
        set p.commentCount = p.commentCount + 1
        where p.id = :postId
    """)
    void increaseCommentCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Post p
        set p.commentCount = p.commentCount - 1
        where p.id = :postId
          and p.commentCount > 0
    """)
    void decreaseCommentCount(@Param("postId") Long postId);
}
