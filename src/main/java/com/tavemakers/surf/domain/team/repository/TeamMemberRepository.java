package com.tavemakers.surf.domain.team.repository;

import com.tavemakers.surf.domain.team.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    /** 회원 제명 안전망 — Team 컬렉션 처리에서 누락된 team_member 행 직접 정리 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TeamMember tm WHERE tm.member.id = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}
