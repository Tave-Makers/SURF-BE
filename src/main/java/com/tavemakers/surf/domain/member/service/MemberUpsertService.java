package com.tavemakers.surf.domain.member.service;

import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfo;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberUpsertService {

    private final MemberRepository memberRepository;

    /**
     * Finds a Member by the Kakao OAuth ID from the provided info, or creates and persists a new registering Member using that info.
     *
     * @param info OAuth user information whose `oauthId()` is used to identify the member
     * @return the existing or newly persisted Member corresponding to the Kakao ID
     * @throws org.springframework.dao.DataIntegrityViolationException if a data integrity conflict occurs during save and no Member can be found afterward
     */
    @Transactional
    public Member upsertRegisteringFromKakao(OAuthUserInfo info) {
        Long kakaoId = Long.parseLong(info.oauthId());
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