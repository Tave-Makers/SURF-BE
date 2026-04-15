package com.tavemakers.surf.domain.member.dto.response;

import com.tavemakers.surf.domain.member.entity.Member;
import lombok.Builder;

@Builder
public record AdminPageLoginResDTO(
        String accessToken,
        String username,
        String role
) {
    /**
     * Create an AdminPageLoginResDTO containing the provided access token and the given member's identity.
     *
     * @param accessToken the access token to include in the response DTO
     * @param member the member whose name and role are used to populate `username` and `role`
     * @return an AdminPageLoginResDTO populated with `accessToken`, `username` (from member.getName()), and `role` (from member.getRole().name())
     */
    public static AdminPageLoginResDTO of(final String accessToken, Member member) {
        return AdminPageLoginResDTO.builder()
                .accessToken(accessToken)
                .username(member.getName())
                .role(member.getRole().name())
                .build();
    }
}
