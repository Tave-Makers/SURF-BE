package com.tavemakers.surf.application.member.usecase;

import com.tavemakers.surf.application.block.query.BlockGetService;
import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.member.entity.CustomUserDetails;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.entity.enums.MemberType;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.presentation.member.dto.response.MemberSearchDetailResDTO;
import com.tavemakers.surf.presentation.member.dto.response.MemberSearchSliceResDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** 회원 검색의 단방향 차단 표기를 검증한다 */
@ExtendWith(MockitoExtension.class)
class MemberUsecaseBlockFlagTest {

    private static final long REQUESTER_ID = 1L;
    private static final long BLOCKED_ID = 2L;
    private static final long NORMAL_ID = 3L;

    @Mock
    private MemberGetService memberGetService;

    @Mock
    private BlockGetService blockGetService;

    @Mock
    private LogEventEmitter logEventEmitter;

    @InjectMocks
    private MemberUsecase memberUsecase;

    @BeforeEach
    void setUp() {
        authenticateAs(REQUESTER_ID);

        Slice<Member> slice = new SliceImpl<>(
                List.of(member(BLOCKED_ID, "차단회원"), member(NORMAL_ID, "정상회원")),
                PageRequest.of(0, 20), false);

        given(memberGetService.searchMembers(any(), any(), any(), any())).willReturn(slice);
        given(memberGetService.countSearchingMembers(any(), any(), any())).willReturn(2L);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("차단한 회원도 결과에 그대로 포함되고 blockedByMe 로만 구분된다 — 제외가 아니라 표기다")
    void 차단_회원은_제외되지_않고_표기된다() {
        given(blockGetService.getMyBlockedIdsRaw(REQUESTER_ID)).willReturn(Set.of(BLOCKED_ID));

        MemberSearchSliceResDTO result = memberUsecase.searchMembers(0, 20, null, null, null);

        assertThat(result.content())
                .extracting(MemberSearchDetailResDTO::memberId, MemberSearchDetailResDTO::blockedByMe)
                .as("차단 회원이 결과에서 사라지면 사용자가 차단을 해제할 방법이 없어진다")
                .containsExactly(
                        tuple(BLOCKED_ID, true),
                        tuple(NORMAL_ID, false));
    }

    @Test
    @DisplayName("totalCount 는 차단과 무관하게 유지된다 — count 쿼리에 필터를 적용하지 않는다")
    void totalCount는_유지된다() {
        given(blockGetService.getMyBlockedIdsRaw(REQUESTER_ID)).willReturn(Set.of(BLOCKED_ID));

        MemberSearchSliceResDTO result = memberUsecase.searchMembers(0, 20, null, null, null);

        assertThat(result.totalCount())
                .as("차단 1건이 있어도 검색 대상 2명이 그대로 집계되어야 한다")
                .isEqualTo(2L);
        assertThat(result.numberOfElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("상대가 나를 차단한 경우는 blockedByMe 가 false 다 — 단방향")
    void 상대가_나를_차단한_것은_표기하지_않는다() {
        // 내가 차단한 목록이 비어 있다 = 내가 차단한 사람은 없다.
        // 양방향(existsBetween)으로 구현했다면 여기서 true 가 되어 상대의 차단 사실이 노출된다.
        given(blockGetService.getMyBlockedIdsRaw(REQUESTER_ID)).willReturn(Set.of());

        MemberSearchSliceResDTO result = memberUsecase.searchMembers(0, 20, null, null, null);

        assertThat(result.content()).extracting(MemberSearchDetailResDTO::blockedByMe)
                .as("차단은 상대에게 알리지 않는 것이 이 기능의 전제다")
                .containsOnly(false);
    }

    @Test
    @DisplayName("차단이 0건이면 빈 Set 이 그대로 쓰인다 — 여기서는 sentinel 을 쓰지 않는다")
    void 차단이_없으면_모두_false다() {
        given(blockGetService.getMyBlockedIdsRaw(REQUESTER_ID)).willReturn(Set.of());

        MemberSearchSliceResDTO result = memberUsecase.searchMembers(0, 20, null, null, null);

        assertThat(result.content()).extracting(MemberSearchDetailResDTO::blockedByMe).containsOnly(false);
        verify(blockGetService, times(0)).getMyBlockedMemberIds(anyLong());
    }

    @Test
    @DisplayName("차단 목록은 페이지당 1회만 조회한다 — 회원마다 조회하면 N+1 이다")
    void 차단_목록은_한_번만_조회한다() {
        given(blockGetService.getMyBlockedIdsRaw(REQUESTER_ID)).willReturn(Set.of(BLOCKED_ID));

        memberUsecase.searchMembers(0, 20, null, null, null);

        verify(blockGetService, times(1)).getMyBlockedIdsRaw(REQUESTER_ID);
        verify(blockGetService, times(0)).isBlockedByMe(anyLong(), anyLong());
    }

    private void authenticateAs(Long memberId) {
        Member requester = member(memberId, "요청자");
        CustomUserDetails principal = new CustomUserDetails(requester);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Member member(Long id, String name) {
        Member member = Member.builder()
                .name(name)
                .email(id + "@test.com")
                .phoneNumber(String.valueOf(id))
                .status(MemberStatus.APPROVED)
                .role(MemberRole.MEMBER)
                .memberType(MemberType.YB)
                .activityStatus(true)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
