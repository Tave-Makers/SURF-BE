package com.tavemakers.surf.domain.auth.kakao.service;

import com.tavemakers.surf.domain.auth.common.dto.OAuthUserInfo;
import com.tavemakers.surf.domain.auth.common.service.OAuthApiClient;
import com.tavemakers.surf.domain.auth.kakao.config.KakaoOAuthProps;
import com.tavemakers.surf.domain.auth.kakao.dto.KakaoTokenResDTO;
import com.tavemakers.surf.domain.auth.kakao.dto.KakaoUserInfoDTO;
import com.tavemakers.surf.domain.auth.kakao.exception.KakaoAuthErrorMessage;
import com.tavemakers.surf.domain.auth.kakao.exception.KakaoAuthException;
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
import java.util.Optional;

@Slf4j
@Service
public class KakaoApiClient implements OAuthApiClient {

    private final RestTemplate restTemplate;
    private final KakaoOAuthProps props;

    /**
     * Creates a KakaoApiClient with the HTTP client and Kakao OAuth configuration it will use for API calls.
     *
     * @param restTemplate RestTemplate configured for Kakao API requests (bean qualified as "kakaoRestTemplate")
     * @param props        Kakao OAuth configuration (client id, redirect URI, optional client secret)
     */
    public KakaoApiClient(@Qualifier("kakaoRestTemplate") RestTemplate restTemplate, KakaoOAuthProps props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }

    /**
     * Exchange an authorization code for a Kakao access token.
     *
     * @param code the authorization code received from Kakao after user consent
     * @return a KakaoTokenResDTO containing the access token, refresh token, and related token metadata
     * @throws KakaoAuthException if the token exchange fails (e.g., Kakao returns an error or an unexpected error occurs)
     */
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

            KakaoTokenResDTO body = Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> {
                        log.error("[KAKAO][TOKEN] response body is null");
                        return new KakaoAuthException(
                                KakaoAuthErrorMessage.KAKAO_TOKEN_EXCHANGE_FAILED.getStatus(),
                                KakaoAuthErrorMessage.KAKAO_TOKEN_EXCHANGE_FAILED.getMessage()
                        );
                    });

            log.info("[KAKAO][TOKEN] exchange success");
            return body;

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[KAKAO][TOKEN] kakao error status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw handleError(e, KakaoAuthErrorMessage.KAKAO_TOKEN_EXCHANGE_FAILED);
        } catch (KakaoAuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("[KAKAO][TOKEN] unexpected error", e);
            throw handleError(e, KakaoAuthErrorMessage.KAKAO_TOKEN_EXCHANGE_FAILED);
        }
    }

    /**
     * Retrieves user information from Kakao using the provided access token and maps it to a common OAuthUserInfo.
     *
     * @param accessToken the Kakao access token used for authentication
     * @return an OAuthUserInfo containing the user's id (as a string), email, nickname, and profile image URL
     */
    @Override
    public OAuthUserInfo fetchUserInfo(String accessToken) {
        KakaoUserInfoDTO raw = callKakaoUserInfo(accessToken);
        return new OAuthUserInfo(
                String.valueOf(raw.id()),
                raw.kakaoAccount().email(),
                raw.kakaoAccount().profile().nickname(),
                raw.kakaoAccount().profile().profileImageUrl()
        );
    }

    /**
     * Validate a Kakao access token and retrieve its token information.
     *
     * @param accessToken the Kakao access token to validate
     * @return a Map<String, Object> containing token metadata returned by Kakao (for example `id`, `expires_in`, `client_id`)
     */
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
            throw handleError(e, KakaoAuthErrorMessage.KAKAO_TOKEN_VALIDATE_FAILED);
        } catch (Exception e) {
            log.error("[KAKAO][TOKEN-INFO] unexpected error", e);
            throw handleError(e, KakaoAuthErrorMessage.KAKAO_TOKEN_VALIDATE_FAILED);
        }
    }

    /**
     * Retrieve the Kakao user profile associated with the provided access token.
     *
     * @param accessToken the Bearer access token to authenticate the request
     * @return a {@link KakaoUserInfoDTO} containing the user's Kakao profile data, or `null` if the response has no body
     * @throws KakaoAuthException if the Kakao API call fails or an unexpected error occurs while fetching user info
     */
    private KakaoUserInfoDTO callKakaoUserInfo(String accessToken) {
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
            throw handleError(e, KakaoAuthErrorMessage.KAKAO_USER_INFO_FAILED);
        } catch (Exception e) {
            log.error("[KAKAO][USER] unexpected error", e);
            throw handleError(e, KakaoAuthErrorMessage.KAKAO_USER_INFO_FAILED);
        }
    }

    /**
     * Logs the original exception and constructs a KakaoAuthException based on the provided Kakao error message.
     *
     * @param ex the original exception encountered during a Kakao API call
     * @param errorMessage the Kakao-specific error descriptor used to populate the returned exception
     * @return a KakaoAuthException with the status and message from {@code errorMessage}
     */
    private KakaoAuthException handleError(Exception ex, KakaoAuthErrorMessage errorMessage) {
        log.error("[KAKAO][API] {}: {}", errorMessage.getMessage(), ex.getMessage(), ex);
        return new KakaoAuthException(errorMessage.getStatus(), errorMessage.getMessage());
    }
}
