package com.tavemakers.surf.domain.post.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 게시글 좋아요 이벤트 - 게시글 작성자에게 알림 발송용
 */
@Getter
@RequiredArgsConstructor
public class PostLikedEvent {
    private final Long receiverId;      // 알림 받을 사람 (게시글 작성자)
    private final String actorName;     // 좋아요 누른 사람 이름
    private final Long actorId;         // 좋아요 누른 사람 ID
    private final Long boardId;
    private final Long postId;
}
