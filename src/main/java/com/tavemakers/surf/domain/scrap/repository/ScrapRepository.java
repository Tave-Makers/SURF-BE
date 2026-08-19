package com.tavemakers.surf.domain.scrap.repository;

import com.tavemakers.surf.domain.post.entity.Post;
import com.tavemakers.surf.domain.scrap.entity.Scrap;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    boolean existsByMemberIdAndPostId(Long memberId, Long postId);

    int deleteByMemberIdAndPostId(Long memberId, Long postId);

    @Query("""
           select s.post.id
           from Scrap s
           where s.member.id = :memberId
           """)
    List<Long> findPostIdsByMemberId(Long memberId);

    /** 차단 작성자를 제외하고 스크랩한 순서로 게시글을 조회한다 */
    @Query(
            value = """
            select p
            from Scrap s
            join s.post p
            join fetch p.member
            join fetch p.board
            left join fetch p.category
            where s.member.id = :memberId
              and p.member.id not in :excludedAuthorIds
            order by s.createdAt desc
            """
    )
    Slice<Post> findPostsByMemberIdExcludingAuthors(Long memberId,
                                                    Set<Long> excludedAuthorIds,
                                                    Pageable pageable);

    @Query("""
           select s.post.id
           from Scrap s
           where s.member.id = :memberId
             and s.post.id in :postIds
           """)
    Set<Long> findScrappedPostIdsByMemberAndPostIds(Long memberId, Collection<Long> postIds);

    void deleteByPostId(Long postId);

    void deleteByMemberId(Long memberId);
}
