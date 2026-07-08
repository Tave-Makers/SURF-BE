package com.tavemakers.surf.domain.post.domain.dto;

public record PostViewUpdateDto(
        Long postId,
        int viewCountDelta
) {
}
