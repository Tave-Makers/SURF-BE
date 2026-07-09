package com.tavemakers.surf.presentation.member.dto.response;

import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.global.logging.LogPropsProvider;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class OnboardingCheckResDTO implements LogPropsProvider{

    public Long memberId;
    public Boolean needOnboarding;
    public MemberStatus memberStatus;
    public MemberRole memberRole;

    public static OnboardingCheckResDTO of(Long memberId, Boolean needOnboarding, MemberStatus memberStatus, MemberRole memberRole) {
        return OnboardingCheckResDTO.builder()
                .memberId(memberId)
                .needOnboarding(needOnboarding)
                .memberStatus(memberStatus)
                .memberRole(memberRole).build();
    }
    @Override
    public Map<String, Object> buildProps() {
        return Map.of(
                "need_onboarding", needOnboarding != null ? needOnboarding : false
        );
    }
}
