package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfoDTO;
import com.tavemakers.surf.domain.auth.common.enums.Provider;
import com.tavemakers.surf.domain.auth.common.exception.EmailConflictException;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberUpsertService {

    private final MemberRepository memberRepository;

    /**
     * OAuth provider 정보로 회원 생성 또는 기존 회원 반환 (D1).
     * 동일 이메일이 다른 provider 로 이미 가입돼 있으면 EmailConflictException 발생 (D6).
     */
    @Transactional
    public Member upsertRegisteringFromOAuth(Provider provider, OAuthUserInfoDTO info) {
        return memberRepository.findByProviderAndProviderId(provider, info.oauthId())
                .orElseGet(() -> createWithEmailConflictGuard(provider, info));
    }

    private Member createWithEmailConflictGuard(Provider provider, OAuthUserInfoDTO info) {
        memberRepository.findByEmail(info.email()).ifPresent(existing -> {
            if (existing.getProvider() != provider) {
                throw new EmailConflictException(existing.getProvider());
            }
        });
        Member toSave = Member.createRegisteringFromOAuth(provider, info);
        try {
            return memberRepository.saveAndFlush(toSave);
        } catch (DataIntegrityViolationException e) {
            return memberRepository.findByProviderAndProviderId(provider, info.oauthId())
                    .orElseThrow(() -> e);
        }
    }
}
