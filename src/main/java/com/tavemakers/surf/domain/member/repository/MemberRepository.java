package com.tavemakers.surf.domain.member.repository;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByIdAndStatus(Long memberId, MemberStatus status);

    List<Member> findAllByIdInAndStatus(List<Long> ids, MemberStatus status);

    boolean existsByEmail(String email);
  
    Optional<Member> findByEmail(String email);

    //현재 활동 중 + 특정 이름을 가진 회원 리스트 반환
    List<Member> findByActivityStatusAndNameAndStatusNot(Boolean activityStatus, String name, MemberStatus status);

    Optional<Member> findByEmailAndStatus(String email, MemberStatus status);
  
    Optional<Member> findByKakaoId(Long kakaoId);

    // 댓글(comment)에서 멘션할 회원을 검색할 때 사용
    @Query("""
        SELECT m
        FROM Member m
        LEFT JOIN m.tracks t
        WHERE m.status <> :status
              AND LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        GROUP BY m.id
        ORDER BY MAX(t.generation) DESC
    """)
    List<Member> findMentionCandidates(
            @Param("keyword") String keyword,
            @Param("status") MemberStatus status);

    @Query("""
        select m.id
        from Member m
        where m.activityStatus = true
          and m.status <> :status
    """)
    List<Long> findActiveMemberIdsExcludeStatus(@Param("status") MemberStatus status);

    boolean existsByIdAndStatusNot(Long id, MemberStatus status);

    @Query("select m from Member m where m.status = :status")
    Slice<Member> findByMemberListStatus(@Param("status") MemberStatus status, Pageable pageable);


    @Query("select m from Member m " +
            "left join fetch m.tracks " +
            "where m.id = :memberId")
    Optional<Member> findByIdWithTracks(@Param("memberId") Long memberId);

    long countByStatusAndIsDeletedFalse(MemberStatus status);

    @Query("""
        select m 
        from Member m 
        where m.id in :memberIds
    """)
    List<Member> findMembersByIds(@Param("memberIds") List<Long> memberIds);

}
