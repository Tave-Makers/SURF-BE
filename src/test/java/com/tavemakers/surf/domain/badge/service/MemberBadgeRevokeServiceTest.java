package com.tavemakers.surf.domain.badge.service;

import com.tavemakers.surf.domain.badge.entity.Badge;
import com.tavemakers.surf.domain.badge.entity.MemberBadge;
import com.tavemakers.surf.domain.badge.exception.BadgeNotFoundException;
import com.tavemakers.surf.domain.badge.exception.MemberBadgeNotFoundException;
import com.tavemakers.surf.domain.badge.repository.BadgeRepository;
import com.tavemakers.surf.domain.badge.repository.MemberBadgeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * MemberBadgeRevokeService 단위 테스트 — 회원 ID 중복 제거, 배지 존재 검증,
 * 조회된 매핑 수와 요청 수 불일치 시 예외 경로를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class MemberBadgeRevokeServiceTest {

    @Mock
    private MemberBadgeRepository memberBadgeRepository;

    @Mock
    private BadgeRepository badgeRepository;

    @InjectMocks
    private MemberBadgeRevokeService memberBadgeRevokeService;

    private Badge badge;

    @BeforeEach
    void setUp() {
        badge = new Badge("배지", "url", "설명", "요건");
        ReflectionTestUtils.setField(badge, "id", 1L);
    }

    @Test
    @DisplayName("중복된 회원 ID는 제거된 뒤 조회 및 삭제에 사용된다")
    void 중복된_회원ID는_제거한_뒤_조회하고_삭제한다() {
        given(badgeRepository.findById(1L)).willReturn(Optional.of(badge));
        MemberBadge mb1 = mock(MemberBadge.class);
        MemberBadge mb2 = mock(MemberBadge.class);
        given(memberBadgeRepository.findByBadgeIdAndMemberIdIn(1L, List.of(1L, 2L)))
                .willReturn(List.of(mb1, mb2));

        memberBadgeRevokeService.revokeBadge(1L, List.of(1L, 1L, 2L));

        then(memberBadgeRepository).should().findByBadgeIdAndMemberIdIn(1L, List.of(1L, 2L));
        then(memberBadgeRepository).should().deleteAll(List.of(mb1, mb2));
    }

    @Test
    @DisplayName("배지가 존재하지 않으면 BadgeNotFoundException이 발생하고 매핑 조회/삭제는 일어나지 않는다")
    void 배지가_없으면_BadgeNotFoundException() {
        given(badgeRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberBadgeRevokeService.revokeBadge(1L, List.of(1L)))
                .isInstanceOf(BadgeNotFoundException.class);

        then(memberBadgeRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("조회된 매핑 수가 요청 회원 수와 다르면 MemberBadgeNotFoundException이 발생한다")
    void 조회된_매핑수가_요청수와_다르면_MemberBadgeNotFoundException() {
        given(badgeRepository.findById(1L)).willReturn(Optional.of(badge));
        given(memberBadgeRepository.findByBadgeIdAndMemberIdIn(1L, List.of(1L, 2L)))
                .willReturn(List.of(mock(MemberBadge.class))); // 1건만 조회됨 (2건 요청)

        assertThatThrownBy(() -> memberBadgeRevokeService.revokeBadge(1L, List.of(1L, 2L)))
                .isInstanceOf(MemberBadgeNotFoundException.class);

        then(memberBadgeRepository).should(never()).deleteAll(anyList());
    }
}
