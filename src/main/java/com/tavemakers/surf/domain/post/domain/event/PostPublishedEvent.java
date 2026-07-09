package com.tavemakers.surf.domain.post.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostPublishedEvent {
    private final Long postId;
}
