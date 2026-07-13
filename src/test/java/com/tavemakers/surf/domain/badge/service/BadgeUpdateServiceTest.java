package com.tavemakers.surf.domain.badge.service;

import com.tavemakers.surf.domain.badge.entity.Badge;
import com.tavemakers.surf.domain.badge.exception.BadgeNotFoundException;
import com.tavemakers.surf.domain.badge.repository.BadgeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * BadgeUpdateService 단위 테스트 — 원시값을 실제 Badge 엔티티(update)로 위임하는 흐름과
 * 조회 실패 시 예외 경로를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class BadgeUpdateServiceTest {

    @Mock
    private BadgeRepository badgeRepository;

    @InjectMocks
    private BadgeUpdateService badgeUpdateService;

    @Test
    @DisplayName("존재하는 배지를 수정하면 공백/null이 아닌 필드만 갱신된다")
    void 존재하는_배지를_수정하면_공백이_아닌_필드만_갱신된다() {
        Badge badge = new Badge("기존이름", "기존url", "기존설명", "기존요건");
        given(badgeRepository.findById(1L)).willReturn(Optional.of(badge));

        badgeUpdateService.updateBadge(1L, "새이름", "   ", null, "새요건");

        assertThat(badge.getName()).isEqualTo("새이름");
        assertThat(badge.getImageUrl()).as("공백 문자열은 기존 값을 유지해야 한다").isEqualTo("기존url");
        assertThat(badge.getDescription()).as("null은 기존 값을 유지해야 한다").isEqualTo("기존설명");
        assertThat(badge.getRequirement()).isEqualTo("새요건");
    }

    @Test
    @DisplayName("존재하지 않는 배지를 수정하려 하면 BadgeNotFoundException이 발생한다")
    void 존재하지_않는_배지를_수정하면_BadgeNotFoundException이_발생한다() {
        given(badgeRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> badgeUpdateService.updateBadge(99L, "a", "b", "c", "d"))
                .isInstanceOf(BadgeNotFoundException.class);
    }
}
