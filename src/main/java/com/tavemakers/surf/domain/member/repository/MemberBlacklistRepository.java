package com.tavemakers.surf.domain.member.repository;

import com.tavemakers.surf.domain.member.entity.MemberBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberBlacklistRepository extends JpaRepository<MemberBlacklist, Long> {

    boolean existsByKakaoId(Long kakaoId);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);
}
