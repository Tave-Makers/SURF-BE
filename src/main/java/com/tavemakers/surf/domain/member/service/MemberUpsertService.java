package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfoDTO;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberUpsertService {

    private final MemberRepository memberRepository;
    private final MemberBlacklistGetService memberBlacklistGetService;

    /** 카카오 정보로 회원 생성 또는 기존 회원 반환 */
    @Transactional
    public Member upsertRegisteringFromKakao(OAuthUserInfoDTO info) {
        Long kakaoId = Long.parseLong(info.oauthId());
        memberBlacklistGetService.validateNotBlacklisted(kakaoId, info.email(), null);

        return memberRepository.findByKakaoId(kakaoId).orElseGet(() -> {
            Member toSave = Member.createRegisteringFromKakao(info);
            try {
                return memberRepository.saveAndFlush(toSave);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                return memberRepository.findByKakaoId(kakaoId)
                        .orElseThrow(() -> e);
            }
        });
    }
}
