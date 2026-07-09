package com.tavemakers.surf.presentation.post.dto.response;

import com.tavemakers.surf.presentation.member.dto.response.MemberLikeListResDTO;
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
