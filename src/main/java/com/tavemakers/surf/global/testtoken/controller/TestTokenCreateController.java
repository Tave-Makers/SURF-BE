package com.tavemakers.surf.global.testtoken.controller;

import com.tavemakers.surf.application.member.query.MemberGetService;
import com.tavemakers.surf.domain.member.entity.Member;
import com.tavemakers.surf.global.common.response.ApiResponse;
import com.tavemakers.surf.global.jwt.JwtService;
import com.tavemakers.surf.global.testtoken.dto.TestTokenCreateReqDTO;
import com.tavemakers.surf.global.testtoken.dto.TestTokenCreateResDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로컬 수동 테스트 전용 액세스 토큰 발급 API — <b>운영 환경 활성화 금지</b>.
 *
 * <p>소셜 로그인(Kakao/Apple)만 있는 구조라 Postman 수동 테스트 시 JWT_SECRET 으로 직접 서명해야 하는
 * 불편을 없애기 위한 개발 편의 기능이다. 인증 없이 임의 회원의 액세스 토큰을 발급하므로,
 * 활성화되면 사실상 인증 우회 백도어가 된다.
 *
 * <p>따라서 {@code test-token.enabled=true} 를 명시적으로 준 경우에만 빈이 생성된다
 * (기본값 false — 미설정 시 빈 자체가 없어 엔드포인트가 존재하지 않는다).
 * SecurityConfig 의 경로 허용도 동일 프로퍼티로 조건화되어 있다.
 *
 * <p>발급 대상의 역할은 DB 의 {@code member.role} 을 그대로 사용한다(임의 역할 주입 불가).
 * 리프레시 토큰·RTR 은 다루지 않는다.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "test-token.enabled", havingValue = "true", matchIfMissing = false)
@Tag(name = "테스트 전용", description = "로컬 수동 테스트 전용 API (운영 비활성)")
public class TestTokenCreateController {

    private final MemberGetService memberGetService;
    private final JwtService jwtService;

    /** 실존 회원의 DB 역할 그대로 액세스 토큰을 발급한다 (테스트 전용) */
    @Operation(
            summary = "[테스트 전용] 액세스 토큰 발급",
            description = "test-token.enabled=true 일 때만 노출되는 로컬 테스트 전용 API. "
                    + "실존 memberId 의 DB 역할로 액세스 토큰을 발급한다. 운영 활성화 금지."
    )
    @PostMapping("/test/tokens")
    public ApiResponse<TestTokenCreateResDTO> createTestToken(@Valid @RequestBody TestTokenCreateReqDTO dto) {
        Member member = memberGetService.getMember(dto.memberId());
        String accessToken = jwtService.createAccessToken(member.getId(), member.getRole().name());

        log.warn("[테스트 전용] 액세스 토큰 발급 — memberId={}, role={}", member.getId(), member.getRole());
        return ApiResponse.response(HttpStatus.OK, "테스트 액세스 토큰 발급 성공", TestTokenCreateResDTO.from(accessToken));
    }

}
