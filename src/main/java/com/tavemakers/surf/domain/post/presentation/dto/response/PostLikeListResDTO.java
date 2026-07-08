package com.tavemakers.surf.domain.post.presentation.dto.response;

import com.tavemakers.surf.domain.member.presentation.dto.response.MemberLikeListResDTO;
import java.util.List;
import lombok.Builder;

@Builder
public record PostLikeListResDTO(
        List<MemberLikeListResDTO> likes
) {
    public static PostLikeListResDTO from(List<MemberLikeListResDTO> likes) {
        return PostLikeListResDTO.builder()
                .likes(likes)
                .build();
    }
}
