package com.tavemakers.surf.application.notification.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.notification.entity.Notification;
import com.tavemakers.surf.domain.notification.entity.NotificationCategory;
import com.tavemakers.surf.domain.notification.entity.NotificationType;
import com.tavemakers.surf.domain.notification.repository.NotificationRepository;
import com.tavemakers.surf.domain.notification.service.NotificationRenderService;
import com.tavemakers.surf.presentation.notification.dto.response.NotificationResDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * 알림 목록 조회 페이지네이션 회귀 테스트.
 * 무제한 전체 조회(List)에서 Slice 기반 무한스크롤로 전환한 동작을 검증한다.
 */
@DataJpaTest
@Import({NotificationGetService.class, NotificationRenderService.class,
        NotificationGetServicePagingTest.ObjectMapperConfig.class})
class NotificationGetServicePagingTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;

    @TestConfiguration
    static class ObjectMapperConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private NotificationGetService notificationGetService;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private MemberGetService memberGetService;

    @BeforeEach
    void setUp() {
        given(memberGetService.getMembers(any())).willReturn(List.of());

        // 대상 회원: ACTIVITY 25건 + SCHEDULE(NOTICE) 3건
        for (int i = 0; i < 25; i++) {
            notificationRepository.save(Notification.of(
                    MEMBER_ID, NotificationType.POST_LIKE,
                    "{\"actorId\": 9, \"actorName\": \"서퍼\", \"boardId\": 1, \"postId\": " + i + "}"));
        }
        for (int i = 0; i < 3; i++) {
            notificationRepository.save(Notification.of(
                    MEMBER_ID, NotificationType.NOTICE,
                    "{\"boardName\": \"공지\", \"boardId\": 2, \"postId\": " + i + "}"));
        }
        // 타 회원 알림은 조회에 섞이면 안 된다
        notificationRepository.save(Notification.of(
                OTHER_MEMBER_ID, NotificationType.POST_LIKE,
                "{\"actorId\": 9, \"actorName\": \"서퍼\", \"boardId\": 1, \"postId\": 99}"));
    }

    @Test
    @DisplayName("전체 조회: page 0/size 20 → 20건 + hasNext, page 1 → 8건 + hasNext 없음")
    void 전체_알림이_페이지_단위로_조회된다() {
        Slice<NotificationResDTO> first =
                notificationGetService.getNotifications(MEMBER_ID, null, PageRequest.of(0, 20));

        assertThat(first.getContent()).hasSize(20);
        assertThat(first.hasNext()).isTrue();

        Slice<NotificationResDTO> second =
                notificationGetService.getNotifications(MEMBER_ID, null, PageRequest.of(1, 20));

        assertThat(second.getContent()).hasSize(8);
        assertThat(second.hasNext()).isFalse();
    }

    @Test
    @DisplayName("최신(id 내림차순) 순서가 페이지에 걸쳐 유지된다")
    void 최신순_정렬이_유지된다() {
        Slice<NotificationResDTO> first =
                notificationGetService.getNotifications(MEMBER_ID, null, PageRequest.of(0, 20));

        List<Long> ids = first.getContent().stream().map(NotificationResDTO::id).toList();
        assertThat(ids).isSortedAccordingTo((a, b) -> Long.compare(b, a));
    }

    @Test
    @DisplayName("카테고리 필터: SCHEDULE 조회 시 NOTICE 3건만 반환된다")
    void 카테고리_필터가_페이지네이션과_함께_동작한다() {
        Slice<NotificationResDTO> result = notificationGetService.getNotifications(
                MEMBER_ID, NotificationCategory.SCHEDULE, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.getContent())
                .allSatisfy(dto -> assertThat(dto.category()).isEqualTo("SCHEDULE"));
    }
}
