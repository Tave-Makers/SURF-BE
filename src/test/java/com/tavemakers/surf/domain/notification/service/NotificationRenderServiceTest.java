package com.tavemakers.surf.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tavemakers.surf.domain.notification.entity.Notification;
import com.tavemakers.surf.domain.notification.entity.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * NotificationRenderService 단위 테스트.
 * ObjectMapper는 순수 라이브러리 협력자이므로 실 객체를 사용하고 렌더링/파싱 로직만 검증한다.
 */
class NotificationRenderServiceTest {

    private final NotificationRenderService renderService = new NotificationRenderService(new ObjectMapper());

    @Test
    @DisplayName("renderBody는 payload 값으로 본문 템플릿의 플레이스홀더를 치환한다")
    void renderBody_치환할_변수를_페이로드로_대체한다() {
        Notification notification = Notification.of(
                1L,
                NotificationType.POST_LIKE,
                "{\"actorId\":10,\"actorName\":\"홀트\",\"boardId\":1,\"postId\":2}"
        );

        String body = renderService.renderBody(notification);

        assertThat(body).isEqualTo("홀트님이 회원님의 게시글에 좋아요를 남겼습니다.");
    }

    @Test
    @DisplayName("renderDeeplink는 payload 값으로 딥링크 템플릿의 플레이스홀더를 치환한다")
    void renderDeeplink_치환할_변수를_페이로드로_대체한다() {
        Notification notification = Notification.of(
                1L,
                NotificationType.POST_COMMENT,
                "{\"actorId\":10,\"actorName\":\"홀트\",\"boardId\":3,\"postId\":7}"
        );

        String deeplink = renderService.renderDeeplink(notification);

        assertThat(deeplink).isEqualTo("board/3/post/7");
    }

    @Test
    @DisplayName("extractActorId는 payload에 actorId가 있으면 Long으로 변환해 반환한다")
    void extractActorId_페이로드에_actorId가_있으면_반환한다() {
        Notification notification = Notification.of(
                1L,
                NotificationType.COMMENT_LIKE,
                "{\"actorId\":42,\"boardId\":1,\"postId\":2}"
        );

        Long actorId = renderService.extractActorId(notification);

        assertThat(actorId).isEqualTo(42L);
    }

    @Test
    @DisplayName("extractActorId는 payload에 actorId가 없으면(시스템 알림) null을 반환한다")
    void extractActorId_페이로드에_actorId가_없으면_null을_반환한다() {
        Notification notification = Notification.of(
                1L,
                NotificationType.BADGE_UPDATE,
                "{}"
        );

        Long actorId = renderService.extractActorId(notification);

        assertThat(actorId).isNull();
    }

    @Test
    @DisplayName("payload가 손상된 JSON이면 IllegalStateException으로 변환해 던진다")
    void renderBody_페이로드가_잘못된_JSON이면_예외를_던진다() {
        Notification notification = Notification.of(
                1L,
                NotificationType.SCORE_UPDATE,
                "not-a-json"
        );

        assertThatThrownBy(() -> renderService.renderBody(notification))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid notification payload");
    }
}
