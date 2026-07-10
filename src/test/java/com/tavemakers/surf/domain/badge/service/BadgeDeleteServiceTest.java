package com.tavemakers.surf.domain.badge.service;

import com.tavemakers.surf.domain.badge.entity.Badge;
import com.tavemakers.surf.domain.badge.exception.BadgeNotFoundException;
import com.tavemakers.surf.domain.badge.repository.BadgeRepository;
import com.tavemakers.surf.domain.badge.repository.MemberBadgeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

/**
 * BadgeDeleteService 단위 테스트 — 배지 부여 기록을 먼저 삭제한 뒤 배지를 삭제하는 순서와
 * 존재하지 않는 배지에 대한 예외 경로를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class BadgeDeleteServiceTest {

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private MemberBadgeRepository memberBadgeRepository;

    @InjectMocks
    private BadgeDeleteService badgeDeleteService;

    @Test
    @DisplayName("배지 삭제 시 배지 부여 기록을 먼저 삭제한 뒤 배지를 삭제한다")
    void 배지_삭제시_부여기록을_먼저_삭제한_뒤_배지를_삭제한다() {
        Badge badge = new Badge("이름", "url", "설명", "요건");
        given(badgeRepository.findById(1L)).willReturn(Optional.of(badge));

        badgeDeleteService.deleteBadge(1L);

        InOrder inOrder = inOrder(memberBadgeRepository, badgeRepository);
        inOrder.verify(memberBadgeRepository).deleteByBadgeId(1L);
        inOrder.verify(badgeRepository).delete(badge);
    }

    @Test
    @DisplayName("존재하지 않는 배지를 삭제하려 하면 예외가 발생하고 부여 기록 삭제는 호출되지 않는다")
    void 존재하지_않는_배지_삭제시_예외이고_부여기록삭제는_호출되지_않는다() {
        given(badgeRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> badgeDeleteService.deleteBadge(99L))
                .isInstanceOf(BadgeNotFoundException.class);

        then(memberBadgeRepository).shouldHaveNoInteractions();
    }
}
