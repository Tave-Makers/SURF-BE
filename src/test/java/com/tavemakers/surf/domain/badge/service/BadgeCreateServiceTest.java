package com.tavemakers.surf.domain.badge.service;

import com.tavemakers.surf.domain.badge.entity.Badge;
import com.tavemakers.surf.domain.badge.repository.BadgeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * BadgeCreateService 단위 테스트 — 원시값을 받아 Badge 엔티티를 생성하고 저장한 뒤
 * 저장된(생성된) id를 반환하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class BadgeCreateServiceTest {

    @Mock
    private BadgeRepository badgeRepository;

    @InjectMocks
    private BadgeCreateService badgeCreateService;

    @Test
    @DisplayName("원시값으로 Badge 엔티티를 생성해 저장하고, 저장 후 부여된 id를 반환한다")
    void 배지_생성시_원시값으로_엔티티를_만들어_저장하고_id를_반환한다() {
        given(badgeRepository.save(any(Badge.class))).willAnswer(invocation -> {
            Badge badge = invocation.getArgument(0);
            ReflectionTestUtils.setField(badge, "id", 10L);
            return badge;
        });

        Long id = badgeCreateService.createBadge("배지명", "url", "설명", "요건");

        assertThat(id).isEqualTo(10L);

        ArgumentCaptor<Badge> captor = ArgumentCaptor.forClass(Badge.class);
        then(badgeRepository).should().save(captor.capture());
        Badge saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("배지명");
        assertThat(saved.getImageUrl()).isEqualTo("url");
        assertThat(saved.getDescription()).isEqualTo("설명");
        assertThat(saved.getRequirement()).isEqualTo("요건");
    }
}
