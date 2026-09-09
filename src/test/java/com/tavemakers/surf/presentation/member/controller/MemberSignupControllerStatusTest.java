package com.tavemakers.surf.presentation.member.controller;

import com.tavemakers.surf.application.member.usecase.MemberUsecase;
import com.tavemakers.surf.domain.member.entity.CustomUserDetails;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.domain.member.entity.enums.MemberRole;
import com.tavemakers.surf.domain.member.entity.enums.MemberStatus;
import com.tavemakers.surf.domain.member.exception.InvalidMemberInfoException;
import com.tavemakers.surf.domain.member.exception.MemberAlreadyExistsException;
import com.tavemakers.surf.domain.member.exception.MemberBlacklistedException;
import com.tavemakers.surf.domain.member.exception.MemberSignupRejectedException;
import com.tavemakers.surf.global.common.advice.GlobalExceptionHandler;
import com.tavemakers.surf.global.common.exception.BaseException;
import com.tavemakers.surf.global.logging.LogEventEmitter;
import com.tavemakers.surf.presentation.member.dto.request.MemberSignupReqDTO;
import com.tavemakers.surf.presentation.member.dto.response.MemberSignupResDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 회원가입 실패 사유가 body.code 뿐 아니라 실제 HTTP 상태로도 전달되는지 확인한다.
 * 과거에는 컨트롤러가 예외를 삼켜 200 + body.code=403 으로 응답했다(운영 이슈).
 */
class MemberSignupControllerStatusTest {

    private static final String URL = "/v1/user/members/signup";
    private static final String BODY = """
            {"name":"홍길동","university":"서울대","email":"test@example.com","phoneNumber":"01012345678","tracks":[{"generation":15,"part":"BACKEND"}]}
            """;

    private final MemberUsecase memberUsecase = Mockito.mock(MemberUsecase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MemberSignupController(memberUsecase, Mockito.mock(LogEventEmitter.class)))
                .setControllerAdvice(new GlobalExceptionHandler(Mockito.mock(LogEventEmitter.class)))
                .build();

        Member requester = Member.builder().status(MemberStatus.REGISTERING).role(MemberRole.MEMBER).build();
        ReflectionTestUtils.setField(requester, "id", 1L);
        CustomUserDetails principal = new CustomUserDetails(requester);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    static Stream<Arguments> failures() {
        return Stream.of(
                Arguments.of(new MemberBlacklistedException(), 403),
                Arguments.of(new MemberSignupRejectedException(), 403),
                Arguments.of(new MemberAlreadyExistsException(), 409),
                Arguments.of(new InvalidMemberInfoException("소셜 계정이 연결되지 않은 회원은 온보딩할 수 없습니다."), 400)
        );
    }

    @ParameterizedTest(name = "{0} → HTTP {1}")
    @MethodSource("failures")
    @DisplayName("실패 예외는 전역 핸들러를 타서 실제 HTTP 상태와 body.code 가 일치한다")
    void failure_propagatesRealHttpStatus(BaseException ex, int expectedStatus) throws Exception {
        given(memberUsecase.signup(eq(1L), any(MemberSignupReqDTO.class))).willThrow(ex);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(expectedStatus))
                .andExpect(jsonPath("$.message").value(ex.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("성공 시 실제 HTTP 201 과 body.code=201 이 일치한다")
    void success_returns201() throws Exception {
        MemberSignupResDTO res = new MemberSignupResDTO(
                1L, null, "홍길동", List.of(), "서울대", null, "test@example.com", "01012345678");
        given(memberUsecase.signup(eq(1L), any(MemberSignupReqDTO.class))).willReturn(res);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.memberId").value(1));
    }
}
