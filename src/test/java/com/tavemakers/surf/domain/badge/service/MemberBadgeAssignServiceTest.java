package com.tavemakers.surf.domain.badge.service;

import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.badge.entity.Badge;
import com.tavemakers.surf.domain.badge.entity.MemberBadge;
import com.tavemakers.surf.domain.badge.exception.BadgeNotFoundException;
import com.tavemakers.surf.domain.badge.exception.MemberBadgeAlreadyExistsException;
import com.tavemakers.surf.domain.badge.repository.BadgeRepository;
import com.tavemakers.surf.domain.badge.repository.MemberBadgeRepository;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.exception.MemberNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * MemberBadgeAssignService 단위 테스트.
 *
 * 핵심 검증 대상: 동시 부여 race — 사전 exists 체크를 통과해도 saveAllAndFlush 에서
 * unique 제약(DataIntegrityViolationException)이 발생하면 이를 도메인 예외
 * (MemberBadgeAlreadyExistsException)로 변환해야 한다.
 */
@ExtendWith(MockitoExtension.class)
class MemberBadgeAssignServiceTest {

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private MemberGetService memberGetService;

    @Mock
    private MemberBadgeRepository memberBadgeRepository;

    @InjectMocks
    private MemberBadgeAssignService memberBadgeAssignService;

    private Badge badge;

    @BeforeEach
    void setUp() {
        badge = new Badge("배지", "url", "설명", "요건");
        ReflectionTestUtils.setField(badge, "id", 1L);
    }

    @Test
    @DisplayName("배지가 존재하지 않으면 BadgeNotFoundException이 발생하고 회원 조회는 일어나지 않는다")
    void 배지가_없으면_BadgeNotFoundException() {
        given(badgeRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberBadgeAssignService.assignBadge(1L, List.of(1L)))
                .isInstanceOf(BadgeNotFoundException.class);

        then(memberGetService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("요청한 회원 수와 조회된 회원 수가 다르면 MemberNotFoundException이 발생한다")
    void 요청한_회원수와_조회된_회원수가_다르면_MemberNotFoundException() {
        given(badgeRepository.findById(1L)).willReturn(Optional.of(badge));
        given(memberGetService.getMembersByIds(List.of(1L, 2L)))
                .willReturn(List.of(mock(Member.class))); // 1명만 조회됨 (2명 요청)

        assertThatThrownBy(() -> memberBadgeAssignService.assignBadge(1L, List.of(1L, 2L)))
                .isInstanceOf(MemberNotFoundException.class);

        then(memberBadgeRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이미 해당 배지를 보유한 회원이 있으면 사전 체크에서 MemberBadgeAlreadyExistsException이 발생한다")
    void 이미_배지를_보유한_회원이_있으면_사전체크에서_예외() {
        Member member = mock(Member.class);
        given(badgeRepository.findById(1L)).willReturn(Optional.of(badge));
        given(memberGetService.getMembersByIds(List.of(1L))).willReturn(List.of(member));
        given(memberBadgeRepository.findByBadgeIdAndMemberIdIn(1L, List.of(1L)))
                .willReturn(List.of(mock(MemberBadge.class)));

        assertThatThrownBy(() -> memberBadgeAssignService.assignBadge(1L, List.of(1L)))
                .isInstanceOf(MemberBadgeAlreadyExistsException.class);

        then(memberBadgeRepository).should(never()).saveAllAndFlush(anyList());
    }

    @Test
    @DisplayName("사전 체크를 통과해도 saveAllAndFlush에서 무결성 제약 위반이 발생하면 도메인 예외로 변환한다 (동시 부여 race)")
    void saveAllAndFlush에서_무결성제약_위반이_발생하면_도메인예외로_변환한다() {
        Member member = mock(Member.class);
        given(badgeRepository.findById(1L)).willReturn(Optional.of(badge));
        given(memberGetService.getMembersByIds(List.of(1L))).willReturn(List.of(member));
        given(memberBadgeRepository.findByBadgeIdAndMemberIdIn(1L, List.of(1L))).willReturn(List.of());
        given(memberBadgeRepository.saveAllAndFlush(anyList()))
                .willThrow(new DataIntegrityViolationException("unique constraint violated"));

        assertThatThrownBy(() -> memberBadgeAssignService.assignBadge(1L, List.of(1L)))
                .as("DataIntegrityViolationException이 그대로 새어 나가면 안 되고 도메인 예외로 변환되어야 한다")
                .isInstanceOf(MemberBadgeAlreadyExistsException.class);
    }

    @Test
    @DisplayName("정상 부여 시 모든 회원에 대해 MemberBadge를 생성해 saveAllAndFlush로 저장한다")
    void 정상_부여시_모든_회원에_대해_MemberBadge를_생성해_저장한다() {
        Member m1 = mock(Member.class);
        Member m2 = mock(Member.class);
        given(badgeRepository.findById(1L)).willReturn(Optional.of(badge));
        given(memberGetService.getMembersByIds(List.of(1L, 2L))).willReturn(List.of(m1, m2));
        given(memberBadgeRepository.findByBadgeIdAndMemberIdIn(1L, List.of(1L, 2L))).willReturn(List.of());

        memberBadgeAssignService.assignBadge(1L, List.of(1L, 2L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MemberBadge>> captor = ArgumentCaptor.forClass(List.class);
        then(memberBadgeRepository).should().saveAllAndFlush(captor.capture());

        List<MemberBadge> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(MemberBadge::getMember).containsExactly(m1, m2);
        assertThat(saved).allSatisfy(mb -> assertThat(mb.getBadge()).isEqualTo(badge));
    }
}
