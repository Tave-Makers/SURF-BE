package com.tavemakers.surf.domain.auth.service;

import com.tavemakers.surf.domain.auth.config.KakaoOAuthProps;
import com.tavemakers.surf.domain.auth.dto.response.KakaoTokenResDTO;
import com.tavemakers.surf.domain.auth.dto.response.KakaoUserInfoDTO;
import com.tavemakers.surf.domain.auth.exception.AuthErrorMessage;
import com.tavemakers.surf.domain.auth.exception.KakaoAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class KakaoApiClient {

    private final RestTemplate restTemplate;
    private final KakaoOAuthProps props;

    public KakaoApiClient(@Qualifier("kakaoRestTemplate") RestTemplate restTemplate, KakaoOAuthProps props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }

    /** 인가 코드로 카카오 토큰 교환 */
    public KakaoTokenResDTO exchangeCodeForToken(String code) {
        log.info("[KAKAO][TOKEN] exchange start codeLength={}", code.length());
        try {
            String url = "https://kauth.kakao.com/oauth/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", props.getClientId());
            params.add("redirect_uri", props.getRedirectUri());
            params.add("code", code);

            log.info("[KAKAO][TOKEN] request redirectUri={}", props.getRedirectUri());
            log.debug("[KAKAO][TOKEN] params={}", params);

            if (props.getClientSecret() != null && !props.getClientSecret().isBlank()) {
                params.add("client_secret", props.getClientSecret());
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<KakaoTokenResDTO> response =
                    restTemplate.postForEntity(url, request, KakaoTokenResDTO.class);

            log.info("[KAKAO][TOKEN] response status={}", response.getStatusCode());

            KakaoTokenResDTO body = java.util.Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> {
                        log.error("[KAKAO][TOKEN] response body is null");
                        return new KakaoAuthException(
                                AuthErrorMessage.KAKAO_TOKEN_EXCHANGE_FAILED.getStatus(),
                                AuthErrorMessage.KAKAO_TOKEN_EXCHANGE_FAILED.getMessage()
                        );
                    });

            log.info("[KAKAO][TOKEN] exchange success");
            return body;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[KAKAO][TOKEN] kakao error status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw handleError(e, AuthErrorMessage.KAKAO_TOKEN_EXCHANGE_FAILED);
        } catch (KakaoAuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("[KAKAO][TOKEN] unexpected error", e);
            throw handleError(e, AuthErrorMessage.KAKAO_TOKEN_EXCHANGE_FAILED);
        }
    }

    /** AccessToken으로 카카오 사용자 정보 요청 */
    public KakaoUserInfoDTO getUserInfo(String accessToken) {
        log.info("[KAKAO][USER] get user info start");
        try {
            String url = "https://kapi.kakao.com/v2/user/me";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<KakaoUserInfoDTO> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, KakaoUserInfoDTO.class
            );

            log.info("[KAKAO][USER] response status={}", response.getStatusCode());
            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[KAKAO][USER] kakao error status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw handleError(e, AuthErrorMessage.KAKAO_USER_INFO_FAILED);
        } catch (Exception e) {
            log.error("[KAKAO][USER] unexpected error", e);
            throw handleError(e, AuthErrorMessage.KAKAO_USER_INFO_FAILED);
        }
    }

    /** AccessToken 유효성 검증 */
    public Map<String, Object> getAccessTokenInfo(String accessToken) {
        log.info("[KAKAO][TOKEN-INFO] validate start");
        try {
            String url = "https://kapi.kakao.com/v1/user/access_token_info";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, Map.class
            );

            log.info("[KAKAO][TOKEN-INFO] response status={}", response.getStatusCode());
            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[KAKAO][TOKEN-INFO] kakao error status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw handleError(e, AuthErrorMessage.KAKAO_TOKEN_VALIDATE_FAILED);
        } catch (Exception e) {
            log.error("[KAKAO][TOKEN-INFO] unexpected error", e);
            throw handleError(e, AuthErrorMessage.KAKAO_TOKEN_VALIDATE_FAILED);
        }
    }

    /** 카카오 API 오류 처리 공통 로직 */
    private KakaoAuthException handleError(Exception ex, AuthErrorMessage errorMessage) {
        log.error("[KAKAO][API] {}: {}", errorMessage.getMessage(), ex.getMessage(), ex);
        return new KakaoAuthException(errorMessage.getStatus(), errorMessage.getMessage());
    }
}
