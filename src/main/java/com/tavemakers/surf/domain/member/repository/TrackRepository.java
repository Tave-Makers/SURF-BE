package com.tavemakers.surf.domain.member.repository;

import com.tavemakers.surf.domain.member.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrackRepository extends JpaRepository<Track, Long> {

    /**
     * 주어진 회원 ID 리스트에 대해, 각 회원의 '가장 최신 기수' 트랙만 조회합니다.
     * @param memberIds 조회할 회원들의 ID 리스트
     * @return 각 회원의 최신 트랙 리스트
     */
    @Query("SELECT t FROM Track t WHERE t.member.id IN :memberIds " +
            "AND t.generation = (SELECT MAX(t2.generation) FROM Track t2 WHERE t2.member = t.member)")
    List<Track> findLatestTracksByMemberIds(@Param("memberIds") List<Long> memberIds);

    /**
     * Track을 조회할 때 연관된 현재 활동 중인 Member도 함께 조회하되,
     * 각 회원의 '가장 최신 기수' 트랙만 조회하여 N+1 문제를 방지
     * @return 현재 활동 중인 회원들의 최신 트랙 리스트
     */
    @Query("SELECT t FROM Track t JOIN FETCH t.member m " +
            "WHERE m.activityStatus = true " +
            "AND m.status = com.tavemakers.surf.domain.member.entity.enums.MemberStatus.APPROVED " +
            "AND t.generation = (SELECT MAX(t2.generation) FROM Track t2 WHERE t2.member = t.member)")
    List<Track> findAllWithActiveMember();

    List<Track> findByMemberId(Long memberId);

    @Query("SELECT DISTINCT t.generation FROM Track t ORDER BY t.generation DESC")
    List<Integer> findAllDistinctGenerations();

    @Query("""
        select t
          from Track t
         where t.member.id in :memberIds
    """)
    List<Track> findAllByMemberIds(@Param("memberIds") List<Long> memberIds);
}
