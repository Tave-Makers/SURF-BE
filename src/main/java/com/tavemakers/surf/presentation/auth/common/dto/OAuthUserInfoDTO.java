package com.tavemakers.surf.presentation.auth.common.dto;

/**
 * OAuth provider 공통 사용자 정보 DTO
 * - Kakao, Apple 등 모든 OAuth provider의 사용자 정보를 통일된 형태로 표현
 */
public record OAuthUserInfoDTO(
        String oauthId,
        String email,
        String nickname,
        String profileImageUrl
) {}
