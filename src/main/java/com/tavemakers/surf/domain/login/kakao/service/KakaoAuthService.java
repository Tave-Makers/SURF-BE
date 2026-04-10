package com.tavemakers.surf.domain.login.kakao.service;

import com.tavemakers.surf.domain.login.kakao.config.KakaoOAuthProps;
import com.tavemakers.surf.domain.login.kakao.dto.KakaoTokenResDTO;
import com.tavemakers.surf.domain.login.kakao.dto.KakaoUserInfoDTO;
import com.tavemakers.surf.global.logging.LogEvent;
import com.tavemakers.surf.global.logging.LogParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private final RestTemplate restTemplate;
    private final KakaoOAuthProps props;

    /** 인가 URL 생성 (요청 환경 기반) */
    public String buildAuthorizeUrl(String redirectUri) {

        logAuthorize("kakao", redirectUri);

        log.info("[KAKAO][AUTHORIZE] start redirectUri={}", redirectUri);

        return UriComponentsBuilder
                .fromHttpUrl("https://kauth.kakao.com/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", props.getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "account_email profile_nickname profile_image")
                .build()
                .toUriString();
    }

    /** 인가 코드로 토큰 교환 */
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
                        return new ResponseStatusException(
                                HttpStatus.BAD_GATEWAY,
                                "Empty token response from Kakao"
                        );
                    });

            log.info("[KAKAO][TOKEN] exchange success");
            return body;

        } catch (HttpClientErrorException | HttpServerErrorException e) {

            log.error(
                    "[KAKAO][TOKEN] kakao error status={} body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );
            throw handleError(e, HttpStatus.BAD_REQUEST, "카카오 인증 요청 오류");
        } catch (Exception e) {
            log.error("[KAKAO][TOKEN] unexpected error", e);
            throw handleError(e, HttpStatus.INTERNAL_SERVER_ERROR, "카카오 인증 요청 실패");
        }
    }

    /** AccessToken으로 사용자 정보 요청 */
    public KakaoUserInfoDTO getUserInfo(String accessToken) {
        log.info("[KAKAO][USER] get user info start");
        try {
            String url = "https://kapi.kakao.com/v2/user/me";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<KakaoUserInfoDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    KakaoUserInfoDTO.class
            );

            log.info("[KAKAO][USER] response status={}", response.getStatusCode());

            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error(
                    "[KAKAO][USER] kakao error status={} body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );
            throw handleError(e, HttpStatus.BAD_REQUEST, "카카오 사용자 정보 요청 오류");
        } catch (Exception e) {
            log.error("[KAKAO][USER] unexpected error", e);
            throw handleError(e, HttpStatus.INTERNAL_SERVER_ERROR, "카카오 사용자 정보 요청 실패");
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
                    url,
                    HttpMethod.GET,
                    request,
                    Map.class
            );
            log.info("[KAKAO][TOKEN-INFO] response status={}", response.getStatusCode());

            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {

            log.error(
                    "[KAKAO][TOKEN-INFO] kakao error status={} body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );
            throw handleError(e, HttpStatus.BAD_REQUEST, "AccessToken 검증 실패");
        } catch (Exception e) {
            log.error("[KAKAO][TOKEN-INFO] unexpected error", e);

            throw handleError(e, HttpStatus.INTERNAL_SERVER_ERROR, "AccessToken 검증 중 서버 오류");
        }
    }

    /** 로그인 관련 공통 에러 처리 및 로그 기록 */
    protected ResponseStatusException handleError(Exception ex, HttpStatus status, String message) {
        log.error("{}: {}", message, ex.getMessage(), ex);
        return new ResponseStatusException(status, message, ex);
    }

    /** 카카오 인가 요청 로그 */
    @LogEvent("login.kakao.request")
    public void logAuthorize(
            @LogParam("login_method") String loginMethod,
            @LogParam("redirect_uri") String redirectUri
    ) {}

    /** 카카오 콜백 로그 */
    @LogEvent("login.kakao.callback")
    public void logCallback(
            @LogParam("provider") String provider,
            @LogParam("code_length") int codeLength
    ) {}

    /** 로그인 성공 로그 */
    @LogEvent("login.succeeded")
    public void logLoginSuccess(
            @LogParam("user_id") Long userId,
            @LogParam("issued_token") String issuedToken
    ) {}

    /** 로그인 실패 로그 */
    @LogEvent("login.failed")
    public void logLoginFailed(
            @LogParam("error_code") int errorCode,
            @LogParam("error_msg") String errorMsg
    ) {
        throw new IllegalStateException(errorMsg);
    }
}
