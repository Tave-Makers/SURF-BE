package com.tavemakers.surf.domain.post.dto;

public record PostViewUpdateDto(
        Long postId,
        int viewCountDelta
) {
}
