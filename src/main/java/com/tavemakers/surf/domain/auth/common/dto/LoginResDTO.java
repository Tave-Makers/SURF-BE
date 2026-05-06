package com.tavemakers.surf.domain.auth.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 로그인 성공 시 클라이언트로 내려주는 통합 응답 DTO.
 * <ul>
 *   <li>WEB: refreshToken=null → 직렬화 생략, RefreshToken은 Set-Cookie로 전달</li>
 *   <li>APP: refreshToken=set → 응답 본문에 포함</li>
 * </ul>
 */
@Schema(description = "로그인 성공 응답 DTO")
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResDTO(

        @Schema(description = "사용자 닉네임", example = "홍길동")
        String nickname,

        @Schema(description = "사용자 이메일", example = "hong@example.com")
        String email,

        @Schema(description = "JWT Access Token")
        String accessToken,

        @Schema(description = "JWT Refresh Token. APP 클라이언트만 포함, WEB은 null로 생략됨.")
        String refreshToken,

        @Schema(description = "프로필 이미지 URL", example = "https://k.kakaocdn.net/.../img_640x640.jpg")
        String profileImageUrl
) {

    /** WEB: refreshToken 미포함 */
    public static LoginResDTO of(
            String nickname,
            String email,
            String accessToken,
            String profileImageUrl
    ) {
        return LoginResDTO.builder()
                .nickname(nickname)
                .email(email)
                .accessToken(accessToken)
                .profileImageUrl(profileImageUrl)
                .build();
    }

    /** APP: refreshToken 포함 */
    public static LoginResDTO ofApp(
            String nickname,
            String email,
            String accessToken,
            String refreshToken,
            String profileImageUrl
    ) {
        return LoginResDTO.builder()
                .nickname(nickname)
                .email(email)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .profileImageUrl(profileImageUrl)
                .build();
    }
}
