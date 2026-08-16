package com.tavemakers.surf.application.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tavemakers.surf.application.block.query.BlockGetService;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.application.notification.query.NotificationGetService;
import com.tavemakers.surf.application.notification.usecase.NotificationUsecase;
import com.tavemakers.surf.application.post.query.PostGetService;
import com.tavemakers.surf.domain.block.entity.Block;
import com.tavemakers.surf.domain.block.repository.BlockRepository;
import com.tavemakers.surf.domain.comment.event.CommentCreatedEvent;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.notification.repository.NotificationRepository;
import com.tavemakers.surf.domain.notification.service.NotificationCreateService;
import com.tavemakers.surf.domain.notification.service.NotificationService;
import com.tavemakers.surf.domain.post.event.PostLikedEvent;
import com.tavemakers.surf.global.logging.LogEventEmitterImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * 차단 관계에서 개인 알림이 <b>저장 단계부터</b> 발생하지 않는지 실제 block·notification 테이블로 검증한다.
 *
 * <p>BlockGetService를 mock하지 않는다. mock으로 existsBetween을 stub하면 "양방향 차단"을 mock이
 * 대신 주장하게 되어, 구현이 단방향 조회로 바뀌어도 테스트가 통과한다. 실제 block 행을 정방향·역방향으로
 * 각각 심어 리포지토리 JPQL의 OR 절부터 알림 미저장까지를 한 번에 고정한다.
 *
 * <p>리스너 메서드를 직접 호출한다. @Async·AFTER_COMMIT 배선이 아니라 가드가 검증 대상이다.
 */
@DataJpaTest
@Import({
        NotificationEventListener.class,
        NotificationUsecase.class,
        NotificationCreateService.class,
        BlockGetService.class,
        ObjectMapper.class,
})
class NotificationBlockGuardIntegrationTest {

    private static final Long RECEIVER = 1L;
    private static final Long ACTOR = 2L;
    private static final Long BOARD_ID = 10L;
    private static final Long POST_ID = 20L;

    @Autowired
    private NotificationEventListener listener;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private MemberGetService memberGetService;
    @MockBean
    private NotificationGetService notificationGetService;
    @MockBean
    private NotificationService notificationService;
    @MockBean
    private PostGetService postGetService;
    @MockBean
    private LogEventEmitterImpl logEventEmitter;

    /**
     * 수신자를 항상 활성 회원으로 둔다.
     *
     * <p>createAndSend는 비활성 수신자면 스스로 조기 반환한다. 이 stub이 없으면 mock 기본값(false)
     * 때문에 <b>가드가 없어도</b> 알림이 저장되지 않아, 차단 테스트가 잘못된 이유로 통과한다.
     * 저장을 막는 유일한 원인이 차단 가드가 되도록 고정한다.
     */
    @BeforeEach
    void setUp() {
        given(memberGetService.existsByIdAndStatusNot(RECEIVER, MemberStatus.WITHDRAWN)).willReturn(true);
    }

    @Test
    @DisplayName("내가 차단한 상대의 댓글 알림은 Notification이 저장되지 않는다")
    void 내가_차단한_상대의_알림은_저장되지_않는다() {
        blockRepository.saveAndFlush(Block.of(RECEIVER, ACTOR));

        listener.handleCommentCreated(commentCreatedEvent());

        assertThat(notificationRepository.count())
                .as("FCM만 막으면 알림함·미읽음 뱃지에 이름이 쌓이므로 저장 자체가 없어야 한다")
                .isZero();
    }

    @Test
    @DisplayName("나를 차단한 상대의 게시글 좋아요 알림도 저장되지 않는다 — 실제 레코드는 반대 방향뿐이다")
    void 나를_차단한_상대의_알림도_저장되지_않는다() {
        blockRepository.saveAndFlush(Block.of(ACTOR, RECEIVER));

        listener.handlePostLiked(new PostLikedEvent(RECEIVER, "좋아요누른사람", ACTOR, BOARD_ID, POST_ID));

        assertThat(notificationRepository.count()).isZero();
    }

    @Test
    @DisplayName("차단 관계가 없으면 알림이 정상 저장된다")
    void 차단이_없으면_알림이_저장된다() {
        listener.handleCommentCreated(commentCreatedEvent());

        assertThat(notificationRepository.count())
                .as("가드가 정상 알림까지 막으면 안 된다")
                .isEqualTo(1);
    }

    private CommentCreatedEvent commentCreatedEvent() {
        return new CommentCreatedEvent(RECEIVER, "댓글작성자", ACTOR, BOARD_ID, POST_ID);
    }
}
