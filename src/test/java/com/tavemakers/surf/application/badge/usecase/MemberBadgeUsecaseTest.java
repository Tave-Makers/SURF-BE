package com.tavemakers.surf.application.badge.usecase;

import com.tavemakers.surf.application.badge.query.BadgeGetService;
import com.tavemakers.surf.application.badge.query.MemberBadgeGetService;
import com.tavemakers.surf.domain.badge.entity.Badge;
import com.tavemakers.surf.domain.badge.service.MemberBadgeAssignService;
import com.tavemakers.surf.domain.badge.service.MemberBadgeRevokeService;
import com.tavemakers.surf.domain.member.entity.CustomUserDetails;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.presentation.badge.dto.request.MemberBadgeReqDTO;
import com.tavemakers.surf.presentation.badge.dto.response.MemberOwnedBadgeResDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * MemberBadgeUsecase 단위 테스트 — 도메인 서비스는 mock 처리하고,
 * 부여/회수 위임 및 부여 이벤트 발행에 필요한 정보 조합(배지명·회원 ID·부여자)을 검증한다.
 *
 * assignBadge는 SecurityUtils.getCurrentMemberId()를 사용하므로, Spring 컨텍스트 없이
 * SecurityContextHolder(순수 spring-security-core 라이브러리 객체)에 인증 정보를 직접 주입한다.
 */
@ExtendWith(MockitoExtension.class)
class MemberBadgeUsecaseTest {

    @Mock
    private MemberBadgeAssignService memberBadgeAssignService;

    @Mock
    private MemberBadgeRevokeService memberBadgeRevokeService;

    @Mock
    private MemberBadgeGetService memberBadgeGetService;

    @Mock
    private BadgeGetService badgeGetService;

    @Mock
    private LogEventEmitter logEventEmitter;

    @InjectMocks
    private MemberBadgeUsecase memberBadgeUsecase;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setCurrentMember(Long memberId) {
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberId);
        CustomUserDetails userDetails = new CustomUserDetails(member);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("배지 부여 시 부여 서비스로 위임한 뒤, 배지명·회원목록·부여자ID를 담은 부여 이벤트를 발행한다")
    void 배지_부여시_부여서비스로_위임한_뒤_부여이벤트를_발행한다() {
        setCurrentMember(99L);

        MemberBadgeReqDTO dto = new MemberBadgeReqDTO();
        ReflectionTestUtils.setField(dto, "memberIds", List.of(1L, 2L));

        Badge badge = new Badge("우수회원", "url", "설명", "요건");
        ReflectionTestUtils.setField(badge, "id", 5L);
        given(badgeGetService.getBadgeDetail(5L)).willReturn(badge);

        memberBadgeUsecase.assignBadge(5L, dto);

        then(memberBadgeAssignService).should().assignBadge(5L, List.of(1L, 2L));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        then(logEventEmitter).should().emit(eq("badge.granted"), captor.capture());
        Map<String, Object> props = captor.getValue();
        assertThat(props.get("badge_id")).isEqualTo(5L);
        assertThat(props.get("badge_name")).isEqualTo("우수회원");
        assertThat(props.get("member_ids")).isEqualTo(List.of(1L, 2L));
        assertThat(props.get("awarded_by")).isEqualTo(99L);
    }

    @Test
    @DisplayName("배지 회수 요청은 id와 DTO의 회원 목록을 그대로 회수 서비스로 위임한다")
    void 배지_회수_요청은_id와_회원목록을_그대로_회수서비스로_위임한다() {
        MemberBadgeReqDTO dto = new MemberBadgeReqDTO();
        ReflectionTestUtils.setField(dto, "memberIds", List.of(3L));

        memberBadgeUsecase.revokeBadge(5L, dto);

        then(memberBadgeRevokeService).should().revokeBadge(5L, List.of(3L));
    }

    @Test
    @DisplayName("특정 회원의 배지 전체 조회는 조회 서비스의 결과를 그대로 반환한다")
    void 특정회원의_배지목록조회는_조회서비스결과를_그대로_반환한다() {
        List<MemberOwnedBadgeResDTO> expected =
                List.of(new MemberOwnedBadgeResDTO(1L, "배지", "url", "설명", LocalDate.now()));
        given(memberBadgeGetService.getAllByMemberId(7L)).willReturn(expected);

        List<MemberOwnedBadgeResDTO> result = memberBadgeUsecase.getAllMemberBadges(7L);

        assertThat(result).isEqualTo(expected);
        then(memberBadgeGetService).should().getAllByMemberId(7L);
    }
}
