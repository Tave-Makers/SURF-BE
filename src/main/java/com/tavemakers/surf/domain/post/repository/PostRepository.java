package com.tavemakers.surf.domain.post.repository;

import com.tavemakers.surf.domain.post.entity.Post;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByMemberId(Long memberId);

    /** 게시글 행 잠금 조회 — postId 단위 직렬화가 필요한 작업(예약 변경)이 앵커로 사용 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :id")
    Optional<Post> findByIdForUpdate(@Param("id") Long id);

    Slice<Post> findByBoardId(Long boardId, Pageable pageable);

    Slice<Post> findByBoardIdAndCategoryId(Long boardId, Long categoryId, Pageable pageable);

    Slice<Post> findByBoardIdAndIsReservedFalse(Long boardId, Pageable pageable);

    Slice<Post> findByBoardIdAndCategoryIdAndIsReservedFalse(Long boardId, Long categoryId, Pageable pageable);

    boolean existsByCategoryId(Long categoryId);

    Slice<Post> findByMemberId(Long memberId, Pageable pageable);

    // ===== 차단 필터 변형 (이슈 #370) =====
    // 위 파생 메서드는 내부 작업·관리자 흐름용으로 그대로 두고, 사용자 조회용 변형을 따로 둔다.
    // excludedAuthorIds 는 BlockGetService.getMyBlockedMemberIds 가 돌려주는 값이라 절대 비어 있지 않다
    // (빈 컬렉션을 JPQL not in 에 넘기면 공급자·DB별로 렌더링이 갈려 목록이 통째로 비거나 문법 오류가 난다).

    /** 게시판 전체 — 차단 작성자 제외 (관리자 뷰) */
    @Query("""
        select p from Post p
        where p.board.id = :boardId
          and p.member.id not in :excludedAuthorIds
    """)
    Slice<Post> findByBoardIdExcludingAuthors(@Param("boardId") Long boardId,
                                              @Param("excludedAuthorIds") Set<Long> excludedAuthorIds,
                                              Pageable pageable);

    /** 게시판 전체 — 예약글 제외 + 차단 작성자 제외 (일반 사용자 뷰) */
    @Query("""
        select p from Post p
        where p.board.id = :boardId
          and p.isReserved = false
          and p.member.id not in :excludedAuthorIds
    """)
    Slice<Post> findByBoardIdAndIsReservedFalseExcludingAuthors(
            @Param("boardId") Long boardId,
            @Param("excludedAuthorIds") Set<Long> excludedAuthorIds,
            Pageable pageable);

    /** 게시판 + 카테고리 — 차단 작성자 제외 (관리자 뷰) */
    @Query("""
        select p from Post p
        where p.board.id = :boardId
          and p.category.id = :categoryId
          and p.member.id not in :excludedAuthorIds
    """)
    Slice<Post> findByBoardIdAndCategoryIdExcludingAuthors(
            @Param("boardId") Long boardId,
            @Param("categoryId") Long categoryId,
            @Param("excludedAuthorIds") Set<Long> excludedAuthorIds,
            Pageable pageable);

    /** 게시판 + 카테고리 — 예약글 제외 + 차단 작성자 제외 (일반 사용자 뷰) */
    @Query("""
        select p from Post p
        where p.board.id = :boardId
          and p.category.id = :categoryId
          and p.isReserved = false
          and p.member.id not in :excludedAuthorIds
    """)
    Slice<Post> findByBoardIdAndCategoryIdAndIsReservedFalseExcludingAuthors(
            @Param("boardId") Long boardId,
            @Param("categoryId") Long categoryId,
            @Param("excludedAuthorIds") Set<Long> excludedAuthorIds,
            Pageable pageable);

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

    /** 특정 게시판 내 제목/내용 검색 (boardId 지정 검색용) */
    @Query("""
        select p from Post p
        where p.board.id = :boardId
          and (lower(p.title) like lower(concat('%', :param, '%'))
            or lower(p.content) like lower(concat('%', :param, '%')))
    """)
    Slice<Post> searchInBoard(@Param("boardId") Long boardId,
                              @Param("param") String param,
                              Pageable pageable);

    /** 통합 제목/내용 검색 — 차단 작성자 제외 */
    @Query("""
        select p from Post p
        where (lower(p.title) like lower(concat('%', :param, '%'))
            or lower(p.content) like lower(concat('%', :param, '%')))
          and p.member.id not in :excludedAuthorIds
    """)
    Slice<Post> searchExcludingAuthors(@Param("param") String param,
                                       @Param("excludedAuthorIds") Set<Long> excludedAuthorIds,
                                       Pageable pageable);

    /** 게시판 내 제목/내용 검색 — 차단 작성자 제외 */
    @Query("""
        select p from Post p
        where p.board.id = :boardId
          and (lower(p.title) like lower(concat('%', :param, '%'))
            or lower(p.content) like lower(concat('%', :param, '%')))
          and p.member.id not in :excludedAuthorIds
    """)
    Slice<Post> searchInBoardExcludingAuthors(@Param("boardId") Long boardId,
                                              @Param("param") String param,
                                              @Param("excludedAuthorIds") Set<Long> excludedAuthorIds,
                                              Pageable pageable);

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
