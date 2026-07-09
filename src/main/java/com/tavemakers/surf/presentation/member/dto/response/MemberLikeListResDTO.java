package com.tavemakers.surf.presentation.member.dto.response;

import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import lombok.Builder;

@Builder
public record MemberLikeListResDTO(
        Long id,
        String name,
        String profileImageUrl
){
    public static MemberLikeListResDTO from(Member member) {
        return MemberLikeListResDTO.builder()
                .id(member.getStatus() == MemberStatus.WITHDRAWN ? null : member.getId())
                .name(member.getName())
                .profileImageUrl(member.getProfileImageUrl())
                .build();
    }
}
