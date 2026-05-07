package com.tavemakers.surf.domain.post.repository;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.post.entity.PostLike;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Set;
import org.springframework.data.repository.query.Param;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPostIdAndMemberId(Long postId, Long memberId);


    @Query("""
           select pl.post.id
           from PostLike pl
           where pl.member.id = :memberId
           """)
    List<Long> findPostIdsByMemberId(@Param("memberId") Long memberId);

    @Query("""
           select pl.post.id
           from PostLike pl
           where pl.member.id = :memberId
             and pl.post.id in :postIds
           """)
    Set<Long> findLikedPostIdsByMemberAndPostIds(Long memberId, Collection<Long> postIds);

    long deleteByPostIdAndMemberId(Long postId, Long memberId);

    @Query("SELECT pl.member " +
            "FROM PostLike pl " +
            "WHERE pl.post.id = :postId")
    List<Member> findLikedMembersByPostId(@Param("postId") Long postId);

    void deleteByPostId(Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PostLike pl WHERE pl.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}
