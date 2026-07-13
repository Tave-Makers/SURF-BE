package com.tavemakers.surf.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tavemakers.surf.domain.notification.entity.Notification;
import com.tavemakers.surf.domain.notification.entity.NotificationType;
import com.tavemakers.surf.domain.notification.event.NotificationCreatedEvent;
import com.tavemakers.surf.domain.notification.repository.NotificationRepository;
import java.util.List;
import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * NotificationCreateService 단위 테스트.
 * ObjectMapper는 실 객체를 사용하되, 직렬화 실패 경로만 mock으로 강제 유도한다.
 */
@ExtendWith(MockitoExtension.class)
class NotificationCreateServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private NotificationCreateService notificationCreateService;

    @BeforeEach
    void setUp() {
        notificationCreateService = new NotificationCreateService(notificationRepository, objectMapper, eventPublisher);
    }

    @Test
    @DisplayName("create는 payload를 직렬화해 저장하고 저장된 엔티티를 반환한다")
    void create_알림을_저장하고_저장된_엔티티를_반환한다() {
        Notification saved = Notification.of(1L, NotificationType.POST_LIKE, "{}");
        ReflectionTestUtils.setField(saved, "id", 100L);
        given(notificationRepository.save(any(Notification.class))).willReturn(saved);

        Notification result = notificationCreateService.create(1L, NotificationType.POST_LIKE, Map.of("actorId", 10));

        assertThat(result.getId()).isEqualTo(100L);
        then(notificationRepository).should().save(any(Notification.class));
    }

    @Test
    @DisplayName("payload 직렬화에 실패하면 저장을 시도하지 않고 RuntimeException으로 감싼다")
    void create_직렬화에_실패하면_저장없이_RuntimeException을_던진다() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        given(failingMapper.writeValueAsString(any())).willThrow(new JsonProcessingException("boom") {});
        NotificationCreateService service =
                new NotificationCreateService(notificationRepository, failingMapper, eventPublisher);

        assertThatThrownBy(() -> service.create(1L, NotificationType.POST_LIKE, Map.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create notification");

        then(notificationRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("createAndSendBulk는 수신자별로 저장된 알림마다 알림ID/수신자로 이벤트를 발행한다")
    void createAndSendBulk_저장된_알림마다_이벤트를_발행한다() {
        List<Long> receiverIds = List.of(1L, 2L, 3L);
        List<Notification> savedList = receiverIds.stream()
                .map(id -> {
                    Notification n = Notification.of(id, NotificationType.NOTICE, "{}");
                    ReflectionTestUtils.setField(n, "id", id * 10);
                    return n;
                })
                .toList();
        given(notificationRepository.saveAll(anyList())).willReturn(savedList);

        notificationCreateService.createAndSendBulk(receiverIds, NotificationType.NOTICE, Map.of("boardId", 1));

        ArgumentCaptor<NotificationCreatedEvent> captor = ArgumentCaptor.forClass(NotificationCreatedEvent.class);
        then(eventPublisher).should(times(3)).publishEvent(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(NotificationCreatedEvent::notificationId, NotificationCreatedEvent::receiverId)
                .containsExactly(
                        Tuple.tuple(10L, 1L),
                        Tuple.tuple(20L, 2L),
                        Tuple.tuple(30L, 3L)
                );
    }

    @Test
    @DisplayName("createAndSendBulk는 저장 결과가 비어 있으면 이벤트를 발행하지 않는다")
    void createAndSendBulk_저장결과가_없으면_이벤트를_발행하지_않는다() {
        given(notificationRepository.saveAll(anyList())).willReturn(List.of());

        notificationCreateService.createAndSendBulk(List.of(), NotificationType.NOTICE, Map.of());

        then(eventPublisher).should(never()).publishEvent(any());
    }
}
