package com.tavemakers.surf.domain.member.domain.repository;

import com.tavemakers.surf.domain.auth.common.domain.enums.Provider;
import com.tavemakers.surf.domain.member.domain.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    /** provider + providerId 복합 식별자로 소셜 계정을 조회한다 (로그인 upsert 진입점). */
    Optional<SocialAccount> findByProviderAndProviderId(Provider provider, String providerId);
}
