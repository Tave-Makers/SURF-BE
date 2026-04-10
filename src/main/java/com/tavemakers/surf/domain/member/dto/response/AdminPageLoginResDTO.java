package com.tavemakers.surf.domain.member.dto.response;

import com.tavemakers.surf.domain.member.entity.Member;
import lombok.Builder;

@Builder
public record AdminPageLoginResDTO(
        String accessToken,
        String username,
        String role
) {
    public static AdminPageLoginResDTO of(final String accessToken, Member member) {
        return AdminPageLoginResDTO.builder()
                .accessToken(accessToken)
                .username(member.getName())
                .role(member.getRole().name())
                .build();
    }
}
