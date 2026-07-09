package com.tavemakers.surf.presentation.notification.dto.response;

import com.tavemakers.surf.domain.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record NotificationResDTO (
        @Schema(description = "알람 ID", example = "1")
        Long id,

        @Schema(description = "알람 유형", example = "POST_LIKE")
        NotificationType type,

        @Schema(description = "알람 카테고리", example = "ACTIVITY")
        String category,       // ACTIVITY / SCHEDULE

        @Schema(description = "알람 본문 내용", example = "누군가가 회원님의 게시글을 좋아합니다.")
        String body,

        @Schema(description = "딥링크 주소", example = "board/1/post/2")
        String deepLink,

        @Schema(description = "알람 읽음 여부", example = "false")
        boolean read,

        @Schema(description = "알람 생성 일시", example = "2023-10-05T14:48:00")
        LocalDateTime createdAt,

        @Schema(description = "행위자 프로필 이미지 URL", example = "https://s3.amazonaws.com/surf/profiles/123.jpg")
        String actorProfileImageUrl
){
}
