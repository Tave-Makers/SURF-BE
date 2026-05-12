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
        return doExchange("[APPLE][TOKEN]", code,
                props.getServiceClientId(), clientSecretGenerator.generate(), props.getRedirectUri());
    }

    /** App authorizationCode 교환 — client_id=appBundleId, redirect_uri 없음 */
    public AppleTokenResDTO exchangeAppCodeForToken(String code) {
        return doExchange("[APPLE][TOKEN][APP]", code,
                props.getAppBundleId(), clientSecretGenerator.generateForApp(), null);
    }

    /** Web 계정 연결 해제 — client_id=serviceClientId */
    public void revokeToken(String refreshToken) {
        doRevoke("[APPLE][REVOKE]", refreshToken,
                props.getServiceClientId(), clientSecretGenerator.generate());
    }

    /** App 계정 연결 해제 — client_id=appBundleId */
    public void revokeAppToken(String refreshToken) {
        doRevoke("[APPLE][REVOKE][APP]", refreshToken,
                props.getAppBundleId(), clientSecretGenerator.generateForApp());
    }

    private AppleTokenResDTO doExchange(String logPrefix, String code, String clientId, String clientSecret, String redirectUri) {
        log.info("{} exchange start", logPrefix);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("code", code);
            params.add("grant_type", "authorization_code");
            if (redirectUri != null) {
                params.add("redirect_uri", redirectUri);
            }

            ResponseEntity<AppleTokenResDTO> response = appleRestTemplate.postForEntity(
                    "https://appleid.apple.com/auth/token",
                    new HttpEntity<>(params, headers),
                    AppleTokenResDTO.class
            );

            log.info("{} exchange success status={}", logPrefix, response.getStatusCode());
            return Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> new AppleAuthException(
                            AppleAuthErrorMessage.APPLE_TOKEN_EXCHANGE_FAILED.getStatus(),
                            AppleAuthErrorMessage.APPLE_TOKEN_EXCHANGE_FAILED.getMessage()
                    ));
        } catch (AppleAuthException e) {
            throw e;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("{} apple error status={} body={}", logPrefix, e.getStatusCode(), e.getResponseBodyAsString());
            throw new AppleAuthException(
                    AppleAuthErrorMessage.APPLE_TOKEN_EXCHANGE_FAILED.getStatus(),
                    AppleAuthErrorMessage.APPLE_TOKEN_EXCHANGE_FAILED.getMessage()
            );
        } catch (Exception e) {
            log.error("{} unexpected error", logPrefix, e);
            throw new AppleAuthException(
                    AppleAuthErrorMessage.APPLE_TOKEN_EXCHANGE_FAILED.getStatus(),
                    AppleAuthErrorMessage.APPLE_TOKEN_EXCHANGE_FAILED.getMessage()
            );
        }
    }

    private void doRevoke(String logPrefix, String refreshToken, String clientId, String clientSecret) {
        log.info("{} start", logPrefix);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("token", refreshToken);
            params.add("token_type_hint", "refresh_token");

            appleRestTemplate.postForEntity(
                    "https://appleid.apple.com/auth/revoke",
                    new HttpEntity<>(params, headers),
                    String.class
            );

            log.info("{} success", logPrefix);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("{} 실패: status={}, body={}", logPrefix, e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("{} 예기치 못한 오류", logPrefix, e);
        }
    }
}
