package com.tavemakers.surf.domain.member.domain.repository;

import com.tavemakers.surf.domain.member.domain.entity.Career;
import com.tavemakers.surf.domain.member.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface CareerRepository extends JpaRepository<Career,Long> {

    List<Career> findAllByMemberAndIdIn(Member member,  Set<Long> ids);

    List<Career> findAllByMemberAndIdIn(Member member,  List<Long> ids);

    List<Career> findByMemberId(Long memberId);

}
