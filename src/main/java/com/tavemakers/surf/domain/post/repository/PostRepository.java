package com.tavemakers.surf.domain.post.repository;

import com.tavemakers.surf.domain.post.entity.Post;
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

    Slice<Post> findByMemberId(Long memberId, Pageable pageable);

    @Query("select p.version from Post p where p.id = :id")
    Long findVersionById(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.scrapCount = p.scrapCount + 1, p.version = p.version + 1 " +
            "where p.id = :id and p.version = :version")
    int increaseScrapCount(@Param("id") Long id, @Param("version") Long version);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.scrapCount = p.scrapCount - 1, p.version = p.version + 1 " +
            "where p.id = :id and p.version = :version and p.scrapCount > 0")
    int decreaseScrapCount(@Param("id") Long id, @Param("version") Long version);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.likeCount = p.likeCount + 1, p.version = p.version + 1 " +
            "where p.id = :id and p.version = :version")
    int increaseLikeCount(@Param("id") Long postId, @Param("version") Long version);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.likeCount = p.likeCount - 1, p.version   = p.version + 1" +
            " where p.id = :id and p.version = :version and p.likeCount > 0")
    int decreaseLikeCount(@Param("id") Long postId, @Param("version") Long version);

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
        set p.commentCount = p.commentCount - 1
        where p.id = :postId
          and p.commentCount > 0
    """)
    void decreaseCommentCount(@Param("postId") Long postId);
}
