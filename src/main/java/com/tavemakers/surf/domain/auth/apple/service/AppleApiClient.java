package com.tavemakers.surf.domain.auth.apple.service;

import com.tavemakers.surf.domain.auth.apple.config.AppleOAuthProps;
import com.tavemakers.surf.domain.auth.apple.dto.AppleTokenResDTO;
import com.tavemakers.surf.domain.auth.apple.exception.AppleAuthErrorMessage;
import com.tavemakers.surf.domain.auth.apple.exception.AppleAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/** Apple /auth/token API 호출 클라이언트 */
@Slf4j
@Service
public class AppleApiClient {

    private final RestTemplate appleRestTemplate;
    private final AppleOAuthProps props;
    private final AppleClientSecretGenerator clientSecretGenerator;

    public AppleApiClient(
            @Qualifier("appleRestTemplate") RestTemplate appleRestTemplate,
            AppleOAuthProps props,
            AppleClientSecretGenerator clientSecretGenerator
    ) {
        this.appleRestTemplate = appleRestTemplate;
        this.props = props;
        this.clientSecretGenerator = clientSecretGenerator;
    }

    /** 인가 코드로 Apple /auth/token 호출 — id_token 포함 응답 반환 */
    public AppleTokenResDTO exchangeCodeForToken(String code) {
        log.info("[APPLE][TOKEN] exchange start");
        try {
            String url = "https://appleid.apple.com/auth/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", props.getServiceClientId());
            params.add("client_secret", clientSecretGenerator.generate());
            params.add("code", code);
            params.add("grant_type", "authorization_code");
            params.add("redirect_uri", props.getRedirectUri());

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<AppleTokenResDTO> response =
                    appleRestTemplate.postForEntity(url, request, AppleTokenResDTO.class);

            log.info("[APPLE][TOKEN] exchange success status={}", response.getStatusCode());

            return Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> new AppleAuthException(
                            AppleAuthErrorMessage.APPLE_TOKEN_EXCHANGE_FAILED.getStatus(),
                            AppleAuthErrorMessage.APPLE_TOKEN_EXCHANGE_FAILED.getMessage()
                    ));
        } catch (AppleAuthException e) {
            throw e;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[APPLE][TOKEN] apple error status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AppleAuthException(
                    AppleAuthErrorMessage.APPLE_TOKEN_EXCHANGE_FAILED.getStatus(),
                    AppleAuthErrorMessage.APPLE_TOKEN_EXCHANGE_FAILED.getMessage()
            );
        } catch (Exception e) {
            log.error("[APPLE][TOKEN] unexpected error", e);
            throw new AppleAuthException(
                    AppleAuthErrorMessage.APPLE_TOKEN_EXCHANGE_FAILED.getStatus(),
                    AppleAuthErrorMessage.APPLE_TOKEN_EXCHANGE_FAILED.getMessage()
            );
        }
    }

    /** Apple 계정 연결 해제 — 탈퇴 시 refresh_token 폐기. 실패해도 탈퇴 트랜잭션은 롤백하지 않는다. */
    public void revokeToken(String refreshToken) {
        log.info("[APPLE][REVOKE] start");
        try {
            String url = "https://appleid.apple.com/auth/revoke";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", props.getServiceClientId());
            params.add("client_secret", clientSecretGenerator.generate());
            params.add("token", refreshToken);
            params.add("token_type_hint", "refresh_token");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            appleRestTemplate.postForEntity(url, request, String.class);

            log.info("[APPLE][REVOKE] success");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[APPLE][REVOKE] 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("[APPLE][REVOKE] 예기치 못한 오류", e);
        }
    }
}
