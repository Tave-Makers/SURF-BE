package com.tavemakers.surf.domain.comment.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 댓글 생성 이벤트 - 게시글 작성자에게 알림 발송용
 */
@Getter
@RequiredArgsConstructor
public class CommentCreatedEvent {
    private final Long receiverId;      // 알림 받을 사람 (게시글 작성자)
    private final String actorName;     // 댓글 작성자 이름
    private final Long actorId;         // 댓글 작성자 ID
    private final Long boardId;
    private final Long postId;
}
