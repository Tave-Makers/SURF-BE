package com.tavemakers.surf.domain.member.repository;

import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.member.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    /** 소셜 제공자와 제공자 식별자로 소셜 계정을 조회한다. */
    Optional<SocialAccount> findByProviderAndProviderId(Provider provider, String providerId);

    /** 회원에게 연결된 소셜 계정 목록을 조회한다. */
    List<SocialAccount> findAllByMemberId(Long memberId);
}
