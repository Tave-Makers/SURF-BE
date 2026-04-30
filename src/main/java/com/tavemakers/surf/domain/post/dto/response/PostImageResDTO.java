package com.tavemakers.surf.domain.post.dto.response;

import com.tavemakers.surf.domain.post.entity.PostImageUrl;
import lombok.Builder;

@Builder
public record PostImageResDTO(
        Long imageId,
        String originalUrl,
        Integer sequence
) {
    public static PostImageResDTO from(PostImageUrl postImageUrl) {
        return PostImageResDTO.builder()
                .imageId(postImageUrl.getId())
                .originalUrl(postImageUrl.getOriginalUrl())
                .sequence(postImageUrl.getSequence())
                .build();
    }
}
