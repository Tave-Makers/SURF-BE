package com.tavemakers.surf.domain.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    POST_LIKE(
            NotificationCategory.ACTIVITY,
            "{actorName}님이 회원님의 게시글에 좋아요를 남겼습니다.",
            "board/{boardId}/post/{postId}"
    ),

    POST_COMMENT(
            NotificationCategory.ACTIVITY,
            "{actorName}님이 회원님의 게시글에 댓글을 남겼습니다.",
            "board/{boardId}/post/{postId}"
    ),

    COMMENT_LIKE(
            NotificationCategory.ACTIVITY,
            "{actorName}님이 회원님의 댓글에 좋아요를 달았습니다",
            "board/{boardId}/post/{postId}"
    ),

    COMMENT_REPLY(
            NotificationCategory.ACTIVITY,
            "{actorName}님이 회원님의 댓글에 답글을 달았습니다",
            "board/{boardId}/post/{postId}"
    ),

    MESSAGE(
            NotificationCategory.ACTIVITY,
            "{actorName}님이 회원님에게 쪽지를 보냈습니다. 등록하신 이메일을 확인해주세요.",
            ""
    ),

    BADGE_UPDATE(
            NotificationCategory.ACTIVITY,
            "활동 뱃지가 업데이트 되었습니다",
            ""
    ),

    SCORE_UPDATE(
            NotificationCategory.ACTIVITY,
            "활동 점수가 업데이트 되었습니다",
            ""
    ),

    NOTICE(
            NotificationCategory.SCHEDULE,
            "[{boardName}] 새로운 공지사항이 업데이트 되었습니다",
            "board/{boardId}/post/{postId}"
    );

    private final NotificationCategory category;
    private final String template;
    private final String deeplink;
}