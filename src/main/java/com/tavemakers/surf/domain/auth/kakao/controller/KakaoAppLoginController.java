package com.tavemakers.surf.domain.auth.kakao.controller;

import com.tavemakers.surf.domain.auth.common.dto.ClientType;
import com.tavemakers.surf.domain.auth.common.dto.LoginPayloadResDTO;
import com.tavemakers.surf.domain.auth.kakao.dto.KakaoAppLoginReqDTO;
import com.tavemakers.surf.domain.auth.kakao.usecase.KakaoLoginUsecase;
import com.tavemakers.surf.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "인증/인가", description = "로그인/토큰 발급 관련 API")
public class KakaoAppLoginController {

    private final KakaoLoginUsecase kakaoLoginUsecase;

    /**
     * 카카오 SDK 앱 로그인.
     * <p>카카오 SDK가 발급한 AccessToken을 검증하여 SURF JWT를 발급한다.
     * APP 클라이언트는 응답 본문에 {@code loginRes}(accessToken 포함) + {@code refreshToken}을 수신한다.
     */
    @Operation(
            summary = "카카오 앱 SDK 로그인",
            description = "카카오 SDK AccessToken을 서버에서 /v2/user/me로 검증 후 SURF JWT 발급. " +
                          "응답 본문에 refreshToken 포함 (APP 흐름)."
    )
    @PostMapping("/login/kakao/app")
    public ApiResponse<LoginPayloadResDTO> kakaoAppLogin(
            @RequestBody @Valid KakaoAppLoginReqDTO req,
            ClientType clientType
    ) {
        LoginPayloadResDTO payload = kakaoLoginUsecase.executeAppLogin(req, clientType);
        return ApiResponse.response(HttpStatus.OK, "로그인 성공", payload);
    }
}
